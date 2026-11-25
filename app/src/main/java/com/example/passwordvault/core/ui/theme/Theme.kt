package com.example.passwordvault.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passwordvault.core.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A5FD4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF0A2458),
    secondary = Color(0xFF188C68),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFF4E5),
    onSecondaryContainer = Color(0xFF053827),
    tertiary = Color(0xFFB87E14),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFEBC2),
    onTertiaryContainer = Color(0xFF3A2500),
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF5F7FC),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE6ECF7),
    onSurfaceVariant = Color(0xFF4B5568),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEDF1F9),
    surfaceContainerHigh = Color(0xFFE3E9F5),
    surfaceContainerHighest = Color(0xFFD9E1F0),
    outline = Color(0xFF8C9BB5),
    outlineVariant = Color(0xFFD6DFEE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF72AAFF),
    onPrimary = Color(0xFF062354),
    primaryContainer = Color(0xFF163874),
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFF5AD8AD),
    onSecondary = Color(0xFF003827),
    secondaryContainer = Color(0xFF0C513B),
    onSecondaryContainer = Color(0xFFC5F7E3),
    tertiary = Color(0xFFF2BE54),
    onTertiary = Color(0xFF3F2A00),
    tertiaryContainer = Color(0xFF5A3F08),
    onTertiaryContainer = Color(0xFFFFEBC2),
    background = Color(0xFF091121),
    onBackground = Color(0xFFEBF0FA),
    surface = Color(0xFF091121),
    onSurface = Color(0xFFEBF0FA),
    surfaceVariant = Color(0xFF1A253D),
    onSurfaceVariant = Color(0xFFA0AEC6),
    surfaceContainerLowest = Color(0xFF060C18),
    surfaceContainerLow = Color(0xFF101A2F),
    surfaceContainer = Color(0xFF142038),
    surfaceContainerHigh = Color(0xFF1B2945),
    surfaceContainerHighest = Color(0xFF233354),
    outline = Color(0xFF5D6F91),
    outlineVariant = Color(0xFF24334F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

@Composable
fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun PasswordVaultTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = shouldUseDarkTheme(themeMode)
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
