package com.payaq.opengatevpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import com.payaq.opengatevpn.ui.main.MainScreen
import com.payaq.opengatevpn.ui.main.VpnViewModel
import com.payaq.opengatevpn.ui.theme.OpenGateVPNTheme
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        StartActivityForResult(),
        ::handleVpnPermissionResult
    )

    /**
     * Single field-based listener: registered in [onCreate], removed in [onDestroy].
     * The previous anonymous-listener-per-onCreate leaked into VpnStatus's static
     * registry and stacked duplicates across Activity recreations.
     */
    private val vpnStateListener = object : VpnStatus.StateListener {
        override fun updateState(
            state: String?,
            logmessage: String?,
            localizedResId: Int,
            level: ConnectionStatus?,
            intent: Intent?
        ) {
            runOnUiThread {
                viewModel.updateVpnStatus(state, logmessage, level)
            }
        }

        override fun setConnectedVPN(uuid: String?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Listen for live OpenVPN status updates and forward them to the ViewModel
        VpnStatus.addStateListener(vpnStateListener)

        setContent {
            OpenGateVPNTheme {
                MainScreen(
                    viewModel = viewModel,
                    onConnectClicked = ::checkPermissionAndConnect
                )
            }
        }
    }

    override fun onDestroy() {
        VpnStatus.removeStateListener(vpnStateListener)
        super.onDestroy()
    }

    private fun handleVpnPermissionResult(result: ActivityResult) {
        val config = viewModel.pendingConfig
        viewModel.pendingConfig = null

        if (result.resultCode == RESULT_OK && config != null) {
            viewModel.onConnectRequested()
            if (!viewModel.vpnManager.startVpn(config)) {
                // Config died between request and grant — fail visibly, not via watchdog.
                viewModel.onLaunchFailed()
            }
        } else if (result.resultCode != RESULT_OK) {
            // User denied / dismissed the VPN permission dialog.
            viewModel.onPermissionDenied()
        } else {
            // Granted but no pending config (e.g., selection changed mid-dialog).
            viewModel.onPermissionDenied()
        }
    }

    private fun checkPermissionAndConnect(configBase64: String) {
        viewModel.pendingConfig = configBase64
        val intent = VpnService.prepare(this)

        if (intent != null) {
            // Needs user permission: Show system dialog
            vpnPermissionLauncher.launch(intent)
        } else {
            // Already granted: Connect directly
            if (!viewModel.vpnManager.startVpn(configBase64)) {
                viewModel.onLaunchFailed()
            }
        }
    }
}
