package com.example.zhijiexing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.AppState
import com.example.zhijiexing.data.QuestionOption
import com.example.zhijiexing.data.ReviewDetail
import com.example.zhijiexing.data.ReviewQuestion
import com.example.zhijiexing.data.SavedQuestion
import com.example.zhijiexing.data.UnauthorizedException
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.AppTopBar
import com.example.zhijiexing.ui.components.DecoBackground
import com.example.zhijiexing.ui.components.EmptyState
import com.example.zhijiexing.ui.components.ErrorBox
import com.example.zhijiexing.ui.components.GhostButton
import com.example.zhijiexing.ui.components.LoadingBox
import com.example.zhijiexing.ui.components.PrimaryButton
import com.example.zhijiexing.ui.components.ProgressBar
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.Danger
import com.example.zhijiexing.ui.theme.DangerSoft
import com.example.zhijiexing.ui.theme.GreenDeep
import com.example.zhijiexing.ui.theme.GreenSoft
import com.example.zhijiexing.ui.theme.Line
import com.example.zhijiexing.ui.theme.Primary
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.PrimarySoft
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub

@Composable
fun ReviewListScreen(appState: AppState, attemptId: String) {
    var detail by remember { mutableStateOf<ReviewDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(attemptId) {
        loading = true
        error = null
        runCatching { appState.repository.review(attemptId) }
            .onSuccess { detail = it }
            .onFailure { if (it !is UnauthorizedException) error = it.message ?: "解析加载失败" }
        loading = false
    }

    val questions = detail?.questions.orEmpty()
    val wrongCount = questions.count { !it.isCorrect }
    val bookmarkCount = questions.count { appState.repository.store.isBookmarked(it.questionId) }
    val filtered = remember(questions, filter, refresh) {
        questions.mapIndexed { index, question -> index to question }.filter { (_, question) ->
            when (filter) {
                1 -> !question.isCorrect
                2 -> appState.repository.store.isBookmarked(question.questionId)
                else -> true
            }
        }
    }

    fun toggleBookmark(question: ReviewQuestion) {
        val added = appState.repository.toggleBookmark(question, detail?.paperName ?: "")
        refresh++
        appState.bump()
        appState.showMessage(if (added) "已加入收藏" else "已取消收藏")
    }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = "答题解析",
                subtitle = detail?.paperName ?: "加载中…",
                onBack = { appState.pop() }
            )
            when {
                loading -> LoadingBox()
                error != null || detail == null -> ErrorBox(message = error ?: "解析不存在", onRetry = { appState.pop() })
                else -> {
                    val data = detail!!
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
                    ) {
                        item {
                            ReviewSummary(data, bookmarkCount)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "全部 ${questions.size}",
                                    "答错 $wrongCount",
                                    "收藏 $bookmarkCount"
                                ).forEachIndexed { index, label ->
                                    val selected = filter == index
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (selected) PrimaryDeep else Color.White)
                                            .clickable { filter = index }
                                            .padding(horizontal = 15.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 12.sp,
                                            color = if (selected) Color.White else TextSub,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                        if (filtered.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Outlined.Check,
                                    title = when (filter) {
                                        1 -> "没有答错的题"
                                        2 -> "还没有收藏题目"
                                        else -> "暂无解析"
                                    },
                                    subtitle = when (filter) {
                                        1 -> "这套卷发挥得很稳，继续保持"
                                        2 -> "在解析页点击书签即可收藏"
                                        else -> "完成一套试卷后可以查看解析"
                                    }
                                )
                            }
                        } else {
                            items(filtered.size) { position ->
                                val (index, question) = filtered[position]
                                ReviewRow(
                                    index = index,
                                    question = question,
                                    bookmarked = appState.repository.store.isBookmarked(question.questionId),
                                    onToggleBookmark = { toggleBookmark(question) },
                                    onClick = { appState.push(com.example.zhijiexing.Screen.ReviewDetail(attemptId, index)) }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                        item {
                            Spacer(Modifier.height(10.dp))
                            PrimaryButton(
                                text = "复习错题本",
                                onClick = { appState.push(com.example.zhijiexing.Screen.WrongBook) },
                                dark = true
                            )
                            Spacer(Modifier.height(10.dp))
                            GhostButton(
                                text = "返回成绩页",
                                onClick = { appState.pop() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSummary(data: ReviewDetail, bookmarkCount: Int) {
    val correct = data.questions.count { it.isCorrect }
    AppCard(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${data.score}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (data.passed) GreenDeep else AccentOrange
                    )
                    Text(
                        " / ${data.totalScore}",
                        fontSize = 12.sp,
                        color = TextHint,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (data.passed) "已通过 · 继续保持" else "未通过 · 重点复盘错题",
                    fontSize = 10.sp,
                    color = TextSub
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SummaryPill("答对 $correct", GreenDeep, GreenSoft)
                Spacer(Modifier.width(6.dp))
                SummaryPill("答错 ${data.questions.size - correct}", Danger, DangerSoft)
                Spacer(Modifier.width(6.dp))
                SummaryPill("收藏 $bookmarkCount", PrimaryDeep, PrimarySoft)
            }
        }
        Spacer(Modifier.height(13.dp))
        ProgressBar(
            value = if (data.questions.isEmpty()) 0f else correct.toFloat() / data.questions.size,
            color = GreenDeep,
            height = 7.dp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "错题已自动收入错题本，建议隔天再做一遍",
            fontSize = 10.sp,
            color = TextHint
        )
    }
}

@Composable
private fun SummaryPill(text: String, color: Color, background: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 9.sp, color = color)
    }
}

@Composable
private fun ReviewRow(
    index: Int,
    question: ReviewQuestion,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (question.isCorrect) GreenSoft else DangerSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (question.isCorrect) Icons.Outlined.Check else Icons.Outlined.Close,
                    null,
                    tint = if (question.isCorrect) GreenDeep else Danger,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("第 ${index + 1} 题", fontSize = 11.sp, color = TextSub, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Text(question.typeLabel, fontSize = 9.sp, color = TextHint)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onToggleBookmark() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "收藏",
                    tint = if (bookmarked) Primary else TextHint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = TextHint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text(
            question.content,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = TextMain,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "我的 ${question.userAnswer?.takeIf { it.isNotBlank() } ?: "未答"}",
                fontSize = 10.sp,
                color = if (question.isCorrect) GreenDeep else Danger
            )
            Spacer(Modifier.width(10.dp))
            Text("正确 ${question.correctAnswer}", fontSize = 10.sp, color = GreenDeep)
            Spacer(Modifier.weight(1f))
            Text(
                "${question.earnedScore}/${question.maxScore} 分",
                fontSize = 10.sp,
                color = TextHint
            )
        }
    }
}

@Composable
fun ReviewDetailScreen(appState: AppState, attemptId: String, index: Int) {
    var detail by remember { mutableStateOf<ReviewDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(attemptId) {
        loading = true
        error = null
        runCatching { appState.repository.review(attemptId) }
            .onSuccess { detail = it }
            .onFailure { if (it !is UnauthorizedException) error = it.message ?: "解析加载失败" }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            val data = detail
            val question = data?.questions?.getOrNull(index)
            AppTopBar(
                title = "第 ${index + 1} 题",
                subtitle = data?.paperName ?: "",
                onBack = { appState.pop() }
            )
            when {
                loading -> LoadingBox()
                error != null || question == null || data == null -> ErrorBox(
                    message = error ?: "题目不存在",
                    onRetry = { appState.pop() }
                )
                else -> {
                    val bookmarked = remember(refresh) { appState.repository.store.isBookmarked(question.questionId) }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                    ) {
                        Spacer(Modifier.height(6.dp))
                        SolutionBody(
                            typeLabel = question.typeLabel,
                            content = question.content,
                            options = question.options,
                            correctAnswer = question.correctAnswer,
                            userAnswer = question.userAnswer,
                            analysis = question.analysis,
                            isCorrect = question.isCorrect,
                            earnedScore = question.earnedScore,
                            maxScore = question.maxScore,
                            bookmarked = bookmarked,
                            onToggleBookmark = {
                                val added = appState.repository.toggleBookmark(question, data.paperName)
                                refresh++
                                appState.bump()
                                appState.showMessage(if (added) "已加入收藏" else "已取消收藏")
                            }
                        )
                        Spacer(Modifier.height(18.dp))
                        Row {
                            Box(Modifier.weight(1f)) {
                                GhostButton(
                                    text = "上一题",
                                    onClick = {
                                        if (index > 0) appState.replaceTop(com.example.zhijiexing.Screen.ReviewDetail(attemptId, index - 1))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    tint = if (index > 0) TextSub else TextHint.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) {
                                PrimaryButton(
                                    text = if (index < data.questions.size - 1) "下一题" else "看完了",
                                    onClick = {
                                        if (index < data.questions.size - 1) {
                                            appState.replaceTop(com.example.zhijiexing.Screen.ReviewDetail(attemptId, index + 1))
                                        } else {
                                            appState.pop()
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SavedQuestionScreen(appState: AppState, questionId: String, bookmark: Boolean) {
    var refresh by remember { mutableIntStateOf(0) }
    val question = remember(questionId, refresh, bookmark) {
        val list = if (bookmark) appState.repository.store.bookmarks() else appState.repository.store.wrongQuestions()
        list.firstOrNull { it.questionId == questionId }
    }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = if (bookmark) "收藏题解析" else "错题解析",
                subtitle = question?.paperName ?: "",
                onBack = { appState.pop() }
            )
            if (question == null) {
                EmptyState(
                    icon = Icons.Outlined.EditNote,
                    title = "题目已移除",
                    subtitle = "这道题不在当前列表中了",
                    action = "返回",
                    onAction = { appState.pop() }
                )
            } else {
                val bookmarked = remember(refresh) { appState.repository.store.isBookmarked(question.questionId) }
                val isCorrect = question.userAnswer?.takeIf { it.isNotBlank() } == question.correctAnswer
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(6.dp))
                    SolutionBody(
                        typeLabel = when (question.type) {
                            "JUDGE" -> "判断题"
                            "MULTI" -> "多选题"
                            else -> "单选题"
                        },
                        content = question.content,
                        options = question.options,
                        correctAnswer = question.correctAnswer,
                        userAnswer = question.userAnswer,
                        analysis = question.analysis,
                        isCorrect = isCorrect,
                        earnedScore = null,
                        maxScore = null,
                        bookmarked = bookmarked,
                        onToggleBookmark = {
                            val added = appState.repository.toggleBookmark(question)
                            refresh++
                            appState.bump()
                            appState.showMessage(if (added) "已加入收藏" else "已取消收藏")
                        }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SolutionBody(
    typeLabel: String,
    content: String,
    options: List<QuestionOption>,
    correctAnswer: String,
    userAnswer: String?,
    analysis: String,
    isCorrect: Boolean,
    earnedScore: Int?,
    maxScore: Int?,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isCorrect) GreenSoft else DangerSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (isCorrect) "回答正确" else "回答错误",
                    fontSize = 10.sp,
                    color = if (isCorrect) GreenDeep else Danger,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(PrimarySoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(typeLabel, fontSize = 10.sp, color = PrimaryDeep)
            }
            if (earnedScore != null && maxScore != null) {
                Spacer(Modifier.width(7.dp))
                Text("$earnedScore/$maxScore 分", fontSize = 10.sp, color = TextHint)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (bookmarked) PrimarySoft else Color(0xFFF3F6FC))
                    .clickable { onToggleBookmark() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "收藏",
                    tint = if (bookmarked) PrimaryDeep else TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(content, fontSize = 16.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium, color = TextMain)
        Spacer(Modifier.height(16.dp))
        options.forEach { option ->
            SolutionOption(
                label = option.label,
                content = option.content,
                isCorrectOption = correctAnswer.contains(option.label),
                isUserOption = userAnswer?.contains(option.label) == true
            )
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("我的答案", fontSize = 9.sp, color = TextHint)
                Spacer(Modifier.height(3.dp))
                Text(
                    userAnswer?.takeIf { it.isNotBlank() } ?: "未作答",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) GreenDeep else Danger
                )
            }
            Box(Modifier.width(1.dp).height(28.dp).background(Line))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("正确答案", fontSize = 9.sp, color = TextHint)
                Spacer(Modifier.height(3.dp))
                Text(correctAnswer, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenDeep)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFFBF2))
                .border(1.dp, Color(0xFFFFEDCF), RoundedCornerShape(16.dp))
                .padding(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lightbulb, null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("解析", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A5A12))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                analysis.takeIf { it.isNotBlank() } ?: "暂无解析，先记住正确答案与对应考点，再回到题库练同类型题目。",
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = Color(0xFF7A5A2A)
            )
        }
    }
}

@Composable
private fun SolutionOption(
    label: String,
    content: String,
    isCorrectOption: Boolean,
    isUserOption: Boolean
) {
    val isWrongPick = isUserOption && !isCorrectOption
    val background = when {
        isCorrectOption -> GreenSoft
        isWrongPick -> DangerSoft
        else -> Color.White
    }
    val borderColor = when {
        isCorrectOption -> GreenDeep.copy(alpha = 0.5f)
        isWrongPick -> Danger.copy(alpha = 0.55f)
        else -> Line
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(background)
            .border(1.2.dp, borderColor, RoundedCornerShape(15.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(25.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCorrectOption -> GreenDeep
                        isWrongPick -> Danger
                        else -> Color(0xFFF3F6FC)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCorrectOption || isWrongPick) Color.White else TextSub
            )
        }
        Spacer(Modifier.width(11.dp))
        Text(
            content,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = when {
                isCorrectOption -> Color(0xFF1F6E45)
                isWrongPick -> Color(0xFF9A3B3B)
                else -> TextMain
            },
            modifier = Modifier.weight(1f)
        )
        if (isCorrectOption || isWrongPick) {
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    isCorrectOption && isUserOption -> "我的选择 · 正确"
                    isCorrectOption -> "正确答案"
                    else -> "我的选择"
                },
                fontSize = 9.sp,
                color = if (isCorrectOption) GreenDeep else Danger
            )
        }
    }
}
