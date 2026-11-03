'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')

const { createTestApp, call, auth } = require('../test-support/helpers')
const { beijingEpochDay } = require('../lib/ai')
const { ApiError } = require('../lib/errors')

const DAY_MS = 24 * 60 * 60 * 1000
const NOW = 1700000000000
const TODAY = beijingEpochDay(NOW)

async function register(app) {
  const response = await call(app, {
    method: 'POST',
    path: '/api/auth/register',
    body: { username: 'ai_runner', password: 'orbit2026' },
  })
  assert.equal(response.statusCode, 201)
  return response.json
}

function createFakeAi(overrides = {}) {
  const calls = { plan: [], review: [], chat: [] }
  return {
    planPromptVersion: 'plan-v1',
    reviewPromptVersion: 'review-v1',
    chatPromptVersion: 'chat-v1',
    calls,
    async plan(input, options) {
      calls.plan.push({ input, options })
      return {
        goalTitle: '通过六级',
        goalDescription: '按周推进',
        targetDateEpochDay: input.targetDateEpochDay,
        stages: [
          {
            title: '基础',
            targetDateEpochDay: input.todayEpochDay + 7,
            tasks: [
              {
                title: '完成一套真题',
                dueDateEpochDay: input.todayEpochDay + 1,
                priority: 'HIGH',
                description: '',
              },
            ],
          },
        ],
      }
    },
    async review(input, options) {
      calls.review.push({ input, options })
      return {
        headline: '稳步推进',
        summary: '本周完成了大部分计划。',
        wins: ['完成任务'],
        risks: [],
        nextWeek: [{ title: '继续听力', reason: '薄弱点' }],
      }
    },
    async chat(input, options) {
      calls.chat.push({ input, options })
      return '先用 25 分钟做一套听力真题，再用 5 分钟整理错题。'
    },
    ...overrides,
  }
}

function planBody() {
  return {
    goal: '三个月通过英语六级',
    targetDateEpochDay: TODAY + 30,
    dailyMinutes: 45,
    weakPoints: '听力',
  }
}

function reviewBody() {
  return {
    planned: 10,
    completed: 6,
    studyPlanned: 8,
    studyCompleted: 5,
    habitPossible: 14,
    habitCheckIns: 9,
    xp: 80,
    goals: [{ title: '英语六级', progress: 0.4, stageCount: 3, completedStages: 1 }],
  }
}

function chatBody() {
  return {
    message: '今晚只有 30 分钟，英语听力怎么练？',
    history: [
      { role: 'user', content: '我在准备六级' },
      { role: 'assistant', content: '先确认你的听力和阅读哪项更弱。' },
    ],
    context: {
      todayPlanned: 3,
      todayCompleted: 1,
      pendingCount: 5,
      completedCount: 9,
      habitTodayChecked: 2,
      habitTodayTotal: 4,
      totalXp: 320,
      thisWeekXp: 80,
      goals: [{ title: '英语六级', progress: 0.4 }],
    },
  }
}

test('AI status reports configuration, limits and daily usage without a provider call', async () => {
  const ai = createFakeAi()
  const { app } = createTestApp({ ai, aiLimits: { plan: 2, review: 1 } })
  const registered = await register(app)
  const status = await call(app, {
    path: '/api/ai/status',
    headers: auth(registered.token),
  })
  assert.equal(status.statusCode, 200)
  assert.deepEqual(status.json, {
    configured: true,
    planPromptVersion: 'plan-v1',
    reviewPromptVersion: 'review-v1',
    chatPromptVersion: 'chat-v1',
    timeoutSeconds: 25,
    limits: { plan: 2, review: 1, chat: 20 },
    usage: { planUsed: 0, reviewUsed: 0, chatUsed: 0 },
    dayKey: '2023-11-15',
  })

  const anonymous = await call(app, { path: '/api/ai/status' })
  assert.equal(anonymous.statusCode, 401)
})

test('AI plan requires a session, validates input and returns the normalized plan', async () => {
  const ai = createFakeAi()
  const { app, store } = createTestApp({ ai })
  const registered = await register(app)

  const anonymous = await call(app, { method: 'POST', path: '/api/ai/plan', body: planBody() })
  assert.equal(anonymous.statusCode, 401)

  const invalid = await call(app, {
    method: 'POST',
    path: '/api/ai/plan',
    headers: auth(registered.token),
    body: { goal: ' ', targetDateEpochDay: TODAY + 10 },
  })
  assert.equal(invalid.statusCode, 400)
  assert.equal(invalid.json.code, 'AI_INPUT_INVALID')
  assert.equal(ai.calls.plan.length, 0)

  const response = await call(app, {
    method: 'POST',
    path: '/api/ai/plan',
    headers: auth(registered.token),
    body: planBody(),
  })
  assert.equal(response.statusCode, 200)
  assert.equal(response.json.promptVersion, 'plan-v1')
  assert.match(response.json.requestId, /^[a-f0-9]{16}$/)
  assert.equal(response.json.plan.goalTitle, '通过六级')
  assert.equal(ai.calls.plan.length, 1)
  assert.equal(ai.calls.plan[0].input.goal, '三个月通过英语六级')
  assert.equal(ai.calls.plan[0].options.userId, 'orbit_' + registered.user.id)
  assert.equal(store.users[0].aiPlanCount, 1)
  assert.equal(store.users[0].aiDayKey, '2023-11-15')
})

test('AI plan quota is per Beijing day and returns 429 when exhausted', async () => {
  const ai = createFakeAi()
  const harness = createTestApp({ ai, aiLimits: { plan: 1, review: 1 } })
  const { app, store } = harness
  const registered = await register(app)
  const headers = auth(registered.token)

  const first = await call(app, {
    method: 'POST',
    path: '/api/ai/plan',
    headers,
    body: planBody(),
  })
  assert.equal(first.statusCode, 200)

  const second = await call(app, {
    method: 'POST',
    path: '/api/ai/plan',
    headers,
    body: planBody(),
  })
  assert.equal(second.statusCode, 429)
  assert.equal(second.json.code, 'AI_QUOTA_EXCEEDED')
  assert.equal(ai.calls.plan.length, 1)

  harness.advance(DAY_MS)
  const nextDay = await call(app, {
    method: 'POST',
    path: '/api/ai/plan',
    headers,
    body: planBody(),
  })
  assert.equal(nextDay.statusCode, 200)
  assert.equal(store.users[0].aiPlanCount, 1)
})

test('AI review rejects an empty week and enforces its own quota', async () => {
  const ai = createFakeAi()
  const { app } = createTestApp({ ai, aiLimits: { plan: 1, review: 1 } })
  const registered = await register(app)
  const headers = auth(registered.token)

  const empty = await call(app, {
    method: 'POST',
    path: '/api/ai/review',
    headers,
    body: {
      planned: 0,
      completed: 0,
      studyPlanned: 0,
      studyCompleted: 0,
      habitPossible: 0,
      habitCheckIns: 0,
      xp: 0,
      goals: [],
    },
  })
  assert.equal(empty.statusCode, 400)
  assert.equal(empty.json.code, 'AI_NO_DATA')
  assert.equal(ai.calls.review.length, 0)

  const success = await call(app, {
    method: 'POST',
    path: '/api/ai/review',
    headers,
    body: reviewBody(),
  })
  assert.equal(success.statusCode, 200)
  assert.equal(success.json.promptVersion, 'review-v1')
  assert.equal(success.json.review.headline, '稳步推进')
  assert.equal(ai.calls.review[0].input.completionPercent, 60)

  const limited = await call(app, {
    method: 'POST',
    path: '/api/ai/review',
    headers,
    body: reviewBody(),
  })
  assert.equal(limited.statusCode, 429)
  assert.equal(limited.json.code, 'AI_QUOTA_EXCEEDED')
})

test('AI chat validates the question, passes aggregate context and counts separately', async () => {
  const ai = createFakeAi()
  const { app, store } = createTestApp({ ai, aiLimits: { chat: 2 } })
  const registered = await register(app)
  const headers = auth(registered.token)

  const anonymous = await call(app, {
    method: 'POST',
    path: '/api/ai/chat',
    body: chatBody(),
  })
  assert.equal(anonymous.statusCode, 401)

  const invalid = await call(app, {
    method: 'POST',
    path: '/api/ai/chat',
    headers,
    body: { message: ' ' },
  })
  assert.equal(invalid.statusCode, 400)
  assert.equal(invalid.json.code, 'AI_INPUT_INVALID')
  assert.equal(ai.calls.chat.length, 0)

  const response = await call(app, {
    method: 'POST',
    path: '/api/ai/chat',
    headers,
    body: chatBody(),
  })
  assert.equal(response.statusCode, 200)
  assert.equal(response.json.promptVersion, 'chat-v1')
  assert.match(response.json.requestId, /^[a-f0-9]{16}$/)
  assert.match(response.json.reply, /听力真题/)
  assert.equal(ai.calls.chat.length, 1)
  assert.equal(ai.calls.chat[0].input.message, '今晚只有 30 分钟，英语听力怎么练？')
  assert.equal(ai.calls.chat[0].input.history.length, 2)
  assert.equal(ai.calls.chat[0].input.todayPlanned, 3)
  assert.equal(ai.calls.chat[0].input.goals[0].title, '英语六级')
  assert.equal(ai.calls.chat[0].options.userId, 'orbit_' + registered.user.id)
  assert.equal(store.users[0].aiChatCount, 1)
  assert.equal(store.users[0].aiPlanCount, 0)

  const plan = await call(app, {
    method: 'POST',
    path: '/api/ai/plan',
    headers,
    body: planBody(),
  })
  assert.equal(plan.statusCode, 200)
  assert.equal(store.users[0].aiPlanCount, 1)
})

test('AI chat quota is independent and returns 429 when exhausted', async () => {
  const ai = createFakeAi()
  const harness = createTestApp({ ai, aiLimits: { chat: 1 } })
  const { app, store } = harness
  const registered = await register(app)
  const headers = auth(registered.token)

  const first = await call(app, {
    method: 'POST',
    path: '/api/ai/chat',
    headers,
    body: chatBody(),
  })
  assert.equal(first.statusCode, 200)

  const second = await call(app, {
    method: 'POST',
    path: '/api/ai/chat',
    headers,
    body: chatBody(),
  })
  assert.equal(second.statusCode, 429)
  assert.equal(second.json.code, 'AI_QUOTA_EXCEEDED')
  assert.equal(ai.calls.chat.length, 1)

  harness.advance(DAY_MS)
  const nextDay = await call(app, {
    method: 'POST',
    path: '/api/ai/chat',
    headers,
    body: chatBody(),
  })
  assert.equal(nextDay.statusCode, 200)
  assert.equal(store.users[0].aiChatCount, 1)
})

test('AI routes are unavailable until the provider is configured', async () => {
  const { app } = createTestApp()
  const registered = await register(app)
  const headers = auth(registered.token)

  const status = await call(app, { path: '/api/ai/status', headers })
  assert.equal(status.statusCode, 200)
  assert.equal(status.json.configured, false)

  const plan = await call(app, {
    method: 'POST',
    path: '/api/ai/plan',
    headers,
    body: planBody(),
  })
  assert.equal(plan.statusCode, 503)
  assert.equal(plan.json.code, 'AI_NOT_CONFIGURED')

  const review = await call(app, {
    method: 'POST',
    path: '/api/ai/review',
    headers,
    body: reviewBody(),
  })
  assert.equal(review.statusCode, 503)
  assert.equal(review.json.code, 'AI_NOT_CONFIGURED')

  const chat = await call(app, {
    method: 'POST',
    path: '/api/ai/chat',
    headers,
    body: chatBody(),
  })
  assert.equal(chat.statusCode, 503)
  assert.equal(chat.json.code, 'AI_NOT_CONFIGURED')
})

test('provider errors keep their mapped status and never leak the token', async () => {
  const ai = createFakeAi({
    async plan() {
      throw new ApiError(504, 'AI_TIMEOUT', 'AI 响应超时，请稍后重试')
    },
  })
  const { app } = createTestApp({ ai })
  const registered = await register(app)
  const response = await call(app, {
    method: 'POST',
    path: '/api/ai/plan',
    headers: auth(registered.token),
    body: planBody(),
  })
  assert.equal(response.statusCode, 504)
  assert.equal(response.json.code, 'AI_TIMEOUT')
  assert.equal(JSON.stringify(response.json).includes('pat_'), false)
})
