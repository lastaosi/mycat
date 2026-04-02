package com.lastaosi.mycat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 컬러 정의
object MyCatColors {
    val Primary        = Color(0xFFFF8C42)
    val OnPrimary      = Color(0xFFFFFFFF)
    val Background     = Color(0xFFFFF8F3)
    val OnBackground   = Color(0xFF5C3A1E)
    val Surface        = Color(0xFFFFE8D6)
    val OnSurface      = Color(0xFF5C3A1E)
    val TextMuted      = Color(0xFFA0785A)
    val Border         = Color(0xFFE8D5C4)
    val Success        = Color(0xFF4CAF50)
    val Secondary      = Color(0xFFD4621A)
}

private val MyCatColorScheme = lightColorScheme(
    primary          = MyCatColors.Primary,
    onPrimary        = MyCatColors.OnPrimary,
    background       = MyCatColors.Background,
    onBackground     = MyCatColors.OnBackground,
    surface          = MyCatColors.Surface,
    onSurface        = MyCatColors.OnSurface,
    secondary        = MyCatColors.Secondary,
    onSecondary      = MyCatColors.OnPrimary,
)

@Composable
fun MyCatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MyCatColorScheme,
        content = content
    )
}