'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')

const { ApiError } = require('../lib/errors')
const {
  emptySnapshot,
  normalizePush,
  snapshotFromDoc,
  MAX_ROWS_PER_KIND,
} = require('../lib/sync')

function expectApiError(fn, code, status) {
  assert.throws(fn, (error) => {
    assert.ok(error instanceof ApiError)
    assert.equal(error.code, code)
    assert.equal(error.status, status)
    return true
  })
}

test('empty snapshots mirror the Android defaults', () => {
  assert.deepEqual(emptySnapshot(), {
    revision: 0,
    goals: [],
    stages: [],
    tasks: [],
    habits: [],
    habitRecords: [],
    growthEvents: [],
  })
  assert.deepEqual(snapshotFromDoc(null), emptySnapshot())
})

test('push payloads are normalised, sorted and stripped of unknown fields', () => {
  const request = normalizePush({
    baseRevision: 3,
    extra: 'drop me',
    goals: [
      {
        id: 9,
        title: 'B',
        description: null,
        tone: 'mint',
        status: 'ACTIVE',
        targetDate: null,
        createdAt: 10,
        updatedAt: 11,
        completedAt: null,
        sortOrder: 2,
        ignored: true,
      },
      {
        id: 2,
        title: 'A',
        tone: 'amber',
        status: 'ACTIVE',
        createdAt: 1,
        updatedAt: 2,
        sortOrder: 1,
      },
    ],
    stages: [
      { id: 4, goalId: 9, title: 'second', position: 2, createdAt: 1, updatedAt: 1 },
      { id: 3, goalId: 9, title: 'first', position: 1, createdAt: 1, updatedAt: 1 },
    ],
    tasks: [
      { id: 8, title: 'later', priority: 'LOW', category: 'HOME', createdAt: 1 },
      { id: 7, title: 'earlier', priority: 'HIGH', category: 'WORK', createdAt: 1 },
    ],
    habits: [
      { id: 5, title: 'stretch', tone: 'mint', createdAt: 1, sortOrder: 2 },
      { id: 6, title: 'read', tone: 'amber', createdAt: 1, sortOrder: 1 },
    ],
    habitRecords: [
      { id: 11, habitId: 6, epochDay: 20000, checkedAt: 5 },
      { id: 10, habitId: 6, epochDay: 19999, checkedAt: 4 },
    ],
    growthEvents: [
      { id: 13, type: 'TASK_COMPLETED', sourceKey: 'task:1', xp: 10, occurredAt: 9, epochDay: 20000 },
      { id: 12, type: 'HABIT_CHECKED', sourceKey: 'habit:1', xp: 4, occurredAt: 8, epochDay: 19999 },
    ],
  })

  assert.equal(request.baseRevision, 3)
  assert.deepEqual(request.snapshot.goals.map((goal) => goal.id), [2, 9])
  assert.equal(request.snapshot.goals[0].description, null)
  assert.equal(request.snapshot.goals[0].sortOrder, 1)
  assert.equal('ignored' in request.snapshot.goals[1], false)
  assert.deepEqual(request.snapshot.stages.map((stage) => stage.id), [3, 4])
  assert.deepEqual(request.snapshot.tasks.map((task) => task.id), [7, 8])
  assert.equal(request.snapshot.tasks[0].isCompleted, false)
  assert.deepEqual(request.snapshot.habits.map((habit) => habit.id), [6, 5])
  assert.deepEqual(request.snapshot.habitRecords.map((record) => record.id), [10, 11])
  assert.deepEqual(request.snapshot.growthEvents.map((event) => event.id), [12, 13])
})

test('missing and null lists become empty arrays', () => {
  const request = normalizePush({ baseRevision: 0 })
  assert.deepEqual(request.snapshot, {
    goals: [],
    stages: [],
    tasks: [],
    habits: [],
    habitRecords: [],
    growthEvents: [],
  })
  const nulls = normalizePush({
    baseRevision: 0,
    goals: null,
    stages: null,
    tasks: null,
    habits: null,
    habitRecords: null,
    growthEvents: null,
  })
  assert.deepEqual(nulls.snapshot, request.snapshot)
})

test('base revision must be a non-negative integer', () => {
  for (const baseRevision of [-1, 1.5, 'abc', null, undefined]) {
    expectApiError(() => normalizePush({ baseRevision }), 'SYNC_REVISION', 400)
  }
  assert.equal(normalizePush({ baseRevision: '2' }).baseRevision, 2)
})

test('non-array lists are rejected', () => {
  expectApiError(
    () => normalizePush({ baseRevision: 0, goals: { id: 1 } }),
    'SYNC_INVALID',
    400,
  )
})

test('row counts keep the Spring 5000-row ceiling', () => {
  const rows = new Array(MAX_ROWS_PER_KIND + 1).fill({ id: 1 })
  expectApiError(
    () => normalizePush({ baseRevision: 0, goals: rows }),
    'SYNC_TOO_LARGE',
    400,
  )
})

test('database metadata is removed from stored snapshots', () => {
  const snapshot = snapshotFromDoc({
    _id: 'doc-1',
    userId: 42,
    revision: 7,
    updatedAt: 123,
    goals: [
      {
        id: 1,
        title: 'goal',
        description: null,
        tone: 'mint',
        status: 'ACTIVE',
        targetDate: null,
        createdAt: 1,
        updatedAt: 2,
        completedAt: null,
        sortOrder: 0,
      },
    ],
  })
  assert.equal(snapshot.revision, 7)
  assert.equal(snapshot.goals.length, 1)
  assert.equal('_id' in snapshot, false)
  assert.equal('userId' in snapshot, false)
  assert.equal('updatedAt' in snapshot, false)
  assert.deepEqual(snapshot.stages, [])
})
