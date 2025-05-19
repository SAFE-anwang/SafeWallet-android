package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.proposal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawModule
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class WithdrawAvailableViewModel(
    val address: Address,
    val service: WithdrawService,
    private val connectivityManager: ConnectivityManager
): ViewModelUiState<WithdrawModule.WithDrawNodeUiState>() {


    private val disposables = CompositeDisposable()
    private var nodeInfo: NodeInfo? = null
    private var withdrawList : List<WithdrawModule.WithDrawInfo>? = null

    private var showConfirmationDialog = false

    private var isWithdrawing = AtomicBoolean(false)
    var sendResult by mutableStateOf<SendResult?>(null)

    override fun createState(): WithdrawModule.WithDrawNodeUiState {
        return getUiState()
    }

    private fun getUiState(): WithdrawModule.WithDrawNodeUiState {
        return WithdrawModule.WithDrawNodeUiState(
            withdrawList,
            withdrawList?.filter { it.checked }?.isNotEmpty() ?: false,
            showConfirmationDialog
        )
    }

    init {
        service.itemsObservableAvailable
            .subscribeIO {
                withdrawList = it
                emitState()
            }.let {
                disposables.add(it)
            }
        start()
    }

    fun start() {
        viewModelScope.launch(Dispatchers.Default) {
               service.loadItemsAvailable(0)
        }
    }

    fun onBottomReached() {
        viewModelScope.launch(Dispatchers.IO) {
            service.loadNext()
        }
    }

    fun check(lockId: Int) {
        withdrawList?.forEach {
            if (it.id == lockId) {
                it.checked = !it.checked
            }
        }
        emitState()
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    fun closeDialog() {
        showConfirmationDialog = false
        emitState()
    }

    fun showConfirmation() {
        if (isWithdrawing.get())	return
        showConfirmationDialog = true
        emitState()
    }

    fun withdraw() {
        closeDialog()
        viewModelScope.launch(Dispatchers.IO) {
            withdrawList?.let { list ->
                val checkedList = list.filter { it.checked }.map { it.id }
                try {
                    service.withdraw(checkedList)
                    sendResult = SendResult.Sent
                    withdrawList = list.filter { !it.checked }
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

