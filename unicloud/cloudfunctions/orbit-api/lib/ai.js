'use strict'

const { ApiError } = require('./errors')
const { CozeError } = require('./coze')
const {
  PLAN_PROMPT_VERSION,
  REVIEW_PROMPT_VERSION,
  CHAT_PROMPT_VERSION,
  buildPlanMessage,
  buildPlanRepairMessage,
  buildReviewMessage,
  buildReviewRepairMessage,
  buildChatMessage,
} = require('./prompts')

const AI_LIMITS = Object.freeze({ plan: 6, review: 4, chat: 20 })
const AI_TIMEOUT_MS = 25 * 1000
const BEIJING_OFFSET_MS = 8 * 60 * 60 * 1000
const MILLIS_PER_DAY = 24 * 60 * 60 * 1000
const DAY_MS = MILLIS_PER_DAY

class AiValidationError extends Error {
  constructor(message) {
    super(message)
    this.name = 'AiValidationError'
  }
}

function beijingDayKey(timestamp) {
  return new Date(Number(timestamp) + BEIJING_OFFSET_MS).toISOString().slice(0, 10)
}

function beijingEpochDay(timestamp) {
  return Math.floor((Number(timestamp) + BEIJING_OFFSET_MS) / MILLIS_PER_DAY)
}

function isoDate(epochDay) {
  return new Date(Number(epochDay) * MILLIS_PER_DAY).toISOString().slice(0, 10)
}

function weekStartEpochDay(epochDay) {
  const weekday = new Date(Number(epochDay) * MILLIS_PER_DAY).getUTCDay()
  return Number(epochDay) - ((weekday + 6) % 7)
}

function cleanText(value) {
  return String(value == null ? '' : value)
    .replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function readText(value, maxLength, field, options = {}) {
  const text = cleanText(value)
  const required = options.required !== false
  if (!text && required) {
    throw new AiValidationError(field + ' is required')
  }
  if (text.length > maxLength) {
    if (options.truncate) return text.slice(0, maxLength)
    throw new AiValidationError(field + ' exceeds ' + maxLength + ' characters')
  }
  return text
}

function readOptionalText(value, maxLength, field) {
  return readText(value, maxLength, field, { required: false, truncate: true })
}

function readInt(value, min, max, field) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || !Number.isInteger(parsed)) {
    throw new AiValidationError(field + ' must be an integer')
  }
  if (parsed < min || parsed > max) {
    throw new AiValidationError(field + ' is out of range')
  }
  return parsed
}

function readCount(value, field) {
  const parsed = Number(value == null ? 0 : value)
  if (!Number.isFinite(parsed) || parsed < 0) {
    throw new AiValidationError(field + ' must be a non-negative number')
  }
  return Math.floor(parsed)
}

function readProgress(value) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return 0
  const normalized = parsed > 1 ? parsed / 100 : parsed
  return Math.max(0, Math.min(1, normalized))
}

function readPriority(value) {
  const text = cleanText(value).toUpperCase()
  if (text === 'HIGH' || text === '高') return 'HIGH'
  if (text === 'LOW' || text === '低') return 'LOW'
  return 'MEDIUM'
}

function formatRate(completed, planned) {
  if (!planned) return 0
  return Math.round((completed / planned) * 10000) / 10000
}

function requireObject(value, field) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new AiValidationError(field + ' must be an object')
  }
  return value
}

/**
 * Locates the first complete JSON object in a model answer. Coze sometimes
 * wraps JSON in a Markdown fence or adds a sentence before it; we never accept
 * trailing prose as data.
 */
function parseJsonObject(value) {
  let text = String(value == null ? '' : value).replace(/^\uFEFF/, '').trim()
  const fenced = /```(?:json)?\s*([\s\S]*?)```/i.exec(text)
  if (fenced) text = fenced[1].trim()

  const start = text.indexOf('{')
  if (start < 0) throw new AiValidationError('No JSON object found')

  let depth = 0
  let inString = false
  let escaped = false
  for (let index = start; index < text.length; index += 1) {
    const character = text[index]
    if (inString) {
      if (escaped) {
        escaped = false
      } else if (character === '\\') {
        escaped = true
      } else if (character === '"') {
        inString = false
      }
      continue
    }
    if (character === '"') {
      inString = true
    } else if (character === '{') {
      depth += 1
    } else if (character === '}') {
      depth -= 1
      if (depth === 0) {
        const candidate = text.slice(start, index + 1)
        try {
          return JSON.parse(candidate)
        } catch (error) {
          throw new AiValidationError('Malformed JSON: ' + error.message)
        }
      }
    }
  }
  throw new AiValidationError('Incomplete JSON object')
}

function validatePlanInput(body, nowMillis = Date.now()) {
  const today = beijingEpochDay(nowMillis)
  const goal = readText(body.goal || body.goalTitle, 60, 'goal')
  const target = body.targetDateEpochDay == null
    ? today + 56
    : readInt(body.targetDateEpochDay, today, today + 730, 'targetDateEpochDay')
  const dailyMinutes = body.dailyMinutes == null
    ? 45
    : readInt(body.dailyMinutes, 10, 480, 'dailyMinutes')

  return {
    goal,
    targetDateEpochDay: target,
    dailyMinutes,
    currentLevel: readOptionalText(body.currentLevel, 200, 'currentLevel'),
    weakPoints: readOptionalText(body.weakPoints, 200, 'weakPoints'),
    todayEpochDay: today,
    todayLabel: isoDate(today),
    targetDateLabel: isoDate(target),
  }
}

function normalizePlan(payload, input) {
  const root = requireObject(payload, 'plan')
  const today = input.todayEpochDay
  const maxDay = Math.max(0, input.targetDateEpochDay - today)
  const rawStages = root.stages
  if (!Array.isArray(rawStages) || rawStages.length < 1 || rawStages.length > 6) {
    throw new AiValidationError('stages must contain 1-6 items')
  }

  let totalTasks = 0
  let previousEnd = 0
  const stages = rawStages.map((rawStage, stageIndex) => {
    const stage = requireObject(rawStage, 'stage')
    const tasksRaw = stage.tasks
    if (!Array.isArray(tasksRaw) || tasksRaw.length < 1 || tasksRaw.length > 5) {
      throw new AiValidationError('each stage must contain 1-5 tasks')
    }
    totalTasks += tasksRaw.length
    if (totalTasks > 20) {
      throw new AiValidationError('plan contains more than 20 tasks')
    }

    const tasks = tasksRaw.map((rawTask, taskIndex) => {
      const task = requireObject(rawTask, 'task')
      const dayOffset = readInt(
        task.dayOffset,
        0,
        maxDay,
        'task[' + stageIndex + '][' + taskIndex + '].dayOffset',
      )
      return {
        title: readText(task.title, 80, 'task title'),
        dueDateEpochDay: today + dayOffset,
        priority: readPriority(task.priority),
        description: readOptionalText(task.note || task.description, 100, 'task note'),
      }
    })

    const maxTaskDay = tasks.reduce(
      (latest, task) => Math.max(latest, task.dueDateEpochDay - today),
      0,
    )
    let endOffset = readInt(
      stage.endDayOffset == null ? maxTaskDay : stage.endDayOffset,
      0,
      maxDay,
      'stage[' + stageIndex + '].endDayOffset',
    )
    endOffset = Math.max(endOffset, maxTaskDay, previousEnd)
    endOffset = Math.min(endOffset, maxDay)
    previousEnd = endOffset

    return {
      title: readText(stage.title, 60, 'stage title'),
      targetDateEpochDay: today + endOffset,
      tasks,
    }
  })

  const targetDateEpochDay = input.targetDateEpochDay
  return {
    goalTitle: readText(root.goalTitle || input.goal, 60, 'goalTitle'),
    goalDescription: readOptionalText(root.goalDescription, 160, 'goalDescription') ||
      '由 AI 根据你的目标和时间安排生成，可随时修改。',
    targetDateEpochDay,
    stages,
  }
}

function validateReviewInput(body, nowMillis = Date.now()) {
  const today = beijingEpochDay(nowMillis)
  const currentWeekStart = weekStartEpochDay(today)
  const weekStart = body.weekStartEpochDay == null
    ? currentWeekStart
    : readInt(body.weekStartEpochDay, currentWeekStart - 7, today, 'weekStartEpochDay')

  const planned = readCount(body.planned, 'planned')
  const completed = Math.min(readCount(body.completed, 'completed'), planned)
  const studyPlanned = Math.min(readCount(body.studyPlanned, 'studyPlanned'), planned)
  const studyCompleted = Math.min(
    readCount(body.studyCompleted, 'studyCompleted'),
    studyPlanned,
  )
  const habitPossible = readCount(body.habitPossible, 'habitPossible')
  const habitCheckIns = Math.min(
    readCount(body.habitCheckIns, 'habitCheckIns'),
    habitPossible,
  )
  const xp = readCount(body.xp, 'xp')
  const rawGoals = Array.isArray(body.goals) ? body.goals.slice(0, 8) : []
  const goals = rawGoals.map((rawGoal, index) => {
    const goal = requireObject(rawGoal, 'goal')
    return {
      title: readText(goal.title, 60, 'goal[' + index + '].title'),
      progress: readProgress(goal.progress),
      stageCount: readCount(goal.stageCount, 'goal stageCount'),
      completedStages: readCount(goal.completedStages, 'goal completedStages'),
    }
  })

  if (planned === 0 && habitPossible === 0 && xp === 0 && goals.length === 0) {
    throw new ApiError(400, 'AI_NO_DATA', '本周还没有可复盘的数据，先完成一个任务或打卡')
  }

  return {
    weekStartEpochDay: weekStart,
    weekStartLabel: isoDate(weekStart),
    todayLabel: isoDate(today),
    planned,
    completed,
    completionPercent: Math.round(formatRate(completed, planned) * 100),
    studyPlanned,
    studyCompleted,
    habitCheckIns,
    habitPossible,
    xp,
    goals,
  }
}

function normalizeReview(payload) {
  const root = requireObject(payload, 'review')
  const winsRaw = Array.isArray(root.wins) ? root.wins : []
  const risksRaw = Array.isArray(root.risks) ? root.risks : []
  const nextRaw = Array.isArray(root.nextWeek) ? root.nextWeek : []

  const wins = winsRaw
    .slice(0, 4)
    .map((item, index) => readOptionalText(item, 80, 'wins[' + index + ']'))
    .filter(Boolean)
  if (wins.length < 1) {
    throw new AiValidationError('wins must contain at least one item')
  }

  const risks = risksRaw
    .slice(0, 3)
    .map((item, index) => readOptionalText(item, 80, 'risks[' + index + ']'))
    .filter(Boolean)

  const nextWeek = nextRaw.slice(0, 3).map((item, index) => {
    if (typeof item === 'string') {
      return {
        title: readText(item, 80, 'nextWeek[' + index + ']', { truncate: true }),
        reason: '',
      }
    }
    const entry = requireObject(item, 'nextWeek[' + index + ']')
    return {
      title: readText(entry.title, 80, 'nextWeek title'),
      reason: readOptionalText(entry.reason, 100, 'nextWeek reason'),
    }
  })
  if (nextWeek.length < 1) {
    throw new AiValidationError('nextWeek must contain at least one item')
  }

  return {
    headline: readText(root.headline, 40, 'headline'),
    summary: readText(root.summary, 240, 'summary'),
    wins,
    risks,
    nextWeek,
  }
}

/**
 * A chat turn is intentionally looser than plan/review: plain text out, a
 * bounded transcript in, and only aggregate numbers about the learner.
 */
function validateChatInput(body, nowMillis = Date.now()) {
  const message = readText(body.message, 600, 'message')

  const rawHistory = Array.isArray(body.history) ? body.history.slice(-8) : []
  const history = []
  let historyChars = 0
  for (const rawTurn of rawHistory) {
    if (!rawTurn || typeof rawTurn !== 'object' || Array.isArray(rawTurn)) continue
    const role = cleanText(rawTurn.role).toLowerCase() === 'assistant' ? 'assistant' : 'user'
    const content = readOptionalText(rawTurn.content, 700, 'history content')
    if (!content) continue
    if (historyChars + content.length > 2400) break
    history.push({ role, content })
    historyChars += content.length
  }

  const rawContext = body.context &&
    typeof body.context === 'object' &&
    !Array.isArray(body.context)
    ? body.context
    : {}
  const rawGoals = Array.isArray(rawContext.goals) ? rawContext.goals.slice(0, 5) : []
  const goals = rawGoals.map((rawGoal, index) => {
    const goal = requireObject(rawGoal, 'goal[' + index + ']')
    return {
      title: readText(goal.title, 60, 'goal title'),
      progress: readProgress(goal.progress),
    }
  })

  const todayPlanned = readCount(rawContext.todayPlanned, 'todayPlanned')
  const habitTodayTotal = readCount(rawContext.habitTodayTotal, 'habitTodayTotal')

  return {
    todayLabel: isoDate(beijingEpochDay(nowMillis)),
    message,
    history,
    todayPlanned,
    todayCompleted: Math.min(
      readCount(rawContext.todayCompleted, 'todayCompleted'),
      todayPlanned,
    ),
    pendingCount: readCount(rawContext.pendingCount, 'pendingCount'),
    completedCount: readCount(rawContext.completedCount, 'completedCount'),
    habitTodayChecked: Math.min(
      readCount(rawContext.habitTodayChecked, 'habitTodayChecked'),
      habitTodayTotal,
    ),
    habitTodayTotal,
    totalXp: readCount(rawContext.totalXp, 'totalXp'),
    thisWeekXp: readCount(rawContext.thisWeekXp, 'thisWeekXp'),
    goals,
  }
}

function normalizeChatReply(value) {
  let reply = String(value == null ? '' : value)
    .replace(/\r\n/g, '\n')
    .replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g, ' ')
    .trim()

  const fenced = /^```(?:markdown|md|text)?\s*\n?([\s\S]*?)\n?```$/i.exec(reply)
  if (fenced) {
    reply = fenced[1].trim()
  } else if (reply.startsWith('```')) {
    const lineBreak = reply.indexOf('\n')
    if (lineBreak > -1) reply = reply.slice(lineBreak + 1).trim()
    if (reply.endsWith('```')) reply = reply.slice(0, -3).trim()
  }
  reply = reply.replace(/^助手[：:]\s*/, '').trim()

  if (!reply) {
    throw new AiValidationError('Chat reply is empty')
  }
  if (reply.length > 1200) {
    reply = reply.slice(0, 1199).trimEnd() + '…'
  }
  return reply
}

function mapCozeFailure(error, logger) {
  if (error instanceof ApiError) return error
  if (logger && logger.warn) {
    logger.warn(
      'orbit-api: Coze failure',
      error && error.code ? error.code : 'UNKNOWN',
      error && error.status ? error.status : '',
    )
  }
  if (error instanceof CozeError) {
    if (error.code === 'COZE_TIMEOUT') {
      return new ApiError(504, 'AI_TIMEOUT', 'AI 响应超时，请稍后重试')
    }
    if (error.code === 'COZE_BUSY') {
      return new ApiError(503, 'AI_BUSY', 'AI 当前繁忙，请稍后重试')
    }
    if (error.code === 'COZE_AUTH' || error.code === 'COZE_CONFIG') {
      return new ApiError(502, 'AI_UNAVAILABLE', 'AI 服务配置有误，请联系管理员')
    }
  }
  return new ApiError(502, 'AI_UNAVAILABLE', 'AI 服务暂时不可用，请稍后重试')
}

function createAiService({
  coze,
  now = Date.now,
  timeoutMs = AI_TIMEOUT_MS,
  logger = console,
} = {}) {
  if (!coze || typeof coze.chat !== 'function') {
    throw new Error('coze client with chat() is required')
  }

  async function runWithRepair({ initialMessage, repairMessage, normalize, userId }) {
    const started = now()
    let text
    try {
      text = await coze.chat([{ role: 'user', content: initialMessage }], {
        userId,
        timeoutMs,
      })
    } catch (error) {
      throw mapCozeFailure(error, logger)
    }

    try {
      return normalize(parseJsonObject(text))
    } catch (firstError) {
      const remaining = timeoutMs - (now() - started)
      if (remaining < 3000) {
        throw new ApiError(502, 'AI_INVALID_OUTPUT', 'AI 返回内容不符合要求，请重试')
      }
      try {
        text = await coze.chat(
          [{ role: 'user', content: repairMessage(text, firstError.message) }],
          { userId, timeoutMs: remaining },
        )
      } catch (error) {
        throw mapCozeFailure(error, logger)
      }
      try {
        return normalize(parseJsonObject(text))
      } catch (secondError) {
        if (logger && logger.warn) {
          logger.warn('orbit-api: AI output validation failed', secondError.message)
        }
        throw new ApiError(502, 'AI_INVALID_OUTPUT', 'AI 返回内容不符合要求，请重试')
      }
    }
  }

  return {
    planPromptVersion: PLAN_PROMPT_VERSION,
    reviewPromptVersion: REVIEW_PROMPT_VERSION,
    chatPromptVersion: CHAT_PROMPT_VERSION,
    timeoutMs,

    async plan(input, options = {}) {
      return runWithRepair({
        initialMessage: buildPlanMessage(input),
        repairMessage: (previous, reason) =>
          buildPlanRepairMessage(input, previous, reason),
        normalize: (payload) => normalizePlan(payload, input),
        userId: options.userId,
      })
    },

    async review(input, options = {}) {
      return runWithRepair({
        initialMessage: buildReviewMessage(input),
        repairMessage: (previous, reason) =>
          buildReviewRepairMessage(input, previous, reason),
        normalize: (payload) => normalizeReview(payload),
        userId: options.userId,
      })
    },

    async chat(input, options = {}) {
      let text
      try {
        text = await coze.chat(
          [{ role: 'user', content: buildChatMessage(input) }],
          { userId: options.userId, timeoutMs },
        )
      } catch (error) {
        throw mapCozeFailure(error, logger)
      }
      try {
        return normalizeChatReply(text)
      } catch (error) {
        if (error instanceof AiValidationError) {
          throw new ApiError(502, 'AI_INVALID_OUTPUT', 'AI 返回内容为空，请重试')
        }
        throw error
      }
    },
  }
}

module.exports = {
  AI_LIMITS,
  AI_TIMEOUT_MS,
  AiValidationError,
  beijingDayKey,
  beijingEpochDay,
  isoDate,
  weekStartEpochDay,
  parseJsonObject,
  validatePlanInput,
  normalizePlan,
  validateReviewInput,
  normalizeReview,
  validateChatInput,
  normalizeChatReply,
  createAiService,
}
