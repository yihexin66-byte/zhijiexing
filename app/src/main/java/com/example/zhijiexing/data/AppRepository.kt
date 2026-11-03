package com.example.zhijiexing.data

import android.content.Context

data class StudyStats(
    val todayMinutes: Int,
    val weekMinutes: Int,
    val streak: Int,
    val totalMinutes: Int,
    val dailyGoal: Int,
    val dailyProgress: Float,
    val last7Days: List<Int>,
    val last7Labels: List<String>
)

class AppRepository(context: Context) {
    val store = SessionStore(context)
    val apiClient = ApiClient(store)
    val authApi = AuthApi(apiClient)
    val examApi = ExamApi(apiClient)

    private var papersCache: List<PaperInfo>? = null
    private var recordsCache: List<AttemptRecord>? = null
    private val reviewCache = HashMap<String, ReviewDetail>()
    private val resultCache = HashMap<String, SubmitResult>()

    suspend fun papers(force: Boolean = false): List<PaperInfo> {
        if (!force) papersCache?.let { return it }
        val data = examApi.papers()
        papersCache = data
        return data
    }

    suspend fun records(force: Boolean = false): List<AttemptRecord> {
        if (!force) recordsCache?.let { return it }
        val data = examApi.records()
        recordsCache = data
        return data
    }

    suspend fun review(attemptId: String, force: Boolean = false): ReviewDetail {
        if (!force) reviewCache[attemptId]?.let { return it }
        val data = examApi.review(attemptId)
        reviewCache[attemptId] = data
        if (data.questions.isNotEmpty()) {
            store.saveWrongQuestions(
                data.questions.filter { !it.isCorrect }.map {
                    SavedQuestion(
                        questionId = it.questionId,
                        paperName = data.paperName,
                        type = it.type,
                        content = it.content,
                        correctAnswer = it.correctAnswer,
                        userAnswer = it.userAnswer,
                        analysis = it.analysis,
                        options = it.options
                    )
                }
            )
        }
        return data
    }

    suspend fun result(attemptId: String, force: Boolean = false): SubmitResult {
        if (!force) resultCache[attemptId]?.let { return it }
        val data = examApi.result(attemptId)
        resultCache[attemptId] = data
        return data
    }

    fun cacheResult(result: SubmitResult) {
        resultCache[result.attemptId] = result
        recordsCache = null
        papersCache = null
    }

    fun cachedReview(attemptId: String): ReviewDetail? = reviewCache[attemptId]

    fun clearNetworkCaches() {
        papersCache = null
        recordsCache = null
    }

    fun toggleBookmark(question: ReviewQuestion, paperName: String): Boolean =
        store.toggleBookmark(
            SavedQuestion(
                questionId = question.questionId,
                paperName = paperName,
                type = question.type,
                content = question.content,
                correctAnswer = question.correctAnswer,
                userAnswer = question.userAnswer,
                analysis = question.analysis,
                options = question.options
            )
        )

    fun toggleBookmark(question: SavedQuestion): Boolean = store.toggleBookmark(question)

    fun recordStudyMinute() {
        store.addStudyMinute()
    }

    fun studyStats(): StudyStats {
        val local = store.studyMinutesByDay()
        val records = recordsCache.orEmpty()
        val attemptsByDay = HashMap<String, Int>()
        for (record in records) {
            val millis = DateUtils.parseServerTime(record.startTime) ?: continue
            val key = DateUtils.dayKey(millis)
            attemptsByDay[key] = (attemptsByDay[key] ?: 0) + record.usedTime / 60
        }
        fun minutesOn(key: String): Int {
            val localMinutes = local[key] ?: 0
            val attemptMinutes = attemptsByDay[key] ?: 0
            return maxOf(localMinutes, attemptMinutes)
        }
        val today = DateUtils.todayKey()
        val todayMinutes = minutesOn(today)
        val week = DateUtils.weekRange()
        var weekMinutes = 0
        val total = local.values.sum() + attemptsByDay.entries.sumOf { entry ->
            if (!local.containsKey(entry.key)) entry.value else 0
        }
        val weekStartKey = DateUtils.dayKey(week.first)
        val weekEndKey = DateUtils.dayKey(week.second - 1)
        for ((day, minutes) in local) {
            if (day in weekStartKey..weekEndKey) weekMinutes += maxOf(minutes, attemptsByDay[day] ?: 0)
        }
        for ((day, minutes) in attemptsByDay) {
            if (day in weekStartKey..weekEndKey && !local.containsKey(day)) weekMinutes += minutes
        }

        var streak = 0
        var cursor = 0
        if (minutesOn(today) == 0) cursor = -1
        while (streak < 365) {
            val key = DateUtils.dayKey(DateUtils.startOfDayOffset(cursor))
            if (minutesOn(key) > 0) {
                streak++
                cursor--
            } else {
                break
            }
        }

        val dayStarts = DateUtils.lastNDayStarts(7)
        val last7 = dayStarts.map { minutesOn(DateUtils.dayKey(it)) }
        val labels = dayStarts.map { DateUtils.weekdayLabel(it) }
        val goal = store.dailyGoal
        return StudyStats(
            todayMinutes = todayMinutes,
            weekMinutes = weekMinutes,
            streak = streak,
            totalMinutes = total,
            dailyGoal = goal,
            dailyProgress = (todayMinutes.toFloat() / goal).coerceIn(0f, 1f),
            last7Days = last7,
            last7Labels = labels
        )
    }

    fun todayTasks(plan: StudyPlan): List<Pair<Int, PlanTask>> {
        val result = ArrayList<Pair<Int, PlanTask>>()
        plan.stages.forEachIndexed { si, stage ->
            stage.tasks.forEach { task ->
                if (!task.done) result.add(si to task)
            }
        }
        return result.take(3)
    }

    fun planForTodayProgress(plan: StudyPlan): Pair<Int, Int> {
        val tasks = plan.stages.flatMap { it.tasks }
        val preview = tasks.take(5)
        return preview.count { it.done } to preview.size
    }
}
