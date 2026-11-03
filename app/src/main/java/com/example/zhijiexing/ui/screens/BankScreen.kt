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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.PaperInfo
import com.example.zhijiexing.data.UnauthorizedException
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.EmptyState
import com.example.zhijiexing.ui.components.ErrorBox
import com.example.zhijiexing.ui.components.LoadingBox
import com.example.zhijiexing.ui.components.PageTitle
import com.example.zhijiexing.ui.components.PrimaryButton
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.AccentPurple
import com.example.zhijiexing.ui.theme.Danger
import com.example.zhijiexing.ui.theme.GreenDeep
import com.example.zhijiexing.ui.theme.GreenSoft
import com.example.zhijiexing.ui.theme.Line
import com.example.zhijiexing.ui.theme.Navy
import com.example.zhijiexing.ui.theme.OrangeSoft
import com.example.zhijiexing.ui.theme.Primary
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.PrimarySoft
import com.example.zhijiexing.ui.theme.PurpleSoft
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub

@Composable
fun BankScreen(appState: AppState) {
    val version = appState.dataVersion
    var papers by remember { mutableStateOf<List<PaperInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var keyword by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("全部") }
    var pendingPaper by remember { mutableStateOf<PaperInfo?>(null) }

    LaunchedEffect(version) {
        loading = papers.isEmpty()
        error = null
        runCatching { appState.repository.papers() }
            .onSuccess { papers = it }
            .onFailure { if (it !is UnauthorizedException) error = it.message ?: "网络连接失败" }
        loading = false
    }

    val levels = remember(papers) {
        listOf("全部") + papers.map { it.levelName }.filter { it.isNotBlank() }.distinct()
    }
    val visible = remember(papers, keyword, level) {
        papers.filter { paper ->
            (level == "全部" || paper.levelName == level) &&
                (keyword.isBlank() || paper.name.contains(keyword.trim(), ignoreCase = true) ||
                    paper.description.contains(keyword.trim(), ignoreCase = true))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            PageTitle(
                title = "题库",
                subtitle = "${papers.size} 套试卷 · 联网同步考试记录"
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                item {
                    TextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索试卷名称", fontSize = 13.sp, color = TextHint) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, null, tint = TextHint, modifier = Modifier.size(19.dp))
                        },
                        trailingIcon = {
                            if (keyword.isNotEmpty()) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "清空",
                                    tint = TextHint,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { keyword = "" }
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Primary
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                }
                if (levels.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(levels) { item ->
                                val selected = level == item
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) PrimaryDeep else Color.White)
                                        .clickable { level = item }
                                        .padding(horizontal = 15.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        item,
                                        fontSize = 12.sp,
                                        color = if (selected) Color.White else TextSub,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
                when {
                    loading -> item { LoadingBox() }
                    error != null -> item { ErrorBox(message = error!!, onRetry = { appState.bump() }) }
                    visible.isEmpty() -> item {
                        EmptyState(
                            icon = Icons.Outlined.MenuBook,
                            title = if (keyword.isBlank()) "暂无试卷" else "没有找到相关试卷",
                            subtitle = if (keyword.isBlank()) "等待服务端发布新的练习试卷" else "换个关键词或切换难度再试试"
                        )
                    }
                    else -> items(visible, key = { it.id }) { paper ->
                        PaperBankCard(paper) { pendingPaper = paper }
                        Spacer(Modifier.height(12.dp))
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    pendingPaper?.let { paper ->
        StartExamDialog(
            paper = paper,
            onDismiss = { pendingPaper = null },
            onStart = {
                pendingPaper = null
                appState.push(Screen.Quiz(paper.id, paper.name))
            }
        )
    }
}

@Composable
private fun PaperBankCard(paper: PaperInfo, onClick: () -> Unit) {
    AppCard(onClick = onClick, padding = 17.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7FA6FF), PrimaryDeep))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MenuBook, null, tint = Color.White, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        paper.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (paper.levelName.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimarySoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(paper.levelName, fontSize = 9.sp, color = PrimaryDeep)
                        }
                    }
                }
                if (paper.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        paper.description,
                        fontSize = 11.sp,
                        color = TextHint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetaChip("${paper.questionCount} 题")
                    Spacer(Modifier.width(6.dp))
                    MetaChip("${paper.duration} 分钟")
                    Spacer(Modifier.width(6.dp))
                    MetaChip("${paper.passScore} 分及格")
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.8f)))
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (paper.bestScore > 0) {
                Icon(Icons.Outlined.EmojiEvents, null, tint = AccentOrange, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("最高 ${paper.bestScore} 分", fontSize = 11.sp, color = TextSub)
            } else {
                Text("还没有练习记录", fontSize = 11.sp, color = TextHint)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary)
                    .padding(horizontal = 15.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (paper.attemptCount > 0) "再练一次" else "开始练习",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF4F7FC))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 10.sp, color = TextSub)
    }
}

@Composable
fun StartExamDialog(paper: PaperInfo, onDismiss: () -> Unit, onStart: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(Modifier.padding(22.dp)) {
                Text("开始练习", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(paper.name, fontSize = 13.sp, color = TextSub, lineHeight = 19.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7FAFF))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DialogStat("题目", "${paper.questionCount} 题")
                    DialogStat("时长", "${paper.duration} 分钟")
                    DialogStat("总分", "${paper.totalScore} 分")
                    DialogStat("及格", "${paper.passScore} 分")
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Security, null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "考试期间把应用切到后台会被记录并上报，累计 3 次自动交卷，请一次完成。",
                        fontSize = 11.sp,
                        color = TextHint,
                        lineHeight = 17.sp
                    )
                }
                Spacer(Modifier.height(18.dp))
                PrimaryButton(text = "开始答题", onClick = onStart)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("再想想", fontSize = 13.sp, color = TextHint)
                }
            }
        }
    }
}

@Composable
private fun DialogStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, color = TextHint)
    }
}
