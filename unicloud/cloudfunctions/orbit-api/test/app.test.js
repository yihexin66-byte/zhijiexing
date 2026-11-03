'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')

const { createTestApp, call, auth } = require('../test-support/helpers')

const DAY_MS = 24 * 60 * 60 * 1000

async function register(app, username = 'nova_1027', password = 'orbit2026') {
  const response = await call(app, {
    method: 'POST',
    path: '/api/auth/register',
    body: { username, password },
  })
  return response
}

function goalPayload(overrides = {}) {
  return {
    id: 1,
    title: 'Orbit-2.3-E2E',
    description: null,
    tone: 'mint',
    status: 'ACTIVE',
    targetDate: null,
    createdAt: 100,
    updatedAt: 100,
    completedAt: null,
    sortOrder: 0,
    ...overrides,
  }
}

function taskPayload(overrides = {}) {
  return {
    id: 1,
    title: '同步一条任务',
    description: null,
    priority: 'HIGH',
    category: 'WORK',
    dueDate: null,
    isCompleted: false,
    createdAt: 100,
    completedAt: null,
    goalId: 1,
    stageId: null,
    ...overrides,
  }
}

test('health is public and works with or without the URL prefix', async () => {
  const { app } = createTestApp()
  const prefixed = await call(app, { path: '/api/health' })
  assert.equal(prefixed.statusCode, 200)
  assert.equal(prefixed.mpserverlessComposedResponse, true)
  assert.equal(prefixed.json.status, 'ok')
  assert.equal(prefixed.json.backend, 'unicloud')
  assert.equal(prefixed.headers['access-control-allow-origin'], '*')

  const bare = await call(app, { path: '/api/health', prefix: false })
  assert.equal(bare.statusCode, 200)
})

test('CORS preflight is answered without touching the database', async () => {
  const { app } = createTestApp()
  const response = await call(app, { method: 'OPTIONS', path: '/api/sync' })
  assert.equal(response.statusCode, 200)
  assert.deepEqual(response.json, {})
  assert.equal(response.headers['access-control-allow-methods'], 'GET, POST, PUT, PATCH, OPTIONS')
})

test('register creates an account, a token and the username nickname', async () => {
  const { app, store } = createTestApp()
  const response = await register(app)

  assert.equal(response.statusCode, 201)
  assert.equal(response.json.token.length, 64)
  assert.equal(response.json.user.username, 'nova_1027')
  assert.equal(response.json.user.nickname, 'nova_1027')
  assert.equal(response.json.user.avatarPath, null)
  assert.ok(Number.isSafeInteger(response.json.user.id))
  assert.ok(response.json.user.id > 0)
  assert.equal(response.json.user.createdAt, 1700000000000)
  assert.equal(response.json.user.passwordHash, undefined)
  assert.equal(store.users.length, 1)
  assert.equal(store.tokens.length, 1)
  assert.notEqual(store.users[0].passwordHash, 'orbit2026')

  const duplicate = await register(app)
  assert.equal(duplicate.statusCode, 409)
  assert.equal(duplicate.json.code, 'ACCOUNT_EXISTS')
  assert.equal(store.users.length, 1)
})

test('register validates username and password like the Spring backend', async () => {
  const { app } = createTestApp()
  const cases = [
    [{ username: '', password: 'orbit2026' }, 'username 用户名不能为空'],
    [{ username: 'ab', password: 'orbit2026' }, 'username 需为 3-24 位字母、数字或下划线'],
    [{ username: 'nova-1027', password: 'orbit2026' }, 'username 需为 3-24 位字母、数字或下划线'],
    [{ username: 'nova_1027', password: '' }, 'password 密码不能为空'],
    [{ username: 'nova_1027', password: '12345' }, 'password 密码长度需为 6-64 位'],
  ]
  for (const [body, message] of cases) {
    const response = await call(app, { method: 'POST', path: '/api/auth/register', body })
    assert.equal(response.statusCode, 400)
    assert.equal(response.json.code, 'VALIDATION_FAILED')
    assert.equal(response.json.message, message)
  }
})

test('login, profile, logout and 401 cleanup follow the account contract', async () => {
  const { app, store } = createTestApp()
  const registered = await register(app)

  const wrong = await call(app, {
    method: 'POST',
    path: '/api/auth/login',
    body: { username: 'nova_1027', password: 'wrong-password' },
  })
  assert.equal(wrong.statusCode, 401)
  assert.equal(wrong.json.code, 'LOGIN_FAILED')

  const missing = await call(app, {
    method: 'POST',
    path: '/api/auth/login',
    body: { username: 'nova_1027' },
  })
  assert.equal(missing.statusCode, 400)

  const login = await call(app, {
    method: 'POST',
    path: '/api/auth/login',
    body: { username: ' nova_1027 ', password: 'orbit2026' },
  })
  assert.equal(login.statusCode, 200)
  assert.notEqual(login.json.token, registered.json.token)
  assert.equal(store.tokens.length, 2)

  const profile = await call(app, {
    path: '/api/me',
    headers: auth(login.json.token),
  })
  assert.equal(profile.statusCode, 200)
  assert.equal(profile.json.id, registered.json.user.id)
  assert.equal(profile.json.nickname, 'nova_1027')

  const anonymous = await call(app, { path: '/api/me' })
  assert.equal(anonymous.statusCode, 401)
  assert.equal(anonymous.json.code, 'UNAUTHORIZED')

  const logout = await call(app, {
    method: 'POST',
    path: '/api/auth/logout',
    headers: auth(login.json.token),
  })
  assert.equal(logout.statusCode, 200)
  assert.equal(logout.json.status, 'ok')

  const afterLogout = await call(app, {
    path: '/api/me',
    headers: auth(login.json.token),
  })
  assert.equal(afterLogout.statusCode, 401)
  assert.equal(store.tokens.length, 1)
})

test('tokens expire after 30 days and are removed on use', async () => {
  const harness = createTestApp()
  const { app, store } = harness
  const registered = await register(app)

  harness.advance(30 * DAY_MS + 1)
  const response = await call(app, {
    path: '/api/me',
    headers: auth(registered.json.token),
  })
  assert.equal(response.statusCode, 401)
  assert.equal(response.json.code, 'UNAUTHORIZED')
  assert.equal(store.tokens.length, 0)
})

test('nickname updates immediately and rejects empty or long values', async () => {
  const { app } = createTestApp()
  const registered = await register(app)
  const token = registered.json.token

  const updated = await call(app, {
    method: 'PATCH',
    path: '/api/me',
    headers: auth(token),
    body: { nickname: '  NovaPilot  ' },
  })
  assert.equal(updated.statusCode, 200)
  assert.equal(updated.json.nickname, 'NovaPilot')

  const profile = await call(app, { path: '/api/me', headers: auth(token) })
  assert.equal(profile.json.nickname, 'NovaPilot')

  for (const nickname of ['', '   ', 'x'.repeat(25)]) {
    const response = await call(app, {
      method: 'PATCH',
      path: '/api/me',
      headers: auth(token),
      body: { nickname },
    })
    assert.equal(response.statusCode, 400)
    assert.equal(response.json.code, 'NICKNAME_INVALID')
  }
})

test('sync starts empty, accepts a snapshot and rejects stale revisions with a raw snapshot', async () => {
  const { app, store } = createTestApp()
  const registered = await register(app)
  const token = registered.json.token
  const userId = registered.json.user.id

  const empty = await call(app, { path: '/api/sync', headers: auth(token) })
  assert.equal(empty.statusCode, 200)
  assert.deepEqual(empty.json, {
    revision: 0,
    goals: [],
    stages: [],
    tasks: [],
    habits: [],
    habitRecords: [],
    growthEvents: [],
  })

  const firstPush = await call(app, {
    method: 'PUT',
    path: '/api/sync',
    headers: auth(token),
    body: {
      baseRevision: 0,
      goals: [goalPayload()],
      stages: [],
      tasks: [taskPayload()],
      habits: [],
      habitRecords: [],
      growthEvents: [],
    },
  })
  assert.equal(firstPush.statusCode, 200)
  assert.equal(firstPush.json.revision, 1)
  assert.equal(firstPush.json.goals[0].title, 'Orbit-2.3-E2E')
  assert.equal(firstPush.json.tasks[0].title, '同步一条任务')
  assert.equal(store.snapshots.length, 1)
  assert.equal(store.snapshots[0].userId, userId)

  const pull = await call(app, { path: '/api/sync', headers: auth(token) })
  assert.equal(pull.json.revision, 1)
  assert.equal(pull.json.tasks[0].isCompleted, false)

  const stale = await call(app, {
    method: 'PUT',
    path: '/api/sync',
    headers: auth(token),
    body: {
      baseRevision: 0,
      goals: [goalPayload({ title: 'stale' })],
      stages: [],
      tasks: [],
      habits: [],
      habitRecords: [],
      growthEvents: [],
    },
  })
  assert.equal(stale.statusCode, 409)
  assert.equal(stale.json.revision, 1)
  assert.equal(stale.json.code, undefined)
  assert.equal(stale.json.goals[0].title, 'Orbit-2.3-E2E')
  assert.equal(store.snapshots.length, 1)

  const nextPush = await call(app, {
    method: 'PUT',
    path: '/api/sync',
    headers: auth(token),
    body: {
      baseRevision: 1,
      goals: [goalPayload({ title: 'renamed', updatedAt: 200 })],
      stages: [],
      tasks: [taskPayload({ isCompleted: true, completedAt: 200 })],
      habits: [],
      habitRecords: [],
      growthEvents: [],
    },
  })
  assert.equal(nextPush.statusCode, 200)
  assert.equal(nextPush.json.revision, 2)
  assert.equal(nextPush.json.goals[0].title, 'renamed')
  assert.equal(nextPush.json.tasks[0].isCompleted, true)
  assert.equal(store.snapshots.length, 1)
})

test('sync accepts base64 URL化 bodies and validates its input', async () => {
  const { app } = createTestApp()
  const registered = await register(app)
  const token = registered.json.token

  const base64Push = await call(app, {
    method: 'PUT',
    path: '/api/sync',
    headers: auth(token),
    body: {
      baseRevision: 0,
      goals: [],
      stages: [],
      tasks: [],
      habits: [],
      habitRecords: [],
      growthEvents: [],
    },
    base64: true,
  })
  assert.equal(base64Push.statusCode, 200)
  assert.equal(base64Push.json.revision, 1)

  const malformed = await call(app, {
    method: 'PUT',
    path: '/api/sync',
    headers: auth(token),
    body: '{',
  })
  assert.equal(malformed.statusCode, 400)
  assert.equal(malformed.json.code, 'MALFORMED_BODY')

  const negative = await call(app, {
    method: 'PUT',
    path: '/api/sync',
    headers: auth(token),
    body: { baseRevision: -1 },
  })
  assert.equal(negative.statusCode, 400)
  assert.equal(negative.json.code, 'SYNC_REVISION')

  const anonymous = await call(app, { method: 'PUT', path: '/api/sync', body: { baseRevision: 0 } })
  assert.equal(anonymous.statusCode, 401)
})

test('avatar uploads use JSON base64, redirect media and replace the old file', async () => {
  const { app, uploads, deleted } = createTestApp()
  const registered = await register(app)
  const token = registered.json.token
  const userId = registered.json.user.id

  const first = await call(app, {
    method: 'POST',
    path: '/api/me/avatar',
    headers: auth(token),
    body: {
      contentBase64: Buffer.from('first-jpeg').toString('base64'),
      contentType: 'image/jpeg',
    },
  })
  assert.equal(first.statusCode, 200)
  assert.match(first.json.avatarPath, /^\/media\/avatars\/[a-f0-9]{32}$/)
  assert.equal(uploads.length, 1)
  assert.equal(uploads[0].extension, 'jpg')
  assert.equal(uploads[0].contentType, 'image/jpeg')
  assert.equal(uploads[0].buffer.toString(), 'first-jpeg')
  assert.match(uploads[0].key, new RegExp('^' + userId + '-[a-f0-9]{32}$'))

  const key = first.json.avatarPath.split('/').pop()
  const media = await call(app, { path: first.json.avatarPath })
  assert.equal(media.statusCode, 302)
  assert.match(media.headers.location, /^https:\/\/files\.example\//)

  const second = await call(app, {
    method: 'POST',
    path: '/api/me/avatar',
    headers: auth(token),
    body: {
      contentBase64: Buffer.from('second-png').toString('base64'),
      contentType: 'image/png',
    },
  })
  assert.equal(second.statusCode, 200)
  assert.notEqual(second.json.avatarPath, first.json.avatarPath)
  assert.equal(deleted.length, 1)
  assert.match(deleted[0], new RegExp('^cloud://test-space/' + userId + '-' + key + '\\.jpg$'))

  const oldMedia = await call(app, { path: first.json.avatarPath })
  assert.equal(oldMedia.statusCode, 404)
  const missingMedia = await call(app, { path: '/media/avatars/not-a-key' })
  assert.equal(missingMedia.statusCode, 404)
})

test('avatar uploads reject missing, malformed, oversized or unsupported payloads', async () => {
  const { app } = createTestApp()
  const registered = await register(app)
  const token = registered.json.token

  const cases = [
    [{ contentBase64: '', contentType: 'image/jpeg' }, 400, 'AVATAR_MISSING'],
    [{ contentBase64: '!!!!', contentType: 'image/jpeg' }, 400, 'AVATAR_INVALID'],
    [{ contentBase64: 'abcd', contentType: 'image/gif' }, 415, 'AVATAR_TYPE'],
    [{ contentBase64: 'A'.repeat(2_000_000), contentType: 'image/jpeg' }, 413, 'AVATAR_TOO_LARGE'],
  ]
  for (const [body, status, code] of cases) {
    const response = await call(app, {
      method: 'POST',
      path: '/api/me/avatar',
      headers: auth(token),
      body,
    })
    assert.equal(response.statusCode, status)
    assert.equal(response.json.code, code)
  }

  const anonymous = await call(app, {
    method: 'POST',
    path: '/api/me/avatar',
    body: { contentBase64: 'abcd', contentType: 'image/jpeg' },
  })
  assert.equal(anonymous.statusCode, 401)
})

test('unknown routes and unexpected failures keep the JSON error envelope', async () => {
  const { app } = createTestApp()
  const missing = await call(app, { path: '/api/nope' })
  assert.equal(missing.statusCode, 404)
  assert.equal(missing.json.code, 'NOT_FOUND')

  const brokenStore = {
    async findUserByUsername() {
      throw new Error('database down')
    },
    async findUserById() {
      throw new Error('database down')
    },
    async findSession() {
      throw new Error('database down')
    },
  }
  const failing = createTestApp({ store: brokenStore })
  const failure = await call(failing.app, {
    method: 'POST',
    path: '/api/auth/login',
    body: { username: 'nova_1027', password: 'orbit2026' },
  })
  assert.equal(failure.statusCode, 500)
  assert.equal(failure.json.code, 'INTERNAL_ERROR')
})
