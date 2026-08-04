package com.example.gittracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimerColors.AccentFgDark,
    onPrimary = PrimerColors.CanvasDefaultDark,
    primaryContainer = PrimerColors.BorderDefaultDark,
    onPrimaryContainer = PrimerColors.FgDefaultDark,
    secondary = PrimerColors.FgMutedDark,
    onSecondary = PrimerColors.CanvasDefaultDark,
    tertiary = PrimerColors.SuccessFgDark,
    background = PrimerColors.CanvasDefaultDark,
    onBackground = PrimerColors.FgDefaultDark,
    surface = PrimerColors.CanvasMutedDark,
    onSurface = PrimerColors.FgDefaultDark,
    surfaceVariant = PrimerColors.CanvasMutedDark,
    onSurfaceVariant = PrimerColors.FgMutedDark,
    outline = PrimerColors.BorderDefaultDark,
    error = PrimerColors.DangerFgDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimerColors.AccentFgLight,
    onPrimary = PrimerColors.CanvasDefaultLight,
    primaryContainer = PrimerColors.BorderDefaultLight,
    onPrimaryContainer = PrimerColors.FgDefaultLight,
    secondary = PrimerColors.FgMutedLight,
    onSecondary = PrimerColors.CanvasDefaultLight,
    tertiary = PrimerColors.SuccessFgLight,
    background = PrimerColors.CanvasDefaultLight,
    onBackground = PrimerColors.FgDefaultLight,
    surface = PrimerColors.CanvasMutedLight,
    onSurface = PrimerColors.FgDefaultLight,
    surfaceVariant = PrimerColors.CanvasMutedLight,
    onSurfaceVariant = PrimerColors.FgMutedLight,
    outline = PrimerColors.BorderDefaultLight,
    error = PrimerColors.DangerFgLight
)

@Composable
fun GitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
