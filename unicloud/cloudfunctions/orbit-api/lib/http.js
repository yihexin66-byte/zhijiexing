'use strict'

const { ApiError } = require('./errors')

const CORS_HEADERS = Object.freeze({
  'access-control-allow-origin': '*',
  'access-control-allow-headers': 'authorization, content-type',
  'access-control-allow-methods': 'GET, POST, PUT, PATCH, OPTIONS',
  'access-control-max-age': '600',
})

function headerMap(headers) {
  const map = {}
  for (const [key, value] of Object.entries(headers || {})) {
    if (value == null) continue
    map[String(key).toLowerCase()] = Array.isArray(value) ? value.join(', ') : String(value)
  }
  return map
}

function rawPath(event) {
  return String(
    (event && (event.path || event.rawPath || event.requestPath)) || '/',
  )
}

/**
 * URL化 requests arrive with the console-configured prefix (default /orbit).
 * Both prefixed and bare /api paths are accepted so the function can also be
 * invoked directly during local testing.
 */
function normalizePath(value, prefix) {
  let path = String(value || '/')
  const queryIndex = path.indexOf('?')
  if (queryIndex >= 0) path = path.slice(0, queryIndex)
  path = path.replace(/\/{2,}/g, '/')
  if (!path.startsWith('/')) path = '/' + path
  if (path.length > 1) path = path.replace(/\/+$/, '')

  // Alipay cloud's default URL化 domain keeps the gateway segment in the
  // request path (https://<space-id>/http/<route>); drop it before matching.
  if (path === '/http') {
    path = '/'
  } else if (path.startsWith('/http/')) {
    path = path.slice('/http'.length)
  }

  const cleanPrefix = prefix ? '/' + String(prefix).replace(/^\/+|\/+$/g, '') : ''
  if (cleanPrefix && cleanPrefix !== '/') {
    if (path === cleanPrefix) return '/'
    if (path.startsWith(cleanPrefix + '/')) return path.slice(cleanPrefix.length) || '/'
  }
  return path || '/'
}

function bodyText(event) {
  const raw = event ? event.body : null
  if (raw == null) return ''
  if (typeof raw === 'string') {
    return event.isBase64Encoded ? Buffer.from(raw, 'base64').toString('utf8') : raw
  }
  if (Buffer.isBuffer(raw)) return raw.toString('utf8')
  if (typeof raw === 'object') return JSON.stringify(raw)
  return String(raw)
}

function parseJsonBody(event) {
  const raw = bodyText(event)
  if (!raw.trim()) return {}
  try {
    const parsed = JSON.parse(raw)
    if (parsed == null || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('not an object')
    }
    return parsed
  } catch (error) {
    throw new ApiError(400, 'MALFORMED_BODY', '请求内容不是合法的 JSON')
  }
}

function parseAuthorization(headers) {
  const header = headers.authorization || headers.Authorization
  if (!header) return null
  const match = /^Bearer\s+(.+)$/i.exec(String(header).trim())
  if (!match) return null
  const token = match[1].trim()
  return token || null
}

function compose(status, body, headers) {
  return {
    mpserverlessComposedResponse: true,
    isBase64Encoded: false,
    statusCode: status,
    headers: { ...CORS_HEADERS, ...(headers || {}) },
    body,
  }
}

function json(status, payload, headers) {
  return compose(status, JSON.stringify(payload), {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store',
    ...(headers || {}),
  })
}

function redirect(location) {
  return compose(302, '', {
    location,
    'cache-control': 'no-store',
  })
}

module.exports = {
  headerMap,
  rawPath,
  normalizePath,
  bodyText,
  parseJsonBody,
  parseAuthorization,
  json,
  redirect,
}
