package com.payaq.opengatevpn.data.model

data class VpnServer(
    val hostName: String,
    val ip: String,
    val score: Long,
    val ping: Int,
    val speed: Long,
    val countryLong: String,
    val countryShort: String,
    val numVpnSessions: Long,
    val uptime: Long,
    val totalUsers: Long,
    val totalTraffic: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val openVpnConfigDataBase64: String
)