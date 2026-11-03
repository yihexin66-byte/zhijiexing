'use strict'

const crypto = require('crypto')

// scrypt parameters are persisted inside every hash so they can be raised later
// without invalidating existing accounts.
const SCRYPT = Object.freeze({ N: 16384, r: 8, p: 1, keylen: 64, maxmem: 64 * 1024 * 1024 })

function hashPassword(password) {
  const salt = crypto.randomBytes(16)
  const derived = crypto.scryptSync(String(password), salt, SCRYPT.keylen, {
    N: SCRYPT.N,
    r: SCRYPT.r,
    p: SCRYPT.p,
    maxmem: SCRYPT.maxmem,
  })
  return [
    'scrypt',
    SCRYPT.N,
    SCRYPT.r,
    SCRYPT.p,
    salt.toString('hex'),
    derived.toString('hex'),
  ].join('$')
}

function verifyPassword(password, stored) {
  const parts = String(stored || '').split('$')
  if (parts.length !== 6 || parts[0] !== 'scrypt') return false

  const N = Number(parts[1])
  const r = Number(parts[2])
  const p = Number(parts[3])
  if (!Number.isInteger(N) || !Number.isInteger(r) || !Number.isInteger(p)) return false
  if (N < 1024 || N > 1048576 || r < 1 || r > 32 || p < 1 || p > 16) return false

  let salt
  let expected
  try {
    salt = Buffer.from(parts[4], 'hex')
    expected = Buffer.from(parts[5], 'hex')
  } catch (error) {
    return false
  }
  if (!salt.length || !expected.length || expected.length > 128) return false

  let actual
  try {
    actual = crypto.scryptSync(String(password), salt, expected.length, {
      N,
      r,
      p,
      maxmem: SCRYPT.maxmem,
    })
  } catch (error) {
    return false
  }
  return actual.length === expected.length && crypto.timingSafeEqual(actual, expected)
}

function sha256Hex(value) {
  return crypto.createHash('sha256').update(String(value)).digest('hex')
}

function randomHex(bytes) {
  return crypto.randomBytes(bytes).toString('hex')
}

/** Numeric user ids stay below Number.MAX_SAFE_INTEGER and inside Gson's Long. */
function randomUserId() {
  return crypto.randomBytes(6).readUIntBE(0, 6) + 1
}

module.exports = {
  hashPassword,
  verifyPassword,
  sha256Hex,
  randomHex,
  randomUserId,
}
