package com.bruce.controller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BruceColors.Primary,
    secondary = BruceColors.Secondary,
    tertiary = BruceColors.Accent,
    background = BruceColors.Background,
    surface = BruceColors.Surface,
    surfaceVariant = BruceColors.SurfaceVariant,
    onPrimary = BruceColors.OnBackground,
    onSecondary = BruceColors.OnBackground,
    onTertiary = BruceColors.OnBackground,
    onBackground = BruceColors.OnBackground,
    onSurface = BruceColors.OnSurface,
    onSurfaceVariant = BruceColors.SnowStorm2,
    error = BruceColors.Error,
)

@Composable
fun BruceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
