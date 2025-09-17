package com.sora.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SoraBlack,
    onPrimary = SoraWhite,
    primaryContainer = SoraGray,
    secondary = SoraRed,
    onSecondary = SoraWhite,
    background = SoraBlack,
    onBackground = SoraWhite,
    surface = SoraBlack,
    onSurface = SoraWhite,
    surfaceVariant = SoraGrayDark,
    onSurfaceVariant = SoraGrayLight,
    error = SoraRed,
    onError = SoraWhite,
    outline = SoraGrayMedium,
    outlineVariant = SoraGrayDark
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