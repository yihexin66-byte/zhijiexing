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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.PaperInfo
import com.example.zhijiexing.data.UnauthorizedException
import com.example.zhijiexing.data.knowledgePoints
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.AppBrandHeader
import com.example.zhijiexing.ui.components.Avatar
import com.example.zhijiexing.ui.components.ErrorBox
import com.example.zhijiexing.ui.components.LoadingBox
import com.example.zhijiexing.ui.components.ProgressBar
import com.example.zhijiexing.ui.components.SectionTitle
import com.example.zhijiexing.ui.components.SoftIconBox
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.AccentPurple
import com.example.zhijiexing.ui.theme.Danger
import com.example.zhijiexing.ui.theme.DangerSoft
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
import java.util.Calendar

@Composable
fun HomeScreen(appState: AppState) {
    val version = appState.dataVersion
    val stats = remember(version) { appState.stats() }
    val readKnowledge = appState.readKnowledge
    val recommendedKnowledge = remember(readKnowledge) {
        val unread = knowledgePoints.filterNot { it.id in readKnowledge }
        val read = knowledgePoints.filter { it.id in readKnowledge }
        (unread + read).distinctBy { it.id }.take(3)
    }
    var papers by remember { mutableStateOf<List<PaperInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editProfile by remember { mutableStateOf(false) }

    LaunchedEffect(version) {
        loading = papers.isEmpty()
        error = null
        runCatching { appState.repository.papers() }
            .onSuccess { papers = it }
            .onFailure {
                if (it !is UnauthorizedException) error = it.message ?: "网络连接失败"
            }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            AppBrandHeader()
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${greeting()}，${appState.displayName}",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (readKnowledge.isEmpty()) "每天一个知识点，配合刷题更扎实"
                        else "已学习 ${readKnowledge.size}/${knowledgePoints.size} 个知识点，保持节奏",
                        fontSize = 12.sp,
                        color = TextHint
                    )
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .clickable { editProfile = true }
                ) {
                    Avatar(46.dp, appState.avatarPath)
                }
            }
            Spacer(Modifier.height(18.dp))

            TodayHeroCard(
                minutes = stats.todayMinutes,
                goal = stats.dailyGoal,
                progress = stats.dailyProgress,
                streak = stats.streak,
                onStart = { appState.goTab(1) }
            )
            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAction("题库练习", Icons.Outlined.MenuBook, Primary, PrimarySoft) { appState.goTab(1) }
                QuickAction("错题本", Icons.Outlined.Assignment, Danger, DangerSoft) { appState.push(Screen.WrongBook) }
                QuickAction("我的收藏", Icons.Outlined.BookmarkBorder, AccentPurple, PurpleSoft) { appState.push(Screen.Bookmarks) }
            }
            Spacer(Modifier.height(22.dp))

            SectionTitle(
                title = "知识学习",
                badge = "已学 ${readKnowledge.size}/${knowledgePoints.size}",
                action = "全部",
                onAction = { appState.push(Screen.KnowledgeList) }
            )
            Spacer(Modifier.height(10.dp))
            AppCard(padding = 10.dp) {
                recommendedKnowledge.forEachIndexed { index, point ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Line.copy(alpha = 0.7f))
                        )
                    }
                    KnowledgeRow(point, readKnowledge.contains(point.id)) {
                        appState.push(Screen.KnowledgeDetail(point.id))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))

            SectionTitle(title = "推荐练习", action = "全部题库", onAction = { appState.goTab(1) })
            Spacer(Modifier.height(10.dp))
            when {
                loading -> LoadingBox()
                error != null -> ErrorBox(message = error!!, onRetry = { appState.bump() })
                papers.isEmpty() -> Text("暂无可用试卷", fontSize = 12.sp, color = TextHint)
                else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(papers.take(6)) { paper ->
                        HomePaperCard(paper) {
                            appState.push(Screen.Quiz(paper.id, paper.name))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (editProfile) {
        ProfileEditDialog(appState, onDismiss = { editProfile = false })
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 6 -> "夜深了"
        hour < 12 -> "早上好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
}

@Composable
private fun TodayHeroCard(
    minutes: Int,
    goal: Int,
    progress: Float,
    streak: Int,
    onStart: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4C7BF0), Primary, Color(0xFF7FA8FF))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color(0x1AFFFFFF), radius = size.width * 0.34f, center = Offset(size.width * 0.92f, size.height * 0.05f))
            drawCircle(Color(0x14FFFFFF), radius = size.width * 0.26f, center = Offset(size.width * 0.72f, size.height * 0.95f))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今日学习", color = Color(0xFFE4ECFF), fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocalFireDepartment, null, tint = Color(0xFFFFD98A), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("连续 $streak 天", fontSize = 10.sp, color = Color.White)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$minutes", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("分钟", fontSize = 13.sp, color = Color(0xFFDCE7FF), modifier = Modifier.padding(bottom = 7.dp))
                    Spacer(Modifier.weight(1f))
                    Text("目标 $goal 分钟", fontSize = 11.sp, color = Color(0xFFCFDDFF), modifier = Modifier.padding(bottom = 8.dp))
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0x3DFFFFFF))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { onStart() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("继续刷题", fontSize = 12.sp, color = PrimaryDeep, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = PrimaryDeep, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            GoalRing(progress = progress)
        }
    }
}

@Composable
private fun GoalRing(progress: Float) {
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 8.dp.toPx()
            val inset = sw / 2
            val arcSize = Size(size.width - sw, size.height - sw)
            drawArc(
                color = Color(0x3DFFFFFF),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(sw, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("今日目标", fontSize = 9.sp, color = Color(0xFFDCE7FF))
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SoftIconBox(icon, tint, background, size = 48.dp, iconSize = 23.dp, radius = 16.dp)
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 11.sp, color = TextSub)
    }
}

@Composable
private fun HomePaperCard(paper: PaperInfo, onClick: () -> Unit) {
    Box(
        Modifier
            .width(228.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(15.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MenuBook, null, tint = PrimaryDeep, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                if (paper.bestScore > 0) {
                    Text("最高 ${paper.bestScore}", fontSize = 10.sp, color = GreenDeep)
                } else {
                    Text("未开始", fontSize = 10.sp, color = TextHint)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                paper.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, tint = TextHint, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
                Text("${paper.duration} 分钟", fontSize = 10.sp, color = TextHint)
                Spacer(Modifier.width(10.dp))
                Text("${paper.questionCount} 题", fontSize = 10.sp, color = TextHint)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, tint = PrimaryDeep, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
