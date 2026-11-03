package com.example.zhijiexing

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.zhijiexing.data.AppRepository
import com.example.zhijiexing.data.StudyPlan
import com.example.zhijiexing.data.StudyStats
import com.example.zhijiexing.data.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

sealed interface Screen {
    data class Quiz(val paperId: String, val paperName: String) : Screen
    data class Result(val attemptId: String) : Screen
    data class ReviewList(val attemptId: String) : Screen
    data class ReviewDetail(val attemptId: String, val index: Int) : Screen
    data class SavedQuestionDetail(val questionId: String, val bookmark: Boolean) : Screen
    data object Records : Screen
    data object Certificates : Screen
    data object WrongBook : Screen
    data object Bookmarks : Screen
    data object Settings : Screen
    data object KnowledgeList : Screen
    data class KnowledgeDetail(val id: String) : Screen
}

class AppState(context: Context) {
    val repository = AppRepository(context.applicationContext)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val snackbar = SnackbarHostState()

    var booting by mutableStateOf(true)
        private set
    var session by mutableStateOf(if (repository.store.token.isNotBlank()) repository.store.readUser() else null)
        private set

    var selectedTab by mutableIntStateOf(0)
    val stack = mutableStateListOf<Screen>()
    var dataVersion by mutableIntStateOf(0)
        private set
    var plan by mutableStateOf(repository.store.loadPlan())
        private set

    var displayName by mutableStateOf(
        session?.username?.let { repository.store.displayName(it) } ?: "同学"
    )
        private set

    var avatarPath by mutableStateOf(
        session?.username?.let { repository.store.avatarPath(it) }
    )
        private set

    var readKnowledge by mutableStateOf(repository.store.readKnowledge())
        private set

    init {
        repository.apiClient.onUnauthorized = {
            scope.launch {
                if (session != null) {
                    forceLogout("登录状态已失效，请重新登录")
                }
            }
        }
        scope.launch {
            kotlinx.coroutines.delay(1050)
            booting = false
        }
        if (session != null) {
            scope.launch {
                runCatching { repository.papers(force = true) }
                    .onFailure { error ->
                        if (error !is com.example.zhijiexing.data.UnauthorizedException) {
                            // 网络不可用时保留本地会话，进入首页后仍可重试
                        }
                    }
            }
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    suspend fun login(username: String, password: String): Result<UserInfo> = runCatching {
        val (token, user) = repository.authApi.login(username.trim(), password)
        repository.store.saveSession(token, user)
        session = user
        syncProfile()
        stack.clear()
        selectedTab = 0
        bump()
        user
    }

    suspend fun register(username: String, password: String, confirm: String): Result<UserInfo> = runCatching {
        repository.authApi.register(username.trim(), password, confirm)
        val (token, user) = repository.authApi.login(username.trim(), password)
        repository.store.saveSession(token, user)
        session = user
        syncProfile()
        stack.clear()
        selectedTab = 0
        bump()
        user
    }

    fun logout() {
        repository.store.clearSession()
        repository.clearNetworkCaches()
        session = null
        syncProfile()
        stack.clear()
        selectedTab = 0
    }

    private fun syncProfile() {
        val username = session?.username
        displayName = username?.let { repository.store.displayName(it) } ?: "同学"
        avatarPath = username?.let { repository.store.avatarPath(it) }
    }

    fun forceLogout(message: String) {
        logout()
        showMessage(message)
    }

    fun bump() {
        dataVersion++
    }

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun replaceTop(screen: Screen) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack.add(screen)
    }

    fun goTab(index: Int) {
        selectedTab = index
        stack.clear()
    }

    fun applyPlan(newPlan: StudyPlan) {
        plan = newPlan
        repository.store.savePlan(newPlan)
        bump()
    }

    fun togglePlanTask(stageIndex: Int, taskIndex: Int) {
        applyPlan(plan.toggle(stageIndex, taskIndex))
    }

    fun saveProfile(name: String, avatarFilePath: String? = null) {
        val username = session?.username ?: return
        repository.store.saveDisplayName(username, name)
        displayName = name.trim()
        if (avatarFilePath != null) {
            repository.store.saveAvatarPath(username, avatarFilePath)
            avatarPath = avatarFilePath
        }
        bump()
    }

    fun markKnowledgeRead(id: String) {
        if (readKnowledge.contains(id)) return
        repository.store.markKnowledgeRead(id)
        readKnowledge = repository.store.readKnowledge()
    }

    fun resetKnowledgeProgress() {
        readKnowledge = repository.store.readKnowledge()
    }

    fun stats(): StudyStats = repository.studyStats()

    fun onAppResume() {
        bump()
    }
}
