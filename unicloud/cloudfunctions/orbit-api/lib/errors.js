'use strict'

/** An error that maps directly onto the JSON error contract of the Spring backend. */
class ApiError extends Error {
  constructor(status, code, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

module.exports = { ApiError }
