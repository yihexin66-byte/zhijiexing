package com.example.zhijiexing.data

import org.json.JSONArray
import org.json.JSONObject

data class UserInfo(
    val id: String,
    val username: String,
    val role: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("username", username)
        .put("role", role)

    companion object {
        fun fromJson(obj: JSONObject): UserInfo = UserInfo(
            id = obj.str("id"),
            username = obj.str("username"),
            role = obj.str("role")
        )
    }
}

data class PaperInfo(
    val id: String,
    val name: String,
    val description: String,
    val totalScore: Int,
    val passScore: Int,
    val duration: Int,
    val questionCount: Int,
    val categoryName: String,
    val levelName: String,
    val attemptCount: Int,
    val bestScore: Int
)

data class QuestionOption(
    val label: String,
    val content: String
)

data class QuestionItem(
    val id: String,
    val type: String,
    val content: String,
    val score: Int,
    val options: List<QuestionOption>,
    val myAnswer: String?,
    val isFlagged: Boolean
) {
    val typeLabel: String
        get() = when (type) {
            "JUDGE" -> "判断题"
            "MULTI" -> "多选题"
            else -> "单选题"
        }
}

data class AttemptInfo(
    val attemptId: String,
    val paperName: String,
    val remainingSeconds: Int,
    val totalScore: Int,
    val passScore: Int,
    val duration: Int,
    val questions: List<QuestionItem>
)

data class SubmitResult(
    val attemptId: String,
    val score: Int,
    val totalScore: Int,
    val passScore: Int,
    val correctCount: Int,
    val totalQuestions: Int,
    val passed: Boolean,
    val usedTime: Int,
    val submitType: String,
    val paperId: String,
    val certificateGenerated: Boolean
)

data class ReviewQuestion(
    val questionId: String,
    val type: String,
    val content: String,
    val correctAnswer: String,
    val userAnswer: String?,
    val isCorrect: Boolean,
    val earnedScore: Int,
    val maxScore: Int,
    val analysis: String,
    val options: List<QuestionOption>
) {
    val typeLabel: String
        get() = when (type) {
            "JUDGE" -> "判断题"
            "MULTI" -> "多选题"
            else -> "单选题"
        }
}

data class ReviewDetail(
    val attemptId: String,
    val paperName: String,
    val score: Int,
    val totalScore: Int,
    val passed: Boolean,
    val questions: List<ReviewQuestion>
)

data class AttemptRecord(
    val id: String,
    val startTime: String,
    val score: Int,
    val passed: Boolean,
    val usedTime: Int,
    val submitType: String,
    val paperId: String,
    val paperName: String
)

data class CertificateInfo(
    val id: String,
    val certificateNo: String,
    val name: String,
    val issueDate: String,
    val expiryDate: String,
    val status: String
)

/** 本地保存的题目（收藏 / 错题），字段足以离线还原解析页。 */
data class SavedQuestion(
    val questionId: String,
    val paperName: String,
    val type: String,
    val content: String,
    val correctAnswer: String,
    val userAnswer: String?,
    val analysis: String,
    val options: List<QuestionOption>,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("questionId", questionId)
        .put("paperName", paperName)
        .put("type", type)
        .put("content", content)
        .put("correctAnswer", correctAnswer)
        .put("userAnswer", userAnswer ?: JSONObject.NULL)
        .put("analysis", analysis)
        .put("savedAt", savedAt)
        .put("options", JSONArray().apply {
            options.forEach { o ->
                put(JSONObject().put("label", o.label).put("content", o.content))
            }
        })

    companion object {
        fun fromJson(obj: JSONObject): SavedQuestion = SavedQuestion(
            questionId = obj.str("questionId"),
            paperName = obj.str("paperName"),
            type = obj.str("type"),
            content = obj.str("content"),
            correctAnswer = obj.str("correctAnswer"),
            userAnswer = if (obj.isNull("userAnswer")) null else obj.optString("userAnswer"),
            analysis = obj.str("analysis"),
            options = obj.optJSONArray("options").toOptions(),
            savedAt = obj.optLong("savedAt", System.currentTimeMillis())
        )
    }
}

data class PlanTask(
    val title: String,
    val minutes: Int,
    val done: Boolean = false
)

data class PlanStage(
    val title: String,
    val range: String,
    val description: String,
    val tasks: List<PlanTask>
)

data class StudyPlan(
    val name: String,
    val startDate: String,
    val endDate: String,
    val stages: List<PlanStage>
) {
    val totalTasks: Int get() = stages.sumOf { it.tasks.size }
    val doneTasks: Int get() = stages.sumOf { stage -> stage.tasks.count { it.done } }
    val progress: Int get() = if (totalTasks == 0) 0 else doneTasks * 100 / totalTasks

    fun toggle(stageIndex: Int, taskIndex: Int): StudyPlan {
        val newStages = stages.mapIndexed { si, stage ->
            if (si != stageIndex) stage else stage.copy(tasks = stage.tasks.mapIndexed { ti, task ->
                if (ti != taskIndex) task else task.copy(done = !task.done)
            })
        }
        return copy(stages = newStages)
    }
}

data class PlanTemplate(
    val key: String,
    val name: String,
    val days: Int,
    val tag: String,
    val description: String
)

fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

fun JSONObject.optStr(key: String, fallback: String = ""): String =
    if (isNull(key)) fallback else optString(key, fallback)

fun JSONArray?.toOptions(): List<QuestionOption> {
    if (this == null) return emptyList()
    val list = ArrayList<QuestionOption>()
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        list.add(QuestionOption(o.str("label"), o.str("content")))
    }
    return list
}

fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    val list = ArrayList<String>()
    for (i in 0 until length()) list.add(optString(i))
    return list
}
