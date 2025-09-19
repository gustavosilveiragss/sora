package com.sora.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SoraBlue,
    onPrimary = SoraWhite,
    primaryContainer = SoraGrayLight,
    secondary = SoraRed,
    onSecondary = SoraWhite,
    background = SoraBackground,
    onBackground = SoraTextPrimary,
    surface = SoraSurface,
    onSurface = SoraTextPrimary,
    surfaceVariant = SoraGrayLight,
    onSurfaceVariant = SoraTextSecondary,
    error = SoraRed,
    onError = SoraWhite,
    outline = SoraTextTertiary,
    outlineVariant = SoraGrayLight
)

@Composable
fun SoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}