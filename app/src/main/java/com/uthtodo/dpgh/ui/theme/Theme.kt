package com.uthtodo.dpgh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.uthtodo.dpgh.data.local.AppPreferences

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun BaiTap5Theme(
    content: @Composable () -> Unit
) {
    val settings by AppPreferences.settings.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (settings.themeMode) {
        "Tối" -> true
        "Sáng" -> false
        else -> systemDark
    }
    val accent = settings.accentColor

    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary = accent,
            secondary = PurpleGrey80,
            tertiary = Pink80,
            background = Color(0xFF121214),
            surface = Color(0xFF1C1C1E),
            onPrimary = Color.White,
            onBackground = Color(0xFFF2F2F7),
            onSurface = Color(0xFFF2F2F7),
            onSurfaceVariant = Color(0xFFAEAEB2)
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = PurpleGrey40,
            tertiary = Pink40,
            background = Color(0xFFF5F5F7),
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Color(0xFF1C1C1E),
            onSurface = Color(0xFF1C1C1E),
            onSurfaceVariant = Color(0xFF6B6B70)
        )
    }

    CompositionLocalProvider(LocalIsDarkTheme provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
