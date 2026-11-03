package com.example.zhijiexing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.AppState
import com.example.zhijiexing.Screen
import com.example.zhijiexing.data.KnowledgePoint
import com.example.zhijiexing.data.KnowledgeSection
import com.example.zhijiexing.data.knowledgeById
import com.example.zhijiexing.data.knowledgePoints
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.AppTopBar
import com.example.zhijiexing.ui.components.DecoBackground
import com.example.zhijiexing.ui.components.InfoPill
import com.example.zhijiexing.ui.components.ProgressBar
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
import com.example.zhijiexing.ui.theme.SoftBlueBg
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub

/** 知识点标签对应的图标与配色，首页和列表共用 */
internal fun knowledgeVisual(tag: String): Triple<ImageVector, Color, Color> = when (tag) {
    "ArkTS" -> Triple(Icons.Outlined.Code, Primary, PrimarySoft)
    "状态管理" -> Triple(Icons.Outlined.Sync, AccentPurple, PurpleSoft)
    "ArkUI" -> Triple(Icons.Outlined.Dashboard, GreenDeep, GreenSoft)
    "路由" -> Triple(Icons.Outlined.AltRoute, PrimaryDeep, PrimarySoft)
    "网络" -> Triple(Icons.Outlined.CloudQueue, AccentOrange, OrangeSoft)
    "数据" -> Triple(Icons.Outlined.Save, GreenDeep, GreenSoft)
    "并发" -> Triple(Icons.Outlined.Bolt, Danger, DangerSoft)
    "组件" -> Triple(Icons.Outlined.Widgets, AccentPurple, PurpleSoft)
    "性能" -> Triple(Icons.Outlined.Speed, AccentOrange, OrangeSoft)
    "工程" -> Triple(Icons.Outlined.Inventory2, PrimaryDeep, PrimarySoft)
    "安全" -> Triple(Icons.Outlined.Lock, GreenDeep, GreenSoft)
    "后台" -> Triple(Icons.Outlined.Schedule, AccentPurple, PurpleSoft)
    "通知" -> Triple(Icons.Outlined.Notifications, AccentOrange, OrangeSoft)
    "日志" -> Triple(Icons.Outlined.BugReport, Primary, PrimarySoft)
    else -> Triple(Icons.Outlined.PhoneAndroid, PrimaryDeep, PrimarySoft)
}

@Composable
internal fun KnowledgeRow(
    point: KnowledgePoint,
    read: Boolean,
    onClick: () -> Unit
) {
    val (icon, tint, background) = knowledgeVisual(point.tag)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoftIconBox(icon, tint, background, size = 42.dp, iconSize = 21.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    point.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (read) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(GreenSoft)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("已读", fontSize = 8.sp, color = GreenDeep)
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                point.summary,
                fontSize = 10.sp,
                color = TextHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("${point.minutes} 分钟", fontSize = 10.sp, color = TextHint)
            Spacer(Modifier.height(2.dp))
            Icon(Icons.Outlined.ChevronRight, null, tint = TextHint.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun KnowledgeListScreen(appState: AppState) {
    val read = appState.readKnowledge
    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(title = "知识学习", subtitle = "华为官方文档精选", onBack = { appState.pop() })
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppCard(padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("学习进度", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "已学 ${read.size}/${knowledgePoints.size} 个知识点，配合题库效果更好",
                                    fontSize = 10.sp,
                                    color = TextHint
                                )
                            }
                            Text(
                                "${read.size * 100 / knowledgePoints.size.coerceAtLeast(1)}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDeep
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        ProgressBar(read.size.toFloat() / knowledgePoints.size.coerceAtLeast(1))
                    }
                }
                items(knowledgePoints, key = { it.id }) { point ->
                    AppCard(padding = 6.dp) {
                        KnowledgeRow(point, read.contains(point.id)) {
                            appState.push(Screen.KnowledgeDetail(point.id))
                        }
                    }
                }
                item {
                    Text(
                        "内容整理自华为开发者联盟官方指南，完整定义与版本差异以原文为准。",
                        fontSize = 10.sp,
                        color = TextHint,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KnowledgeDetailScreen(appState: AppState, id: String) {
    val point = knowledgeById(id)
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(id) {
        appState.markKnowledgeRead(id)
    }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = "知识学习",
                subtitle = point?.tag ?: "知识点",
                onBack = { appState.pop() }
            )
            if (point == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("知识点不存在或已下线", fontSize = 13.sp, color = TextHint)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (icon, tint, background) = knowledgeVisual(point.tag)
                                SoftIconBox(icon, tint, background, size = 36.dp, iconSize = 18.dp)
                                Spacer(Modifier.width(8.dp))
                                InfoPill(point.tag)
                                Spacer(Modifier.width(6.dp))
                                Text("约 ${point.minutes} 分钟", fontSize = 10.sp, color = TextHint)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                point.title,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain,
                                lineHeight = 31.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(point.summary, fontSize = 12.sp, color = TextSub, lineHeight = 20.sp)
                        }
                    }
                    item {
                        AppCard(padding = 18.dp) {
                            point.sections.forEachIndexed { index, section ->
                                if (index > 0) {
                                    Spacer(Modifier.height(16.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Line.copy(alpha = 0.7f))
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                                KnowledgeSectionBlock(section)
                            }
                        }
                    }
                    item {
                        AppCard(padding = 16.dp) {
                            Text("内容来源", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                            Spacer(Modifier.height(6.dp))
                            Text(point.sourceTitle, fontSize = 11.sp, color = TextSub)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimarySoft)
                                    .clickable {
                                        runCatching { uriHandler.openUri(point.sourceUrl) }
                                            .onFailure { appState.showMessage("无法打开浏览器，请稍后重试") }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "查看官方原文",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryDeep,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Outlined.OpenInNew,
                                    null,
                                    tint = PrimaryDeep.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "以上为官方指南的要点整理，版本差异以原文为准。",
                                fontSize = 9.sp,
                                color = TextHint,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeSectionBlock(section: KnowledgeSection) {
    Text(section.heading, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
    Spacer(Modifier.height(6.dp))
    Text(section.body, fontSize = 12.sp, color = TextSub, lineHeight = 20.sp)
    section.code?.let { code ->
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SoftBlueBg)
                .padding(horizontal = 13.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(Primary.copy(alpha = 0.55f)))
                Spacer(Modifier.width(5.dp))
                Text("示例代码", fontSize = 9.sp, color = TextHint)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                code,
                fontSize = 10.5.sp,
                color = TextMain,
                fontFamily = FontFamily.Monospace,
                lineHeight = 17.sp
            )
        }
    }
}
