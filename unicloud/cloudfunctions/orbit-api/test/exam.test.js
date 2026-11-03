'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')

const {
  createExamTestApp,
  loginStudent,
  bearer,
  seedData,
} = require('../test-support/exam-helpers')

const BASIC_PAPER = seedData.papers[0]
const ADVANCED_PAPER = seedData.papers[1]

async function startPaper(app, token, paper = BASIC_PAPER) {
  const response = await app.call({
    method: 'POST',
    path: '/api/exam/attempts',
    headers: bearer(token),
    body: { paperId: paper.paperId },
  })
  assert.equal(response.statusCode, 200)
  assert.equal(response.json.code, 200)
  return response.json.data
}

async function answerAll(app, token, attemptId, paper) {
  for (const question of paper.questions) {
    const response = await app.call({
      method: 'PUT',
      path: '/api/exam/attempts/' + attemptId + '/answer',
      headers: bearer(token),
      body: {
        questionId: question.id,
        answer: question.correctAnswer,
        isFlagged: false,
      },
    })
    assert.equal(response.json.code, 200)
  }
}

test('login rejects a wrong password without triggering the 401 logout path', async () => {
  const app = createExamTestApp()
  const response = await app.call({
    method: 'POST',
    path: '/api/auth/login',
    body: { username: 'student', password: 'wrong-pass' },
  })
  assert.notEqual(response.statusCode, 401)
  assert.equal(response.json.code, 400)
  assert.match(response.json.message, /用户名或密码错误/)
})

test('login returns the Spring-compatible envelope and a reusable token', async () => {
  const app = createExamTestApp()
  const response = await loginStudent(app)
  assert.equal(response.statusCode, 200)
  assert.equal(response.json.code, 200)
  assert.equal(response.json.message, '操作成功')
  assert.equal(response.json.data.userInfo.username, 'student')
  assert.equal(response.json.data.userInfo.role, '考生')
  assert.equal(typeof response.json.data.userInfo.id, 'string')
  assert.ok(response.json.data.token.length >= 32)

  const papers = await app.call({
    path: '/api/exam/papers',
    headers: bearer(response.json.data.token),
  })
  assert.equal(papers.json.code, 200)
  assert.equal(papers.json.data.length, 2)
})

test('protected routes return HTTP 401 for missing or expired tokens', async () => {
  const app = createExamTestApp()
  const missing = await app.call({ path: '/api/exam/papers' })
  assert.equal(missing.statusCode, 401)
  assert.equal(missing.json.code, 401)

  const login = await loginStudent(app)
  const token = login.json.data.token
  app.advance(31 * 24 * 60 * 60 * 1000)
  const expired = await app.call({ path: '/api/exam/papers', headers: bearer(token) })
  assert.equal(expired.statusCode, 401)
})

test('papers expose counts, pass score and per-user best score', async () => {
  const app = createExamTestApp()
  const login = await loginStudent(app)
  const token = login.json.data.token
  const response = await app.call({ path: '/api/exam/papers', headers: bearer(token) })
  const [basic, advanced] = response.json.data
  assert.equal(basic.name, BASIC_PAPER.name)
  assert.equal(basic.questionCount, 35)
  assert.equal(basic.passScore, 60)
  assert.equal(basic.duration, 60)
  assert.equal(basic.levelName, '基础')
  assert.equal(advanced.questionCount, 30)
  assert.equal(advanced.passScore, 80)
  assert.equal(advanced.levelName, '高级')
  assert.equal(basic.attemptCount, 0)
  assert.equal(basic.bestScore, 0)
})

test('starting an exam returns the paper questions and resumes an active attempt', async () => {
  const app = createExamTestApp()
  const login = await loginStudent(app)
  const token = login.json.data.token
  const first = await startPaper(app, token)
  assert.equal(first.remainingSeconds, 3600)
  assert.equal(first.totalScore, 100)
  assert.equal(first.passScore, 60)
  assert.equal(first.questions.length, 35)
  assert.equal(first.questions[0].id, BASIC_PAPER.questions[0].id)
  assert.equal(first.questions[0].myAnswer, null)
  assert.equal(first.questions[0].isFlagged, false)

  const saved = await app.call({
    method: 'PUT',
    path: '/api/exam/attempts/' + first.attemptId + '/answer',
    headers: bearer(token),
    body: {
      questionId: BASIC_PAPER.questions[0].id,
      answer: BASIC_PAPER.questions[0].correctAnswer,
      isFlagged: true,
    },
  })
  assert.equal(saved.json.code, 200)

  const resumed = await startPaper(app, token)
  assert.equal(resumed.attemptId, first.attemptId)
  assert.equal(resumed.questions[0].myAnswer, BASIC_PAPER.questions[0].correctAnswer)
  assert.equal(resumed.questions[0].isFlagged, true)
})

test('tab switching is adjudicated by the server at the 3rd report', async () => {
  const app = createExamTestApp()
  const login = await loginStudent(app)
  const token = login.json.data.token
  const attempt = await startPaper(app, token)

  for (let count = 1; count <= 2; count += 1) {
    const response = await app.call({
      method: 'PUT',
      path: '/api/exam/attempts/' + attempt.attemptId + '/tab-switch',
      headers: bearer(token),
      body: { tabSwitchCount: count },
    })
    assert.equal(response.json.data.tabSwitchCount, count)
    assert.equal(response.json.data.tabSwitchLimit, 3)
    assert.equal(response.json.data.shouldAutoSubmit, false)
  }

  const third = await app.call({
    method: 'PUT',
    path: '/api/exam/attempts/' + attempt.attemptId + '/tab-switch',
    headers: bearer(token),
    body: { tabSwitchCount: 3 },
  })
  assert.equal(third.json.data.shouldAutoSubmit, true)
})

test('manual submit grades the paper, and result/review/records are consistent', async () => {
  const app = createExamTestApp()
  const login = await loginStudent(app)
  const token = login.json.data.token
  const attempt = await startPaper(app, token)
  const first = BASIC_PAPER.questions[0]
  const second = BASIC_PAPER.questions[1]
  await answerAll(app, token, attempt.attemptId, {
    questions: [first, second],
  })

  const submitted = await app.call({
    method: 'POST',
    path: '/api/exam/attempts/' + attempt.attemptId + '/submit',
    headers: bearer(token),
    body: { submitType: 'MANUAL', tabSwitchCount: 0 },
  })
  assert.equal(submitted.json.code, 200)
  const result = submitted.json.data
  assert.equal(result.score, first.score + second.score)
  assert.equal(result.correctCount, 2)
  assert.equal(result.totalQuestions, 35)
  assert.equal(result.passed, false)
  assert.equal(result.submitType, 'MANUAL')
  assert.equal(result.certificateGenerated, false)
  assert.equal(result.usedTime, 0)

  const again = await app.call({
    method: 'POST',
    path: '/api/exam/attempts/' + attempt.attemptId + '/submit',
    headers: bearer(token),
    body: { submitType: 'MANUAL', tabSwitchCount: 0 },
  })
  assert.equal(again.json.data.score, result.score)

  const resultRoute = await app.call({
    path: '/api/exam/attempts/' + attempt.attemptId + '/result',
    headers: bearer(token),
  })
  assert.equal(resultRoute.json.data.submitType, 'MANUAL')

  const review = await app.call({
    path: '/api/exam/attempts/' + attempt.attemptId + '/review',
    headers: bearer(token),
  })
  assert.equal(review.json.code, 200)
  assert.equal(review.json.data.isPassed, 0)
  assert.equal(review.json.data.questions.length, 35)
  const reviewed = review.json.data.questions[0]
  assert.equal(reviewed.questionId, first.id)
  assert.equal(reviewed.userAnswer, first.correctAnswer)
  assert.equal(reviewed.isCorrect, true)
  assert.equal(reviewed.earnedScore, first.score)
  assert.equal(reviewed.maxScore, first.score)
  assert.equal(reviewed.correctAnswer, first.correctAnswer)
  assert.equal(reviewed.options.length, first.options.length)
  assert.equal(review.json.data.questions[2].userAnswer, null)
  assert.equal(review.json.data.questions[2].isCorrect, false)

  const records = await app.call({
    path: '/api/exam/attempts',
    headers: bearer(token),
    query: { page: '1', size: '100' },
  })
  assert.equal(records.json.data.total, 1)
  assert.equal(records.json.data.records.length, 1)
  const record = records.json.data.records[0]
  assert.equal(record.id, attempt.attemptId)
  assert.equal(record.paperName, BASIC_PAPER.name)
  assert.equal(record.submitType, 'MANUAL')
  assert.match(record.startTime, /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)

  const papers = await app.call({ path: '/api/exam/papers', headers: bearer(token) })
  assert.equal(papers.json.data[0].attemptCount, 1)
  assert.equal(papers.json.data[0].bestScore, result.score)
})

test('a passing advanced exam issues a certificate exactly once', async () => {
  const app = createExamTestApp()
  const login = await loginStudent(app)
  const token = login.json.data.token
  const attempt = await startPaper(app, token, ADVANCED_PAPER)
  await answerAll(app, token, attempt.attemptId, ADVANCED_PAPER)

  const submitted = await app.call({
    method: 'POST',
    path: '/api/exam/attempts/' + attempt.attemptId + '/submit',
    headers: bearer(token),
    body: { submitType: 'MANUAL', tabSwitchCount: 0 },
  })
  const result = submitted.json.data
  assert.equal(result.score, 100)
  assert.equal(result.correctCount, 30)
  assert.equal(result.passed, true)
  assert.equal(result.certificateGenerated, true)

  const again = await app.call({
    method: 'POST',
    path: '/api/exam/attempts/' + attempt.attemptId + '/submit',
    headers: bearer(token),
    body: { submitType: 'MANUAL', tabSwitchCount: 0 },
  })
  assert.equal(again.json.data.certificateGenerated, true)

  const list = await app.call({
    path: '/api/exam/certificates',
    headers: bearer(token),
  })
  assert.equal(list.json.code, 200)
  assert.equal(list.json.data.length, 1)
  const certificate = list.json.data[0]
  assert.equal(certificate.name, ADVANCED_PAPER.certificateName)
  assert.match(certificate.certificateNo, /^CERT-\d{8}-\d{6}$/)
  assert.match(certificate.issueDate, /^\d{4}-\d{2}-\d{2}$/)
  assert.match(certificate.expiryDate, /^\d{4}-\d{2}-\d{2}$/)
  assert.equal(certificate.status, '有效')
})

test('tab-switch submit forces the TAB_SWITCH type and rejects later answers', async () => {
  const app = createExamTestApp()
  const login = await loginStudent(app)
  const token = login.json.data.token
  const attempt = await startPaper(app, token)

  for (let count = 1; count <= 3; count += 1) {
    await app.call({
      method: 'PUT',
      path: '/api/exam/attempts/' + attempt.attemptId + '/tab-switch',
      headers: bearer(token),
      body: { tabSwitchCount: count },
    })
  }
  const submitted = await app.call({
    method: 'POST',
    path: '/api/exam/attempts/' + attempt.attemptId + '/submit',
    headers: bearer(token),
    body: { submitType: 'TAB_SWITCH', tabSwitchCount: 3 },
  })
  assert.equal(submitted.json.data.submitType, 'TAB_SWITCH')

  const late = await app.call({
    method: 'PUT',
    path: '/api/exam/attempts/' + attempt.attemptId + '/answer',
    headers: bearer(token),
    body: { questionId: BASIC_PAPER.questions[0].id, answer: 'A' },
  })
  assert.equal(late.json.code, 400)
  assert.match(late.json.message, /已结束/)
})

test('timeout is enforced by the server when the clock passes the duration', async () => {
  const app = createExamTestApp()
  const login = await loginStudent(app)
  const token = login.json.data.token
  const attempt = await startPaper(app, token)
  app.advance(60 * 60 * 1000 + 1000)

  const late = await app.call({
    method: 'PUT',
    path: '/api/exam/attempts/' + attempt.attemptId + '/answer',
    headers: bearer(token),
    body: { questionId: BASIC_PAPER.questions[0].id, answer: BASIC_PAPER.questions[0].correctAnswer },
  })
  assert.equal(late.json.code, 400)
  assert.match(late.json.message, /自动交卷/)

  const submitted = await app.call({
    method: 'POST',
    path: '/api/exam/attempts/' + attempt.attemptId + '/submit',
    headers: bearer(token),
    body: { submitType: 'MANUAL', tabSwitchCount: 0 },
  })
  assert.equal(submitted.json.data.submitType, 'TIMEOUT')
  assert.equal(submitted.json.data.usedTime, 3600)

  const next = await startPaper(app, token)
  assert.notEqual(next.attemptId, attempt.attemptId)
})

test('register creates a candidate account that can log in immediately', async () => {
  const app = createExamTestApp()
  const created = await app.call({
    method: 'POST',
    path: '/api/auth/register',
    body: { username: 'new_user', password: 'secret12', confirmPassword: 'secret12' },
  })
  assert.equal(created.json.code, 200)
  assert.match(created.json.message, /注册成功/)

  const duplicate = await app.call({
    method: 'POST',
    path: '/api/auth/register',
    body: { username: 'new_user', password: 'secret12', confirmPassword: 'secret12' },
  })
  assert.equal(duplicate.json.code, 400)

  const login = await app.call({
    method: 'POST',
    path: '/api/auth/login',
    body: { username: 'new_user', password: 'secret12' },
  })
  assert.equal(login.json.code, 200)
  assert.equal(login.json.data.userInfo.role, '考生')
})

test('one user cannot read or mutate another user attempt', async () => {
  const app = createExamTestApp()
  const student = await loginStudent(app)
  const studentToken = student.json.data.token
  const attempt = await startPaper(app, studentToken)
  await app.call({
    method: 'POST',
    path: '/api/auth/register',
    body: { username: 'other_user', password: 'secret12', confirmPassword: 'secret12' },
  })
  const other = await app.call({
    method: 'POST',
    path: '/api/auth/login',
    body: { username: 'other_user', password: 'secret12' },
  })
  const otherToken = other.json.data.token
  const review = await app.call({
    path: '/api/exam/attempts/' + attempt.attemptId + '/review',
    headers: bearer(otherToken),
  })
  assert.equal(review.json.code, 400)
  const answer = await app.call({
    method: 'PUT',
    path: '/api/exam/attempts/' + attempt.attemptId + '/answer',
    headers: bearer(otherToken),
    body: { questionId: BASIC_PAPER.questions[0].id, answer: 'A' },
  })
  assert.equal(answer.json.code, 400)
})
