'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')

const { ApiError } = require('../lib/errors')
const { CozeError } = require('../lib/coze')
const {
  AI_LIMITS,
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
} = require('../lib/ai')

const NOW = Date.UTC(2026, 9, 28, 4, 0, 0) // 2026-10-28 12:00 Beijing
const TODAY = beijingEpochDay(NOW)

function planPayload(overrides = {}) {
  return {
    goalTitle: '三个月通过英语六级',
    goalDescription: '以真题为主，听力优先。',
    stages: [
      {
        title: '基础诊断',
        endDayOffset: 14,
        tasks: [
          {
            title: '完成一套真题',
            dayOffset: 2,
            priority: 'high',
            note: '先做听力',
          },
          {
            title: '整理错题',
            dayOffset: 5,
            priority: '中',
            note: '',
          },
        ],
      },
      {
        title: '专项突破',
        endDayOffset: 42,
        tasks: [
          {
            title: '听力精听三套',
            dayOffset: 20,
            priority: 'HIGH',
            note: '每天 30 分钟',
          },
        ],
      },
    ],
    ...overrides,
  }
}

function reviewPayload(overrides = {}) {
  return {
    planned: 12,
    completed: 8,
    studyPlanned: 8,
    studyCompleted: 6,
    habitPossible: 21,
    habitCheckIns: 14,
    xp: 120,
    goals: [{ title: '英语六级', progress: 42, stageCount: 3, completedStages: 1 }],
    ...overrides,
  }
}

test('beijing day helpers keep the quota window on China time', () => {
  assert.equal(beijingDayKey(NOW), '2026-10-28')
  assert.equal(isoDate(TODAY), '2026-10-28')
  assert.equal(weekStartEpochDay(TODAY), TODAY - 2) // Wednesday -> Monday
})

test('parseJsonObject accepts fenced JSON and ignores surrounding prose', () => {
  assert.deepEqual(
    parseJsonObject('好的，结果如下：\n```json\n{"a":1,"nested":{"b":"}"}}\n```\n'),
    { a: 1, nested: { b: '}' } },
  )
  assert.deepEqual(parseJsonObject('{"text":"brace } inside"}'), {
    text: 'brace } inside',
  })
  assert.throws(() => parseJsonObject('no object'), AiValidationError)
  assert.throws(() => parseJsonObject('{"a":'), AiValidationError)
})

test('plan input validates the goal, target window and daily minutes', () => {
  const input = validatePlanInput(
    {
      goal: '  通过软考  ',
      targetDateEpochDay: TODAY + 30,
      dailyMinutes: 45,
      weakPoints: ' 算法  ',
    },
    NOW,
  )
  assert.equal(input.goal, '通过软考')
  assert.equal(input.dailyMinutes, 45)
  assert.equal(input.weakPoints, '算法')
  assert.equal(input.todayEpochDay, TODAY)

  const defaults = validatePlanInput({ goal: '学习 Java' }, NOW)
  assert.equal(defaults.targetDateEpochDay, TODAY + 56)
  assert.equal(defaults.dailyMinutes, 45)

  assert.throws(() => validatePlanInput({ goal: '' }, NOW), AiValidationError)
  assert.throws(
    () => validatePlanInput({ goal: 'x', targetDateEpochDay: TODAY - 1 }, NOW),
    AiValidationError,
  )
  assert.throws(
    () => validatePlanInput({ goal: 'x', dailyMinutes: 5 }, NOW),
    AiValidationError,
  )
})

test('normalizePlan converts offsets to epoch days and clamps stage ends', () => {
  const input = validatePlanInput(
    { goal: '通过六级', targetDateEpochDay: TODAY + 60, dailyMinutes: 45 },
    NOW,
  )
  const plan = normalizePlan(planPayload(), input)

  assert.equal(plan.goalTitle, '三个月通过英语六级')
  assert.equal(plan.stages.length, 2)
  assert.equal(plan.stages[0].targetDateEpochDay, TODAY + 14)
  assert.equal(plan.stages[0].tasks[0].dueDateEpochDay, TODAY + 2)
  assert.equal(plan.stages[0].tasks[0].priority, 'HIGH')
  assert.equal(plan.stages[0].tasks[1].priority, 'MEDIUM')
  assert.equal(plan.stages[0].tasks[1].description, '')
  assert.equal(plan.stages[1].targetDateEpochDay, TODAY + 42)
  assert.equal(plan.targetDateEpochDay, TODAY + 60)
})

test('normalizePlan rejects plans that exceed the task ceiling', () => {
  const input = validatePlanInput(
    { goal: '通过六级', targetDateEpochDay: TODAY + 60 },
    NOW,
  )
  const stages = Array.from({ length: 5 }, (_, stageIndex) => ({
    title: '阶段 ' + stageIndex,
    endDayOffset: 10 + stageIndex,
    tasks: Array.from({ length: 5 }, (_, taskIndex) => ({
      title: '任务 ' + stageIndex + '-' + taskIndex,
      dayOffset: taskIndex,
    })),
  }))
  assert.throws(
    () => normalizePlan({ stages }, input),
    /more than 20 tasks/,
  )
})

test('review input derives percentages and rejects an empty week', () => {
  const input = validateReviewInput(reviewPayload(), NOW)
  assert.equal(input.weekStartEpochDay, weekStartEpochDay(TODAY))
  assert.equal(input.completionPercent, 67)
  assert.equal(input.goals[0].progress, 0.42)
  assert.equal(input.habitCheckIns, 14)

  assert.throws(
    () =>
      validateReviewInput(
        {
          planned: 0,
          completed: 0,
          studyPlanned: 0,
          studyCompleted: 0,
          habitPossible: 0,
          habitCheckIns: 0,
          xp: 0,
          goals: [],
        },
        NOW,
      ),
    (error) => error instanceof ApiError && error.code === 'AI_NO_DATA',
  )
})

test('normalizeReview trims lists and accepts string actions', () => {
  const review = normalizeReview({
    headline: '  稳步推进  ',
    summary: '本周完成了大部分计划。',
    wins: ['完成 8 个任务', '坚持打卡'],
    risks: [],
    nextWeek: ['继续听力训练'],
  })
  assert.equal(review.headline, '稳步推进')
  assert.equal(review.wins.length, 2)
  assert.deepEqual(review.nextWeek, [{ title: '继续听力训练', reason: '' }])
})

test('AI service repairs one invalid answer and returns the normalized plan', async () => {
  const calls = []
  const coze = {
    async chat(messages, options) {
      calls.push({ messages, options })
      if (calls.length === 1) return '这不是 JSON'
      return JSON.stringify(planPayload())
    },
  }
  const service = createAiService({ coze, now: () => NOW })
  const input = validatePlanInput(
    { goal: '通过六级', targetDateEpochDay: TODAY + 60 },
    NOW,
  )
  const plan = await service.plan(input, { userId: 'orbit_1' })
  assert.equal(plan.stages.length, 2)
  assert.equal(calls.length, 2)
  assert.match(calls[1].messages[0].content, /上一次的输出无法通过校验/)
  assert.equal(calls[0].options.userId, 'orbit_1')
  assert.equal(service.planPromptVersion, 'plan-v1')
})

test('AI service maps Coze failures without leaking provider details', async () => {
  const timeoutService = createAiService({
    coze: {
      async chat() {
        throw new CozeError('COZE_TIMEOUT', 'provider timeout')
      },
    },
    now: () => NOW,
  })
  await assert.rejects(
    () =>
      timeoutService.plan(
        validatePlanInput({ goal: 'x', targetDateEpochDay: TODAY + 10 }, NOW),
      ),
    (error) =>
      error instanceof ApiError &&
      error.status === 504 &&
      error.code === 'AI_TIMEOUT' &&
      !error.message.includes('provider'),
  )

  const brokenService = createAiService({
    coze: { async chat() { return '{}' } },
    now: () => NOW,
  })
  await assert.rejects(
    () =>
      brokenService.review(
        validateReviewInput(reviewPayload(), NOW),
      ),
    (error) => error instanceof ApiError && error.code === 'AI_INVALID_OUTPUT',
  )
})

test('chat input keeps a bounded transcript and clamps aggregate context', () => {
  const history = Array.from({ length: 10 }, (_, index) => ({
    role: index % 2 === 0 ? 'user' : 'assistant',
    content: 'turn-' + index,
  }))
  const input = validateChatInput(
    {
      message: '今晚 30 分钟怎么练听力？',
      history,
      context: {
        todayPlanned: 3,
        todayCompleted: 9,
        pendingCount: 4,
        completedCount: 12,
        habitTodayTotal: 2,
        habitTodayChecked: 5,
        totalXp: 320,
        thisWeekXp: 80,
        goals: [{ title: '英语六级', progress: 50 }],
      },
    },
    NOW,
  )

  assert.equal(input.message, '今晚 30 分钟怎么练听力？')
  assert.equal(input.history.length, 8)
  assert.equal(input.history[0].content, 'turn-2')
  assert.equal(input.todayLabel, '2026-10-28')
  assert.equal(input.todayCompleted, 3)
  assert.equal(input.habitTodayChecked, 2)
  assert.equal(input.goals[0].progress, 0.5)
  assert.throws(
    () => validateChatInput({ message: ' ' }, NOW),
    AiValidationError,
  )
})

test('chat replies drop fences and assistant prefixes, and stay within 1200 chars', () => {
  assert.equal(normalizeChatReply('```markdown\n先做一套听力\n```'), '先做一套听力')
  assert.equal(normalizeChatReply('助手：先做一套听力'), '先做一套听力')
  assert.throws(() => normalizeChatReply('   '), AiValidationError)
  const capped = normalizeChatReply('字'.repeat(1300))
  assert.equal(capped.length, 1200)
  assert.equal(capped.endsWith('…'), true)
})

test('AI service chat sends one transcript prompt and returns plain text', async () => {
  const calls = []
  const coze = {
    async chat(messages, options) {
      calls.push({ messages, options })
      return '先用 25 分钟做听力真题，再用 5 分钟整理错题。'
    },
  }
  const service = createAiService({ coze, now: () => NOW })
  const input = validateChatInput(
    {
      message: '今晚怎么练听力？',
      history: [{ role: 'user', content: '我在准备六级' }],
      context: { todayPlanned: 3, todayCompleted: 1, goals: [] },
    },
    NOW,
  )
  const reply = await service.chat(input, { userId: 'orbit_1' })

  assert.match(reply, /听力真题/)
  assert.equal(calls.length, 1)
  assert.equal(calls[0].messages.length, 1)
  assert.match(calls[0].messages[0].content, /我在准备六级/)
  assert.match(calls[0].messages[0].content, /今日任务：计划 3 个，完成 1 个/)
  assert.match(calls[0].messages[0].content, /今晚怎么练听力/)
  assert.equal(calls[0].options.userId, 'orbit_1')
  assert.equal(service.chatPromptVersion, 'chat-v1')
})

test('quota defaults match the public contract', () => {
  assert.deepEqual(AI_LIMITS, { plan: 6, review: 4, chat: 20 })
})
