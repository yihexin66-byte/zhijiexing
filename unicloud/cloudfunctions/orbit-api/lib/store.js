'use strict'

const USERS = 'orbit_users'
const TOKENS = 'orbit_tokens'
const SNAPSHOTS = 'orbit_snapshots'

function firstRow(result) {
  if (!result) return null
  const rows = Array.isArray(result.data)
    ? result.data
    : result.data && Array.isArray(result.data.data)
      ? result.data.data
      : []
  return rows.length ? rows[0] : null
}

function updatedCount(result) {
  if (!result) return 0
  const values = [
    result.updated,
    result.stats && result.stats.updated,
    result.data && result.data.updated,
  ]
  for (const value of values) {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return 0
}

/**
 * Thin wrapper over the uniCloud document database. Collection names and the
 * exact queries live here so the HTTP layer stays testable without uniCloud.
 */
function createStore(uniCloud) {
  const db = uniCloud.database()
  const users = db.collection(USERS)
  const tokens = db.collection(TOKENS)
  const snapshots = db.collection(SNAPSHOTS)

  return {
    async findUserByUsername(username) {
      const result = await users.where({ username }).limit(1).get()
      return firstRow(result)
    },

    async findUserById(userId) {
      const result = await users.where({ userId }).limit(1).get()
      return firstRow(result)
    },

    async createUser(user) {
      await users.add(user)
    },

    async updateUser(documentId, patch) {
      await users.doc(documentId).update(patch)
    },

    async createToken(token) {
      await tokens.add(token)
    },

    async findSession(tokenHash) {
      const result = await tokens.where({ tokenHash }).limit(1).get()
      return firstRow(result)
    },

    async deleteToken(documentId) {
      await tokens.doc(documentId).remove()
    },

    async purgeExpiredTokens(timestamp) {
      await tokens.where({ expiresAt: db.command.lte(timestamp) }).remove()
    },

    async getSnapshot(userId) {
      const result = await snapshots.where({ userId }).limit(1).get()
      return firstRow(result)
    },

    async createSnapshot(snapshot) {
      await snapshots.add(snapshot)
    },

    async updateSnapshotIfRevision(documentId, revision, patch) {
      const result = await snapshots
        .where({ _id: documentId, revision })
        .update(patch)
      return updatedCount(result) > 0
    },

    async findUserByAvatarKey(key) {
      const result = await users.where({ avatarKey: key }).limit(1).get()
      return firstRow(result)
    },
  }
}

module.exports = { createStore, USERS, TOKENS, SNAPSHOTS }
