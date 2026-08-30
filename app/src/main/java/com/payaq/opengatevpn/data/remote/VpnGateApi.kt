package com.payaq.opengatevpn.data.remote

import android.util.Log
import com.payaq.opengatevpn.data.model.VpnServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.BufferedReader
import java.io.StringReader

/**
 * Result of a VPN Gate directory fetch. Distinguishes the three outcomes the UI
 * must present honestly: usable data, a reachable-but-empty feed, and failure.
 */
sealed interface VpnGateResult {
    data class Success(
        val servers: List<VpnServer>,
        val duplicatesRemoved: Int = 0
    ) : VpnGateResult

    /** The feed answered but contained zero usable rows (not an error). */
    data object EmptyFeed : VpnGateResult

    /** Network/timeout/parse abort — the list on screen is stale, not complete. */
    data class NetworkError(val cause: Throwable?) : VpnGateResult
}

class VpnGateApi {
    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
            requestTimeoutMillis = 20_000
        }
    }

    /** Fetches the raw CSV. Throws on network failure so callers can tell errors from empty feeds. */
    suspend fun fetchVpnGate(): String {
        val response = client.get("https://www.vpngate.net/api/iphone/")
        val rawData = response.bodyAsText()
        Log.d("VPN_Gate_REQUEST", "Fetched successfully")
        return rawData
    }

    suspend fun parseVpnGate(): VpnGateResult {
        val rawCsv = try {
            fetchVpnGate()
        } catch (e: Exception) {
            Log.e("VPN_Gate_REQUEST", "Request failed", e)
            return VpnGateResult.NetworkError(e)
        }

        if (rawCsv.isBlank()) return VpnGateResult.EmptyFeed

        val serverList = mutableListOf<VpnServer>()
        try {
            val reader = BufferedReader(StringReader(rawCsv))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line!!.trim()
                if (currentLine.startsWith("*") || currentLine.startsWith("#")) { continue }
                val tokens = currentLine.split(",")
                if (tokens.size >= 15) {
                    val rawConfig = tokens.last().trim()
                    // Strict OpenVPN filter: Must have a valid Base64 config
                    if (rawConfig.length < 150) continue

                    val ip = tokens[1].trim()
                    if (ip.isBlank()) continue

                    val ping = tokens[3].toIntOrNull() ?: -1
                    if (ping <= 0 || ping > 600) continue

                    val speed = tokens[4].toLongOrNull() ?: 0L
                    if (speed <= 0) continue

                    val server = VpnServer(
                        hostName = tokens[0],
                        ip = ip,
                        score = tokens[2].toLongOrNull() ?: 0L,
                        ping = ping,
                        speed = speed,
                        countryLong = tokens[5],
                        countryShort = tokens[6],
                        numVpnSessions = tokens[7].toLongOrNull() ?: 0L,
                        uptime = tokens[8].toLongOrNull() ?: 0L,
                        totalUsers = tokens[9].toLongOrNull() ?: 0L,
                        totalTraffic = tokens[10].toLongOrNull() ?: 0L,
                        logType = tokens[11],
                        operator = tokens[12],
                        message = tokens.subList(13, tokens.size - 1).joinToString(","),
                        openVpnConfigDataBase64 = rawConfig
                    )
                    serverList.add(server)
                }
            }
        } catch (e: Exception) {
            // A mid-stream parse abort means the data itself is suspect — never
            // return a truncated list dressed up as a complete one.
            Log.e("VPN_Gate_Parse", "Error parsing api rawData", e)
            return VpnGateResult.NetworkError(e)
        }

        if (serverList.isEmpty()) return VpnGateResult.EmptyFeed

        // The feed repeats hosts (multi-protocol/mirror rows). Duplicate IPs would crash
        // LazyColumn keyed by IP — keep only the highest-scored row per address.
        val deduped = serverList
            .groupBy { it.ip }
            .map { (_, rows) -> rows.maxByOrNull { it.score }!! }
        return VpnGateResult.Success(
            servers = deduped,
            duplicatesRemoved = serverList.size - deduped.size
        )
    }
}
