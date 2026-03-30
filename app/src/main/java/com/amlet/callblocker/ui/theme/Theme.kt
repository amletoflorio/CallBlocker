package com.amlet.callblocker.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = Emerald500,
    onPrimary        = Slate950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald500,

    background       = Slate950,
    onBackground     = Slate100,

    surface          = Slate900,
    onSurface        = Slate100,
    surfaceVariant   = Slate800,
    onSurfaceVariant = Slate400,

    outline          = Slate700,
    error            = Red500,
    onError          = Color.White,
    errorContainer   = Red900
)

@Composable
fun CallBlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}