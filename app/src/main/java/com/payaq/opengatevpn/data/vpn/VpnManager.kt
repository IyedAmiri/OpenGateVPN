package com.payaq.opengatevpn.data.vpn

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.ProfileManager.setTemporaryProfile
import de.blinkt.openvpn.core.VPNLaunchHelper.startOpenVpn
import java.io.StringReader

class VpnManager(private val context: Context) {

    private var activeConfig: String? = null

    /**
     * Launches the OpenVPN process for the given config.
     * @return true if the launch was initiated, false if config parsing/launch failed
     * (callers must surface that immediately instead of waiting on the watchdog).
     */
    fun startVpn(base64ConfigString: String): Boolean {
        return try {
            activeConfig = base64ConfigString

            val decodedBytes = Base64.decode(base64ConfigString, Base64.DEFAULT)
            var ovpnConfigText = String(decodedBytes, Charsets.UTF_8)

            // Inject fail-fast, clean dynamic socket port, and graceful exit directives
            if (!ovpnConfigText.contains("nobind")) {
                ovpnConfigText += "\nnobind\n"
            }
            if (!ovpnConfigText.contains("connect-retry-max")) {
                ovpnConfigText += "\nconnect-retry-max 2\nconnect-timeout 8\nresolv-retry 2\n"
            }
            if (!ovpnConfigText.contains("explicit-exit-notify")) {
                ovpnConfigText += "\nexplicit-exit-notify 2\n"
            }

            val parser = ConfigParser()
            parser.parseConfig(StringReader(ovpnConfigText))
            val profile = parser.convertProfile()

            profile.mUsername = "vpn"
            profile.mPassword = "vpn"
            profile.mProfileCreator = context.packageName

            setTemporaryProfile(context, profile)
            startOpenVpn(profile, context, "VPN Gate Connection", true)
            true

        } catch (e: Exception) {
            Log.e(TAG, "startVpn failed to parse/launch config", e)
            false
        }
    }

    /** @return true if a stop request was dispatched to the service. */
    fun stopVpn(): Boolean {
        return try {
            ProfileManager.setConntectedVpnProfileDisconnected(context)

            val disconnectIntent = Intent(context, OpenVPNService::class.java).apply {
                action = OpenVPNService.DISCONNECT_VPN
            }
            context.startService(disconnectIntent)

            activeConfig = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "stopVpn failed", e)
            false
        }
    }

    fun clearActiveConfig() {
        activeConfig = null
    }

    /** Tears down the tunnel but remembers its config so resume can restart it. */
    fun pauseVpn(): Boolean {
        val lastRunning = activeConfig
        val stopped = stopVpn()
        // Keep memory of activeConfig so resume knows which one to restart
        activeConfig = lastRunning
        return stopped
    }

    /**
     * Restarts the paused tunnel from its stored config.
     * @return false when there is no stored session to resume — the caller must fail
     * fast ("session expired") instead of showing RECONNECTING until the watchdog.
     */
    fun resumeVpn(fallbackConfig: String? = null): Boolean {
        val configToResume = activeConfig ?: fallbackConfig
        return if (configToResume != null) {
            startVpn(configToResume)
        } else {
            Log.w(TAG, "resumeVpn called with no stored or fallback config")
            false
        }
    }

    companion object {
        private const val TAG = "VpnManager"
    }
}
