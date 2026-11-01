package com.savoj.vatkalkulator

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Кольори, яких немає в Material 3: частини розподілу зарплати та попередження.
 * Усі кольори точок мають контраст щонайменше 3:1 до поверхні картки в обох темах.
 */
@Immutable
data class AppColors(
    val netto: Color,
    val zus: Color,
    val health: Color,
    val tax: Color,
    val ppk: Color,
    val warning: Color,
    val onWarningContainer: Color,
    val warningContainer: Color
)

private val LightAppColors = AppColors(
    netto = Color(0xFF0E9F6E),
    zus = Color(0xFFB7791F),
    health = Color(0xFF2F6FDB),
    tax = Color(0xFFD9503C),
    ppk = Color(0xFF7C5CD6),
    warning = Color(0xFFB7791F),
    onWarningContainer = Color(0xFF6B3F00),
    warningContainer = Color(0xFFFFF4DB)
)

private val DarkAppColors = AppColors(
    netto = Color(0xFF34D399),
    zus = Color(0xFFF5B94A),
    health = Color(0xFF6EA2F7),
    tax = Color(0xFFFF8672),
    ppk = Color(0xFFA78BFA),
    warning = Color(0xFFF5B94A),
    onWarningContainer = Color(0xFFFFD98A),
    warningContainer = Color(0xFF3A2C0E)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

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
    outline = Color(0xFF7B86A0),
    outlineVariant = Color(0xFFE0E6F2),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDEBE9),
    onErrorContainer = Color(0xFF5C0F0A),
    tertiary = Color(0xFF0E9F6E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7F5E8),
    onTertiaryContainer = Color(0xFF063F2C),
    // Без цих ролей Material 3 підставляє базові фіолетово-рожеві відтінки (наприклад, у Switch)
    surfaceTint = Color(0xFF2563EB),
    surfaceDim = Color(0xFFD9DFEB),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FD),
    surfaceContainer = Color(0xFFEEF2FA),
    surfaceContainerHigh = Color(0xFFE7ECF6),
    surfaceContainerHighest = Color(0xFFE0E6F2),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF3F6FB),
    inversePrimary = Color(0xFF7C9DFF)
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
    error = Color(0xFFFF8A80),
    onError = Color(0xFF0A101C),
    errorContainer = Color(0xFF4A1612),
    onErrorContainer = Color(0xFFFFD6D1),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF06281C),
    tertiaryContainer = Color(0xFF0F3D2E),
    onTertiaryContainer = Color(0xFFB6F0D8),
    surfaceTint = Color(0xFF7C9DFF),
    surfaceDim = Color(0xFF0A101C),
    surfaceBright = Color(0xFF222C44),
    surfaceContainerLowest = Color(0xFF0A101C),
    surfaceContainerLow = Color(0xFF0F1625),
    surfaceContainer = Color(0xFF121A2A),
    surfaceContainerHigh = Color(0xFF182238),
    surfaceContainerHighest = Color(0xFF222C44),
    inverseSurface = Color(0xFFE8EEFB),
    inverseOnSurface = Color(0xFF0A101C),
    inversePrimary = Color(0xFF2563EB)
)

/** Шкала типографіки: дрібний текст не менший за 12 sp, основний 15 sp. */
private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp
    )
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDarkTheme provides dark,
        LocalAppColors provides (if (dark) DarkAppColors else LightAppColors)
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
