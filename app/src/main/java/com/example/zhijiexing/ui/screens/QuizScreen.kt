package com.example.zhijiexing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.AttemptInfo
import com.example.zhijiexing.data.DateUtils
import com.example.zhijiexing.data.QuestionItem
import com.example.zhijiexing.data.SubmitResult
import com.example.zhijiexing.data.UnauthorizedException
import com.example.zhijiexing.ui.components.AppTopBar
import com.example.zhijiexing.ui.components.DecoBackground
import com.example.zhijiexing.ui.components.ErrorBox
import com.example.zhijiexing.ui.components.InfoPill
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.Danger
import com.example.zhijiexing.ui.theme.DangerSoft
import com.example.zhijiexing.ui.theme.GreenDeep
import com.example.zhijiexing.ui.theme.GreenSoft
import com.example.zhijiexing.ui.theme.Line
import com.example.zhijiexing.ui.theme.OrangeSoft
import com.example.zhijiexing.ui.theme.Primary
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.PrimarySoft
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 答题卡配色：已答加深一档蓝色，存疑用黄色系区分 */
private val AnsweredBlue = Color(0xFFD4E3FF)
private val FlagYellowSoft = Color(0xFFFFF1B8)
private val FlagDeep = Color(0xFF8C5E00)
private val FlagAccent = Color(0xFFE8B33A)

/**
 * 答题页：服务端下发起跳时间；切后台由系统生命周期计数并上报，服务端裁决是否强制交卷。
 * 作答与存疑标记实时保存到服务端，断网时保留本地选择并提示。
 */
@Composable
fun QuizScreen(appState: AppState, paperId: String, paperName: String) {
    var attempt by remember { mutableStateOf<AttemptInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var startKey by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }
    val flagged = remember { mutableStateMapOf<String, Boolean>() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var tabCount by remember { mutableIntStateOf(0) }
    var tabLimit by remember { mutableIntStateOf(3) }
    var showSubmitConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 后台切出计数与自动交卷结果先放在普通容器里，回到前台再刷新界面，避免在退后台时重组答题页。
    val switchCount = remember { intArrayOf(0) }
    val reportedLimit = remember { intArrayOf(3) }
    val pendingNotice = remember { booleanArrayOf(false) }
    val autoSubmitQueued = remember { booleanArrayOf(false) }
    val autoSubmitRunning = remember { booleanArrayOf(false) }
    val pendingAutoResult = remember { arrayOfNulls<SubmitResult>(1) }
    val pendingAutoError = remember { arrayOfNulls<String>(1) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    fun persist(questionId: String) {
        val info = attempt ?: return
        val answer = answers[questionId] ?: ""
        val flag = flagged[questionId] == true
        scope.launch {
            runCatching { appState.repository.examApi.saveAnswer(info.attemptId, questionId, answer, flag) }
                .onFailure {
                    if (it !is UnauthorizedException) {
                        appState.showMessage("答案保存失败，请检查网络后重新选择")
                    }
                }
        }
    }

    fun selectOption(question: QuestionItem, label: String) {
        if (submitted) return
        answers[question.id] = if (question.type == "MULTI") {
            val labels = answers[question.id].orEmpty()
                .map { it.toString() }
                .filter { it.isNotBlank() }
                .toMutableList()
            if (labels.contains(label)) labels.remove(label) else labels.add(label)
            labels.sort()
            labels.joinToString("")
        } else {
            label
        }
        persist(question.id)
    }

    fun toggleFlag(question: QuestionItem) {
        if (submitted) return
        flagged[question.id] = !(flagged[question.id] ?: false)
        persist(question.id)
    }

    suspend fun submitExam(type: String) {
        val info = attempt ?: return
        if (submitting || submitted || autoSubmitRunning[0] || autoSubmitQueued[0]) return
        submitting = true
        try {
            val result = appState.repository.examApi.submit(info.attemptId, type, switchCount[0])
            submitted = true
            appState.repository.cacheResult(result)
            appState.bump()
            if (type == "TIMEOUT") appState.showMessage("考试时间到，已自动交卷")
            appState.replaceTop(Screen.Result(result.attemptId))
        } catch (throwable: Exception) {
            submitting = false
            if (throwable !is UnauthorizedException) {
                appState.showMessage(throwable.message ?: "交卷失败，请重试")
            }
        }
    }

    // 自动交卷的网络请求可能在后台完成；结果先暂存，回到前台再切换页面和提示。
    fun finishAutoSubmit() {
        val result = pendingAutoResult[0]
        val error = pendingAutoError[0]
        pendingAutoResult[0] = null
        pendingAutoError[0] = null
        autoSubmitQueued[0] = false
        submitting = false
        if (submitted) return
        if (result != null) {
            submitted = true
            appState.repository.cacheResult(result)
            appState.bump()
            appState.showMessage("切后台次数已达上限，系统已自动交卷")
            appState.replaceTop(Screen.Result(result.attemptId))
        } else if (!error.isNullOrBlank()) {
            appState.showMessage(error)
        }
    }

    fun requestAutoSubmit() {
        val info = attempt ?: return
        if (submitted || submitting || autoSubmitRunning[0]) return
        autoSubmitRunning[0] = true
        val foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (foreground) submitting = true
        scope.launch {
            runCatching { appState.repository.examApi.submit(info.attemptId, "TAB_SWITCH", switchCount[0]) }
                .onSuccess { pendingAutoResult[0] = it }
                .onFailure { throwable ->
                    if (throwable !is UnauthorizedException) {
                        pendingAutoError[0] = throwable.message ?: "交卷失败，请重试"
                    }
                }
            autoSubmitRunning[0] = false
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                finishAutoSubmit()
            }
        }
    }

    LaunchedEffect(paperId, startKey) {
        loading = true
        error = null
        runCatching { appState.repository.examApi.startExam(paperId) }
            .onSuccess { info ->
                attempt = info
                remaining = info.remainingSeconds
                currentIndex = 0
                answers.clear()
                flagged.clear()
                tabCount = 0
                tabLimit = 3
                switchCount[0] = 0
                reportedLimit[0] = 3
                pendingNotice[0] = false
                autoSubmitQueued[0] = false
                autoSubmitRunning[0] = false
                pendingAutoResult[0] = null
                pendingAutoError[0] = null
                info.questions.forEach { question ->
                    question.myAnswer?.takeIf { it.isNotBlank() }?.let { answers[question.id] = it }
                    if (question.isFlagged) flagged[question.id] = true
                }
            }
            .onFailure {
                if (it !is UnauthorizedException) {
                    error = it.message ?: "试卷加载失败，请检查网络"
                }
            }
        loading = false
    }

    // 服务端时间权威：本地只做每秒递减，归零自动交卷
    LaunchedEffect(attempt?.attemptId) {
        val info = attempt ?: return@LaunchedEffect
        remaining = info.remainingSeconds
        while (!submitted) {
            if (remaining <= 0) {
                submitExam("TIMEOUT")
                if (!submitted) delay(3000)
                continue
            }
            delay(1000)
            if (remaining > 0) remaining -= 1
        }
    }

    // 防作弊（对齐参考工程 BackgroundMonitor）：只有整个应用真正退到后台（Home/最近任务）才计数。
    // 应用内返回、切题、弹窗都不会走这里；每次计数如实上报，由服务端裁决是否强制交卷。
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (!submitted && !loading) {
                        val info = attempt
                        if (info != null) {
                            val count = switchCount[0] + 1
                            switchCount[0] = count
                            pendingNotice[0] = true
                            scope.launch {
                                runCatching { appState.repository.examApi.reportTabSwitch(info.attemptId, count) }
                                    .onSuccess { result ->
                                        reportedLimit[0] = result.limit
                                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                            tabLimit = result.limit
                                            if (tabCount < count) tabCount = count
                                        }
                                        if (result.autoSubmit) {
                                            autoSubmitQueued[0] = true
                                            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                                requestAutoSubmit()
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
                // 回到前台再统一刷新计数、提示或自动交卷，避免在后台重组整个答题页。
                Lifecycle.Event.ON_START -> {
                    if (tabCount != switchCount[0]) tabCount = switchCount[0]
                    if (tabLimit != reportedLimit[0]) tabLimit = reportedLimit[0]
                    if (pendingAutoResult[0] != null || pendingAutoError[0] != null) {
                        finishAutoSubmit()
                    } else if (autoSubmitQueued[0] && !submitted) {
                        if (autoSubmitRunning[0]) submitting = true else requestAutoSubmit()
                    }
                    if (pendingNotice[0]) {
                        pendingNotice[0] = false
                        if (!submitted && !autoSubmitQueued[0] && switchCount[0] in 1 until tabLimit) {
                            appState.showMessage("检测到切出应用，已记录 ${switchCount[0]} 次，请专心作答")
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentIndex) {
        scrollState.scrollTo(0)
    }

    BackHandler(enabled = !submitting) {
        if (submitted) {
            appState.pop()
        } else {
            showExitConfirm = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        val info = attempt
        when {
            loading -> {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    AppTopBar(title = paperName, subtitle = "正在准备试卷", onBack = { appState.pop() })
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(14.dp))
                            Text("正在从服务器载入题目…", fontSize = 12.sp, color = TextHint)
                        }
                    }
                }
            }
            error != null || info == null -> {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    AppTopBar(title = paperName, onBack = { appState.pop() })
                    ErrorBox(
                        message = error ?: "试卷不存在或已结束",
                        onRetry = { startKey++ }
                    )
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "返回题库",
                            fontSize = 12.sp,
                            color = TextHint,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { appState.pop() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            else -> {
                val questions = info.questions
                val safeIndex = currentIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
                val question = questions.getOrNull(safeIndex)
                val answeredCount = questions.count { !answers[it.id].isNullOrBlank() }
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    QuizHeader(
                        paperName = info.paperName.ifBlank { paperName },
                        questionCount = questions.size,
                        totalScore = info.totalScore,
                        remaining = remaining,
                        answeredCount = answeredCount,
                        tabCount = tabCount,
                        tabLimit = tabLimit,
                        onBack = { showExitConfirm = true },
                        onCard = { showCard = true }
                    )
                    if (questions.isNotEmpty()) {
                        QuestionStrip(
                            questions = questions,
                            currentIndex = safeIndex,
                            answers = answers,
                            flagged = flagged,
                            onSelect = { currentIndex = it }
                        )
                    }
                    if (question == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("本试卷暂无题目", fontSize = 13.sp, color = TextHint)
                        }
                    } else {
                        Column(
                            Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 20.dp)
                        ) {
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "第 ${safeIndex + 1} 题",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMain
                                )
                                Spacer(Modifier.width(8.dp))
                                InfoPill(question.typeLabel)
                                if (question.type == "MULTI") {
                                    Spacer(Modifier.width(6.dp))
                                    Text("多选", fontSize = 10.sp, color = TextHint)
                                }
                                Spacer(Modifier.weight(1f))
                                Text("${question.score} 分", fontSize = 11.sp, color = TextHint)
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (flagged[question.id] == true) FlagYellowSoft else Color(0xFFF3F6FC)
                                        )
                                        .clickable { toggleFlag(question) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (flagged[question.id] == true) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "标记存疑",
                                        tint = if (flagged[question.id] == true) FlagDeep else TextHint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                question.content,
                                fontSize = 16.sp,
                                lineHeight = 27.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMain
                            )
                            Spacer(Modifier.height(18.dp))
                            question.options.forEach { option ->
                                OptionRow(
                                    label = option.label,
                                    content = option.content,
                                    selected = answers[question.id]?.contains(option.label) == true,
                                    multi = question.type == "MULTI"
                                ) { selectOption(question, option.label) }
                                Spacer(Modifier.height(10.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF7FAFF))
                                    .padding(horizontal = 13.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Security, null, tint = Primary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    "答案实时保存 · 应用切到后台会被记录，累计 $tabLimit 次自动交卷",
                                    fontSize = 10.sp,
                                    color = TextHint,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                        QuizBottomBar(
                            currentIndex = safeIndex,
                            total = questions.size,
                            answered = answeredCount,
                            flagged = flagged[question.id] == true,
                            onPrev = { if (safeIndex > 0) currentIndex = safeIndex - 1 },
                            onNext = { if (safeIndex < questions.size - 1) currentIndex = safeIndex + 1 },
                            onToggleFlag = { toggleFlag(question) },
                            onSubmit = { showSubmitConfirm = true }
                        )
                    }
                }
            }
        }

        if (submitting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x660F1B2D)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(horizontal = 32.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.5.dp, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("正在交卷…", fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("服务端正在阅卷，请稍候", fontSize = 10.sp, color = TextHint)
                }
            }
        }
    }

    if (showSubmitConfirm) {
        val questions = attempt?.questions.orEmpty()
        val unanswered = questions.count { answers[it.id].isNullOrBlank() }
        Dialog(onDismissRequest = { showSubmitConfirm = false }) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
                Column(Modifier.padding(22.dp)) {
                    Text("确认交卷", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (unanswered > 0) {
                            "还有 $unanswered 道题未作答，交卷后不可修改，确定现在交卷吗？"
                        } else {
                            "全部 ${questions.size} 道题已作答，交卷后不可修改，确定提交吗？"
                        },
                        fontSize = 12.sp,
                        color = TextSub,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF0F3F9))
                                .clickable { showSubmitConfirm = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("继续检查", fontSize = 13.sp, color = TextSub)
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(Primary, PrimaryDeep)))
                                .clickable {
                                    showSubmitConfirm = false
                                    scope.launch { submitExam("MANUAL") }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("确认交卷", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirm) {
        Dialog(onDismissRequest = { showExitConfirm = false }) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
                Column(Modifier.padding(22.dp)) {
                    Text("结束本次练习？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "返回会立即交卷并结算成绩，未作答的题目不得分，交卷后不可修改。",
                        fontSize = 12.sp,
                        color = TextSub,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF0F3F9))
                                .clickable { showExitConfirm = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("继续答题", fontSize = 13.sp, color = TextSub)
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(OrangeSoft)
                                .clickable {
                                    showExitConfirm = false
                                    scope.launch { submitExam("MANUAL") }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("交卷并返回", fontSize = 13.sp, color = AccentOrange, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showCard) {
        AnswerCardDialog(
            questions = attempt?.questions.orEmpty(),
            currentIndex = currentIndex,
            answers = answers,
            flagged = flagged,
            onSelect = {
                currentIndex = it
                showCard = false
            },
            onDismiss = { showCard = false }
        )
    }
}

@Composable
private fun QuizHeader(
    paperName: String,
    questionCount: Int,
    totalScore: Int,
    remaining: Int,
    answeredCount: Int,
    tabCount: Int,
    tabLimit: Int,
    onBack: () -> Unit,
    onCard: () -> Unit
) {
    val urgent = remaining <= 300
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = TextMain, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                paperName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "共 $questionCount 题 · 满分 $totalScore 分",
                fontSize = 10.sp,
                color = TextHint
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (urgent) DangerSoft else PrimarySoft)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Timer,
                null,
                tint = if (urgent) Danger else PrimaryDeep,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                DateUtils.formatDuration(remaining),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (urgent) Danger else PrimaryDeep
            )
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("已答 $answeredCount/$questionCount", fontSize = 10.sp, color = TextHint)
        Spacer(Modifier.width(10.dp))
        if (tabCount > 0) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrangeSoft)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("后台 $tabCount/$tabLimit", fontSize = 9.sp, color = AccentOrange)
            }
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCard() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.GridView, null, tint = TextSub, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("答题卡", fontSize = 10.sp, color = TextSub)
        }
    }
}

@Composable
private fun QuestionStrip(
    questions: List<QuestionItem>,
    currentIndex: Int,
    answers: Map<String, String>,
    flagged: Map<String, Boolean>,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(questions.size) { index ->
            val question = questions[index]
            val answered = !answers[question.id].isNullOrBlank()
            val isCurrent = index == currentIndex
            val isFlagged = flagged[question.id] == true
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> Primary
                            answered -> AnsweredBlue
                            else -> Color.White
                        }
                    )
                    .border(
                        width = if (isFlagged && !isCurrent) 1.5.dp else 0.dp,
                        color = if (isFlagged) FlagAccent else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isCurrent -> Color.White
                        answered -> PrimaryDeep
                        else -> TextHint
                    }
                )
                if (isFlagged) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 3.dp, end = 3.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(FlagAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    content: String,
    selected: Boolean,
    multi: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) PrimarySoft else Color.White)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Primary else Line,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(if (multi) RoundedCornerShape(8.dp) else CircleShape)
                .background(if (selected) Primary else Color(0xFFF3F6FC)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else TextSub
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            content,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = if (selected) PrimaryDeep else TextMain,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Outlined.Check, null, tint = Primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuizBottomBar(
    currentIndex: Int,
    total: Int,
    answered: Int,
    flagged: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleFlag: () -> Unit,
    onSubmit: () -> Unit
) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.8f)))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTextButton("上一题", enabled = currentIndex > 0, onClick = onPrev)
            NavTextButton("下一题", enabled = currentIndex < total - 1, onClick = onNext)
            FlagTextButton(flagged = flagged, onClick = onToggleFlag)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF6B97FF), Primary, PrimaryDeep)))
                    .clickable { onSubmit() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("交卷", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Text("已答 $answered/$total", color = Color(0xFFE4ECFF), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun NavTextButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 7.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) PrimaryDeep else TextHint.copy(alpha = 0.4f),
            maxLines = 1
        )
    }
}

@Composable
private fun FlagTextButton(flagged: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (flagged) FlagYellowSoft else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (flagged) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            tint = if (flagged) FlagDeep else TextSub,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "标记存疑",
            fontSize = 12.sp,
            fontWeight = if (flagged) FontWeight.SemiBold else FontWeight.Normal,
            color = if (flagged) FlagDeep else TextSub,
            maxLines = 1
        )
    }
}

@Composable
private fun AnswerCardDialog(
    questions: List<QuestionItem>,
    currentIndex: Int,
    answers: Map<String, String>,
    flagged: Map<String, Boolean>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("答题卡", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${questions.size} 题", fontSize = 11.sp, color = TextHint)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(AnsweredBlue, "已答")
                    Spacer(Modifier.width(12.dp))
                    LegendDot(Color(0xFFF3F6FC), "未答")
                    Spacer(Modifier.width(12.dp))
                    LegendDot(FlagAccent, "存疑")
                }
                Spacer(Modifier.height(16.dp))
                val rows = questions.indices.toList().chunked(5)
                rows.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        row.forEach { index ->
                            val question = questions[index]
                            val answered = !answers[question.id].isNullOrBlank()
                            val isCurrent = index == currentIndex
                            val isFlagged = flagged[question.id] == true
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isFlagged -> FlagYellowSoft
                                            isCurrent -> Primary
                                            answered -> AnsweredBlue
                                            else -> Color(0xFFF5F8FC)
                                        }
                                    )
                                    .border(
                                        width = when {
                                            isCurrent && isFlagged -> 2.dp
                                            isFlagged -> 1.5.dp
                                            else -> 0.dp
                                        },
                                        color = when {
                                            isCurrent && isFlagged -> PrimaryDeep
                                            isFlagged -> FlagAccent
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelect(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isFlagged -> FlagDeep
                                        isCurrent -> Color.White
                                        answered -> PrimaryDeep
                                        else -> TextHint
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF0F3F9))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("关闭", fontSize = 13.sp, color = TextSub)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, color = TextHint)
    }
}
