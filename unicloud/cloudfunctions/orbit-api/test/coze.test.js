'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
const http = require('http')

const { CozeError, createCozeClient } = require('../lib/coze')

function startServer(handler) {
  return new Promise((resolve) => {
    const server = http.createServer(handler)
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      resolve({
        server,
        baseUrl: 'http://127.0.0.1:' + address.port,
      })
    })
  })
}

function stopServer(server) {
  return new Promise((resolve) => server.close(resolve))
}

function sendJson(response, status, payload) {
  response.writeHead(status, { 'content-type': 'application/json' })
  response.end(JSON.stringify(payload))
}

function readBody(request) {
  return new Promise((resolve) => {
    let body = ''
    request.setEncoding('utf8')
    request.on('data', (chunk) => {
      body += chunk
    })
    request.on('end', () => resolve(body))
  })
}

test('Coze client creates a chat, polls and returns the assistant answer', async () => {
  const calls = []
  let retrievals = 0
  const { server, baseUrl } = await startServer(async (request, response) => {
    const url = new URL(request.url, baseUrl)
    const body = await readBody(request)
    calls.push({
      method: request.method,
      path: url.pathname,
      query: url.searchParams,
      body: body ? JSON.parse(body) : null,
      authorization: request.headers.authorization,
    })

    if (request.method === 'POST' && url.pathname === '/v3/chat') {
      sendJson(response, 200, {
        code: 0,
        data: {
          id: 'chat-1',
          conversation_id: 'conv-1',
          status: 'in_progress',
        },
      })
      return
    }
    if (url.pathname === '/v3/chat/retrieve') {
      retrievals += 1
      sendJson(response, 200, {
        code: 0,
        data: {
          id: 'chat-1',
          status: retrievals >= 2 ? 'completed' : 'in_progress',
        },
      })
      return
    }
    if (url.pathname === '/v3/chat/message/list') {
      sendJson(response, 200, {
        code: 0,
        data: [
          { role: 'assistant', type: 'answer', content: '{"ok":true}' },
        ],
      })
      return
    }
    sendJson(response, 404, { code: 404, msg: 'not found' })
  })

  try {
    const client = createCozeClient({
      token: 'pat_test',
      botId: 'bot_1',
      baseUrl,
      sleep: async () => {},
    })
    const answer = await client.chat(
      [{ role: 'user', content: '请规划' }],
      { userId: 'orbit_9', timeoutMs: 5000 },
    )
    assert.equal(answer, '{"ok":true}')
    assert.equal(calls[0].path, '/v3/chat')
    assert.equal(calls[0].authorization, 'Bearer pat_test')
    assert.equal(calls[0].body.bot_id, 'bot_1')
    assert.equal(calls[0].body.user_id, 'orbit_9')
    assert.equal(calls[0].body.stream, false)
    assert.equal(calls[0].body.auto_save_history, true)
    assert.equal(calls[0].body.additional_messages[0].role, 'user')
    assert.equal(calls[0].body.additional_messages[0].content, '请规划')
    assert.ok(calls.some((call) => call.path === '/v3/chat/retrieve'))
    assert.ok(calls.some((call) => call.path === '/v3/chat/message/list'))
  } finally {
    await stopServer(server)
  }
})

test('Coze client maps an expired token to COZE_AUTH', async () => {
  const { server, baseUrl } = await startServer((request, response) => {
    sendJson(response, 401, { code: 401, msg: 'unauthorized' })
  })
  try {
    const client = createCozeClient({
      token: 'pat_expired',
      botId: 'bot_1',
      baseUrl,
      sleep: async () => {},
    })
    await assert.rejects(
      () => client.chat([{ role: 'user', content: 'x' }], { timeoutMs: 2000 }),
      (error) => error instanceof CozeError && error.code === 'COZE_AUTH',
    )
  } finally {
    await stopServer(server)
  }
})

test('Coze client stops polling at the deadline', async () => {
  const { server, baseUrl } = await startServer((request, response) => {
    if (request.method === 'POST' && request.url.startsWith('/v3/chat')) {
      sendJson(response, 200, {
        code: 0,
        data: {
          id: 'chat-1',
          conversation_id: 'conv-1',
          status: 'in_progress',
        },
      })
      return
    }
    sendJson(response, 200, {
      code: 0,
      data: { id: 'chat-1', status: 'in_progress' },
    })
  })
  try {
    const client = createCozeClient({
      token: 'pat_test',
      botId: 'bot_1',
      baseUrl,
      sleep: async () => {},
      requestTimeoutMs: 100,
      pollIntervalMs: 40,
    })
    await assert.rejects(
      () =>
        client.chat([{ role: 'user', content: 'x' }], {
          timeoutMs: 650,
        }),
      (error) => error instanceof CozeError && error.code === 'COZE_TIMEOUT',
    )
  } finally {
    await stopServer(server)
  }
})

test('Coze client exposes failed and empty chats as distinct errors', async () => {
  let mode = 'failed'
  const { server, baseUrl } = await startServer((request, response) => {
    if (request.method === 'POST' && request.url.startsWith('/v3/chat')) {
      sendJson(response, 200, {
        code: 0,
        data: {
          id: 'chat-1',
          conversation_id: 'conv-1',
          status: 'in_progress',
        },
      })
      return
    }
    if (request.url.startsWith('/v3/chat/retrieve')) {
      sendJson(response, 200, {
        code: 0,
        data: { id: 'chat-1', status: mode === 'failed' ? 'failed' : 'completed' },
      })
      return
    }
    sendJson(response, 200, { code: 0, data: [] })
  })
  try {
    const client = createCozeClient({
      token: 'pat_test',
      botId: 'bot_1',
      baseUrl,
      sleep: async () => {},
    })
    await assert.rejects(
      () => client.chat([{ role: 'user', content: 'x' }], { timeoutMs: 2000 }),
      (error) => error instanceof CozeError && error.code === 'COZE_FAILED',
    )

    mode = 'empty'
    await assert.rejects(
      () => client.chat([{ role: 'user', content: 'x' }], { timeoutMs: 2000 }),
      (error) => error instanceof CozeError && error.code === 'COZE_EMPTY',
    )
  } finally {
    await stopServer(server)
  }
})
