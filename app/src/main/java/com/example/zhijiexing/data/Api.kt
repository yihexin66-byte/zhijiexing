package com.example.zhijiexing.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiException(message: String, val code: Int = -1) : Exception(message)

class UnauthorizedException(message: String = "登录状态已失效，请重新登录") : Exception(message)

class ApiClient(private val store: SessionStore) {
    var onUnauthorized: (() -> Unit)? = null

    suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val base = store.baseUrl.trim().trimEnd('/')
        val url = URL(if (path.startsWith("http")) path else base + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            if (store.token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${store.token}")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val httpCode = conn.responseCode
            val stream = if (httpCode in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }.orEmpty()
            Log.d("ZhijiexingApi", "$method ${conn.url} -> HTTP $httpCode, ${raw.length} bytes")

            if (httpCode == 401) {
                store.clearSession()
                onUnauthorized?.invoke()
                throw UnauthorizedException()
            }
            if (raw.isBlank()) {
                if (httpCode in 200..299) return@withContext JSONObject()
                throw ApiException("服务器返回异常（HTTP $httpCode）", httpCode)
            }
            val json = try {
                JSONObject(raw)
            } catch (_: Exception) {
                throw ApiException(if (httpCode in 200..299) "服务器返回格式异常" else "服务器异常（HTTP $httpCode）", httpCode)
            }
            val code = json.optInt("code", httpCode)
            if (code == 401) {
                store.clearSession()
                onUnauthorized?.invoke()
                throw UnauthorizedException()
            }
            if (code != 200) {
                throw ApiException(json.optString("message", "请求失败"), code)
            }
            json
        } finally {
            conn.disconnect()
        }
    }

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    fun dataArray(json: JSONObject): JSONArray = json.optJSONArray("data") ?: JSONArray()

    fun dataObject(json: JSONObject): JSONObject = json.optJSONObject("data") ?: JSONObject()
}

class AuthApi(private val client: ApiClient) {
    suspend fun login(username: String, password: String): Pair<String, UserInfo> {
        val response = client.request(
            "POST", "/api/auth/login",
            JSONObject().put("username", username).put("password", password)
        )
        val data = client.dataObject(response)
        val token = data.str("token")
        val user = UserInfo.fromJson(data.optJSONObject("userInfo") ?: JSONObject())
        if (token.isBlank()) throw ApiException("登录响应缺少 token")
        return token to user
    }

    suspend fun register(username: String, password: String, confirmPassword: String) {
        client.request(
            "POST", "/api/auth/register",
            JSONObject()
                .put("username", username)
                .put("password", password)
                .put("confirmPassword", confirmPassword)
        )
    }
}

class ExamApi(private val client: ApiClient) {

    suspend fun papers(): List<PaperInfo> {
        val response = client.request("GET", "/api/exam/papers")
        val array = client.dataArray(response)
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.toPaper()
        }
    }

    suspend fun startExam(paperId: String): AttemptInfo {
        val response = client.request(
            "POST", "/api/exam/attempts",
            JSONObject().put("paperId", paperId)
        )
        val data = client.dataObject(response)
        val questions = data.optJSONArray("questions") ?: JSONArray()
        return AttemptInfo(
            attemptId = data.optStr("attemptId", data.str("id")),
            paperName = data.str("paperName"),
            remainingSeconds = data.optInt("remainingSeconds", 0),
            totalScore = data.optInt("totalScore", 100),
            passScore = data.optInt("passScore", 60),
            duration = data.optInt("duration", 60),
            questions = (0 until questions.length()).mapNotNull { i ->
                questions.optJSONObject(i)?.toQuestion()
            }
        )
    }

    suspend fun saveAnswer(attemptId: String, questionId: String, answer: String, flagged: Boolean) {
        client.request(
            "PUT", "/api/exam/attempts/$attemptId/answer",
            JSONObject()
                .put("questionId", questionId)
                .put("answer", answer)
                .put("isFlagged", flagged)
        )
    }

    suspend fun reportTabSwitch(attemptId: String, count: Int): TabSwitchResult {
        val response = client.request(
            "PUT", "/api/exam/attempts/$attemptId/tab-switch",
            JSONObject().put("tabSwitchCount", count)
        )
        val data = client.dataObject(response)
        return TabSwitchResult(
            count = data.optInt("tabSwitchCount", count),
            limit = data.optInt("tabSwitchLimit", 3),
            autoSubmit = data.optBoolean("shouldAutoSubmit", false)
        )
    }

    suspend fun submit(attemptId: String, type: String, tabSwitchCount: Int): SubmitResult {
        val response = client.request(
            "POST", "/api/exam/attempts/$attemptId/submit",
            JSONObject().put("submitType", type).put("tabSwitchCount", tabSwitchCount)
        )
        return client.dataObject(response).toSubmitResult()
    }

    suspend fun result(attemptId: String): SubmitResult {
        val response = client.request("GET", "/api/exam/attempts/$attemptId/result")
        return client.dataObject(response).toSubmitResult()
    }

    suspend fun review(attemptId: String): ReviewDetail {
        val response = client.request("GET", "/api/exam/attempts/$attemptId/review")
        val data = client.dataObject(response)
        val questions = data.optJSONArray("questions") ?: JSONArray()
        return ReviewDetail(
            attemptId = data.optStr("attemptId", attemptId),
            paperName = data.str("paperName"),
            score = data.optInt("score", 0),
            totalScore = data.optInt("totalScore", 100),
            passed = data.optInt("isPassed", 0) == 1,
            questions = (0 until questions.length()).mapNotNull { i ->
                questions.optJSONObject(i)?.toReviewQuestion()
            }
        )
    }

    suspend fun records(page: Int = 1, size: Int = 100): List<AttemptRecord> {
        val response = client.request("GET", "/api/exam/attempts?page=$page&size=$size")
        val data = client.dataObject(response)
        val records = data.optJSONArray("records") ?: JSONArray()
        return (0 until records.length()).mapNotNull { i ->
            records.optJSONObject(i)?.toAttemptRecord()
        }
    }

    suspend fun certificates(): List<CertificateInfo> {
        val response = client.request("GET", "/api/exam/certificates")
        val array = client.dataArray(response)
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let { o ->
                CertificateInfo(
                    id = o.str("id"),
                    certificateNo = o.optStr("certificateNo", o.optStr("certNo")),
                    name = o.optStr("name", o.optStr("certName")),
                    issueDate = o.str("issueDate"),
                    expiryDate = o.str("expiryDate"),
                    status = o.str("status")
                )
            }
        }
    }

    private fun JSONObject.toPaper(): PaperInfo = PaperInfo(
        id = str("id"),
        name = optStr("name", optStr("paperName")),
        description = str("description"),
        totalScore = optInt("totalScore", 100),
        passScore = optInt("passScore", 60),
        duration = optInt("duration", 60),
        questionCount = optInt("questionCount", 0),
        categoryName = str("categoryName"),
        levelName = str("levelName"),
        attemptCount = optInt("attemptCount", 0),
        bestScore = optInt("bestScore", 0)
    )

    private fun JSONObject.toQuestion(): QuestionItem = QuestionItem(
        id = str("id"),
        type = optStr("type", "SINGLE"),
        content = str("content"),
        score = optInt("score", 2),
        options = optJSONArray("options").toOptions(),
        myAnswer = if (isNull("myAnswer")) null else optString("myAnswer").takeIf { it.isNotBlank() },
        isFlagged = optBoolean("isFlagged", false)
    )

    private fun JSONObject.toReviewQuestion(): ReviewQuestion = ReviewQuestion(
        questionId = optStr("questionId", str("id")),
        type = optStr("type", "SINGLE"),
        content = str("content"),
        correctAnswer = str("correctAnswer"),
        userAnswer = if (isNull("userAnswer")) null else optString("userAnswer").takeIf { it.isNotBlank() },
        isCorrect = optBoolean("isCorrect", false),
        earnedScore = optInt("earnedScore", 0),
        maxScore = optInt("maxScore", 0),
        analysis = str("analysis"),
        options = optJSONArray("options").toOptions()
    )

    private fun JSONObject.toSubmitResult(): SubmitResult = SubmitResult(
        attemptId = optStr("attemptId", str("id")),
        score = optInt("score", 0),
        totalScore = optInt("totalScore", 100),
        passScore = optInt("passScore", 60),
        correctCount = optInt("correctCount", 0),
        totalQuestions = optInt("totalQuestions", 0),
        passed = optBoolean("passed", false),
        usedTime = optInt("usedTime", 0),
        submitType = str("submitType"),
        paperId = str("paperId"),
        certificateGenerated = optBoolean("certificateGenerated", false)
    )

    private fun JSONObject.toAttemptRecord(): AttemptRecord = AttemptRecord(
        id = str("id"),
        startTime = str("startTime"),
        score = optInt("score", 0),
        passed = optBoolean("passed", false),
        usedTime = optInt("usedTime", 0),
        submitType = str("submitType"),
        paperId = str("paperId"),
        paperName = str("paperName")
    )
}

data class TabSwitchResult(
    val count: Int,
    val limit: Int,
    val autoSubmit: Boolean
)
