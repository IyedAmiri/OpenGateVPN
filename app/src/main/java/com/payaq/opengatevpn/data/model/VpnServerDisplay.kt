package com.payaq.opengatevpn.data.model

import androidx.compose.runtime.Immutable
import com.payaq.opengatevpn.ui.util.countryCodeToFlag
import com.payaq.opengatevpn.ui.util.formatScore
import com.payaq.opengatevpn.ui.util.formatSpeed

/**
 * Immutable display model for the server list. Pre-computes formatting so
 * LazyColumn items stay cheap to render.
 */
@Immutable
data class VpnServerDisplay(
    val hostName: String,
    val ip: String,
    val score: Long,
    val ping: Int,
    val speed: Long,
    val countryLong: String,
    val countryShort: String,
    val numVpnSessions: Long,
    val flagEmoji: String = countryCodeToFlag(countryShort),
    val formattedSpeed: String = formatSpeed(speed),
    val formattedScore: String = formatScore(score),
    val pingText: String = "${ping}ms",
    val qualityScore: Float,
    val overallRankScore: Double,
    /** 0..1 likelihood this is a home connection rather than a datacenter. */
    val residentialScore: Float,
    val isResidential: Boolean
)

// Quality model: the feed's numbers are self-reported and sometimes wrong, so
// points grow logarithmically per signal, implausible values are damped, and
// uptime/traffic (which a dead server can't fake retroactively) count as proof.
private const val PING_MAX_POINTS = 30.0
private const val SPEED_MAX_POINTS = 30.0
private const val SESSIONS_MAX_POINTS = 20.0
private const val TRACK_RECORD_MAX_POINTS = 20.0

/** Saturating curve: quick growth, then diminishing returns past [halfPoint]. */
private fun sat(value: Double, halfPoint: Double): Double = value / (value + halfPoint)

/** Latency points (0..30). Reports under 15 ms are treated as unreliable. */
private fun pingPoints(ping: Int): Double {
    if (ping < 15) return (ping * 0.5).coerceAtMost(8.0)
    val plausibility = ((450 - ping.coerceIn(15, 450)).toDouble() / 435.0)
    return PING_MAX_POINTS * Math.pow(plausibility, 0.7)
}

/** Throughput points (0..30), saturated at ~half a gigabit. */
private fun speedPoints(speed: Long, totalTraffic: Long): Double {
    val mbps = speed / 1_000_000.0
    var pts = SPEED_MAX_POINTS * sat(mbps, 60.0)
    if (mbps > 400 && totalTraffic <= 0) pts *= 0.6 // likely mismeasured
    return pts
}

/** Live-load points (0..20): active but not overloaded. Zero sessions = closed. */
private fun sessionPoints(sessions: Long): Double {
    if (sessions <= 0) return 0.0
    val healthy = SESSIONS_MAX_POINTS * sat(sessions.toDouble(), 20.0)
    val overload = sat((sessions - 60).coerceAtLeast(0).toDouble(), 80.0) * 0.5
    return healthy * (1.0 - overload)
}

/** Track-record points (0..20) from uptime and total served traffic. */
private fun trackRecordPoints(uptimeMs: Long, totalUsers: Long, totalTrafficBytes: Long): Double {
    val uptimeHours = uptimeMs / 3_600_000.0
    val uptimePts = 10.0 * sat(uptimeHours, 24.0)   // 24h → 5.0, 1wk → 8.75
    val trafficGb = totalTrafficBytes / 1_073_741_824.0
    var servedPts = 10.0 * sat(trafficGb, 100.0)    // 100 GB → 5.0, 1 TB → 9.1
    if (totalUsers > 0 && totalTrafficBytes <= 0) servedPts *= 0.3 // inconsistent feed row
    return uptimePts + servedPts
}

/** Quality on a 0..100 scale; [calculateOverallRank] uses the raw points. */
private fun computeScores(
    ping: Int,
    speed: Long,
    numVpnSessions: Long,
    uptime: Long,
    totalUsers: Long,
    totalTraffic: Long
): Pair<Float, Double> {
    var points =
        pingPoints(ping) +
                speedPoints(speed, totalTraffic) +
                sessionPoints(numVpnSessions) +
                trackRecordPoints(uptime, totalUsers, totalTraffic)

    // Red-flag multipliers
    if (numVpnSessions <= 0) points *= 0.45          // not accepting users
    if (ping in 1 until 15) points *= 0.8            // implausible latency
    if (uptime in 1 until 3_600_000L) points *= 0.85 // under an hour old
    if (speed > 800_000_000L) points *= 0.9          // rare for a volunteer host

    points = points.coerceIn(2.0, 97.0)
    return points.toFloat() / 100f to points
}

private fun calculateQualityScore(
    ping: Int, speed: Long, numVpnSessions: Long, uptime: Long, totalUsers: Long, totalTraffic: Long
): Float = computeScores(ping, speed, numVpnSessions, uptime, totalUsers, totalTraffic).first

private fun calculateOverallRank(
    ping: Int, speed: Long, numVpnSessions: Long, uptime: Long, totalUsers: Long, totalTraffic: Long
): Double = computeScores(ping, speed, numVpnSessions, uptime, totalUsers, totalTraffic).second

// Residential likelihood: home connections handle bot-checks better than
// datacenter IPs. Inferred from rDNS naming tokens and host scale (speed,
// concurrent sessions, total traffic) — no network probes.
private val RESIDENTIAL_RDNS_TOKENS = listOf(
    "dynamic", "dyn-ip", "pppoe", "adsl", "vdsl", "dsl-", ".dsl", "fios", "cable",
    "cpe", "pool", "home", "client-", "modem", "fiber", "fibre", "broadband",
    "res-", ".res.", "wireless", "wimax", "mobile", "gprs", "hsd", "wan-"
)

private val DATACENTER_TOKENS = listOf(
    "vps", "dedicated", "hosting", "hosted", "datacenter", "data-center", "colocation",
    "cloud", "amazonaws", "ec2", "compute", "googleusercontent", "azure", "ovh",
    "hetzner", "digitalocean", "linode", "vultr", "contabo", "scaleway", "leaseweb",
    "alibaba", "tencent", "oraclecloud", "choopa", "m247", "datacamp", "server",
    "rack", "node", "vds", "iaas", "paas", "cdn"
)

/** 0..1 likelihood of a home/residential connection. */
private fun calculateResidentialScore(
    hostName: String,
    ip: String,
    operator: String,
    speedBps: Long,
    numVpnSessions: Long,
    totalTrafficBytes: Long
): Float {
    val host = hostName.lowercase()
    val opAndMsg = "$operator $hostName".lowercase()

    var points = 0.0

    if (RESIDENTIAL_RDNS_TOKENS.any { host.contains(it) }) points += 0.35
    if (host.isBlank() || host == ip || !host.contains('.')) points += 0.15
    if (DATACENTER_TOKENS.any { host.contains(it) || opAndMsg.contains(" $it") }) points -= 0.50

    val mbps = speedBps / 1_000_000.0
    points += when {
        mbps in 1.0..100.0 -> 0.20
        mbps <= 250.0 -> 0.05
        else -> -0.15
    }
    points += when {
        numVpnSessions in 1..8 -> 0.15
        numVpnSessions <= 25 -> 0.05
        numVpnSessions > 60 -> -0.15
        else -> 0.0
    }
    val trafficGb = totalTrafficBytes / 1_073_741_824.0
    if (trafficGb < 500) points += 0.10
    if (trafficGb > 5_000) points -= 0.15

    return points.coerceIn(0.0, 1.0).toFloat()
}

private const val RESIDENTIAL_THRESHOLD = 0.55f

/** Convert a full [VpnServer] to its lightweight display counterpart. */
fun VpnServer.toDisplay(): VpnServerDisplay {
    val resScore = calculateResidentialScore(hostName, ip, operator, speed, numVpnSessions, totalTraffic)
    return VpnServerDisplay(
        hostName = hostName,
        ip = ip,
        score = score,
        ping = ping,
        speed = speed,
        countryLong = countryLong,
        countryShort = countryShort,
        numVpnSessions = numVpnSessions,
        qualityScore = calculateQualityScore(ping, speed, numVpnSessions, uptime, totalUsers, totalTraffic),
        overallRankScore = calculateOverallRank(ping, speed, numVpnSessions, uptime, totalUsers, totalTraffic),
        residentialScore = resScore,
        isResidential = resScore >= RESIDENTIAL_THRESHOLD
    )
}
