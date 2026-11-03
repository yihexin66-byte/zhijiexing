'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')

const { ApiError } = require('../lib/errors')
const {
  headerMap,
  normalizePath,
  parseJsonBody,
  parseAuthorization,
  json,
  redirect,
} = require('../lib/http')

test('URL化 prefixes are stripped but bare /api paths still work', () => {
  assert.equal(normalizePath('/orbit/api/sync', '/orbit'), '/api/sync')
  assert.equal(normalizePath('/orbit', '/orbit'), '/')
  assert.equal(normalizePath('/orbit/', '/orbit'), '/')
  assert.equal(normalizePath('/orbit//api//me/', '/orbit'), '/api/me')
  assert.equal(normalizePath('/api/me', '/orbit'), '/api/me')
  assert.equal(normalizePath('/orbitx/api/me', '/orbit'), '/orbitx/api/me')
  assert.equal(normalizePath('/orbit/api/me?x=1', '/orbit'), '/api/me')
  assert.equal(normalizePath('api/me', ''), '/api/me')
  assert.equal(normalizePath('/custom/api/me', '/custom'), '/api/me')
})

test('alipay gateway /http segment is stripped', () => {
  assert.equal(normalizePath('/http/orbit/api/health', '/orbit'), '/api/health')
  assert.equal(normalizePath('/http/api/health', '/orbit'), '/api/health')
  assert.equal(normalizePath('/http/orbit', '/orbit'), '/')
  assert.equal(normalizePath('/http', '/orbit'), '/')
})

test('headers are lower-cased and bearer tokens parsed', () => {
  const headers = headerMap({
    Authorization: 'Bearer abc123',
    'Content-Type': 'application/json',
    'X-List': ['a', 'b'],
    Empty: null,
  })
  assert.equal(headers.authorization, 'Bearer abc123')
  assert.equal(headers['content-type'], 'application/json')
  assert.equal(headers['x-list'], 'a, b')
  assert.equal('empty' in headers, false)

  assert.equal(parseAuthorization(headers), 'abc123')
  assert.equal(parseAuthorization({ authorization: 'bearer xyz' }), 'xyz')
  assert.equal(parseAuthorization({ authorization: 'Basic abc' }), null)
  assert.equal(parseAuthorization({ authorization: 'Bearer ' }), null)
  assert.equal(parseAuthorization({}), null)
})

test('request bodies support plain JSON, base64 and pre-parsed objects', () => {
  assert.deepEqual(parseJsonBody({ body: '{"a":1}' }), { a: 1 })
  assert.deepEqual(
    parseJsonBody({
      body: Buffer.from('{"a":2}', 'utf8').toString('base64'),
      isBase64Encoded: true,
    }),
    { a: 2 },
  )
  assert.deepEqual(parseJsonBody({ body: { a: 3 } }), { a: 3 })
  assert.deepEqual(parseJsonBody({}), {})
})

test('malformed bodies raise MALFORMED_BODY', () => {
  for (const body of ['{', '[]', 'null', '"text"']) {
    assert.throws(
      () => parseJsonBody({ body }),
      (error) => error instanceof ApiError && error.code === 'MALFORMED_BODY' && error.status === 400,
    )
  }
})

test('composed responses match the uniCloud URL化 contract', () => {
  const response = json(201, { ok: true })
  assert.equal(response.mpserverlessComposedResponse, true)
  assert.equal(response.isBase64Encoded, false)
  assert.equal(response.statusCode, 201)
  assert.equal(response.headers['content-type'], 'application/json; charset=utf-8')
  assert.equal(response.headers['access-control-allow-origin'], '*')
  assert.equal(response.body, '{"ok":true}')

  const moved = redirect('https://files.example/a.jpg')
  assert.equal(moved.statusCode, 302)
  assert.equal(moved.headers.location, 'https://files.example/a.jpg')
})
