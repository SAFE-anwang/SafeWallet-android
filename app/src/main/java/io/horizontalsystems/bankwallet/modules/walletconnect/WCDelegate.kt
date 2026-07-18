package io.horizontalsystems.bankwallet.modules.walletconnect

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.dapp.core.DAppManager
import io.horizontalsystems.dapp.core.DAppServiceCallback
import io.horizontalsystems.dapp.core.HSDAppEvent
import io.horizontalsystems.dapp.core.HSDAppProposal
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.dapp.core.HSDAppSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object WCDelegate : DAppServiceCallback {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _walletEvents = MutableSharedFlow<HSDAppEvent>()
    val walletEvents: SharedFlow<HSDAppEvent> = _walletEvents.asSharedFlow()

    private val _pairingEvents = MutableSharedFlow<Unit>()
    val pairingEvents: SharedFlow<Unit> = _pairingEvents.asSharedFlow()

    private val _pendingRequestEvents = MutableSharedFlow<Unit>()
    val pendingRequestEvents: SharedFlow<Unit> = _pendingRequestEvents.asSharedFlow()

    private val _connectionAvailableEvent = MutableStateFlow<Boolean?>(null)
    val connectionAvailableEvent: StateFlow<Boolean?> = _connectionAvailableEvent.asStateFlow()

    var sessionProposalEvent: HSDAppProposal? = null
    var sessionRequestEvent: HSDAppRequest? = null

    /**
     * DApp bridge request that mimics a WC SessionRequest for in-app DApp browser.
     * Set by DAppBrowseFragment when a JS bridge method needs user confirmation.
     */
    data class DAppRequest(
        val id: Long,
        val method: String,
        val params: String,          // JSON array string, e.g. "[{\"from\":\"0x...\",...}]"
        val chainId: String,         // WC format, e.g. "eip155:1"
        val peerName: String,
        val peerUrl: String,
        val peerIcon: String,
        val blockchainType: io.horizontalsystems.marketkit.models.BlockchainType,
        val chainName: String?,
        val onRespond: (Long, String) -> Unit,
        val onReject: (Long) -> Unit,
    )

    var dappRequestEvent: DAppRequest? = null

    init {
        // TODO: Migrate to dapp-core initialization
        // CoreClient.setDelegate(this)
        // Web3Wallet.setWalletDelegate(this)
    }

    // region DAppServiceCallback

    override fun onConnectionStateChange(isAvailable: Boolean) {
        _connectionAvailableEvent.tryEmit(isAvailable)
        scope.launch { _walletEvents.emit(HSDAppEvent.ConnectionState(isAvailable)) }
    }

    override fun onError(throwable: Throwable) {
        scope.launch { _walletEvents.emit(HSDAppEvent.Error(throwable)) }
    }

    override fun onSessionDelete(topic: String) {
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionDelete(topic)) }
    }

    override fun onSessionProposal(proposal: HSDAppProposal) {
        sessionProposalEvent = proposal
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionProposal(proposal)) }
    }

    override fun onSessionRequest(request: HSDAppRequest) {
        sessionRequestEvent = null
        val newRequest = App.wcSessionManager.getNewSessionRequest() ?: return
        if (App.wcWalletRequestHandler.handle(newRequest)) return

        sessionRequestEvent = newRequest
        scope.launch {
            _walletEvents.emit(HSDAppEvent.SessionRequest(newRequest))
            _pendingRequestEvents.emit(Unit)
        }
    }

    override fun onSessionSettled(session: HSDAppSession) {
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionSettled(session)) }
    }

    override fun onSessionSettleError(errorMessage: String) {
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionSettleError(errorMessage)) }
    }

    override fun onSessionUpdate() {
        scope.launch { _walletEvents.emit(HSDAppEvent.SessionUpdate) }
    }

    override fun onPairingDelete(topic: String) {
        scope.launch {
            _walletEvents.emit(HSDAppEvent.PairingDelete(topic))
            _pairingEvents.emit(Unit)
        }
    }

    // endregion

    // region Actions (delegated to DAppManager)

    fun getPairings() = DAppManager.getPairings()

    fun getActiveSessions() = DAppManager.getActiveSessions()

    fun deletePairing(topic: String, onError: (Throwable) -> Unit = {}) {
        DAppManager.disconnectPairing(topic, onError)
        scope.launch { _pairingEvents.emit(Unit) }
    }

    fun deleteAllPairings(onError: (Throwable) -> Unit = {}) {
        DAppManager.disconnectAllPairings(onError)
        scope.launch { _pairingEvents.emit(Unit) }
    }

    fun deleteSession(topic: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        DAppManager.disconnectSession(
            topic = topic,
            onSuccess = {
                scope.launch { _walletEvents.emit(HSDAppEvent.SessionDelete(topic)) }
                onSuccess()
            },
            onError = onError
        )
    }

    fun respondPendingRequest(
        requestId: Long,
        topic: String,
        data: String,
        onSuccessResult: () -> Unit = {},
        onErrorResult: (Throwable) -> Unit = {},
    ) {
        DAppManager.respondRequest(
            topic = topic,
            requestId = requestId,
            result = data,
            onSuccess = {
                // Route through DApp bridge if this is an in-app DApp request
                dappRequestEvent?.let { dapp ->
                    if (dapp.id == requestId) {
                        dapp.onRespond(requestId, data)
                        dappRequestEvent = null
                        scope.launch {
                            _pendingRequestEvents.emit(Unit)
                        }
                    }
                }
                onSuccessResult()
                scope.launch {
                    sessionRequestEvent = null
                    _pendingRequestEvents.emit(Unit)
                }
            },
            onError = { error ->
                sessionRequestEvent = null
                onErrorResult(error)
                scope.launch { _walletEvents.emit(HSDAppEvent.Error(error)) }
            }
        )
    }

    fun rejectRequest(
        topic: String,
        requestId: Long,
        onSuccessResult: () -> Unit = {},
        onErrorResult: (Throwable) -> Unit = {},
    ) {
        DAppManager.rejectRequest(
            topic = topic,
            requestId = requestId,
            onSuccess = {
                // Route through DApp bridge if this is an in-app DApp request
                dappRequestEvent?.let { dapp ->
                    if (dapp.id == requestId) {
                        dapp.onReject(requestId)
                        dappRequestEvent = null
                        scope.launch {
                            _pendingRequestEvents.emit(Unit)
                        }
                    }
                }
                onSuccessResult()
                scope.launch {
                    sessionRequestEvent = null
                    _pendingRequestEvents.emit(Unit)
                }
            },
            onError = { error ->
                sessionRequestEvent = null
                onErrorResult(error)
                scope.launch { _walletEvents.emit(HSDAppEvent.Error(error)) }
            }
        )
    }

    // endregion
}
