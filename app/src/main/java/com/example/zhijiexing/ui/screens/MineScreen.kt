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
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.AttemptRecord
import com.example.zhijiexing.data.CertificateInfo
import com.example.zhijiexing.data.DateUtils
import com.example.zhijiexing.data.UnauthorizedException
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.Avatar
import com.example.zhijiexing.ui.components.SoftIconBox
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.AccentPurple
import com.example.zhijiexing.ui.theme.Danger
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
fun MineScreen(appState: AppState) {
    val version = appState.dataVersion
    val stats = remember(version) { appState.stats() }
    var records by remember { mutableStateOf<List<AttemptRecord>>(emptyList()) }
    var certificates by remember { mutableStateOf<List<CertificateInfo>>(emptyList()) }
    var confirmLogout by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf(false) }
    val bookmarks = remember(version) { appState.repository.store.bookmarks().size }
    val wrongs = remember(version) { appState.repository.store.wrongQuestions().size }

    LaunchedEffect(version) {
        runCatching { appState.repository.records() }
            .onSuccess { records = it }
        runCatching { appState.repository.examApi.certificates() }
            .onSuccess { certificates = it }
            .onFailure { if (it is UnauthorizedException) return@LaunchedEffect }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 26.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("我的", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { appState.push(Screen.Settings) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Settings, "设置", tint = TextSub, modifier = Modifier.size(19.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                ProfileCard(appState, onEdit = { editProfile = true })
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MineStat("学习时长", DateUtils.formatHours(stats.totalMinutes) + "h", Modifier.weight(1f)) { appState.goTab(2) }
                    MineStat("刷题次数", "${records.size}", Modifier.weight(1f)) { appState.push(Screen.Records) }
                    MineStat("获得证书", "${certificates.size}", Modifier.weight(1f)) { appState.push(Screen.Certificates) }
                }
                Spacer(Modifier.height(20.dp))
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("学习工具", fontSize = 12.sp, color = TextHint, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    AppCard(padding = 0.dp) {
                        MineMenuItem(
                            icon = Icons.Outlined.History,
                            tint = PrimaryDeep,
                            background = PrimarySoft,
                            title = "刷题记录",
                            subtitle = if (records.isEmpty()) "还没有考试记录" else "最近一次 ${records.firstOrNull()?.score ?: 0} 分",
                            onClick = { appState.push(Screen.Records) }
                        )
                        MenuDivider()
                        MineMenuItem(
                            icon = Icons.Outlined.Assignment,
                            tint = Danger,
                            background = Color(0xFFFFECEC),
                            title = "错题本",
                            subtitle = if (wrongs == 0) "复盘时自动收集错题" else "$wrongs 道待复盘",
                            onClick = { appState.push(Screen.WrongBook) }
                        )
                        MenuDivider()
                        MineMenuItem(
                            icon = Icons.Outlined.BookmarkBorder,
                            tint = AccentPurple,
                            background = PurpleSoft,
                            title = "我的收藏",
                            subtitle = if (bookmarks == 0) "收藏重点题目随时回看" else "$bookmarks 道已收藏",
                            onClick = { appState.push(Screen.Bookmarks) }
                        )
                        MenuDivider()
                        MineMenuItem(
                            icon = Icons.Outlined.WorkspacePremium,
                            tint = AccentOrange,
                            background = OrangeSoft,
                            title = "学习证书",
                            subtitle = if (certificates.isEmpty()) "通过考试即可获得" else "已获得 ${certificates.size} 张证书",
                            onClick = { appState.push(Screen.Certificates) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("账号与设置", fontSize = 12.sp, color = TextHint, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    AppCard(padding = 0.dp) {
                        MineMenuItem(
                            icon = Icons.Outlined.Settings,
                            tint = TextSub,
                            background = Color(0xFFF2F5FA),
                            title = "学习设置",
                            subtitle = "每日目标与缓存",
                            onClick = { appState.push(Screen.Settings) }
                        )
                        MenuDivider()
                        MineMenuItem(
                            icon = Icons.Outlined.Logout,
                            tint = Danger,
                            background = Color(0xFFFFF1F1),
                            title = "退出登录",
                            subtitle = "退出后仍保留本地学习数据",
                            showChevron = false,
                            onClick = { confirmLogout = true }
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "智解星 v1.0.0\n刷题 · 解析 · 成长",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 10.sp,
                        color = TextHint.copy(alpha = 0.8f),
                        lineHeight = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }

    if (editProfile) {
        ProfileEditDialog(appState, onDismiss = { editProfile = false })
    }

    if (confirmLogout) {
        Dialog(onDismissRequest = { confirmLogout = false }) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
                Column(Modifier.padding(22.dp)) {
                    Text("退出登录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "退出后需重新登录才能同步刷题记录，本地数据会保留。",
                        fontSize = 12.sp,
                        color = TextSub,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF0F3F9))
                                .clickable { confirmLogout = false },
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
                                .clickable {
                                    confirmLogout = false
                                    appState.logout()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("退出登录", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(appState: AppState, onEdit: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF6B97FF), Primary, PrimaryDeep)))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(54.dp, appState.avatarPath)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        appState.displayName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (appState.session?.role == "ADMIN") "管理员" else "学员",
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${appState.repository.store.level} · ${appState.repository.store.direction}",
                            fontSize = 10.sp,
                            color = Color(0xFFDCE7FF)
                        )
                    }
                }
                Icon(Icons.Outlined.EmojiEvents, null, tint = Color(0xFFFFD98A), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("今日目标", fontSize = 10.sp, color = Color(0xFFDCE7FF))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${appState.stats().todayMinutes} / ${appState.stats().dailyGoal} 分钟",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable { onEdit() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("编辑资料", fontSize = 11.sp, color = PrimaryDeep, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MineStat(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = TextHint)
    }
}

@Composable
private fun MineMenuItem(
    icon: ImageVector,
    tint: Color,
    background: Color,
    title: String,
    subtitle: String,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoftIconBox(icon, tint, background, size = 38.dp, iconSize = 19.dp, radius = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = TextHint)
        }
        if (showChevron) {
            Icon(Icons.Outlined.ArrowForwardIos, null, tint = TextHint.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun MenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 66.dp, end = 16.dp)
            .height(1.dp)
            .background(Line.copy(alpha = 0.7f))
    )
}
