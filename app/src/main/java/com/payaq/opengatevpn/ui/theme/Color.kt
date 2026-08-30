package com.payaq.opengatevpn.ui.theme

import androidx.compose.ui.graphics.Color

// ── Obsidian Protocol — Color Palette ──────────────────────────────────────────
// Derived from the Stitch "OpenGate VPN Mobile Interface" design system.

// Canvas & Surfaces
val Void            = Color(0xFF000000)  // Pitch-black background
val Surface         = Color(0xFF131313)  // Primary surface
val SurfaceDim      = Color(0xFF131313)
val SurfaceContainer      = Color(0xFF1F1F1F)
val SurfaceContainerLow   = Color(0xFF1B1B1B)
val SurfaceContainerHigh  = Color(0xFF2A2A2A)
val SurfaceContainerHighest = Color(0xFF353535)
val SurfaceContainerLowest  = Color(0xFF0E0E0E)
val SurfaceBright   = Color(0xFF393939)
val CardBackground  = Color(0xFF0A0A0A)  // Card / elevated surface bg

// Borders
val BorderHairline  = Color(0xFF222222)  // 1px micro-border
val OutlineVariant  = Color(0xFF444748)
val Outline         = Color(0xFF8E9192)

// Text & Content
val OnSurface       = Color(0xFFE2E2E2)  // Primary text on dark surfaces
val OnSurfaceVariant = Color(0xFFC4C7C8)
val TextMuted       = Color(0xFF888888)  // Secondary / muted text

// Primary — Pure White Accent
val Primary         = Color(0xFFFFFFFF)
val OnPrimary       = Color(0xFF2F3131)
val PrimaryContainer = Color(0xFFE2E2E2)

// Secondary
val Secondary       = Color(0xFFC9C6C5)
val SecondaryContainer = Color(0xFF4A4949)

// Functional Status
val StatusSuccess   = Color(0xFF00FF66)  // Connected / Active
val StatusError     = Color(0xFFFF3333)  // Error / Disconnect
val StatusPaused    = Color(0xFFFFBF00)  // Amber — Paused state

// Error (Material3 slots)
val ErrorColor      = Color(0xFFFFB4AB)
val OnError         = Color(0xFF690005)
val ErrorContainer  = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)