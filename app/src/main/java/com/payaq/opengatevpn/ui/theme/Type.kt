package com.payaq.opengatevpn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Obsidian Protocol — Typography ─────────────────────────────────────────────
// Geist Sans → FontFamily.SansSerif (closest system match on Android)
// Geist Mono → FontFamily.Monospace

val GeistSans = FontFamily.SansSerif
val GeistMono = FontFamily.Monospace

/** Monospaced technical-data style for telemetry readouts (IP, ping, speed, timer). */
val TechnicalData = TextStyle(
    fontFamily = GeistMono,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = (-0.01).sp
)

/** Uppercase label-caps style for status badges and section labels. */
val LabelCaps = TextStyle(
    fontFamily = GeistMono,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 13.sp,
    letterSpacing = 0.8.sp
)

// Set of Material typography styles mapped to Stitch design tokens
val Typography = Typography(
    // display-lg: 48sp/600 — heroic display text
    displayLarge = TextStyle(
        fontFamily = GeistSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 53.sp,
        letterSpacing = (-1.92).sp  // -0.04em
    ),

    // headline-lg-mobile: 32sp/600
    headlineSmall = TextStyle(
        fontFamily = GeistSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),

    // headline-md: 24sp/500
    headlineMedium = TextStyle(
        fontFamily = GeistSans,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.48).sp  // -0.02em
    ),

    // body-base: 16sp/400
    bodyLarge = TextStyle(
        fontFamily = GeistSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),

    // body-sm: 14sp/400
    bodyMedium = TextStyle(
        fontFamily = GeistSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),

    // label-caps: 11sp/500
    labelSmall = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.8.sp  // 0.08em equivalent
    ),

    // Additional useful slots
    titleMedium = TextStyle(
        fontFamily = GeistSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    titleSmall = TextStyle(
        fontFamily = GeistSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
)