'use strict'

const { ApiError } = require('./errors')
const {
  hashPassword,
  verifyPassword,
  randomHex,
  randomUserId,
  sha256Hex,
} = require('./crypto')
const {
  AI_LIMITS,
  AI_TIMEOUT_MS,
  beijingDayKey,
  validatePlanInput,
  validateReviewInput,
  validateChatInput,
} = require('./ai')
const { emptySnapshot, normalizePush, snapshotFromDoc } = require('./sync')
const {
  headerMap,
  rawPath,
  normalizePath,
  parseJsonBody,
  parseAuthorization,
  json,
  redirect,
} = require('./http')

const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000
const MAX_AVATAR_BYTES = 1400 * 1024
const MAX_AVATAR_BASE64 = Math.ceil(MAX_AVATAR_BYTES / 3) * 4 + 8
const AVATAR_TYPES = Object.freeze({
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
})
const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,24}$/
const AVATAR_KEY_PATTERN = /^[a-f0-9]{16,64}$/

// A real scrypt hash keeps the unknown-username path as slow as a wrong password.
const DUMMY_PASSWORD_HASH = hashPassword('orbit-dummy-password')

function publicUser(user) {
  return {
    id: Number(user.userId),
    username: user.username,
    nickname: user.nickname,
    avatarPath: user.avatarPath || null,
    createdAt: Number(user.createdAt || 0),
  }
}

function isDuplicateError(error) {
  const text = [
    error && error.code,
    error && error.errCode,
    error && error.message,
    error && error.errMsg,
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return (
    text.includes('duplicate') ||
    text.includes('e11000') ||
    text.includes('already exists') ||
    text.includes('重复')
  )
}

function requireString(body, key) {
  const value = body[key]
  return value == null ? '' : String(value)
}

/**
 * Builds the URL化 handler. The store and storage helpers are injected so the
 * whole REST contract can be tested with node:test and an in-memory store.
 */
function createApp({
  store,
  uploadAvatar,
  deleteAvatar,
  resolveAvatarUrl,
  ai = null,
  aiLimits = AI_LIMITS,
  now = Date.now,
  logger = console,
  urlPrefix = '/orbit',
}) {
  if (!store) throw new Error('store is required')
  if (typeof uploadAvatar !== 'function') throw new Error('uploadAvatar is required')

  async function issueToken(userId) {
    const timestamp = now()
    const token = randomHex(32)
    try {
      await store.purgeExpiredTokens(timestamp)
    } catch (error) {
      if (logger.warn) logger.warn('orbit-api: token purge failed', error && error.message)
    }
    await store.createToken({
      tokenHash: sha256Hex(token),
      userId,
      createdAt: timestamp,
      expiresAt: timestamp + TOKEN_TTL_MS,
    })
    return token
  }

  async function requireSession(headers) {
    const token = parseAuthorization(headers)
    if (!token) {
      throw new ApiError(401, 'UNAUTHORIZED', '登录状态已失效，请重新登录')
    }
    const session = await store.findSession(sha256Hex(token))
    if (!session || Number(session.expiresAt) <= now()) {
      if (session) {
        try {
          await store.deleteToken(session._id)
        } catch (error) {
          // An expired token is unusable even if the cleanup failed.
        }
      }
      throw new ApiError(401, 'UNAUTHORIZED', '登录状态已失效，请重新登录')
    }
    const user = await store.findUserById(Number(session.userId))
    if (!user) {
      throw new ApiError(401, 'USER_NOT_FOUND', '账号不存在，请重新登录')
    }
    return { token, session, user }
  }

  async function register(event) {
    const body = parseJsonBody(event)
    const username = requireString(body, 'username').trim()
    const password = requireString(body, 'password')

    if (!username) {
      throw new ApiError(400, 'VALIDATION_FAILED', 'username 用户名不能为空')
    }
    if (!USERNAME_PATTERN.test(username)) {
      throw new ApiError(
        400,
        'VALIDATION_FAILED',
        'username 需为 3-24 位字母、数字或下划线',
      )
    }
    if (!password) {
      throw new ApiError(400, 'VALIDATION_FAILED', 'password 密码不能为空')
    }
    if (password.length < 6 || password.length > 64) {
      throw new ApiError(400, 'VALIDATION_FAILED', 'password 密码长度需为 6-64 位')
    }

    if (await store.findUserByUsername(username)) {
      throw new ApiError(409, 'ACCOUNT_EXISTS', '这个用户名已经被注册')
    }

    const createdAt = now()
    const user = {
      userId: randomUserId(),
      username,
      passwordHash: hashPassword(password),
      nickname: username,
      avatarPath: null,
      avatarKey: null,
      avatarFileId: null,
      createdAt,
      updatedAt: createdAt,
    }

    try {
      await store.createUser(user)
    } catch (error) {
      if (isDuplicateError(error) || (await store.findUserByUsername(username))) {
        throw new ApiError(409, 'ACCOUNT_EXISTS', '这个用户名已经被注册')
      }
      throw error
    }

    const token = await issueToken(user.userId)
    return json(201, { token, user: publicUser(user) })
  }

  async function login(event) {
    const body = parseJsonBody(event)
    const username = requireString(body, 'username').trim()
    const password = requireString(body, 'password')

    if (!username) {
      throw new ApiError(400, 'VALIDATION_FAILED', 'username 用户名不能为空')
    }
    if (!password) {
      throw new ApiError(400, 'VALIDATION_FAILED', 'password 密码不能为空')
    }

    const user = await store.findUserByUsername(username)
    const matches = verifyPassword(password, user ? user.passwordHash : DUMMY_PASSWORD_HASH)
    if (!user || !matches) {
      throw new ApiError(401, 'LOGIN_FAILED', '用户名或密码不正确')
    }

    const token = await issueToken(user.userId)
    return json(200, { token, user: publicUser(user) })
  }

  async function logout(event, headers) {
    const { session } = await requireSession(headers)
    try {
      await store.deleteToken(session._id)
    } catch (error) {
      // Idempotent logout: the client clears its session regardless.
    }
    // Alipay's URL化 runtime rejects 204 and empty responses, so acknowledge
    // with a small JSON body instead of 204 No Content.
    return json(200, { status: 'ok' })
  }

  async function profile(event, headers) {
    const { user } = await requireSession(headers)
    return json(200, publicUser(user))
  }

  async function updateNickname(event, headers) {
    const { user } = await requireSession(headers)
    const body = parseJsonBody(event)
    const nickname = requireString(body, 'nickname').trim()
    if (!nickname || nickname.length > 24) {
      throw new ApiError(400, 'NICKNAME_INVALID', '昵称长度需为 1-24 个字符')
    }
    const updatedAt = now()
    await store.updateUser(user._id, { nickname, updatedAt })
    return json(200, publicUser({ ...user, nickname, updatedAt }))
  }

  async function uploadUserAvatar(event, headers) {
    const { user } = await requireSession(headers)
    const body = parseJsonBody(event)
    const rawType = requireString(body, 'contentType').toLowerCase()
    const contentType = rawType.split(';')[0].trim() || 'image/jpeg'
    const extension = AVATAR_TYPES[contentType]
    if (!extension) {
      throw new ApiError(415, 'AVATAR_TYPE', '仅支持 JPG、PNG 或 WEBP 图片')
    }

    const base64 = requireString(body, 'contentBase64')
    if (!base64) {
      throw new ApiError(400, 'AVATAR_MISSING', '请选择一张图片')
    }
    if (base64.length > MAX_AVATAR_BASE64) {
      throw new ApiError(413, 'AVATAR_TOO_LARGE', '头像不能超过 1.4 MB，请重新选择照片')
    }
    if (!/^[A-Za-z0-9+/=\r\n]+$/.test(base64)) {
      throw new ApiError(400, 'AVATAR_INVALID', '头像数据不合法')
    }

    const buffer = Buffer.from(base64, 'base64')
    if (!buffer.length) {
      throw new ApiError(400, 'AVATAR_INVALID', '头像数据不合法')
    }
    if (buffer.length > MAX_AVATAR_BYTES) {
      throw new ApiError(413, 'AVATAR_TOO_LARGE', '头像不能超过 1.4 MB，请重新选择照片')
    }

    const key = randomHex(16)
    const previousFileId = user.avatarFileId
    let fileId
    try {
      fileId = await uploadAvatar({
        key: Number(user.userId) + '-' + key,
        extension,
        contentType,
        buffer,
      })
    } catch (error) {
      if (logger.error) logger.error('orbit-api: avatar upload failed', error && error.message)
      throw new ApiError(502, 'AVATAR_UPLOAD', '头像上传失败，请稍后重试')
    }
    if (!fileId || typeof fileId !== 'string') {
      throw new ApiError(502, 'AVATAR_UPLOAD', '头像上传失败，请稍后重试')
    }

    const avatarPath = '/media/avatars/' + key
    const updatedAt = now()
    await store.updateUser(user._id, {
      avatarPath,
      avatarKey: key,
      avatarFileId: fileId,
      updatedAt,
    })

    if (previousFileId && typeof deleteAvatar === 'function') {
      try {
        await deleteAvatar(previousFileId)
      } catch (error) {
        if (logger.warn) logger.warn('orbit-api: old avatar cleanup failed', error && error.message)
      }
    }

    return json(
      200,
      publicUser({
        ...user,
        avatarPath,
        avatarKey: key,
        avatarFileId: fileId,
        updatedAt,
      }),
    )
  }

  async function pullSnapshot(event, headers) {
    const { user } = await requireSession(headers)
    const doc = await store.getSnapshot(Number(user.userId))
    return json(200, snapshotFromDoc(doc))
  }

  async function pushSnapshot(event, headers) {
    const { user } = await requireSession(headers)
    const request = normalizePush(parseJsonBody(event))
    const userId = Number(user.userId)

    const current = await store.getSnapshot(userId)
    const currentRevision = current ? Number(current.revision) || 0 : 0
    if (request.baseRevision !== currentRevision) {
      return json(409, snapshotFromDoc(current))
    }

    const next = {
      ...request.snapshot,
      revision: currentRevision + 1,
      updatedAt: now(),
    }

    if (current) {
      const accepted = await store.updateSnapshotIfRevision(
        current._id,
        currentRevision,
        next,
      )
      if (!accepted) {
        const latest = await store.getSnapshot(userId)
        return json(409, snapshotFromDoc(latest))
      }
      return json(200, snapshotFromDoc({ ...current, ...next }))
    }

    try {
      await store.createSnapshot({ userId, ...next })
    } catch (error) {
      const latest = await store.getSnapshot(userId)
      if (latest) return json(409, snapshotFromDoc(latest))
      throw error
    }
    return json(200, snapshotFromDoc({ userId, ...next }))
  }

  async function serveAvatar(path) {
    const key = path.slice('/media/avatars/'.length)
    if (!AVATAR_KEY_PATTERN.test(key)) {
      throw new ApiError(404, 'AVATAR_NOT_FOUND', '头像不存在')
    }
    const owner = await store.findUserByAvatarKey(key)
    if (!owner || !owner.avatarFileId) {
      throw new ApiError(404, 'AVATAR_NOT_FOUND', '头像不存在')
    }
    if (typeof resolveAvatarUrl !== 'function') {
      throw new ApiError(404, 'AVATAR_NOT_FOUND', '头像不存在')
    }
    let url = null
    try {
      url = await resolveAvatarUrl(owner.avatarFileId)
    } catch (error) {
      if (logger.warn) logger.warn('orbit-api: temp url failed', error && error.message)
    }
    if (!url) {
      throw new ApiError(404, 'AVATAR_NOT_FOUND', '头像不存在')
    }
    return redirect(url)
  }

  function aiUsage(user) {
    const sameDay = user.aiDayKey === beijingDayKey(now())
    return {
      planUsed: sameDay ? Math.max(0, Number(user.aiPlanCount) || 0) : 0,
      reviewUsed: sameDay ? Math.max(0, Number(user.aiReviewCount) || 0) : 0,
      chatUsed: sameDay ? Math.max(0, Number(user.aiChatCount) || 0) : 0,
    }
  }

  async function consumeAiQuota(user, kind) {
    const counters = { plan: 'aiPlanCount', review: 'aiReviewCount', chat: 'aiChatCount' }
    const field = counters[kind]
    if (!field) throw new Error('unknown AI quota kind: ' + kind)
    const timestamp = now()
    const dayKey = beijingDayKey(timestamp)
    const sameDay = user.aiDayKey === dayKey
    const planCount = sameDay ? Math.max(0, Number(user.aiPlanCount) || 0) : 0
    const reviewCount = sameDay ? Math.max(0, Number(user.aiReviewCount) || 0) : 0
    const chatCount = sameDay ? Math.max(0, Number(user.aiChatCount) || 0) : 0
    const counts = { plan: planCount, review: reviewCount, chat: chatCount }
    const current = counts[kind]
    const limit = Math.max(1, Number(aiLimits[kind]) || AI_LIMITS[kind])
    if (current >= limit) {
      throw new ApiError(
        429,
        'AI_QUOTA_EXCEEDED',
        '今日 AI 次数已用完，明天 00:00 重置',
      )
    }

    const patch = {
      aiDayKey: dayKey,
      aiPlanCount: planCount,
      aiReviewCount: reviewCount,
      aiChatCount: chatCount,
    }
    patch[field] = current + 1
    await store.updateUser(user._id, patch)
  }

  async function aiStatus(headers) {
    const { user } = await requireSession(headers)
    const usage = aiUsage(user)
    return json(200, {
      configured: Boolean(ai),
      planPromptVersion: ai ? ai.planPromptVersion : null,
      reviewPromptVersion: ai ? ai.reviewPromptVersion : null,
      chatPromptVersion: ai ? ai.chatPromptVersion : null,
      timeoutSeconds: Math.round(AI_TIMEOUT_MS / 1000),
      limits: {
        plan: Math.max(1, Number(aiLimits.plan) || AI_LIMITS.plan),
        review: Math.max(1, Number(aiLimits.review) || AI_LIMITS.review),
        chat: Math.max(1, Number(aiLimits.chat) || AI_LIMITS.chat),
      },
      usage,
      dayKey: beijingDayKey(now()),
    })
  }

  async function aiPlan(event, headers) {
    const { user } = await requireSession(headers)
    if (!ai) {
      throw new ApiError(503, 'AI_NOT_CONFIGURED', 'AI 服务尚未配置')
    }
    let input
    try {
      input = validatePlanInput(parseJsonBody(event), now())
    } catch (error) {
      if (error instanceof ApiError) throw error
      throw new ApiError(400, 'AI_INPUT_INVALID', '请检查学习目标、目标日期和每日时长')
    }

    await consumeAiQuota(user, 'plan')
    const requestId = randomHex(8)
    const started = now()
    const plan = await ai.plan(input, {
      userId: 'orbit_' + Number(user.userId),
      requestId,
    })
    if (logger.log) {
      logger.log('orbit-api: ai plan ' + requestId + ' ' + (now() - started) + 'ms')
    }
    return json(200, {
      requestId,
      generatedAt: now(),
      promptVersion: ai.planPromptVersion,
      plan,
    })
  }

  async function aiReview(event, headers) {
    const { user } = await requireSession(headers)
    if (!ai) {
      throw new ApiError(503, 'AI_NOT_CONFIGURED', 'AI 服务尚未配置')
    }
    let input
    try {
      input = validateReviewInput(parseJsonBody(event), now())
    } catch (error) {
      if (error instanceof ApiError) throw error
      throw new ApiError(400, 'AI_INPUT_INVALID', '本周统计数据不完整，请稍后重试')
    }

    await consumeAiQuota(user, 'review')
    const requestId = randomHex(8)
    const started = now()
    const review = await ai.review(input, {
      userId: 'orbit_' + Number(user.userId),
      requestId,
    })
    if (logger.log) {
      logger.log('orbit-api: ai review ' + requestId + ' ' + (now() - started) + 'ms')
    }
    return json(200, {
      requestId,
      generatedAt: now(),
      promptVersion: ai.reviewPromptVersion,
      review,
    })
  }

  async function aiChat(event, headers) {
    const { user } = await requireSession(headers)
    if (!ai) {
      throw new ApiError(503, 'AI_NOT_CONFIGURED', 'AI 服务尚未配置')
    }
    let input
    try {
      input = validateChatInput(parseJsonBody(event), now())
    } catch (error) {
      if (error instanceof ApiError) throw error
      throw new ApiError(400, 'AI_INPUT_INVALID', '请输入 1-600 字的学习问题')
    }

    await consumeAiQuota(user, 'chat')
    const requestId = randomHex(8)
    const started = now()
    const reply = await ai.chat(input, {
      userId: 'orbit_' + Number(user.userId),
      requestId,
    })
    if (logger.log) {
      logger.log('orbit-api: ai chat ' + requestId + ' ' + (now() - started) + 'ms')
    }
    return json(200, {
      requestId,
      generatedAt: now(),
      promptVersion: ai.chatPromptVersion,
      reply,
    })
  }

  return async function handle(event) {
    const started = now()
    const headers = headerMap(event && event.headers)
    const method = String((event && (event.httpMethod || event.method)) || 'GET').toUpperCase()
    const path = normalizePath(rawPath(event), urlPrefix)
    let response

    try {
      if (method === 'OPTIONS') {
        response = json(200, {})
      } else if (method === 'GET' && path === '/api/health') {
        response = json(200, {
          status: 'ok',
          backend: 'unicloud',
          time: now(),
        })
      } else if (method === 'POST' && path === '/api/auth/register') {
        response = await register(event)
      } else if (method === 'POST' && path === '/api/auth/login') {
        response = await login(event)
      } else if (method === 'POST' && path === '/api/auth/logout') {
        response = await logout(event, headers)
      } else if (method === 'GET' && path === '/api/me') {
        response = await profile(event, headers)
      } else if (method === 'PATCH' && path === '/api/me') {
        response = await updateNickname(event, headers)
      } else if (method === 'POST' && path === '/api/me/avatar') {
        response = await uploadUserAvatar(event, headers)
      } else if (method === 'GET' && path === '/api/sync') {
        response = await pullSnapshot(event, headers)
      } else if (method === 'PUT' && path === '/api/sync') {
        response = await pushSnapshot(event, headers)
      } else if (method === 'GET' && path === '/api/ai/status') {
        response = await aiStatus(headers)
      } else if (method === 'POST' && path === '/api/ai/plan') {
        response = await aiPlan(event, headers)
      } else if (method === 'POST' && path === '/api/ai/review') {
        response = await aiReview(event, headers)
      } else if (method === 'POST' && path === '/api/ai/chat') {
        response = await aiChat(event, headers)
      } else if (method === 'GET' && path.startsWith('/media/avatars/')) {
        response = await serveAvatar(path)
      } else {
        throw new ApiError(404, 'NOT_FOUND', '接口不存在')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        response = json(error.status, { code: error.code, message: error.message })
      } else {
        if (logger.error) logger.error('orbit-api: unhandled request failure', error)
        response = json(500, { code: 'INTERNAL_ERROR', message: '服务器暂时不可用' })
      }
    }

    if (logger.log) {
      logger.log(method + ' ' + path + ' -> ' + response.statusCode + ' ' + (now() - started) + 'ms')
    }
    return response
  }
}

module.exports = { createApp, emptySnapshot, publicUser, TOKEN_TTL_MS, MAX_AVATAR_BYTES }
