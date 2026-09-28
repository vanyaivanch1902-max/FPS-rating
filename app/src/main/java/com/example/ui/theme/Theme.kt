package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = NeonGreenPrimary,
    onPrimary = NeonGreenOnPrimary,
    primaryContainer = NeonGreenContainer,
    onPrimaryContainer = NeonGreenOnContainer,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    secondaryContainer = CyberCyanContainer,
    onSecondaryContainer = CyberCyan,
    tertiary = CyberAmber,
    onTertiary = Color.Black,
    background = CyberBackground,
    onBackground = TextPrimaryDark,
    surface = CyberSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = CyberRed,
    onError = Color.White,
    outline = BorderGlow
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dedicated gaming dark palette with high-contrast neon accents
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
