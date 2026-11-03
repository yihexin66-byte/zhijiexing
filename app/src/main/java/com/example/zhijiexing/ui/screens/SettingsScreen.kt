package com.example.zhijiexing.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.TrackChanges
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.zhijiexing.AppState
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.AppTopBar
import com.example.zhijiexing.ui.components.Avatar
import com.example.zhijiexing.ui.components.DecoBackground
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
fun SettingsScreen(appState: AppState) {
    val store = appState.repository.store
    var goal by remember { mutableIntStateOf(store.dailyGoal) }
    var direction by remember { mutableStateOf(store.direction) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(title = "学习设置", subtitle = "偏好与账号", onBack = { appState.pop() })
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(44.dp, appState.avatarPath)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            appState.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (appState.session?.role == "ADMIN") {
                                "管理员账号 · ${appState.session?.username ?: ""}"
                            } else {
                                "学习账号 · ${appState.session?.username ?: ""}"
                            },
                            fontSize = 10.sp,
                            color = TextHint
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(PrimarySoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("已登录", fontSize = 9.sp, color = PrimaryDeep)
                    }
                }
                Spacer(Modifier.height(20.dp))

                SettingsSectionTitle("学习偏好")
                AppCard(padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftIconBox(Icons.Outlined.TrackChanges, PrimaryDeep, PrimarySoft, size = 38.dp, iconSize = 19.dp, radius = 12.dp)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("每日学习目标", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
                            Text("当前 $goal 分钟 / 天", fontSize = 10.sp, color = TextHint)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 45, 60, 90, 120).forEach { value ->
                            val selected = goal == value
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) PrimaryDeep else Color(0xFFF4F7FC))
                                    .clickable {
                                        store.dailyGoal = value
                                        goal = value
                                        appState.bump()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "$value 分",
                                    fontSize = 11.sp,
                                    color = if (selected) Color.White else TextSub,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.75f)))
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftIconBox(Icons.Outlined.Info, AccentPurple, PurpleSoft, size = 38.dp, iconSize = 19.dp, radius = 12.dp)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("学习方向", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
                            Text("影响首页推荐", fontSize = 10.sp, color = TextHint)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HarmonyOS 应用开发", "Python 基础", "前端开发").forEach { item ->
                            val selected = direction == item
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) PrimaryDeep else Color(0xFFF4F7FC))
                                    .clickable {
                                        store.direction = item
                                        direction = item
                                        appState.bump()
                                    }
                                    .padding(horizontal = 13.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    item,
                                    fontSize = 11.sp,
                                    color = if (selected) Color.White else TextSub
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))

                SettingsSectionTitle("账号与数据")
                AppCard(padding = 0.dp) {
                    SettingsRow(
                        icon = Icons.Outlined.DeleteOutline,
                        tint = AccentOrange,
                        background = OrangeSoft,
                        title = "清除本地学习数据",
                        subtitle = "收藏、错题与统计，保留登录状态",
                        onClick = { showClearConfirm = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Logout,
                        tint = Danger,
                        background = Color(0xFFFFF1F1),
                        title = "退出登录",
                        subtitle = "清除本机保存的登录令牌",
                        onClick = { showLogoutConfirm = true },
                        showChevron = false
                    )
                }
                Spacer(Modifier.height(18.dp))

                SettingsSectionTitle("安全与关于")
                AppCard(padding = 16.dp) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Security, null, tint = PrimaryDeep, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("考试防作弊", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "答题时把应用切到后台会被记录，累计 3 次自动交卷。",
                                fontSize = 10.sp,
                                color = TextHint,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.75f)))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Lock, null, tint = TextSub, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("登录与令牌", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "登录令牌仅保存在本机，失效后需重新登录。",
                                fontSize = 10.sp,
                                color = TextHint,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "智解星 v1.0.0 · 智能刷题与学习成长助手",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 10.sp,
                    color = TextHint,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
            }
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "清除本地学习数据？",
            message = "收藏、错题、学习统计与知识学习进度将被清空，登录状态与云端记录不受影响。",
            confirmText = "清除",
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                showClearConfirm = false
                store.clearLocalCache()
                appState.resetKnowledgeProgress()
                appState.applyPlan(store.loadPlan())
                goal = store.dailyGoal
                direction = store.direction
                appState.bump()
                appState.showMessage("本地学习数据已清除")
            }
        )
    }

    if (showLogoutConfirm) {
        ConfirmDialog(
            title = "退出登录？",
            message = "退出后需重新登录，本地学习数据会保留。",
            confirmText = "退出登录",
            onDismiss = { showLogoutConfirm = false },
            onConfirm = {
                showLogoutConfirm = false
                appState.logout()
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = TextHint,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
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
            Text("›", fontSize = 18.sp, color = TextHint)
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 66.dp, end = 16.dp)
            .height(1.dp)
            .background(Line.copy(alpha = 0.7f))
    )
}

@Composable
private fun ConfirmDialog(
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
                            .background(if (confirmText == "退出登录" || confirmText == "清除") Danger else PrimaryDeep)
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
