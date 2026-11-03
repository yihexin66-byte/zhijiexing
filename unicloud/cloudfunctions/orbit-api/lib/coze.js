'use strict'

const http = require('http')
const https = require('https')

const MAX_RESPONSE_BYTES = 2 * 1024 * 1024
const DEFAULT_BASE_URL = 'https://api.coze.cn'
const DEFAULT_TIMEOUT_MS = 25 * 1000
const REQUEST_TIMEOUT_MS = 8 * 1000
const POLL_INTERVAL_MS = 1000

class CozeError extends Error {
  constructor(code, message, options = {}) {
    super(message)
    this.name = 'CozeError'
    this.code = code
    this.status = options.status || 0
    this.cause = options.cause
  }
}

function requestJson(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url)
    const transport = parsed.protocol === 'http:' ? http : https
    const body = options.body === undefined ? null : JSON.stringify(options.body)
    const headers = { ...(options.headers || {}) }
    if (body !== null) {
      headers['content-type'] = 'application/json; charset=utf-8'
      headers['content-length'] = Buffer.byteLength(body)
    }

    const request = transport.request(
      parsed,
      {
        method: options.method || 'GET',
        headers,
      },
      (response) => {
        let raw = ''
        response.setEncoding('utf8')
        response.on('data', (chunk) => {
          raw += chunk
          if (raw.length > MAX_RESPONSE_BYTES) {
            request.destroy(
              new CozeError('COZE_HTTP', 'Coze response is too large', {
                status: response.statusCode || 0,
              }),
            )
          }
        })
        response.on('end', () => {
          let json = null
          try {
            json = JSON.parse(raw)
          } catch (error) {
            json = null
          }
          resolve({
            status: Number(response.statusCode) || 0,
            json,
            text: raw,
          })
        })
      },
    )

    request.setTimeout(options.timeoutMs || REQUEST_TIMEOUT_MS, () => {
      request.destroy(new CozeError('COZE_TIMEOUT', 'Coze request timed out'))
    })
    request.on('error', reject)
    if (body !== null) request.write(body)
    request.end()
  })
}

function cozeApiError(payload, status) {
  const code = payload && payload.code !== undefined ? payload.code : null
  const message = payload && (payload.msg || payload.message)
  return new CozeError(
    status === 429 ? 'COZE_BUSY' : 'COZE_API',
    message ? String(message).slice(0, 240) : 'Coze returned an error',
    { status },
  )
}

/**
 * Minimal Coze v3 chat client. A non-streaming chat is three HTTP calls:
 * create, poll until completed, then read the answer messages. The client
 * intentionally has no SDK dependency so it runs unchanged inside uniCloud.
 */
function createCozeClient({
  token,
  botId,
  baseUrl = DEFAULT_BASE_URL,
  timeoutMs = DEFAULT_TIMEOUT_MS,
  requestTimeoutMs = REQUEST_TIMEOUT_MS,
  pollIntervalMs = POLL_INTERVAL_MS,
  sleep = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds)),
  request = requestJson,
} = {}) {
  const cleanToken = String(token || '').trim()
  const cleanBotId = String(botId || '').trim()
  const apiBase = String(baseUrl || DEFAULT_BASE_URL).replace(/\/+$/, '')

  if (!cleanToken || !cleanBotId) {
    throw new CozeError('COZE_CONFIG', 'COZE_API_TOKEN and COZE_BOT_ID are required')
  }

  function headers() {
    return {
      authorization: 'Bearer ' + cleanToken,
      accept: 'application/json',
    }
  }

  async function call(path, options, timeout) {
    let response
    try {
      response = await request(apiBase + path, {
        ...options,
        headers: headers(),
        timeoutMs: Math.min(requestTimeoutMs, timeout),
      })
    } catch (error) {
      if (error instanceof CozeError) throw error
      throw new CozeError('COZE_HTTP', 'Coze network request failed', { cause: error })
    }

    if (response.status === 401 || response.status === 403) {
      throw new CozeError('COZE_AUTH', 'Coze rejected the access token', {
        status: response.status,
      })
    }
    if (response.status === 429) {
      throw new CozeError('COZE_BUSY', 'Coze is rate limiting requests', {
        status: response.status,
      })
    }
    if (response.status < 200 || response.status >= 300) {
      throw cozeApiError(response.json, response.status)
    }
    if (!response.json || typeof response.json !== 'object') {
      throw new CozeError('COZE_API', 'Coze returned a non-JSON response', {
        status: response.status,
      })
    }
    if (response.json.code !== undefined && Number(response.json.code) !== 0) {
      throw cozeApiError(response.json, response.status)
    }
    return response.json.data === undefined ? response.json : response.json.data
  }

  async function chat(messages, options = {}) {
    const started = Date.now()
    const budget = Math.max(1000, Math.min(Number(options.timeoutMs) || timeoutMs, timeoutMs))
    const remaining = () => budget - (Date.now() - started)
    const userId = String(options.userId || 'orbit').slice(0, 64)

    const created = await call(
      '/v3/chat',
      {
        method: 'POST',
        body: {
          bot_id: cleanBotId,
          user_id: userId,
          stream: false,
          // Coze requires auto_save_history for non-streaming chats; without it
          // retrieve/message-list cannot return the answer.
          auto_save_history: true,
          additional_messages: messages.map((message) => ({
            role: message.role === 'assistant' ? 'assistant' : 'user',
            content: String(message.content || ''),
            content_type: 'text',
          })),
        },
      },
      Math.max(1000, remaining()),
    )

    const chatId = created && created.id
    const conversationId = created && created.conversation_id
    if (!chatId || !conversationId) {
      throw new CozeError('COZE_API', 'Coze did not return a chat id')
    }

    let status = String(created.status || 'in_progress')
    while (status !== 'completed') {
      if (status === 'failed' || status === 'canceled' || status === 'requires_action') {
        throw new CozeError('COZE_FAILED', 'Coze chat ended with status ' + status)
      }
      if (remaining() <= pollIntervalMs + 200) {
        throw new CozeError('COZE_TIMEOUT', 'Coze chat exceeded the time budget')
      }
      await sleep(Math.min(pollIntervalMs, Math.max(0, remaining())))
      const query = new URLSearchParams({
        conversation_id: String(conversationId),
        chat_id: String(chatId),
      })
      const retrieved = await call(
        '/v3/chat/retrieve?' + query.toString(),
        { method: 'GET' },
        Math.max(1000, remaining()),
      )
      status = String((retrieved && retrieved.status) || status)
    }

    const query = new URLSearchParams({
      conversation_id: String(conversationId),
      chat_id: String(chatId),
    })
    const listed = await call(
      '/v3/chat/message/list?' + query.toString(),
      { method: 'GET' },
      Math.max(1000, remaining()),
    )
    const messagesOut = Array.isArray(listed)
      ? listed
      : Array.isArray(listed && listed.data)
        ? listed.data
        : []
    const answer = messagesOut.find(
      (message) =>
        message &&
        String(message.type || '') === 'answer' &&
        String(message.role || '') === 'assistant' &&
        String(message.content || '').trim(),
    ) || messagesOut.find(
      (message) =>
        message &&
        String(message.role || '') === 'assistant' &&
        String(message.content || '').trim(),
    )

    const content = answer && String(answer.content || '').trim()
    if (!content) {
      throw new CozeError('COZE_EMPTY', 'Coze returned no assistant answer')
    }
    return content
  }

  return { chat }
}

module.exports = {
  CozeError,
  createCozeClient,
  DEFAULT_BASE_URL,
  DEFAULT_TIMEOUT_MS,
}
