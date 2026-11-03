'use strict'

const { createExam } = require('../lib/exam')
const { hashPassword } = require('../lib/crypto')
const seedData = require('../exam-seed.json')

const SEED_PASSWORD = '123456'

function createMemoryExamStore(seed = seedData) {
  let sequence = 0
  const users = []
  const tokens = []
  const papers = []
  const attempts = []
  const certificates = []

  const nextId = (prefix) => prefix + ++sequence

  return {
    users,
    tokens,
    papers,
    attempts,
    certificates,

    async ensureSeed() {
      if (!papers.length) {
        for (const paper of seed.papers) {
          papers.push({ _id: nextId('paper'), ...JSON.parse(JSON.stringify(paper)) })
        }
      }
      if (!users.some((user) => user.username === 'student')) {
        users.push({
          _id: nextId('user'),
          userId: 'u_student',
          username: 'student',
          passwordHash: hashPassword(SEED_PASSWORD),
          role: '考生',
          status: '正常',
          createdAt: 1700000000000,
        })
      }
    },

    async findUserByUsername(username) {
      return users.find((user) => user.username === username) || null
    },

    async findUserById(userId) {
      return users.find((user) => user.userId === String(userId)) || null
    },

    async createUser(user) {
      if (users.some((existing) => existing.username === user.username)) {
        const error = new Error('duplicate key')
        error.code = 'DUPLICATE'
        throw error
      }
      users.push({ _id: nextId('user'), ...user })
    },

    async createToken(token) {
      tokens.push({ _id: nextId('token'), ...token })
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

    async listPapers() {
      return papers.slice().sort((left, right) => left.sortOrder - right.sortOrder)
    },

    async getPaper(paperId) {
      return papers.find((paper) => paper.paperId === String(paperId)) || null
    },

    async getAttempt(attemptId) {
      return attempts.find((attempt) => attempt.attemptId === String(attemptId)) || null
    },

    async findActiveAttempt(userId, paperId) {
      return attempts.find((attempt) =>
        attempt.userId === String(userId) &&
        attempt.paperId === String(paperId) &&
        attempt.status === '进行中') || null
    },

    async createAttempt(attempt) {
      attempts.push({ _id: nextId('attempt'), ...attempt })
    },

    async updateAttempt(documentId, patch) {
      const attempt = attempts.find((candidate) => candidate._id === documentId)
      if (!attempt) throw new Error('attempt not found: ' + documentId)
      Object.assign(attempt, patch)
    },

    async listSubmittedAttempts(userId) {
      return attempts.filter((attempt) =>
        attempt.userId === String(userId) && attempt.status === '已交卷')
    },

    async findCertificateByAttempt(attemptId) {
      return certificates.find((item) => item.attemptId === String(attemptId)) || null
    },

    async countCertificatesByDay(day) {
      return certificates.filter((item) => item.certDay === String(day)).length
    },

    async createCertificate(certificate) {
      if (certificates.some((item) => item.attemptId === certificate.attemptId)) {
        const error = new Error('duplicate key')
        error.code = 'DUPLICATE'
        throw error
      }
      certificates.push({ _id: nextId('cert'), ...certificate })
    },

    async listCertificates(userId) {
      return certificates.filter((item) => item.userId === String(userId))
    },
  }
}

function silentLogger() {
  return { log() {}, warn() {}, error() {} }
}

function createExamTestApp(options = {}) {
  const {
    store = createMemoryExamStore(),
    now: initialNow = 1750000000000,
    logger = silentLogger(),
  } = options
  let clock = initialNow
  const exam = createExam({ store, now: () => clock, logger })
  return {
    store,
    advance(milliseconds) {
      clock += milliseconds
    },
    async call(request = {}) {
      const {
        method = 'GET',
        path = '/',
        headers = {},
        body,
        query,
        prefix = true,
      } = request
      const event = {
        httpMethod: method,
        path: (prefix ? '/orbit/exam' : '') + path,
        headers: { ...headers },
      }
      if (query) {
        event.queryStringParameters = query
        event.path += '?' + new URLSearchParams(query).toString()
      }
      if (body !== undefined) {
        event.headers['content-type'] = 'application/json'
        event.body = typeof body === 'string' ? body : JSON.stringify(body)
      }
      let fullPath = '/'
      const raw = String(event.path || '')
      if (raw === '/orbit/exam') {
        fullPath = '/'
      } else if (raw.startsWith('/orbit/exam/')) {
        fullPath = raw.slice('/orbit/exam'.length)
      }
      const queryIndex = fullPath.indexOf('?')
      if (queryIndex >= 0) fullPath = fullPath.slice(0, queryIndex)
      const response = await exam(event, fullPath, {})
      let payload = null
      if (response.body) {
        try {
          payload = JSON.parse(response.body)
        } catch (error) {
          payload = response.body
        }
      }
      return { ...response, json: payload }
    },
  }
}

function bearer(token) {
  return { authorization: 'Bearer ' + token }
}

async function loginStudent(app) {
  const response = await app.call({
    method: 'POST',
    path: '/api/auth/login',
    body: { username: 'student', password: SEED_PASSWORD },
  })
  return response
}

module.exports = {
  createMemoryExamStore,
  createExamTestApp,
  loginStudent,
  bearer,
  seedData,
  SEED_PASSWORD,
}
