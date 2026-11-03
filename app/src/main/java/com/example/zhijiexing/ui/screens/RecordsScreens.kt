package com.example.zhijiexing.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.AttemptRecord
import com.example.zhijiexing.data.CertificateInfo
import com.example.zhijiexing.data.DateUtils
import com.example.zhijiexing.data.UnauthorizedException
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.AppTopBar
import com.example.zhijiexing.ui.components.DecoBackground
import com.example.zhijiexing.ui.components.EmptyState
import com.example.zhijiexing.ui.components.ErrorBox
import com.example.zhijiexing.ui.components.LoadingBox
import com.example.zhijiexing.ui.components.StatRing
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.Gold
import com.example.zhijiexing.ui.theme.GreenDeep
import com.example.zhijiexing.ui.theme.GreenSoft
import com.example.zhijiexing.ui.theme.Navy
import com.example.zhijiexing.ui.theme.Primary
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.PrimarySoft
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub

@Composable
fun RecordsScreen(appState: AppState) {
    val version = appState.dataVersion
    var records by remember { mutableStateOf<List<AttemptRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var filter by remember { mutableIntStateOf(0) }

    LaunchedEffect(version, refresh) {
        loading = records.isEmpty()
        error = null
        runCatching { appState.repository.records(force = refresh > 0) }
            .onSuccess { records = it }
            .onFailure { if (it !is UnauthorizedException) error = it.message ?: "记录加载失败" }
        loading = false
    }

    val filtered = remember(records, filter) {
        when (filter) {
            1 -> records.filter { it.passed }
            2 -> records.filter { !it.passed }
            else -> records
        }
    }
    val average = if (records.isEmpty()) 0 else records.sumOf { it.score } / records.size

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = "刷题记录",
                subtitle = "共 ${records.size} 次 · 通过 ${records.count { it.passed }} 次",
                onBack = { appState.pop() },
                actions = {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { refresh++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Refresh, "刷新", tint = TextSub, modifier = Modifier.size(18.dp))
                    }
                }
            )
            when {
                loading -> LoadingBox()
                error != null && records.isEmpty() -> ErrorBox(message = error!!, onRetry = { refresh++ })
                records.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.EmojiEvents,
                    title = "还没有刷题记录",
                    subtitle = "完成第一套试卷后，成绩、用时与交卷方式都会记录在这里。",
                    action = "去题库练习",
                    onAction = { appState.goTab(1) }
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    item {
                        AppCard(padding = 18.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatRing(
                                    progress = average / 100f,
                                    centerTop = "$average",
                                    centerBottom = "平均分",
                                    size = 84.dp,
                                    stroke = 8.dp
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Row {
                                        Column(Modifier.weight(1f)) {
                                            Text("总次数", fontSize = 10.sp, color = TextHint)
                                            Text("${records.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text("通过率", fontSize = 10.sp, color = TextHint)
                                            Text(
                                                "${records.count { it.passed } * 100 / records.size}%",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenDeep
                                            )
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text("最高分", fontSize = 10.sp, color = TextHint)
                                            Text(
                                                "${records.maxOfOrNull { it.score } ?: 0}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDeep
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "最近一次：${records.firstOrNull()?.startTime ?: "--"}",
                                        fontSize = 10.sp,
                                        color = TextHint
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("全部 ${records.size}", "已通过 ${records.count { it.passed }}", "未通过 ${records.count { !it.passed }}")
                                .forEachIndexed { index, label ->
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
                                icon = Icons.Outlined.EmojiEvents,
                                title = "这个分类还没有记录",
                                subtitle = "切换筛选条件看看其他记录。"
                            )
                        }
                    } else {
                        items(filtered.size) { index ->
                            val record = filtered[index]
                            RecordRow(record) { appState.push(Screen.ReviewList(record.id)) }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CertificatesScreen(appState: AppState) {
    val version = appState.dataVersion
    var certificates by remember { mutableStateOf<List<CertificateInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(version, refresh) {
        loading = true
        error = null
        runCatching { appState.repository.examApi.certificates() }
            .onSuccess { certificates = it }
            .onFailure { if (it !is UnauthorizedException) error = it.message ?: "证书加载失败" }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = "学习证书",
                subtitle = "通过考试后由服务端签发",
                onBack = { appState.pop() },
                actions = {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { refresh++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Refresh, "刷新", tint = TextSub, modifier = Modifier.size(18.dp))
                    }
                }
            )
            when {
                loading -> LoadingBox()
                error != null && certificates.isEmpty() -> ErrorBox(message = error!!, onRetry = { refresh++ })
                certificates.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.WorkspacePremium,
                    title = "还没有获得证书",
                    subtitle = "考试达到及格线后自动签发证书，记录会与账号绑定。",
                    action = "去题库练习",
                    onAction = { appState.goTab(1) }
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PrimarySoft)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.WorkspacePremium, null, tint = PrimaryDeep, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "共获得 ${certificates.size} 张证书，可在面试或简历中展示编号",
                                fontSize = 10.sp,
                                color = PrimaryDeep,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    items(certificates.size) { index ->
                        CertificateCard(certificates[index])
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificateCard(cert: CertificateInfo) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF233B5E), Color(0xFF31517D), Color(0xFF456C9E))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color(0x14FFFFFF), radius = size.width * 0.35f, center = Offset(size.width * 0.95f, 0f))
            drawCircle(Color(0x0FFFFFFF), radius = size.width * 0.24f, center = Offset(size.width * 0.05f, size.height))
            drawCircle(Color(0x22FFD98A), radius = 3.dp.toPx(), center = Offset(size.width * 0.8f, size.height * 0.78f))
            drawCircle(Color(0x22FFD98A), radius = 2.dp.toPx(), center = Offset(size.width * 0.86f, size.height * 0.6f))
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFD98A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WorkspacePremium, null, tint = Gold, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("CERTIFICATE", fontSize = 10.sp, color = Color(0xFFC9D9EE), letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0x33FFD98A))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        if (cert.status.isBlank()) "有效" else cert.status,
                        fontSize = 9.sp,
                        color = Gold
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                cert.name.ifBlank { "学习认证证书" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
            Spacer(Modifier.height(12.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("证书编号", fontSize = 9.sp, color = Color(0xFF9FB4CE))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        cert.certificateNo.ifBlank { "--" },
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("签发日期", fontSize = 9.sp, color = Color(0xFF9FB4CE))
                    Spacer(Modifier.height(3.dp))
                    Text(cert.issueDate.ifBlank { "--" }, fontSize = 11.sp, color = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("有效期至", fontSize = 9.sp, color = Color(0xFF9FB4CE))
                    Spacer(Modifier.height(3.dp))
                    Text(cert.expiryDate.ifBlank { "长期" }, fontSize = 11.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.EmojiEvents, null, tint = Gold, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text("智解星 · 学习成就认证", fontSize = 9.sp, color = Color(0xFFC9D9EE))
            }
        }
    }
}
