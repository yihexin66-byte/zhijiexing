package com.example.zhijiexing.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.DateUtils
import com.example.zhijiexing.data.SubmitResult
import com.example.zhijiexing.data.UnauthorizedException
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.DecoBackground
import com.example.zhijiexing.ui.components.ErrorBox
import com.example.zhijiexing.ui.components.GhostButton
import com.example.zhijiexing.ui.components.LoadingBox
import com.example.zhijiexing.ui.components.PrimaryButton
import com.example.zhijiexing.ui.components.StatRing
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.Danger
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

@Composable
fun ResultScreen(appState: AppState, attemptId: String) {
    var result by remember { mutableStateOf<SubmitResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(attemptId) {
        loading = true
        error = null
        runCatching { appState.repository.result(attemptId) }
            .onSuccess { result = it }
            .onFailure { if (it !is UnauthorizedException) error = it.message ?: "成绩加载失败" }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "考试结果",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { appState.pop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, "关闭", tint = TextSub, modifier = Modifier.size(18.dp))
                }
            }
            when {
                loading -> LoadingBox(Modifier.padding(top = 80.dp))
                error != null || result == null -> ErrorBox(
                    message = error ?: "成绩不存在",
                    onRetry = {
                        appState.repository.clearNetworkCaches()
                        appState.pop()
                        appState.push(Screen.Result(attemptId))
                    }
                )
                else -> {
                    val data = result!!
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))
                        ResultHero(data)
                        Spacer(Modifier.height(16.dp))

                        AppCard(padding = 18.dp) {
                            Row(Modifier.fillMaxWidth()) {
                                ResultMetric(
                                    value = "${data.correctCount}/${data.totalQuestions}",
                                    label = "答对题数",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricDivider()
                                ResultMetric(
                                    value = DateUtils.formatUsedTime(data.usedTime),
                                    label = "答题用时",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricDivider()
                                ResultMetric(
                                    value = submitTypeLabel(data.submitType),
                                    label = "交卷方式",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF7FAFF))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.TaskAlt, null, tint = PrimaryDeep, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    if (data.passed) "成绩已达标，继续保持这个节奏" else "还差 ${(data.passScore - data.score).coerceAtLeast(0)} 分及格，先复盘错题再练一次",
                                    fontSize = 11.sp,
                                    color = TextSub
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        if (data.certificateGenerated) {
                            CertificateBanner()
                            Spacer(Modifier.height(12.dp))
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable { appState.push(Screen.ReviewList(data.attemptId)) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = PrimaryDeep, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("查看答案解析", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
                                Text("错题已自动收藏进错题本", fontSize = 10.sp, color = TextHint)
                            }
                            Text("去看看", fontSize = 11.sp, color = PrimaryDeep)
                        }
                        Spacer(Modifier.height(20.dp))

                        PrimaryButton(
                            text = "查看全卷解析",
                            onClick = { appState.push(Screen.ReviewList(data.attemptId)) }
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Box(Modifier.weight(1f)) {
                                GhostButton(
                                    text = "再练一次",
                                    onClick = { appState.push(Screen.Quiz(data.paperId, "再练一次")) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) {
                                GhostButton(
                                    text = "返回",
                                    onClick = { appState.pop() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(Modifier.height(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHero(data: SubmitResult) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (data.passed) Brush.linearGradient(listOf(Color(0xFF32A06B), Color(0xFF5FC98E), Color(0xFF83D9A8)))
                else Brush.linearGradient(listOf(Color(0xFF4C7BF0), Primary, Color(0xFF7FA8FF)))
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color(0x1AFFFFFF), radius = size.width * 0.38f, center = Offset(size.width * 0.95f, 0f))
            drawCircle(Color(0x12FFFFFF), radius = size.width * 0.26f, center = Offset(size.width * 0.15f, size.height))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (data.passed) Icons.Outlined.EmojiEvents else Icons.Outlined.Schedule,
                    null,
                    tint = Color(0xFFFFE3A3),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (data.passed) "恭喜通过" else "本次未通过",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(16.dp))
            ScoreRing(score = data.score, passed = data.passed)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "满分 ${data.totalScore} · 及格 ${data.passScore}",
                    fontSize = 11.sp,
                    color = Color(0xFFE6F2FF)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        submitTypeLabel(data.submitType),
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int, passed: Boolean) {
    Box(Modifier.size(142.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 9.dp.toPx()
            val inset = sw / 2
            val arcSize = Size(size.width - sw, size.height - sw)
            drawArc(
                color = Color(0x33FFFFFF),
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
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("分", fontSize = 13.sp, color = Color(0xFFE6F2FF), modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(
                if (passed) "已达标" else "继续加油",
                fontSize = 11.sp,
                color = Color(0xFFE6F2FF)
            )
        }
    }
}

@Composable
private fun ResultMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = TextHint)
    }
}

@Composable
private fun MetricDivider() {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(34.dp)
            .background(Line)
    )
}

@Composable
private fun CertificateBanner() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFF6E5), Color(0xFFFFEDD0))))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x33FFB84D)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.WorkspacePremium, null, tint = AccentOrange, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("证书已生成", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94621A))
            Text("你已获得新版学习证书，可在「我的 - 学习证书」查看", fontSize = 10.sp, color = Color(0xFFB4813A))
        }
    }
}

fun submitTypeLabel(type: String): String = when (type) {
    "TIMEOUT" -> "超时交卷"
    "TAB_SWITCH" -> "切屏交卷"
    else -> "手动交卷"
}
