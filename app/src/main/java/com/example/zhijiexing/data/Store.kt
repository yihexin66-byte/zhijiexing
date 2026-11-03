package com.example.zhijiexing.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class SessionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    var token: String = prefs.getString(KEY_TOKEN, "") ?: ""
        private set

    val defaultBaseUrl: String = DEFAULT_BASE_URL

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, defaultBaseUrl)?.takeIf { it.isNotBlank() } ?: defaultBaseUrl
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()
        }

    /** 只有显式切换到本机/局域网后端时才需要申请 Android 17 的本地网络权限。 */
    fun usesLocalServer(): Boolean {
        val host = runCatching { java.net.URI(baseUrl).host }.getOrNull()?.lowercase() ?: return false
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") return true
        if (host.startsWith("10.") || host.startsWith("192.168.")) return true
        return Regex("^172\\.(1[6-9]|2\\d|3[01])\\.").containsMatchIn(host)
    }

    var dailyGoal: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, 60)
        set(value) = prefs.edit().putInt(KEY_DAILY_GOAL, value.coerceIn(10, 240)).apply()

    var level: String
        get() = prefs.getString(KEY_LEVEL, "入门") ?: "入门"
        set(value) = prefs.edit().putString(KEY_LEVEL, value).apply()

    var direction: String
        get() = prefs.getString(KEY_DIRECTION, "HarmonyOS 应用开发") ?: "HarmonyOS 应用开发"
        set(value) = prefs.edit().putString(KEY_DIRECTION, value).apply()

    fun readUser(): UserInfo? {
        if (token.isBlank()) return null
        val raw = prefs.getString(KEY_USER, "") ?: return null
        return try {
            UserInfo.fromJson(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }

    fun saveSession(token: String, user: UserInfo) {
        this.token = token
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, user.toJson().toString())
            .apply()
    }

    fun clearSession() {
        token = ""
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
    }

    // ---------- 个人资料（昵称 & 头像，按账号保存） ----------

    fun displayName(username: String): String =
        prefs.getString(KEY_NAME_PREFIX + username, "")?.takeIf { it.isNotBlank() } ?: username

    fun saveDisplayName(username: String, name: String) {
        prefs.edit().putString(KEY_NAME_PREFIX + username, name.trim()).apply()
    }

    fun avatarPath(username: String): String? {
        val path = prefs.getString(KEY_AVATAR_PREFIX + username, "") ?: return null
        if (path.isBlank() || !java.io.File(path).exists()) return null
        return path
    }

    fun saveAvatarPath(username: String, path: String) {
        prefs.edit().putString(KEY_AVATAR_PREFIX + username, path).apply()
    }

    // ---------- 知识学习（已读知识点，仅保存在本机） ----------

    fun readKnowledge(): Set<String> =
        prefs.getStringSet(KEY_KNOWLEDGE_READ, null)?.toSet() ?: emptySet()

    fun markKnowledgeRead(id: String) {
        prefs.edit().putStringSet(KEY_KNOWLEDGE_READ, readKnowledge() + id).apply()
    }

    // ---------- 学习时长（按天累计，前台每分钟记一次） ----------

    fun addStudyMinute() {
        val key = "study_" + DateUtils.todayKey()
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    fun studyMinutesByDay(): Map<String, Int> {
        val result = LinkedHashMap<String, Int>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("study_") && value is Int) {
                result[key.removePrefix("study_")] = value
            }
        }
        return result
    }

    fun studyMinutesOn(day: String): Int = prefs.getInt("study_$day", 0)

    // ---------- 收藏 & 错题 ----------

    fun bookmarks(): List<SavedQuestion> = readQuestions(KEY_BOOKMARKS)

    fun wrongQuestions(): List<SavedQuestion> = readQuestions(KEY_WRONGS)

    fun toggleBookmark(question: SavedQuestion): Boolean {
        val list = bookmarks().toMutableList()
        val index = list.indexOfFirst { it.questionId == question.questionId }
        val added = index < 0
        if (added) list.add(0, question) else list.removeAt(index)
        writeQuestions(KEY_BOOKMARKS, list)
        return added
    }

    fun isBookmarked(questionId: String): Boolean =
        bookmarks().any { it.questionId == questionId }

    fun saveWrongQuestions(items: List<SavedQuestion>) {
        if (items.isEmpty()) return
        val merged = LinkedHashMap<String, SavedQuestion>()
        items.forEach { merged[it.questionId] = it }
        readQuestions(KEY_WRONGS).forEach { merged.putIfAbsent(it.questionId, it) }
        writeQuestions(KEY_WRONGS, merged.values.take(300))
    }

    fun clearWrongQuestions() {
        prefs.edit().remove(KEY_WRONGS).apply()
    }

    private fun readQuestions(key: String): List<SavedQuestion> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)?.let { SavedQuestion.fromJson(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeQuestions(key: String, list: List<SavedQuestion>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    // ---------- 学习计划 ----------

    fun loadPlan(): StudyPlan {
        val raw = prefs.getString(KEY_PLAN, "") ?: ""
        if (raw.isNotBlank()) {
            try {
                return planFromJson(JSONObject(raw))
            } catch (_: Exception) {
                // 数据损坏时回落到默认计划
            }
        }
        return PlanFactory.defaultPlan()
    }

    fun savePlan(plan: StudyPlan) {
        prefs.edit().putString(KEY_PLAN, planToJson(plan).toString()).apply()
    }

    fun clearLocalCache() {
        val keepToken = token
        val keepUser = prefs.getString(KEY_USER, "")
        val keepUrl = baseUrl
        val username = readUser()?.username
        val keepName = username?.let { prefs.getString(KEY_NAME_PREFIX + it, "") }
        val keepAvatar = username?.let { prefs.getString(KEY_AVATAR_PREFIX + it, "") }
        prefs.edit().clear().apply()
        prefs.edit().putString(KEY_TOKEN, keepToken)
            .putString(KEY_USER, keepUser ?: "")
            .putString(KEY_BASE_URL, keepUrl)
            .apply()
        if (username != null) {
            if (!keepName.isNullOrBlank()) prefs.edit().putString(KEY_NAME_PREFIX + username, keepName).apply()
            if (!keepAvatar.isNullOrBlank()) prefs.edit().putString(KEY_AVATAR_PREFIX + username, keepAvatar).apply()
        }
        this.token = keepToken
    }

    private fun planToJson(plan: StudyPlan): JSONObject {
        val stages = JSONArray()
        plan.stages.forEach { stage ->
            val tasks = JSONArray()
            stage.tasks.forEach { task ->
                tasks.put(JSONObject().put("title", task.title)
                    .put("minutes", task.minutes).put("done", task.done))
            }
            stages.put(JSONObject()
                .put("title", stage.title)
                .put("range", stage.range)
                .put("description", stage.description)
                .put("tasks", tasks))
        }
        return JSONObject()
            .put("name", plan.name)
            .put("startDate", plan.startDate)
            .put("endDate", plan.endDate)
            .put("stages", stages)
    }

    private fun planFromJson(obj: JSONObject): StudyPlan {
        val stages = ArrayList<PlanStage>()
        val stageArray = obj.optJSONArray("stages") ?: JSONArray()
        for (i in 0 until stageArray.length()) {
            val s = stageArray.optJSONObject(i) ?: continue
            val tasks = ArrayList<PlanTask>()
            val taskArray = s.optJSONArray("tasks") ?: JSONArray()
            for (j in 0 until taskArray.length()) {
                val t = taskArray.optJSONObject(j) ?: continue
                tasks.add(PlanTask(t.str("title"), t.optInt("minutes", 30), t.optBoolean("done", false)))
            }
            stages.add(PlanStage(s.str("title"), s.str("range"), s.str("description"), tasks))
        }
        return StudyPlan(obj.str("name"), obj.str("startDate"), obj.str("endDate"), stages)
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/exam"

        private const val PREFS_NAME = "zhijiexing_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user_info"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_NAME_PREFIX = "display_name_"
        private const val KEY_AVATAR_PREFIX = "avatar_path_"
        private const val KEY_KNOWLEDGE_READ = "knowledge_read"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_LEVEL = "level"
        private const val KEY_DIRECTION = "direction"
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_WRONGS = "wrong_questions"
        private const val KEY_PLAN = "study_plan"
    }
}

object PlanFactory {
    val templates = listOf(
        PlanTemplate(
            key = "harmony",
            name = "30 天 HarmonyOS 认证冲刺",
            days = 30,
            tag = "推荐",
            description = "ArkTS 基础 → ArkUI 界面 → 真题冲刺，三阶段直通认证。"
        ),
        PlanTemplate(
            key = "python",
            name = "30 天 Python 基础",
            days = 30,
            tag = "入门",
            description = "从语法到数据结构，配合小项目建立编程手感。"
        ),
        PlanTemplate(
            key = "web",
            name = "21 天 前端进阶",
            days = 21,
            tag = "进阶",
            description = "HTML/CSS/JavaScript 核心与移动端适配速成。"
        )
    )

    fun defaultPlan(): StudyPlan = create(templates[0], "30 天 HarmonyOS 认证冲刺")

    fun create(template: PlanTemplate, name: String): StudyPlan {
        val start = DateUtils.startOfDayOffset(0)
        val stageCount = 3
        val perStage = (template.days / stageCount).coerceAtLeast(2)
        val specs: List<Pair<String, List<String>>> = when (template.key) {
            "python" -> listOf(
                "阶段一：Python 基础语法" to listOf(
                    "认识 Python 与开发环境", "变量与数据类型", "条件与循环", "函数基础", "综合练习"
                ),
                "阶段二：数据结构与算法" to listOf(
                    "列表与元组", "字典与集合", "排序与查找", "字符串处理", "小项目实战"
                ),
                "阶段三：项目实践" to listOf(
                    "需求拆解", "模块化开发", "调试与测试", "成品复盘"
                )
            )
            "web" -> listOf(
                "阶段一：HTML 与 CSS" to listOf(
                    "语义化标签", "盒模型与布局", "Flex 与 Grid", "响应式适配"
                ),
                "阶段二：JavaScript 核心" to listOf(
                    "变量与函数", "DOM 操作", "事件与表单", "异步与请求"
                ),
                "阶段三：移动端项目" to listOf(
                    "页面搭建", "接口联调", "性能优化", "发布上线"
                )
            )
            else -> listOf(
                "阶段一：ArkTS 语法筑基" to listOf(
                    "认识 DevEco Studio 与工程结构", "ArkTS 变量与类型", "条件、循环与数组",
                    "函数与类", "状态管理 @State / @Prop", "阶段小测：基础知识"
                ),
                "阶段二：ArkUI 界面开发" to listOf(
                    "布局容器与自适应", "常用组件与样式", "List / Grid 列表", "页面路由与导航",
                    "网络请求与数据解析", "数据持久化", "阶段实战：题库首页"
                ),
                "阶段三：认证冲刺" to listOf(
                    "完成基础模拟卷", "复盘错题与解析", "完成高级模拟卷", "高频考点速记",
                    "模拟考试一次"
                )
            )
        }
        val stages = specs.mapIndexed { index, (title, tasks) ->
            val stageStart = DateUtils.addDays(start, index * perStage)
            val stageEnd = DateUtils.addDays(
                start,
                if (index == specs.lastIndex) template.days - 1 else (index + 1) * perStage - 1
            )
            val minutes = listOf(30, 45, 40, 50, 60, 45, 50)
            PlanStage(
                title = title,
                range = DateUtils.formatPointRange(stageStart, stageEnd),
                description = when (index) {
                    0 -> "打牢 ${template.name.removePrefix("${template.days} 天 ")} 的基础，形成每天稳定学习的节奏。"
                    1 -> "把知识放进真实界面与数据流里，完成可运行的阶段作品。"
                    else -> "用真题与复盘收口，查漏补缺，冲刺目标分数。"
                },
                tasks = tasks.mapIndexed { ti, task ->
                    PlanTask(task, minutes[ti % minutes.size], false)
                }
            )
        }
        return StudyPlan(
            name = name,
            startDate = DateUtils.formatPoint(start),
            endDate = DateUtils.formatPoint(DateUtils.addDays(start, template.days - 1)),
            stages = stages
        )
    }

    fun daysBetween(startDate: String, endDate: String): Int = 30

    fun weekLabels(count: Int = 7): List<String> {
        val cal = Calendar.getInstance()
        return (count - 1 downTo 0).map { offset ->
            cal.timeInMillis = DateUtils.addDays(System.currentTimeMillis(), -offset)
            DateUtils.weekdayLabel(cal.timeInMillis)
        }
    }
}
