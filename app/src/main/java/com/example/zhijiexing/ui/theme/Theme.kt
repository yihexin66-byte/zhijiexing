package com.example.zhijiexing.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryDeep,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = GreenSoft,
    onSecondaryContainer = GreenDeep,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    tertiaryContainer = PurpleSoft,
    onTertiaryContainer = AccentPurple,
    background = Background,
    onBackground = TextMain,
    surface = SurfaceWhite,
    onSurface = TextMain,
    surfaceVariant = SoftBlueBg,
    onSurfaceVariant = TextSub,
    outline = Line,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ZhijiexingTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.White.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
