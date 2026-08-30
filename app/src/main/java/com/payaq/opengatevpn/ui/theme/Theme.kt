package com.payaq.opengatevpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ── Obsidian Protocol — Theme ──────────────────────────────────────────────────
// Always-dark theme. No dynamic color, no light variant.

private val ObsidianColorScheme = darkColorScheme(
    primary              = Primary,
    onPrimary            = OnPrimary,
    primaryContainer     = PrimaryContainer,
    onPrimaryContainer   = OnSurface,
    secondary            = Secondary,
    onSecondary          = OnPrimary,
    secondaryContainer   = SecondaryContainer,
    onSecondaryContainer = OnSurface,
    tertiary             = Primary,
    onTertiary           = OnPrimary,
    background           = Void,
    onBackground         = OnSurface,
    surface              = Surface,
    onSurface            = OnSurface,
    surfaceVariant       = SurfaceContainerHigh,
    onSurfaceVariant     = OnSurfaceVariant,
    surfaceTint          = Primary,
    error                = ErrorColor,
    onError              = OnError,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    outline              = Outline,
    outlineVariant       = OutlineVariant,
    inverseSurface       = OnSurface,
    inverseOnSurface     = Surface,
    inversePrimary       = OnSurfaceVariant,
    surfaceBright        = SurfaceBright,
    surfaceContainer     = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow  = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceDim           = SurfaceDim
)

@Composable
fun OpenGateVPNTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = Typography,
        content = content
    )
}