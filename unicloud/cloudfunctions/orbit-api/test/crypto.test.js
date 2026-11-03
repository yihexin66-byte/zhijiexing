'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')

const {
  hashPassword,
  verifyPassword,
  sha256Hex,
  randomHex,
  randomUserId,
} = require('../lib/crypto')

test('scrypt hashes carry their parameters and verify', () => {
  const hash = hashPassword('orbit2026')
  const parts = hash.split('$')
  assert.equal(parts[0], 'scrypt')
  assert.equal(parts[1], '16384')
  assert.equal(parts[2], '8')
  assert.equal(parts[3], '1')
  assert.equal(parts[4].length, 32)
  assert.equal(parts[5].length, 128)
  assert.equal(verifyPassword('orbit2026', hash), true)
  assert.equal(verifyPassword('orbit2027', hash), false)
})

test('the same password receives a different salt every time', () => {
  const first = hashPassword('orbit2026')
  const second = hashPassword('orbit2026')
  assert.notEqual(first, second)
  assert.equal(verifyPassword('orbit2026', first), true)
  assert.equal(verifyPassword('orbit2026', second), true)
})

test('malformed stored hashes are rejected instead of throwing', () => {
  const bad = [
    '',
    null,
    undefined,
    'plain-text',
    'scrypt$16384$8$1$abcd',
    'bcrypt$16384$8$1$abcd$ef',
    'scrypt$0$8$1$ab$cd',
    'scrypt$16384$8$1$abcd$ef',
  ]
  for (const value of bad) {
    assert.equal(verifyPassword('orbit2026', value), false)
  }
})

test('token helpers produce stable digests and safe ids', () => {
  assert.equal(sha256Hex('abc'), sha256Hex('abc'))
  assert.equal(sha256Hex('abc').length, 64)
  assert.equal(sha256Hex('abc'), 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')
  assert.match(randomHex(16), /^[a-f0-9]{32}$/)
  assert.match(randomHex(32), /^[a-f0-9]{64}$/)
  const id = randomUserId()
  assert.ok(Number.isSafeInteger(id))
  assert.ok(id > 0)
})
