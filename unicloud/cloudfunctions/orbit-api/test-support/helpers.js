'use strict'

const { createApp } = require('../lib/app')

function duplicateError() {
  const error = new Error('E11000 duplicate key error collection')
  error.code = 'DUPLICATE'
  return error
}

function createMemoryStore() {
  let sequence = 0
  const users = []
  const tokens = []
  const snapshots = []
  const nextId = (prefix) => prefix + ++sequence

  return {
    users,
    tokens,
    snapshots,

    async findUserByUsername(username) {
      return users.find((user) => user.username === username) || null
    },

    async findUserById(userId) {
      return users.find((user) => user.userId === userId) || null
    },

    async createUser(user) {
      if (users.some((existing) => existing.username === user.username)) {
        throw duplicateError()
      }
      users.push({ _id: nextId('u'), ...user })
    },

    async updateUser(documentId, patch) {
      const user = users.find((candidate) => candidate._id === documentId)
      if (!user) throw new Error('user not found: ' + documentId)
      Object.assign(user, patch)
    },

    async createToken(token) {
      tokens.push({ _id: nextId('t'), ...token })
    },

    async findSession(tokenHash) {
      return tokens.find((token) => token.tokenHash === tokenHash) || null
    },

    async deleteToken(documentId) {
      const index = tokens.findIndex((token) => token._id === documentId)
      if (index >= 0) tokens.splice(index, 1)
    },

    async purgeExpiredTokens(timestamp) {
      for (let index = tokens.length - 1; index >= 0; index -= 1) {
        if (tokens[index].expiresAt <= timestamp) tokens.splice(index, 1)
      }
    },

    async getSnapshot(userId) {
      return snapshots.find((snapshot) => snapshot.userId === userId) || null
    },

    async createSnapshot(snapshot) {
      if (snapshots.some((existing) => existing.userId === snapshot.userId)) {
        throw duplicateError()
      }
      snapshots.push({ _id: nextId('s'), ...snapshot })
    },

    async updateSnapshotIfRevision(documentId, revision, patch) {
      const snapshot = snapshots.find((candidate) => candidate._id === documentId)
      if (!snapshot || snapshot.revision !== revision) return false
      Object.assign(snapshot, patch)
      return true
    },

    async findUserByAvatarKey(key) {
      return users.find((user) => user.avatarKey === key) || null
    },
  }
}

function silentLogger() {
  return {
    log() {},
    warn() {},
    error() {},
  }
}

function createTestApp(options = {}) {
  const {
    store = createMemoryStore(),
    now: initialNow = 1700000000000,
    logger = silentLogger(),
    urlPrefix = '/orbit',
    ...rest
  } = options

  let clock = initialNow
  const uploads = []
  const deleted = []
  const app = createApp({
    store,
    now: () => clock,
    logger,
    urlPrefix,
    async uploadAvatar(input) {
      uploads.push(input)
      return 'cloud://test-space/' + input.key + '.' + input.extension
    },
    async deleteAvatar(fileID) {
      deleted.push(fileID)
    },
    async resolveAvatarUrl(fileID) {
      return 'https://files.example/' + encodeURIComponent(fileID)
    },
    ...rest,
  })

  return {
    app,
    store,
    uploads,
    deleted,
    advance(milliseconds) {
      clock += milliseconds
    },
  }
}

async function call(app, request = {}) {
  const {
    method = 'GET',
    path = '/',
    headers = {},
    body,
    base64 = false,
    prefix = true,
  } = request

  const event = {
    httpMethod: method,
    path: prefix ? '/orbit' + path : path,
    headers: { ...headers },
  }

  if (body !== undefined) {
    event.headers['content-type'] = 'application/json'
    if (typeof body === 'string') {
      event.body = body
    } else {
      const text = JSON.stringify(body)
      if (base64) {
        event.body = Buffer.from(text, 'utf8').toString('base64')
        event.isBase64Encoded = true
      } else {
        event.body = text
      }
    }
  }

  const response = await app(event, {})
  let payload = null
  if (response.body) {
    try {
      payload = JSON.parse(response.body)
    } catch (error) {
      payload = response.body
    }
  }
  return { ...response, json: payload }
}

function auth(token) {
  return { authorization: 'Bearer ' + token }
}

module.exports = {
  createMemoryStore,
  createTestApp,
  call,
  auth,
  duplicateError,
}
