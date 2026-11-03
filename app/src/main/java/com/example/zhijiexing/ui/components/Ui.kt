package com.example.zhijiexing.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.AppState
import com.example.zhijiexing.R
import com.example.zhijiexing.ui.theme.AccentOrange
import com.example.zhijiexing.ui.theme.Background
import com.example.zhijiexing.ui.theme.ChartTrack
import com.example.zhijiexing.ui.theme.Danger
import com.example.zhijiexing.ui.theme.Line
import com.example.zhijiexing.ui.theme.Navy
import com.example.zhijiexing.ui.theme.Primary
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.PrimarySoft
import com.example.zhijiexing.ui.theme.SoftBlueBg
import com.example.zhijiexing.ui.theme.SurfaceWhite
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x0A274C8A), spotColor = Color(0x0A274C8A))
        .clip(RoundedCornerShape(20.dp))
        .background(SurfaceWhite)
        .border(1.dp, Color(0x66E9EEF5), RoundedCornerShape(20.dp))
    Column(
        modifier = if (onClick != null) base.clickable { onClick() } else base,
        content = { Column(Modifier.padding(padding), content = content) }
    )
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    badge: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimarySoft)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(badge, fontSize = 10.sp, color = PrimaryDeep, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.bodySmall,
                color = TextHint,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SoftIconBox(
    icon: ImageVector,
    tint: Color,
    background: Color,
    size: Dp = 46.dp,
    iconSize: Dp = 23.dp,
    radius: Dp = 15.dp
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    dark: Boolean = false,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val gradient = if (dark) {
        Brush.horizontalGradient(listOf(Navy, Color(0xFF3C5B80)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF6B97FF), Primary, PrimaryDeep))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (enabled) gradient else Brush.horizontalGradient(listOf(Color(0xFFB9C6DA), Color(0xFFA9B8CE))))
            .clickable(enabled = enabled && !loading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = TextSub
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF0F3F9))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = TextMain,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextHint)
            }
        }
        actions?.invoke()
    }
}

@Composable
fun PageTitle(title: String, subtitle: String? = null, actions: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextHint)
            }
        }
        actions?.invoke()
    }
}

@Composable
fun ProgressBar(
    value: Float,
    modifier: Modifier = Modifier,
    color: Color = Primary,
    track: Color = ChartTrack,
    height: Dp = 8.dp
) {
    val animated by animateFloatAsState(value.coerceIn(0f, 1f), tween(500), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun StatRing(
    progress: Float,
    centerTop: String,
    centerBottom: String,
    size: Dp = 100.dp,
    stroke: Dp = 9.dp
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            drawArc(
                color = Color(0xFFEAF0FC),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - sw, this.size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(Color(0xFF9DBCFF), Primary, PrimaryDeep)),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - sw, this.size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTop, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text(centerBottom, fontSize = 9.sp, color = TextHint)
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable (() -> Unit))? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextHint, fontSize = 14.sp) },
        leadingIcon = leadingIcon?.let {
            { Icon(it, null, tint = TextHint, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF8FAFF),
            unfocusedContainerColor = Color(0xFFF8FAFF),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Primary
        )
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary, strokeWidth = 2.5.dp, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F5FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Primary.copy(alpha = 0.75f), modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextHint,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            GhostButton(text = action, onClick = onAction, tint = PrimaryDeep)
        }
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF1F1)),
            contentAlignment = Alignment.Center
        ) {
            Text("!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Danger)
        }
        Spacer(Modifier.height(14.dp))
        Text("加载失败", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = TextHint,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(16.dp))
        GhostButton(text = "重新加载", onClick = onRetry, tint = PrimaryDeep)
    }
}

@Composable
fun LogoBadge(size: Dp = 44.dp, radius: Dp = 14.dp) {
    Image(
        painter = painterResource(R.drawable.app_icon),
        contentDescription = "智解星",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
    )
}

@Composable
fun Avatar(size: Dp = 52.dp, path: String? = null) {
    val bitmap = remember(path) {
        path?.takeIf { it.isNotBlank() }?.let { file ->
            runCatching { BitmapFactory.decodeFile(file) }.getOrNull()
        }
    }
    val modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .border(2.dp, Color.White, CircleShape)
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "头像",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(R.drawable.avatar_mascot),
            contentDescription = "头像",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun AppBrandHeader(
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoBadge(34.dp, 10.dp)
        Spacer(Modifier.width(8.dp))
        Text("智解星", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun BottomNavBar(appState: AppState) {
    val items = listOf(
        Triple("首页", Icons.Outlined.Home, 0),
        Triple("题库", Icons.Outlined.MenuBook, 1),
        Triple("成长", Icons.Outlined.ShowChart, 2),
        Triple("我的", Icons.Outlined.Person, 3)
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .border(width = 0.5.dp, color = Line, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 9.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { (label, icon, index) ->
                val selected = appState.selectedTab == index
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { appState.goTab(index) }
                        .padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (selected) Primary else TextHint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        fontSize = 10.sp,
                        color = if (selected) Primary else TextHint,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
fun DecoBackground(modifier: Modifier = Modifier, strong: Boolean = false) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFFF7FAFF), Background, Color(0xFFF9FBFF))))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x335B8CFF), Color(0x005B8CFF)),
                center = Offset(w * 0.88f, h * (if (strong) 0.10f else 0.04f)),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.88f, h * 0.10f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x26A78BFA), Color(0x00A78BFA)),
                center = Offset(w * 0.05f, h * 0.62f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.05f, h * 0.62f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x2AFFB347), Color(0x00FFB347)),
                center = Offset(w * 0.92f, h * 0.86f),
                radius = w * 0.35f
            ),
            radius = w * 0.35f,
            center = Offset(w * 0.92f, h * 0.86f)
        )
    }
}

@Composable
fun SmallActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String? = null,
    tint: Color = TextSub,
    background: Color = Color(0xFFF3F6FC)
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun SendButton(onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) Primary else Color(0xFFC9D6EA))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Send,
            contentDescription = "发送",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun InfoPill(
    text: String,
    color: Color = PrimaryDeep,
    background: Color = PrimarySoft
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AccentDot(color: Color = AccentOrange, size: Dp = 6.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}
