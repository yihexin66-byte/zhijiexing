package com.example.zhijiexing.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.R
import com.example.zhijiexing.ui.theme.Primary
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub

@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8FBFF), Color(0xFFEDF4FF), Color(0xFFF7FAFF))
                )
            )
            // 左下蓝色液体块
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x665B8CFF), Color(0x005B8CFF)),
                    center = Offset(size.width * 0.05f, size.height * 0.94f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.05f, size.height * 0.94f)
            )
            // 右上淡紫光晕
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x3DA78BFA), Color(0x00A78BFA)),
                    center = Offset(size.width * 0.95f, size.height * 0.12f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.95f, size.height * 0.12f)
            )
            // 橙色小光球
            drawCircle(
                color = Color(0x55FFB347),
                radius = 10.dp.toPx(),
                center = Offset(size.width * 0.82f, size.height * 0.56f)
            )
            drawCircle(
                color = Color(0x335B8CFF),
                radius = 4.dp.toPx(),
                center = Offset(size.width * 0.16f, size.height * 0.66f)
            )
            drawCircle(
                color = Color(0x44A78BFA),
                radius = 5.dp.toPx(),
                center = Offset(size.width * 0.12f, size.height * 0.24f)
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1.25f))
            Image(
                painter = painterResource(R.drawable.app_icon),
                contentDescription = "智解星",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(118.dp)
                    .scale(pulse)
                    .shadow(24.dp, RoundedCornerShape(30.dp), ambientColor = Color(0x335B8CFF), spotColor = Color(0x335B8CFF))
                    .clip(RoundedCornerShape(30.dp))
            )
            Spacer(Modifier.height(30.dp))
            Text("智解星", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextMain, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Text("让刷题更简单", fontSize = 15.sp, color = TextSub)
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "智能刷题与学习成长助手",
                    fontSize = 12.sp,
                    color = TextSub.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1f, 1f, 1f).forEach { alpha ->
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Primary.copy(alpha = if (alpha == 1f) 0.75f else 0.28f))
                    )
                }
            }
            Spacer(Modifier.height(42.dp))
        }
    }
}
