'use strict'

const { ApiError } = require('./errors')
const {
  hashPassword,
  verifyPassword,
  randomHex,
  sha256Hex,
} = require('./crypto')
const {
  headerMap,
  parseJsonBody,
  parseAuthorization,
  json,
} = require('./http')

const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000
const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,20}$/
const SUBMIT_TYPES = new Set(['MANUAL', 'TIMEOUT', 'TAB_SWITCH'])
const BEIJING_OFFSET_MS = 8 * 60 * 60 * 1000

// A real scrypt hash keeps the unknown-username path as slow as a wrong password.
const DUMMY_PASSWORD_HASH = hashPassword('zhijiexing-dummy-password')

function ok(data, message = '操作成功') {
  return json(200, { code: 200, message, data: data == null ? null : data })
}

function fail(status, code, message) {
  return json(status, { code, message, data: null })
}

function text(value) {
  return value == null ? '' : String(value)
}

function digits(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function normalizeAnswer(value) {
  return text(value).toUpperCase().replace(/[^A-Z]/g, '').split('').sort().join('')
}

function pad(value, length) {
  return String(value).padStart(length, '0')
}

/** 服务端按北京时间生成展示用时间，避免云函数容器时区漂移。 */
function beijingDate(stamp) {
  const date = new Date(stamp + BEIJING_OFFSET_MS)
  return {
    year: date.getUTCFullYear(),
    month: date.getUTCMonth() + 1,
    day: date.getUTCDate(),
    hour: date.getUTCHours(),
    minute: date.getUTCMinutes(),
    second: date.getUTCSeconds(),
  }
}

function formatDateTime(stamp) {
  const parts = beijingDate(stamp)
  return parts.year + '-' + pad(parts.month, 2) + '-' + pad(parts.day, 2) +
    ' ' + pad(parts.hour, 2) + ':' + pad(parts.minute, 2) + ':' + pad(parts.second, 2)
}

function formatDate(stamp) {
  const parts = beijingDate(stamp)
  return parts.year + '-' + pad(parts.month, 2) + '-' + pad(parts.day, 2)
}

function dateKey(stamp) {
  const parts = beijingDate(stamp)
  return '' + parts.year + pad(parts.month, 2) + pad(parts.day, 2)
}

function addYears(stamp, years) {
  const parts = beijingDate(stamp)
  const date = new Date(Date.UTC(parts.year + years, parts.month - 1, parts.day))
  const year = date.getUTCFullYear()
  const month = pad(date.getUTCMonth() + 1, 2)
  const day = pad(date.getUTCDate(), 2)
  return year + '-' + month + '-' + day
}

function parseSubmitType(value, fallback = 'MANUAL') {
  const type = text(value).toUpperCase()
  return SUBMIT_TYPES.has(type) ? type : fallback
}

function isDuplicateError(error) {
  const message = [
    error && error.code,
    error && error.errCode,
    error && error.message,
    error && error.errMsg,
  ].filter(Boolean).join(' ').toLowerCase()
  return message.includes('duplicate') || message.includes('e11000') ||
    message.includes('already exists') || message.includes('重复')
}

function routeAttempt(path) {
  const match = /^\/api\/exam\/attempts\/([^/]+)(?:\/(answer|tab-switch|submit|result|review))?$/.exec(path)
  if (!match) return null
  return { attemptId: decodeURIComponent(match[1]), action: match[2] || '' }
}

/**
 * 智解星考试接口。挂在 /exam 前缀下，与 Orbit 自身的 /api 路由完全隔离。
 * 响应统一为 { code, message, data }，与参考 Spring 后端保持一致。
 */
function createExam({ store, now = Date.now, logger = console }) {
  if (!store) throw new Error('store is required')

  async function issueToken(userId) {
    const stamp = now()
    try {
      await store.purgeExpiredTokens(stamp)
    } catch (error) {
      if (logger.warn) logger.warn('exam-api: token purge failed', error && error.message)
    }
    const token = randomHex(32)
    await store.createToken({
      tokenHash: sha256Hex(token),
      userId,
      createdAt: stamp,
      expiresAt: stamp + TOKEN_TTL_MS,
    })
    return token
  }

  async function requireSession(headers) {
    const token = parseAuthorization(headers)
    if (!token) throw new ApiError(401, 401, '登录状态已失效，请重新登录')
    const session = await store.findSession(sha256Hex(token))
    if (!session || digits(session.expiresAt) <= now()) {
      if (session) {
        try {
          await store.deleteToken(session._id)
        } catch (error) {
          // An expired token is unusable even if the cleanup failed.
        }
      }
      throw new ApiError(401, 401, '登录状态已失效，请重新登录')
    }
    const user = await store.findUserById(session.userId)
    if (!user) throw new ApiError(401, 401, '登录状态已失效，请重新登录')
    return { user, session }
  }

  function publicPaper(paper, attempts) {
    const submitted = attempts.filter((item) => item.paperId === paper.paperId)
    let bestScore = 0
    for (const item of submitted) bestScore = Math.max(bestScore, digits(item.score))
    return {
      id: paper.paperId,
      name: paper.name,
      description: paper.description || '',
      totalScore: digits(paper.totalScore),
      passScore: digits(paper.passScore),
      duration: digits(paper.duration),
      questionCount: Array.isArray(paper.questions) ? paper.questions.length : 0,
      categoryName: paper.categoryName || '',
      levelName: paper.levelName || '',
      attemptCount: submitted.length,
      bestScore,
    }
  }

  function attemptResult(attempt, paper, certificateGenerated) {
    return {
      attemptId: attempt.attemptId,
      score: digits(attempt.score),
      totalScore: digits(paper.totalScore),
      passScore: digits(paper.passScore),
      correctCount: digits(attempt.correctCount),
      totalQuestions: Array.isArray(paper.questions) ? paper.questions.length : 0,
      passed: Boolean(attempt.passed),
      usedTime: digits(attempt.usedTime),
      submitType: attempt.submitType || 'MANUAL',
      paperId: paper.paperId,
      certificateGenerated: Boolean(certificateGenerated),
    }
  }

  function remainingSeconds(attempt, paper) {
    const limit = digits(paper.duration) * 60
    const elapsed = Math.floor((now() - digits(attempt.startTime)) / 1000)
    return Math.max(0, limit - elapsed)
  }

  function savedAnswer(attempt, questionId) {
    const answers = attempt.answers && typeof attempt.answers === 'object' ? attempt.answers : {}
    return answers[questionId] || null
  }

  function questionForClient(question, attempt) {
    const saved = savedAnswer(attempt, question.id)
    return {
      id: question.id,
      type: question.type,
      content: question.content,
      score: digits(question.score),
      options: Array.isArray(question.options) ? question.options : [],
      myAnswer: saved && text(saved.answer) ? text(saved.answer) : null,
      isFlagged: Boolean(saved && saved.isFlagged),
    }
  }

  function gradePaper(paper, attempt) {
    const questions = Array.isArray(paper.questions) ? paper.questions : []
    let score = 0
    let correctCount = 0
    const graded = questions.map((question) => {
      const saved = savedAnswer(attempt, question.id)
      const answered = saved && text(saved.answer) ? text(saved.answer) : ''
      const correct = normalizeAnswer(answered) === normalizeAnswer(question.correctAnswer) &&
        normalizeAnswer(answered) !== ''
      if (correct) {
        score += digits(question.score)
        correctCount += 1
      }
      return { questionId: question.id, answered, correct }
    })
    return { score, correctCount, graded }
  }

  async function finalizeAttempt(attempt, paper, submitType, certificateEnabled = true) {
    if (attempt.status === '已交卷') {
      return attempt
    }
    const { score, correctCount, graded } = gradePaper(paper, attempt)
    const stamp = now()
    const limit = digits(paper.duration) * 60
    const usedTime = Math.max(0, Math.min(limit, Math.floor((stamp - digits(attempt.startTime)) / 1000)))
    const passed = score >= digits(paper.passScore)
    attempt.status = '已交卷'
    attempt.submitTime = stamp
    attempt.usedTime = usedTime
    attempt.score = score
    attempt.correctCount = correctCount
    attempt.passed = passed
    attempt.submitType = submitType
    attempt.graded = graded
    await store.updateAttempt(attempt._id, {
      status: attempt.status,
      submitTime: attempt.submitTime,
      usedTime: attempt.usedTime,
      score: attempt.score,
      correctCount: attempt.correctCount,
      passed: attempt.passed,
      submitType: attempt.submitType,
      graded: attempt.graded,
      tabSwitchCount: attempt.tabSwitchCount,
    })

    if (certificateEnabled && passed && paper.enableCertificate) {
      await issueCertificate(attempt, paper)
    }
    return attempt
  }

  async function issueCertificate(attempt, paper) {
    const existing = await store.findCertificateByAttempt(attempt.attemptId)
    if (existing) return existing
    const stamp = now()
    const day = dateKey(stamp)
    let sequence = 1
    try {
      sequence = (await store.countCertificatesByDay(day)) + 1
    } catch (error) {
      sequence = 1
    }
    const certificate = {
      certificateId: 'cert-' + randomHex(8),
      attemptId: attempt.attemptId,
      userId: attempt.userId,
      paperId: paper.paperId,
      name: paper.certificateName || paper.name,
      certificateNo: 'CERT-' + day + '-' + pad(sequence, 6),
      certDay: day,
      issueDate: formatDate(stamp),
      expiryDate: addYears(stamp, 2),
      status: '有效',
      createdAt: stamp,
    }
    try {
      await store.createCertificate(certificate)
      return certificate
    } catch (error) {
      if (isDuplicateError(error)) {
        return store.findCertificateByAttempt(attempt.attemptId)
      }
      throw error
    }
  }

  async function loadPaperForAttempt(attempt) {
    const paper = await store.getPaper(attempt.paperId)
    if (!paper) throw new ApiError(400, 400, '试卷不存在或已下架')
    return paper
  }

  async function login(event) {
    const body = parseJsonBody(event)
    const username = text(body.username).trim()
    const password = text(body.password)
    if (!username) throw new ApiError(400, 400, '请输入用户名')
    if (!password) throw new ApiError(400, 400, '请输入密码')

    const user = await store.findUserByUsername(username)
    const matches = verifyPassword(password, user ? user.passwordHash : DUMMY_PASSWORD_HASH)
    if (!user || !matches) {
      throw new ApiError(400, 400, '用户名或密码错误')
    }
    if (user.status && user.status !== '正常') {
      throw new ApiError(400, 400, '账号已被停用，请联系管理员')
    }
    const token = await issueToken(user.userId)
    return ok({
      userInfo: { id: text(user.userId), username: user.username, role: user.role || '考生' },
      token,
    })
  }

  async function register(event) {
    const body = parseJsonBody(event)
    const username = text(body.username).trim()
    const password = text(body.password)
    const confirmPassword = text(body.confirmPassword)
    if (!username) throw new ApiError(400, 400, '请输入用户名')
    if (!USERNAME_PATTERN.test(username)) {
      throw new ApiError(400, 400, '用户名需为 3-20 位字母、数字或下划线')
    }
    if (!password) throw new ApiError(400, 400, '请输入密码')
    if (password.length < 6 || password.length > 32) {
      throw new ApiError(400, 400, '密码长度需为 6-32 位')
    }
    if (confirmPassword && confirmPassword !== password) {
      throw new ApiError(400, 400, '两次输入的密码不一致')
    }
    if (await store.findUserByUsername(username)) {
      throw new ApiError(400, 400, '这个用户名已经被注册')
    }
    const stamp = now()
    const user = {
      userId: 'u' + randomHex(7),
      username,
      passwordHash: hashPassword(password),
      role: '考生',
      status: '正常',
      createdAt: stamp,
    }
    try {
      await store.createUser(user)
    } catch (error) {
      if (isDuplicateError(error) || (await store.findUserByUsername(username))) {
        throw new ApiError(400, 400, '这个用户名已经被注册')
      }
      throw error
    }
    return ok({ id: user.userId, username: user.username }, '注册成功')
  }

  async function papers(headers) {
    const { user } = await requireSession(headers)
    const allPapers = await store.listPapers()
    const attempts = await store.listSubmittedAttempts(user.userId)
    return ok(allPapers.map((paper) => publicPaper(paper, attempts)))
  }

  async function startAttempt(event, headers) {
    const { user } = await requireSession(headers)
    const body = parseJsonBody(event)
    const paperId = text(body.paperId).trim()
    if (!paperId) throw new ApiError(400, 400, '缺少试卷编号')
    const paper = await store.getPaper(paperId)
    if (!paper) throw new ApiError(400, 400, '试卷不存在或已下架')

    let attempt = await store.findActiveAttempt(user.userId, paperId)
    if (attempt && remainingSeconds(attempt, paper) <= 0) {
      await finalizeAttempt(attempt, paper, 'TIMEOUT', true)
      attempt = null
    }
    if (!attempt) {
      const stamp = now()
      attempt = {
        attemptId: 'a' + randomHex(8),
        userId: user.userId,
        paperId: paper.paperId,
        paperName: paper.name,
        startTime: stamp,
        submitTime: null,
        status: '进行中',
        answers: {},
        tabSwitchCount: 0,
        submitType: '',
        score: 0,
        correctCount: 0,
        passed: false,
        usedTime: 0,
      }
      await store.createAttempt(attempt)
    }

    const questions = Array.isArray(paper.questions) ? paper.questions : []
    return ok({
      attemptId: attempt.attemptId,
      paperName: paper.name,
      remainingSeconds: remainingSeconds(attempt, paper),
      totalScore: digits(paper.totalScore),
      passScore: digits(paper.passScore),
      duration: digits(paper.duration),
      questions: questions.map((question) => questionForClient(question, attempt)),
    })
  }

  async function saveAnswer(event, headers, attemptId) {
    const { user } = await requireSession(headers)
    const attempt = await store.getAttempt(attemptId)
    if (!attempt || attempt.userId !== user.userId) {
      throw new ApiError(400, 400, '考试记录不存在')
    }
    if (attempt.status === '已交卷') throw new ApiError(400, 400, '本场考试已结束')
    const paper = await loadPaperForAttempt(attempt)
    if (remainingSeconds(attempt, paper) <= 0) {
      await finalizeAttempt(attempt, paper, 'TIMEOUT', true)
      throw new ApiError(400, 400, '考试时间已结束，系统已自动交卷')
    }

    const body = parseJsonBody(event)
    const questionId = text(body.questionId).trim()
    const answer = text(body.answer)
    const flagged = Boolean(body.isFlagged)
    const question = (paper.questions || []).find((item) => item.id === questionId)
    if (!question) throw new ApiError(400, 400, '题目不属于本场考试')
    if (answer.length > 16) throw new ApiError(400, 400, '答案格式不正确')

    const answers = attempt.answers && typeof attempt.answers === 'object' ? attempt.answers : {}
    answers[questionId] = { answer, isFlagged: flagged, updatedAt: now() }
    attempt.answers = answers
    await store.updateAttempt(attempt._id, { answers })
    return ok({ saved: true })
  }

  async function reportTabSwitch(event, headers, attemptId) {
    const { user } = await requireSession(headers)
    const attempt = await store.getAttempt(attemptId)
    if (!attempt || attempt.userId !== user.userId) {
      throw new ApiError(400, 400, '考试记录不存在')
    }
    if (attempt.status === '已交卷') {
      throw new ApiError(400, 400, '本场考试已结束')
    }
    const paper = await loadPaperForAttempt(attempt)
    const limit = digits(paper.tabSwitchLimit) > 0 ? digits(paper.tabSwitchLimit) : 3
    if (remainingSeconds(attempt, paper) <= 0) {
      await finalizeAttempt(attempt, paper, 'TIMEOUT', true)
      return ok({ tabSwitchCount: digits(attempt.tabSwitchCount), tabSwitchLimit: limit, shouldAutoSubmit: false })
    }
    const body = parseJsonBody(event)
    const reported = Math.max(0, Math.floor(digits(body.tabSwitchCount)))
    const count = Math.max(digits(attempt.tabSwitchCount), reported)
    attempt.tabSwitchCount = count
    await store.updateAttempt(attempt._id, { tabSwitchCount: count })
    // 达到上限即交卷：第 3 次切屏（上限 3 次）就下发自动交卷。
    return ok({
      tabSwitchCount: count,
      tabSwitchLimit: limit,
      shouldAutoSubmit: count >= limit,
    })
  }

  async function submitAttempt(event, headers, attemptId) {
    const { user } = await requireSession(headers)
    const attempt = await store.getAttempt(attemptId)
    if (!attempt || attempt.userId !== user.userId) {
      throw new ApiError(400, 400, '考试记录不存在')
    }
    const paper = await loadPaperForAttempt(attempt)

    if (attempt.status !== '已交卷') {
      const body = parseJsonBody(event)
      let type = parseSubmitType(body.submitType)
      const reported = Math.max(0, Math.floor(digits(body.tabSwitchCount)))
      const count = Math.max(digits(attempt.tabSwitchCount), reported)
      attempt.tabSwitchCount = count
      const limit = digits(paper.tabSwitchLimit) > 0 ? digits(paper.tabSwitchLimit) : 3
      if (count >= limit) {
        type = 'TAB_SWITCH'
      } else if (type !== 'TAB_SWITCH' && remainingSeconds(attempt, paper) <= 0) {
        type = 'TIMEOUT'
      }
      await finalizeAttempt(attempt, paper, type, true)
    }

    const certificate = await store.findCertificateByAttempt(attempt.attemptId)
    return ok(attemptResult(attempt, paper, Boolean(certificate)))
  }

  async function result(headers, attemptId) {
    const { user } = await requireSession(headers)
    const attempt = await store.getAttempt(attemptId)
    if (!attempt || attempt.userId !== user.userId) {
      throw new ApiError(400, 400, '考试记录不存在')
    }
    if (attempt.status !== '已交卷') throw new ApiError(400, 400, '本场考试尚未交卷')
    const paper = await loadPaperForAttempt(attempt)
    const certificate = await store.findCertificateByAttempt(attempt.attemptId)
    return ok(attemptResult(attempt, paper, Boolean(certificate)))
  }

  async function review(headers, attemptId) {
    const { user } = await requireSession(headers)
    const attempt = await store.getAttempt(attemptId)
    if (!attempt || attempt.userId !== user.userId) {
      throw new ApiError(400, 400, '考试记录不存在')
    }
    if (attempt.status !== '已交卷') throw new ApiError(400, 400, '交卷后才能查看解析')
    const paper = await loadPaperForAttempt(attempt)
    const graded = new Map((attempt.graded || []).map((item) => [item.questionId, item]))
    const questions = (paper.questions || []).map((question) => {
      const item = graded.get(question.id) || {}
      const answered = item.answered != null ? item.answered : ''
      const correct = Boolean(item.correct)
      return {
        questionId: question.id,
        type: question.type,
        content: question.content,
        correctAnswer: question.correctAnswer,
        userAnswer: answered || null,
        isCorrect: correct,
        earnedScore: correct ? digits(question.score) : 0,
        maxScore: digits(question.score),
        analysis: question.analysis || '',
        options: Array.isArray(question.options) ? question.options : [],
      }
    })
    return ok({
      attemptId: attempt.attemptId,
      paperName: paper.name,
      score: digits(attempt.score),
      totalScore: digits(paper.totalScore),
      isPassed: attempt.passed ? 1 : 0,
      questions,
    })
  }

  async function records(event, headers) {
    const { user } = await requireSession(headers)
    const query = (event && event.queryStringParameters) || {}
    let page = Math.max(1, Math.floor(digits(query.page) || 1))
    let size = Math.max(1, Math.floor(digits(query.size) || 100))
    size = Math.min(size, 100)
    const all = await store.listSubmittedAttempts(user.userId)
    all.sort((left, right) => digits(right.startTime) - digits(left.startTime))
    const offset = (page - 1) * size
    const slice = all.slice(offset, offset + size)
    return ok({
      records: slice.map((attempt) => ({
        id: attempt.attemptId,
        startTime: formatDateTime(digits(attempt.startTime)),
        score: digits(attempt.score),
        passed: Boolean(attempt.passed),
        usedTime: digits(attempt.usedTime),
        submitType: attempt.submitType || 'MANUAL',
        paperId: attempt.paperId,
        paperName: attempt.paperName || '',
      })),
      total: all.length,
      page,
      size,
    })
  }

  async function certificates(headers) {
    const { user } = await requireSession(headers)
    const list = await store.listCertificates(user.userId)
    list.sort((left, right) => digits(right.createdAt) - digits(left.createdAt))
    return ok(list.map((item) => ({
      id: item.certificateId,
      certificateNo: item.certificateNo,
      name: item.name,
      issueDate: item.issueDate,
      expiryDate: item.expiryDate,
      status: item.status,
    })))
  }

  return async function handle(event, path) {
    const started = now()
    const headers = headerMap(event && event.headers)
    const method = text(event && (event.httpMethod || event.method) || 'GET').toUpperCase()
    const cleanPath = path && path !== '/' ? path.replace(/\/+$/, '') : '/'

    try {
      if (store.ensureSeed) await store.ensureSeed()
      if (method === 'OPTIONS') {
        return ok({})
      }
      if (method === 'GET' && cleanPath === '/api/health') {
        return ok({ status: 'ok', backend: 'unicloud', time: now() })
      }
      if (method === 'POST' && cleanPath === '/api/auth/login') {
        return await login(event)
      }
      if (method === 'POST' && cleanPath === '/api/auth/register') {
        return await register(event)
      }
      if (method === 'GET' && cleanPath === '/api/exam/papers') {
        return await papers(headers)
      }
      if (method === 'GET' && cleanPath === '/api/exam/attempts') {
        return await records(event, headers)
      }
      if (method === 'GET' && cleanPath === '/api/exam/certificates') {
        return await certificates(headers)
      }
      if (method === 'POST' && cleanPath === '/api/exam/attempts') {
        return await startAttempt(event, headers)
      }

      const route = routeAttempt(cleanPath)
      if (route) {
        if (method === 'PUT' && route.action === 'answer') {
          return await saveAnswer(event, headers, route.attemptId)
        }
        if (method === 'PUT' && route.action === 'tab-switch') {
          return await reportTabSwitch(event, headers, route.attemptId)
        }
        if (method === 'POST' && route.action === 'submit') {
          return await submitAttempt(event, headers, route.attemptId)
        }
        if (method === 'GET' && route.action === 'result') {
          return await result(headers, route.attemptId)
        }
        if (method === 'GET' && route.action === 'review') {
          return await review(headers, route.attemptId)
        }
      }
      return fail(404, 404, '接口不存在')
    } catch (error) {
      if (error instanceof ApiError) {
        return fail(error.status === 401 ? 401 : error.status, error.code, error.message)
      }
      if (logger.error) logger.error('exam-api: unhandled request failure', error)
      return fail(500, 500, '服务器暂时不可用，请稍后再试')
    } finally {
      if (logger.log) {
        logger.log(method + ' ' + cleanPath + ' -> ' + (now() - started) + 'ms')
      }
    }
  }
}

module.exports = {
  createExam,
  normalizeAnswer,
  formatDateTime,
  dateKey,
  TOKEN_TTL_MS,
}
