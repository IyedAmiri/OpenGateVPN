package com.payaq.opengatevpn.data.repository

import com.payaq.opengatevpn.data.remote.VpnGateApi
import com.payaq.opengatevpn.data.remote.VpnGateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VpnRepository(private val api: VpnGateApi = VpnGateApi()) {

    /** Fetches and parses the directory off the main thread (network + CSV parsing are heavy). */
    suspend fun getServers(): VpnGateResult = withContext(Dispatchers.Default) {
        api.parseVpnGate()
    }
}
