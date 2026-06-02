package com.example.aiadflow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 深色主题配色，当前沿用 Compose 模板色；后续可替换为项目品牌色。
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// 浅色主题配色，当前页面大部分颜色在具体组件中声明，这里保留全局 Material 默认值。
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun AIAdFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Android 12+ 支持动态取色；Demo 默认可由调用方关闭，避免系统主题影响设计稿颜色。
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // 优先使用动态取色，其次按当前系统深浅色选择固定色板。
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 应用全局 Material3 主题，子组件可通过 MaterialTheme 读取颜色、排版和形状。
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
