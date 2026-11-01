package com.savoj.vatkalkulator

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF111A3A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF5EE6A8),
    onSecondary = Color(0xFF0A2A1E),
    secondaryContainer = Color(0xFFDCE6FF),
    onSecondaryContainer = Color(0xFF1E3FA8),
    background = Color(0xFFF3F6FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2FA),
    onSurfaceVariant = Color(0xFF55607A),
    outline = Color(0xFF8A94AB),
    outlineVariant = Color(0xFFE0E6F2),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C9DFF),
    onPrimary = Color(0xFF0A101C),
    primaryContainer = Color(0xFF16203A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF5EE6A8),
    onSecondary = Color(0xFF0A2A1E),
    secondaryContainer = Color(0xFF22305C),
    onSecondaryContainer = Color(0xFFC9D7FF),
    background = Color(0xFF0A101C),
    onBackground = Color(0xFFE8EEFB),
    surface = Color(0xFF121A2A),
    onSurface = Color(0xFFE8EEFB),
    surfaceVariant = Color(0xFF182238),
    onSurfaceVariant = Color(0xFFA3AEC6),
    outline = Color(0xFF6B7690),
    outlineVariant = Color(0xFF222C44),
    error = Color(0xFFFF8A80)
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            content = content
        )
    }
}