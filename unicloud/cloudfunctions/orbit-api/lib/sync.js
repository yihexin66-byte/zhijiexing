'use strict'

const { ApiError } = require('./errors')

const MAX_ROWS_PER_KIND = 5000

const KINDS = [
  ['goals', '目标'],
  ['stages', '阶段'],
  ['tasks', '任务'],
  ['habits', '习惯'],
  ['habitRecords', '打卡'],
  ['growthEvents', '成长记录'],
]

function emptySnapshot() {
  return {
    revision: 0,
    goals: [],
    stages: [],
    tasks: [],
    habits: [],
    habitRecords: [],
    growthEvents: [],
  }
}

function text(value, fallback = null) {
  if (value == null) return fallback
  return typeof value === 'string' ? value : String(value)
}

function numberOrNull(value) {
  if (value == null || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function numberOr(value, fallback) {
  const parsed = numberOrNull(value)
  return parsed == null ? fallback : parsed
}

function normalizeGoal(row) {
  return {
    id: numberOr(row.id, 0),
    title: text(row.title, ''),
    description: text(row.description),
    tone: text(row.tone, ''),
    status: text(row.status, ''),
    targetDate: numberOrNull(row.targetDate),
    createdAt: numberOr(row.createdAt, 0),
    updatedAt: numberOr(row.updatedAt, 0),
    completedAt: numberOrNull(row.completedAt),
    sortOrder: numberOr(row.sortOrder, 0),
  }
}

function normalizeStage(row) {
  return {
    id: numberOr(row.id, 0),
    goalId: numberOr(row.goalId, 0),
    title: text(row.title, ''),
    position: numberOr(row.position, 0),
    targetDate: numberOrNull(row.targetDate),
    isCompleted: row.isCompleted === true,
    completedAt: numberOrNull(row.completedAt),
    createdAt: numberOr(row.createdAt, 0),
    updatedAt: numberOr(row.updatedAt, 0),
  }
}

function normalizeTask(row) {
  return {
    id: numberOr(row.id, 0),
    title: text(row.title, ''),
    description: text(row.description),
    priority: text(row.priority, ''),
    category: text(row.category, ''),
    dueDate: numberOrNull(row.dueDate),
    isCompleted: row.isCompleted === true,
    createdAt: numberOr(row.createdAt, 0),
    completedAt: numberOrNull(row.completedAt),
    goalId: numberOrNull(row.goalId),
    stageId: numberOrNull(row.stageId),
  }
}

function normalizeHabit(row) {
  return {
    id: numberOr(row.id, 0),
    title: text(row.title, ''),
    note: text(row.note),
    tone: text(row.tone, ''),
    createdAt: numberOr(row.createdAt, 0),
    archivedAt: numberOrNull(row.archivedAt),
    sortOrder: numberOr(row.sortOrder, 0),
  }
}

function normalizeHabitRecord(row) {
  return {
    id: numberOr(row.id, 0),
    habitId: numberOr(row.habitId, 0),
    epochDay: numberOr(row.epochDay, 0),
    checkedAt: numberOr(row.checkedAt, 0),
  }
}

function normalizeGrowthEvent(row) {
  return {
    id: numberOr(row.id, 0),
    type: text(row.type, ''),
    sourceKey: text(row.sourceKey, ''),
    xp: numberOr(row.xp, 0),
    occurredAt: numberOr(row.occurredAt, 0),
    epochDay: numberOr(row.epochDay, 0),
  }
}

const NORMALIZERS = {
  goals: normalizeGoal,
  stages: normalizeStage,
  tasks: normalizeTask,
  habits: normalizeHabit,
  habitRecords: normalizeHabitRecord,
  growthEvents: normalizeGrowthEvent,
}

function compare(left, right) {
  return left < right ? -1 : left > right ? 1 : 0
}

function sortSnapshot(snapshot) {
  snapshot.goals.sort(
    (a, b) => compare(a.sortOrder, b.sortOrder) || compare(a.id, b.id),
  )
  snapshot.stages.sort(
    (a, b) => compare(a.goalId, b.goalId) || compare(a.position, b.position),
  )
  snapshot.tasks.sort((a, b) => compare(a.id, b.id))
  snapshot.habits.sort(
    (a, b) => compare(a.sortOrder, b.sortOrder) || compare(a.id, b.id),
  )
  snapshot.habitRecords.sort((a, b) => compare(a.id, b.id))
  snapshot.growthEvents.sort((a, b) => compare(a.id, b.id))
  return snapshot
}

/**
 * Converts a client PUT body into the canonical snapshot shape and validates
 * limits the same way the Spring backend did. Unknown fields are discarded.
 */
function normalizePush(body) {
  if (body == null || typeof body !== 'object' || Array.isArray(body)) {
    throw new ApiError(400, 'MALFORMED_BODY', '请求内容不是合法的 JSON')
  }

  const baseRevision = numberOrNull(body.baseRevision)
  if (baseRevision == null || !Number.isInteger(baseRevision) || baseRevision < 0) {
    throw new ApiError(400, 'SYNC_REVISION', '同步版本号不合法')
  }

  const snapshot = {}
  for (const [kind, label] of KINDS) {
    const value = body[kind]
    if (value == null) {
      snapshot[kind] = []
      continue
    }
    if (!Array.isArray(value)) {
      throw new ApiError(400, 'SYNC_INVALID', '同步数据格式不合法')
    }
    if (value.length > MAX_ROWS_PER_KIND) {
      throw new ApiError(400, 'SYNC_TOO_LARGE', label + '数量超过单次同步上限')
    }
    snapshot[kind] = value.map((row) => NORMALIZERS[kind](row && typeof row === 'object' ? row : {}))
  }

  return { baseRevision, snapshot: sortSnapshot(snapshot) }
}

/** Strips database metadata and fills missing arrays with empty lists. */
function snapshotFromDoc(doc) {
  if (!doc) return emptySnapshot()
  const snapshot = {
    revision: numberOr(doc.revision, 0),
    goals: [],
    stages: [],
    tasks: [],
    habits: [],
    habitRecords: [],
    growthEvents: [],
  }
  for (const [kind] of KINDS) {
    const rows = Array.isArray(doc[kind]) ? doc[kind] : []
    snapshot[kind] = rows.map((row) => NORMALIZERS[kind](row && typeof row === 'object' ? row : {}))
  }
  return sortSnapshot(snapshot)
}

module.exports = {
  MAX_ROWS_PER_KIND,
  emptySnapshot,
  normalizePush,
  snapshotFromDoc,
}
