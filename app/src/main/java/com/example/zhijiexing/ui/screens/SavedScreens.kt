package com.example.zhijiexing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.SavedQuestion
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.AppTopBar
import com.example.zhijiexing.ui.components.DecoBackground
import com.example.zhijiexing.ui.components.EmptyState
import com.example.zhijiexing.ui.components.GhostButton
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
fun WrongBookScreen(appState: AppState) {
    val version = appState.dataVersion
    var refresh by remember { mutableIntStateOf(0) }
    val questions = remember(version, refresh) { appState.repository.store.wrongQuestions() }
    var showClearConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = "错题本",
                subtitle = "共 ${questions.size} 道 · 自动收集",
                onBack = { appState.pop() },
                actions = {
                    if (questions.isNotEmpty()) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { showClearConfirm = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, "清空错题", tint = TextHint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            )
            if (questions.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Assignment,
                    title = "错题本是空的",
                    subtitle = "完成试卷后，答错的题目会自动收进这里，方便隔天重做。",
                    action = "去题库练习",
                    onAction = {
                        appState.goTab(1)
                    }
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    item {
                        ReviewTipCard {
                            appState.goTab(1)
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    items(questions.size) { index ->
                        SavedQuestionRow(
                            index = index,
                            question = questions[index],
                            bookmarked = appState.repository.store.isBookmarked(questions[index].questionId),
                            badge = "错题",
                            onClick = {
                                appState.push(Screen.SavedQuestionDetail(questions[index].questionId, bookmark = false))
                            },
                            onToggleBookmark = {
                                val added = appState.repository.toggleBookmark(questions[index])
                                refresh++
                                appState.bump()
                                appState.showMessage(if (added) "已加入收藏" else "已取消收藏")
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        GhostButton(
                            text = "清空错题本",
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            tint = Danger
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        ClearConfirmDialog(
            title = "清空错题本？",
            message = "清空后无法恢复，建议先把高频错题收藏起来。",
            confirmText = "清空",
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                showClearConfirm = false
                appState.repository.store.clearWrongQuestions()
                refresh++
                appState.bump()
                appState.showMessage("错题本已清空")
            }
        )
    }
}

@Composable
fun BookmarksScreen(appState: AppState) {
    val version = appState.dataVersion
    var refresh by remember { mutableIntStateOf(0) }
    val questions = remember(version, refresh) { appState.repository.store.bookmarks() }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = "我的收藏",
                subtitle = "共 ${questions.size} 道 · 随时回看",
                onBack = { appState.pop() }
            )
            if (questions.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = "还没有收藏题目",
                    subtitle = "在解析页点击书签，就能把重点题目收在这里反复看。",
                    action = "去看看题库",
                    onAction = { appState.goTab(1) }
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    items(questions.size) { index ->
                        SavedQuestionRow(
                            index = index,
                            question = questions[index],
                            bookmarked = true,
                            badge = "收藏",
                            onClick = {
                                appState.push(Screen.SavedQuestionDetail(questions[index].questionId, bookmark = true))
                            },
                            onToggleBookmark = {
                                appState.repository.toggleBookmark(questions[index])
                                refresh++
                                appState.bump()
                                appState.showMessage("已取消收藏")
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewTipCard(onPractice: () -> Unit) {
    AppCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(GreenSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Lightbulb, null, tint = GreenDeep, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("复盘比刷题更重要", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Spacer(Modifier.height(3.dp))
                Text(
                    "隔 1 天、3 天、7 天各重做一次，连续答对两次再移出重点。",
                    fontSize = 10.sp,
                    color = TextHint,
                    lineHeight = 15.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimarySoft)
                    .clickable { onPractice() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.MenuBook, null, tint = PrimaryDeep, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("去题库练新题", fontSize = 11.sp, color = PrimaryDeep, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SavedQuestionRow(
    index: Int,
    question: SavedQuestion,
    bookmarked: Boolean,
    badge: String,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
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
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (badge == "错题") DangerSoft else PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badge == "错题") Danger else PrimaryDeep
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                badge,
                fontSize = 9.sp,
                color = if (badge == "错题") Danger else PrimaryDeep
            )
            Spacer(Modifier.width(6.dp))
            Text(
                question.paperName,
                fontSize = 9.sp,
                color = TextHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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
                color = Danger
            )
            Spacer(Modifier.width(10.dp))
            Text("正确 ${question.correctAnswer}", fontSize = 10.sp, color = GreenDeep)
            Spacer(Modifier.weight(1f))
            Text(
                when (question.type) {
                    "JUDGE" -> "判断题"
                    "MULTI" -> "多选题"
                    else -> "单选题"
                },
                fontSize = 9.sp,
                color = TextHint
            )
        }
    }
}

@Composable
private fun ClearConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
            Column(Modifier.padding(22.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(message, fontSize = 12.sp, color = TextSub, lineHeight = 19.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF0F3F9))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("取消", fontSize = 13.sp, color = TextSub)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Danger)
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(confirmText, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
