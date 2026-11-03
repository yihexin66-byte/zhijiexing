'use strict'

const { hashPassword } = require('./crypto')

const USERS = 'zzx_users'
const TOKENS = 'zzx_tokens'
const PAPERS = 'zzx_papers'
const ATTEMPTS = 'zzx_attempts'
const CERTIFICATES = 'zzx_certificates'

const SEED_USERNAME = 'student'
const SEED_PASSWORD = '123456'

function rows(result) {
  if (!result) return []
  if (Array.isArray(result.data)) return result.data
  if (result.data && Array.isArray(result.data.data)) return result.data.data
  return []
}

function first(result) {
  const list = rows(result)
  return list.length ? list[0] : null
}

function total(result) {
  if (!result) return 0
  const value = result.total != null ? result.total : result.data && result.data.total
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

/**
 * 智解星考试数据访问层。集合统一使用 zzx_ 前缀，与 Orbit / Mint 的数据隔离。
 * seed 数据在首次调用时写入一次，之后所有请求直接复用已有试卷。
 */
function createExamStore(uniCloud, seedData) {
  const db = uniCloud.database()
  const users = db.collection(USERS)
  const tokens = db.collection(TOKENS)
  const papers = db.collection(PAPERS)
  const attempts = db.collection(ATTEMPTS)
  const certificates = db.collection(CERTIFICATES)

  let seeding = null

  async function seedOnce() {
    const existing = await papers.limit(1).get()
    if (!rows(existing).length) {
      for (const paper of (seedData && seedData.papers) || []) {
        await papers.add(paper)
      }
    }
    const seedUser = await users.where({ username: SEED_USERNAME }).limit(1).get()
    if (!rows(seedUser).length) {
      await users.add({
        userId: 'u_student',
        username: SEED_USERNAME,
        passwordHash: hashPassword(SEED_PASSWORD),
        role: '考生',
        status: '正常',
        createdAt: Date.now(),
      })
    }
  }

  async function ensureSeed() {
    if (!seeding) {
      seeding = seedOnce().catch((error) => {
        seeding = null
        throw error
      })
    }
    return seeding
  }

  return {
    ensureSeed,

    async findUserByUsername(username) {
      return first(await users.where({ username }).limit(1).get())
    },

    async findUserById(userId) {
      return first(await users.where({ userId: String(userId) }).limit(1).get())
    },

    async createUser(user) {
      await users.add(user)
    },

    async createToken(token) {
      await tokens.add(token)
    },

    async findSession(tokenHash) {
      return first(await tokens.where({ tokenHash }).limit(1).get())
    },

    async deleteToken(documentId) {
      await tokens.doc(documentId).remove()
    },

    async purgeExpiredTokens(timestamp) {
      await tokens.where({ expiresAt: db.command.lte(timestamp) }).remove()
    },

    async listPapers() {
      const result = await papers.orderBy('sortOrder', 'asc').limit(50).get()
      return rows(result)
    },

    async getPaper(paperId) {
      return first(await papers.where({ paperId: String(paperId) }).limit(1).get())
    },

    async getAttempt(attemptId) {
      return first(await attempts.where({ attemptId: String(attemptId) }).limit(1).get())
    },

    async findActiveAttempt(userId, paperId) {
      return first(await attempts.where({
        userId: String(userId),
        paperId: String(paperId),
        status: '进行中',
      }).limit(1).get())
    },

    async createAttempt(attempt) {
      await attempts.add(attempt)
    },

    async updateAttempt(documentId, patch) {
      await attempts.doc(documentId).update(patch)
    },

    async listSubmittedAttempts(userId) {
      const result = await attempts.where({
        userId: String(userId),
        status: '已交卷',
      }).limit(500).get()
      return rows(result)
    },

    async findCertificateByAttempt(attemptId) {
      return first(await certificates.where({ attemptId: String(attemptId) }).limit(1).get())
    },

    async countCertificatesByDay(day) {
      return total(await certificates.where({ certDay: String(day) }).count())
    },

    async createCertificate(certificate) {
      await certificates.add(certificate)
    },

    async listCertificates(userId) {
      const result = await certificates.where({ userId: String(userId) }).limit(100).get()
      return rows(result)
    },
  }
}

module.exports = {
  createExamStore,
  SEED_USERNAME,
  SEED_PASSWORD,
  USERS,
  TOKENS,
  PAPERS,
  ATTEMPTS,
  CERTIFICATES,
}
