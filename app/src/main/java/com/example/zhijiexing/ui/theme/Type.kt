package com.example.zhijiexing.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val family = FontFamily.SansSerif

val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, color = TextMain),
    headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, color = TextMain),
    headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp, color = TextMain),
    titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp, color = TextMain),
    titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, color = TextMain),
    titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = TextMain),
    bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp, color = TextMain),
    bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp, color = TextMain),
    bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp, color = TextSub),
    labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, color = TextMain),
    labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = TextSub),
    labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, color = TextHint)
)
