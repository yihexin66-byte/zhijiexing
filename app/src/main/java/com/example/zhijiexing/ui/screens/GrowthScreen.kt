package com.example.zhijiexing.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.AttemptRecord
import com.example.zhijiexing.data.DateUtils
import com.example.zhijiexing.data.UnauthorizedException
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.PageTitle
import com.example.zhijiexing.ui.components.SectionTitle
import com.example.zhijiexing.ui.components.SoftIconBox
import com.example.zhijiexing.ui.components.StatRing
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.AccentPurple
import com.example.zhijiexing.ui.theme.Danger
import com.example.zhijiexing.ui.theme.GreenDeep
import com.example.zhijiexing.ui.theme.GreenSoft
import com.example.zhijiexing.ui.theme.Line
import com.example.zhijiexing.ui.theme.OrangeSoft
import com.example.zhijiexing.ui.theme.Primary
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.PrimarySoft
import com.example.zhijiexing.ui.theme.PurpleSoft
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub

@Composable
fun GrowthScreen(appState: AppState) {
    val version = appState.dataVersion
    val stats = remember(version) { appState.stats() }
    var records by remember { mutableStateOf<List<AttemptRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(version) {
        loading = records.isEmpty()
        error = null
        runCatching { appState.repository.records() }
            .onSuccess { records = it }
            .onFailure { if (it !is UnauthorizedException) error = it.message ?: "记录同步失败" }
        loading = false
    }

    val attempts = records.size
    val passed = records.count { it.passed }
    val highest = records.maxOfOrNull { it.score } ?: 0
    val average = if (records.isEmpty()) 0 else records.sumOf { it.score } / records.size
    val bestPaper = records.maxByOrNull { it.score }?.paperName

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            PageTitle(title = "成长", subtitle = "用数据看见每天的努力")
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GrowthStat("本周学习", DateUtils.formatMinutes(stats.weekMinutes), PrimaryDeep, PrimarySoft, Modifier.weight(1f))
                        GrowthStat("累计学习", DateUtils.formatHours(stats.totalMinutes) + " 小时", AccentPurple, PurpleSoft, Modifier.weight(1f))
                        GrowthStat("连续打卡", "${stats.streak} 天", AccentOrange, OrangeSoft, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(20.dp))
                }
                item {
                    SectionTitle(title = "学习趋势", badge = "最近 7 天")
                    Spacer(Modifier.height(10.dp))
                    WeeklyChartCard(values = stats.last7Days, labels = stats.last7Labels, goal = stats.dailyGoal)
                    Spacer(Modifier.height(20.dp))
                }
                item {
                    SectionTitle(title = "考试表现", action = "全部记录", onAction = { appState.push(Screen.Records) })
                    Spacer(Modifier.height(10.dp))
                    AppCard(padding = 18.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatRing(
                                progress = average / 100f,
                                centerTop = if (attempts == 0) "--" else "$average",
                                centerBottom = "平均分",
                                size = 96.dp
                            )
                            Spacer(Modifier.width(18.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Column(Modifier.weight(1f)) {
                                        Text("已完成", fontSize = 10.sp, color = TextHint)
                                        Text("$attempts 套", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("通过", fontSize = 10.sp, color = TextHint)
                                        Text("$passed 套", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDeep)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("最高分", fontSize = 10.sp, color = TextHint)
                                        Text(if (attempts == 0) "--" else "$highest", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryDeep)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.8f)))
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    when {
                                        attempts == 0 -> "完成第一套试卷，开始记录你的成长曲线"
                                        average >= 85 -> "状态稳定，保持节奏冲击更高分"
                                        average >= 60 -> "基础已过关，重点复盘错题再提分"
                                        else -> "先把错题本刷一遍，正确率会提升更快"
                                    },
                                    fontSize = 11.sp,
                                    color = TextSub,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        if (bestPaper != null) {
                            Spacer(Modifier.height(13.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFFAF0))
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.EmojiEvents, null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    "最高分试卷：$bestPaper",
                                    fontSize = 11.sp,
                                    color = TextSub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                item {
                    SectionTitle(title = "最近记录", action = if (records.size > 3) "查看全部" else null, onAction = { appState.push(Screen.Records) })
                    Spacer(Modifier.height(10.dp))
                }
                if (loading && records.isEmpty()) {
                    item {
                        AppCard(padding = 18.dp) {
                            Text("正在同步考试记录…", fontSize = 12.sp, color = TextHint)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                } else if (error != null && records.isEmpty()) {
                    item {
                        AppCard(padding = 18.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(error!!, fontSize = 11.sp, color = Danger, modifier = Modifier.weight(1f))
                                Text(
                                    "重试",
                                    fontSize = 12.sp,
                                    color = PrimaryDeep,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { appState.bump() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                } else if (records.isEmpty()) {
                    item {
                        AppCard(padding = 18.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SoftIconBox(Icons.Outlined.ShowChart, Primary, PrimarySoft, size = 42.dp, iconSize = 21.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("还没有考试记录", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                                    Spacer(Modifier.height(3.dp))
                                    Text("去题库完成一套练习，记录会自动同步", fontSize = 10.sp, color = TextHint)
                                }
                            }
                        }
                    }
                } else {
                    items(records.take(3).size) { index ->
                        RecordRow(records[index]) { appState.push(Screen.ReviewList(records[index].id)) }
                        Spacer(Modifier.height(9.dp))
                    }
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    AppCard(onClick = { appState.push(Screen.Certificates) }, padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftIconBox(Icons.Outlined.WorkspacePremium, AccentOrange, OrangeSoft, size = 44.dp, iconSize = 22.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("学习证书", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                                Spacer(Modifier.height(3.dp))
                                Text("通过考试后自动生成，可随时查看", fontSize = 10.sp, color = TextHint)
                            }
                            Icon(Icons.Outlined.ArrowForwardIos, null, tint = TextHint, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthStat(label: String, value: String, tint: Color, background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(vertical = 13.dp, horizontal = 12.dp)
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = TextHint)
    }
}

@Composable
private fun WeeklyChartCard(values: List<Int>, labels: List<String>, goal: Int) {
    val max = maxOf(values.maxOrNull() ?: 0, goal, 1)
    AppCard(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("学习时长", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSub)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.LocalFireDepartment, null, tint = AccentOrange, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
            Text("每日目标 $goal 分钟", fontSize = 10.sp, color = TextHint)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { index, value ->
                val isToday = index == values.lastIndex
                val ratio = (value.toFloat() / max).coerceIn(0f, 1f)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (value > 0) "$value" else "",
                        fontSize = 9.sp,
                        color = if (isToday) PrimaryDeep else TextHint
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((6 + 84 * ratio).dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isToday) Brush.verticalGradient(listOf(Primary, PrimaryDeep))
                                else Brush.verticalGradient(listOf(Color(0xFFDCE7FF), Color(0xFFEAF1FF)))
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { index, label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = if (index == labels.lastIndex) PrimaryDeep else TextHint,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RecordRow(record: AttemptRecord, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (record.passed) GreenSoft else Color(0xFFFFF4E8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${record.score}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (record.passed) GreenDeep else AccentOrange
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                record.paperName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${record.startTime} · ${DateUtils.formatUsedTime(record.usedTime)}",
                fontSize = 10.sp,
                color = TextHint
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (record.passed) "已通过" else "未通过",
                fontSize = 10.sp,
                color = if (record.passed) GreenDeep else TextHint
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (record.submitType == "TAB_SWITCH") "切屏交卷" else "正常交卷",
                fontSize = 9.sp,
                color = TextHint
            )
        }
    }
}
