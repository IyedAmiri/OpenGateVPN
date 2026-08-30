package com.payaq.opengatevpn.ui.util

/**
 * Converts an ISO 3166-1 alpha-2 country code (e.g. "US", "JP") to its
 * Unicode flag emoji (e.g. 🇺🇸, 🇯🇵) using regional indicator symbol pairs.
 *
 * Returns a generic globe emoji 🌐 if the code is invalid or blank.
 */
fun countryCodeToFlag(countryCode: String): String {
    val code = countryCode.uppercase().trim()
    if (code.length != 2) return "🌐"

    val firstChar = Character.toChars(0x1F1E6 - 'A'.code + code[0].code)
    val secondChar = Character.toChars(0x1F1E6 - 'A'.code + code[1].code)
    return String(firstChar) + String(secondChar)
}

/**
 * Formats a speed value from bytes/sec to a human-readable Mbps string.
 * Example: 85_100_000 → "85.1 Mbps"
 */
fun formatSpeed(bytesPerSec: Long): String {
    val mbps = bytesPerSec / 1_000_000.0
    return if (mbps >= 100) {
        "${mbps.toInt()} Mbps"
    } else {
        String.format("%.1f Mbps", mbps)
    }
}

/**
 * Formats elapsed seconds into HH:MM:SS format.
 * Example: 3661 → "01:01:01"
 */
fun formatElapsedTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

/**
 * Formats a server score into a human-readable string.
 * Example: 1_250_000 → "1.3M", 45_200 → "45K"
 */
fun formatScore(score: Long): String {
    return when {
        score >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", score / 1_000_000.0)
        score >= 1_000 -> String.format(java.util.Locale.US, "%.0fK", score / 1_000.0)
        else -> score.toString()
    }
}

