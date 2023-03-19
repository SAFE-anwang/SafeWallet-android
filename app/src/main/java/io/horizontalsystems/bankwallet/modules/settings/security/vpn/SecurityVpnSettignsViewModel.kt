package io.horizontalsystems.bankwallet.modules.settings.security.vpn

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R

class SecurityVpnSettingsViewModel(val sp: SharedPreferences, val onConnectCallback:(Boolean) -> Unit): ViewModel() {

    private fun vpnChecked(): Boolean {
        return sp.getBoolean("vpnOpen", true)
    }

    var vpnConnectionStatus by mutableStateOf(getStatus())
        private set

    var vpnCheckEnabled by mutableStateOf(vpnChecked())
        private set

    fun vpnConnect(connectState: Boolean) {
        sp.edit().putBoolean("vpnOpen", connectState).commit()
        onConnectCallback(connectState)
        vpnConnectionStatus = getStatus()
        vpnCheckEnabled = connectState
    }

    private fun getStatus() = if (sp.getBoolean("vpnOpen", true)) VpnStatus.Connected else VpnStatus.Closed
}


enum class VpnStatus(val value: Int) {
    Connected(R.string.TorPage_Connected),
    Closed(R.string.TorPage_ConnectionClosed),;

    val icon: Int?
        get() = when (this) {
            Connected -> R.drawable.ic_tor_connection_success_24
            Closed -> R.drawable.ic_tor_connection_24
        }
}