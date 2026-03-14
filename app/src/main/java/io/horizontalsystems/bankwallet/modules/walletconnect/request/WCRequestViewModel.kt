package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import com.google.gson.JsonParser
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCUtils
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class WCRequestViewModel(
    private val sessionRequest: Wallet.Model.SessionRequest,
    private val wcAction: AbstractWCAction,
    private val accountManager: IAccountManager
) : ViewModelUiState<WCRequestUiState>() {

    private var approveInProgress = false
    private var finish: Boolean = false
    private var error: Throwable? = null

    private var actionState = wcAction.stateFlow.value

    init {
        viewModelScope.launch {
            wcAction.stateFlow.collect {
                actionState = it

                emitState()
            }
        }

        wcAction.start(viewModelScope)
    }

    override fun createState() = WCRequestUiState(
        walletName = accountManager.activeAccount?.name,
        title = wcAction.getTitle(),
        finish = finish,
        runnable = !approveInProgress && actionState.runnable,
        approveButtonTitle = wcAction.getApproveButtonTitle(),
        contentItems = actionState.items
    )

    fun approve() = viewModelScope.launch(Dispatchers.Default) {
        error = null
        approveInProgress = true
        emitState()

        val actionResult = wcAction.performAction()

        WCDelegate.respondPendingRequest(
            sessionRequest.request.id,
            sessionRequest.topic,
            actionResult,
            onSuccessResult = {
                approveInProgress = false
                finish = true
                emitState()
            },
            onErrorResult = {
                approveInProgress = true
                error = it
                emitState()
            }
        )
    }

    fun reject() = viewModelScope.launch(Dispatchers.Default) {
        error = null

        WCDelegate.rejectRequest(
            sessionRequest.topic,
            sessionRequest.request.id,
            onSuccessResult = {
                finish = true
                emitState()
            },
            onErrorResult = {
                error = it
                emitState()
            }
        )

        emitState()
    }

    class Factory(
        private val sessionRequest: Wallet.Model.SessionRequest,
        private val wcAction: AbstractWCAction,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestViewModel(sessionRequest, wcAction, App.accountManager) as T
        }
    }
}

data class WCRequestUiState(
    val walletName: String?,
    val title: TranslatableString,
    val finish: Boolean,
    val runnable: Boolean,
    val approveButtonTitle: TranslatableString,
    val contentItems: List<SectionViewItem>
)
