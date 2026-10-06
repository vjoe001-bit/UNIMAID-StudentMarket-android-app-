package com.example.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

private val LightColors = lightColorScheme(
    primary = UnimaidBluePrimary,
    onPrimary = LightSurface,
    primaryContainer = UnimaidBlueContainer,
    onPrimaryContainer = UnimaidBlueOnContainer,
    secondary = AccentCoral,
    onSecondary = LightSurface,
    secondaryContainer = AccentCoralContainer,
    onSecondaryContainer = AccentCoralOnContainer,
    tertiary = VerifiedGreen,
    onTertiary = LightSurface,
    tertiaryContainer = VerifiedGreenContainer,
    onTertiaryContainer = VerifiedGreenOnContainer,
    error = ErrorRed,
    onError = LightSurface,
    errorContainer = ErrorRedContainer,
    onErrorContainer = ErrorRed,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = PillInputLight,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

private val DarkColors = darkColorScheme(
    primary = UnimaidBlueLight,
    onPrimary = LightSurface,
    primaryContainer = DarkBlueContainer,
    onPrimaryContainer = DarkBlueOnContainer,
    secondary = AccentCoral,
    onSecondary = LightSurface,
    secondaryContainer = Color(0xFF4A1A1A),
    onSecondaryContainer = Color(0xFFFFCDD2),
    tertiary = VerifiedGreen,
    onTertiary = LightSurface,
    tertiaryContainer = VerifiedGreenOnContainer,
    onTertiaryContainer = VerifiedGreenContainer,
    error = ErrorRedDark,
    onError = DarkBackground,
    errorContainer = ErrorRedContainer,
    onErrorContainer = ErrorRedDark,
    background = Color(0xFF0E121E),
    onBackground = Color(0xFFF1F5F9),
    surface = CardSurfaceDark,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = PillInputDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2C324E),
    outlineVariant = Color(0xFF1D2237)
)

@Composable
fun UnimaidTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false, // Use our handcrafted UNIMAID identity palette
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
