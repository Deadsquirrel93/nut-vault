package com.example.passwordvault.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D63E1),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF58C89A),
    onSecondary = Color(0xFF092217),
    tertiary = Color(0xFF97A7CC),
    background = Color(0xFFF3F7FF),
    onBackground = Color(0xFF12213C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16233E),
    surfaceVariant = Color(0xFFE7EEFF),
    outline = Color(0xFF9BB0D9),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EA8FF),
    onPrimary = Color(0xFF082042),
    secondary = Color(0xFF78E2B7),
    onSecondary = Color(0xFF062217),
    tertiary = Color(0xFFB3C0E8),
    background = Color(0xFF0E1B34),
    onBackground = Color(0xFFF0F5FF),
    surface = Color(0xFF152544),
    onSurface = Color(0xFFF0F5FF),
    surfaceVariant = Color(0xFF203154),
    outline = Color(0xFF91A8D8),
    error = Color(0xFFF2B8B5),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(18),
    medium = RoundedCornerShape(24),
    large = RoundedCornerShape(30),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun PasswordVaultTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
