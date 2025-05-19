package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class WithdrawNodeViewModel(
    val evmKit: EthereumKit,
    val isSuperNode: Boolean,
    val service: WithdrawService,
    private val connectivityManager: ConnectivityManager
): ViewModelUiState<WithdrawModule.WithDrawNodeUiState>() {


    private val disposables = CompositeDisposable()
    private var nodeInfo: NodeInfo? = null
    private val withdrawList = mutableListOf<WithdrawModule.WithDrawInfo>()

    private var showConfirmationDialog = false

    private var isWithdrawing = AtomicBoolean(false)
    var sendResult by mutableStateOf<SendResult?>(null)

    override fun createState(): WithdrawModule.WithDrawNodeUiState {
        return getUiState()
    }

    private fun getUiState(): WithdrawModule.WithDrawNodeUiState {
        if (nodeInfo == null) {
            return WithdrawModule.WithDrawNodeUiState(null,  false, showConfirmationDialog)
        } else {
            val withDrawInfo = nodeInfo!!.founders.filter { it.addr.hex == evmKit.receiveAddress.hex }
                .map {
                    WithdrawModule.WithDrawInfo(
                        it.lockID,
                        it.height,
                        NodeCovertFactory.formatSafe(it.amount),
                        it.addr.hex,
                        it.height > (evmKit.lastBlockHeight ?: 0L)
                    )
                }
            withdrawList.clear()
            withdrawList.addAll(withDrawInfo)
            return WithdrawModule.WithDrawNodeUiState(
                withdrawList,
                withdrawList.filter { it.checked }.isNotEmpty(),
                showConfirmationDialog
            )
        }
    }

    init {
        service.itemsObservable
            .subscribeIO {
                nodeInfo = it
                emitState()
            }.let {
                disposables.add(it)
            }
        start()
    }

    fun start() {
        viewModelScope.launch(Dispatchers.Default) {
               service.getNodeInfo(isSuperNode)
        }
    }

    fun check(lockId: Int) {
        withdrawList.forEach {
            if (it.id == lockId) {
                it.checked = !it.checked
            }
        }
        emitState()
    }

    fun closeDialog() {
        showConfirmationDialog = false
        emitState()
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    fun showConfirmation() {
        if (isWithdrawing.get())	return
        showConfirmationDialog = true
        emitState()
    }

    fun withdraw() {
        closeDialog()
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            withdrawList?.let { list ->
                val checkedList = list.filter { it.checked }.map { it.id }
                try {
                    service.withdraw(checkedList)
                    sendResult = SendResult.Sent
                    withdrawList.clear()
                    emitState()
                } catch (e: Exception) {
                    sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

