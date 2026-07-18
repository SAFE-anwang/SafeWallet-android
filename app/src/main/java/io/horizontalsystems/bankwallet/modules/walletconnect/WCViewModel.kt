package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.dapp.core.HSDAppEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class WCViewModel : ViewModel() {

    val walletEvents = WCDelegate.walletEvents.map { wcEvent ->

        when (wcEvent) {
            is HSDAppEvent.ConnectionState -> { }

            is HSDAppEvent.SessionRequest -> {
                SignEvent.SessionRequest(wcEvent.request.requestId)
            }

            is HSDAppEvent.SessionDelete -> SignEvent.Disconnect

            is HSDAppEvent.SessionProposal -> {
                SignEvent.SessionProposal
            }

            else -> NoAction
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed())

}

sealed interface Web3WalletEvent
object NoAction : Web3WalletEvent

interface SignEvent: Web3WalletEvent {
    object SessionProposal: SignEvent
    data class SessionRequest(val requestId: Long) : SignEvent
    object Disconnect : SignEvent

    data class ConnectionState(val isAvailable: Boolean) : SignEvent
}
interface AuthEvent: Web3WalletEvent {
    data class OnRequest(val id: Long, val message: String) : AuthEvent
}