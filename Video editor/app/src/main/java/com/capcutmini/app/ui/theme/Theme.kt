package com.capcutmini.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CapCutMiniDarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    primaryContainer = MintDim,
    onPrimaryContainer = Fog,
    secondary = FogMuted,
    onSecondary = Ink,
    background = Ink,
    onBackground = Fog,
    surface = InkElevated,
    onSurface = Fog,
    surfaceVariant = InkSurface,
    onSurfaceVariant = FogMuted,
    error = Danger,
    onError = Fog
)

@Composable
fun CapCutMiniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CapCutMiniDarkColors,
        typography = CapCutMiniTypography,
        content = content
    )
}
