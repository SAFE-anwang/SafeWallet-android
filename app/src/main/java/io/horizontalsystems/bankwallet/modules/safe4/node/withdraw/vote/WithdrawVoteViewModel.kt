package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.vote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfoRepository
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordManager
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourLockedVoteService
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawModule
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class WithdrawVoteViewModel(
    val evmKit: EthereumKit,
    val service: WithdrawService,
    private val repository: LockRecordInfoRepository,
    val lockedVoteService: SafeFourLockedVoteService,
    private val connectivityManager: ConnectivityManager
): ViewModelUiState<WithdrawModule.WithDrawNodeUiState>() {


    private val disposables = CompositeDisposable()
    private var nodeInfo: NodeInfo? = null
    private var withdrawList : MutableList<WithdrawModule.WithDrawInfo>? = null

    private var showConfirmationDialog = false

    private var isWithdrawing = AtomicBoolean(false)
    var sendResult by mutableStateOf<SendResult?>(null)


    var lockRecordTotal = 0
    var offset = 0
    var page = 0
    val limit = 20
    private val loading = AtomicBoolean(false)
    var canWithdrawAll = false

    override fun createState(): WithdrawModule.WithDrawNodeUiState {
        return getUiState()
    }

    private fun getUiState(): WithdrawModule.WithDrawNodeUiState {
        return WithdrawModule.WithDrawNodeUiState(
            withdrawList,
            withdrawList?.filter { it.checked }?.isNotEmpty() ?: false,
            showConfirmationDialog,
            canWithdrawAll
        )
    }

    init {
        /*lockedVoteService.itemsObservableLocked
            .subscribeIO {
                withdrawList =
                    it.map {
                        WithdrawModule.WithDrawInfo(it.lockId, it.unlockHeight.toLong(),
                            it.releaseHeight.toLong(),
                            NodeCovertFactory.formatSafe(it.lockValue),  it.address, it.enable)
                    }.filter { it.enable }
                emitState()
            }.let {
                disposables.add(it)
            }*/
        start()
    }


    private fun getTotal() {
        val old = lockRecordTotal
        lockRecordTotal = repository.getEnableReleaseVoteTotal(evmKit.receiveAddress.hex, evmKit.lastBlockHeight?: 0L)
        android.util.Log.d("LockedInfoViewModel", "total nun=$lockRecordTotal, old=$old}")
        if (page == 0) {
            getData()
        }
    }

    fun start() {
        viewModelScope.launch(Dispatchers.IO) {
            getTotal()
            getData()
            service.updateLockedInfo()
        }
        checkWithdrawAllState()
    }

    private fun getData() {
        if (loading.get())  return
        loading.set(true)
        android.util.Log.d("LockedInfoViewModel", "lockRecordTotal=$lockRecordTotal, size=${(withdrawList?.size ?: 0)}")
        if (lockRecordTotal == 0 || lockRecordTotal == (withdrawList?.size ?: 0))   return
        try {
            offset = page * limit
            val records = repository.getEnableReleaseVotedRecordsPaged(evmKit.receiveAddress.hex, evmKit.lastBlockHeight?: 0L, limit, offset)
                .filter { it.id != 0L }
            android.util.Log.d("LockedInfoViewModel", "get cache data: page=$page, offset=$offset, result=${records.map { it.id }}")
            if (records.isNotEmpty()) {
                val lockInfo = records.map {
                    WithdrawModule.WithDrawInfo(it.id, it.unlockHeight,
                        it.releaseHeight,
                        NodeCovertFactory.formatSafe(it.value),  it.address, (it.releaseHeight ?: 0) < (evmKit.lastBlockHeight ?: 0))
                }
                initIfNeed()
                withdrawList?.addAll(lockInfo)
            }
            if (records.isNotEmpty() && records.size == limit && lockRecordTotal > (withdrawList?.size ?: 0)) {
                page ++
            }
            emitState()
        } catch (e: Exception) {
            android.util.Log.e("LockedInfoViewModel", "get record error=$e")
        } finally {
            loading.set(false)
        }
    }

    private fun initIfNeed() {
        if (withdrawList == null) {
            withdrawList = mutableListOf()
        }
    }

    fun checkWithdrawAllState() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allRecordTotal = service.getRecordTotal()
                val localRecordTotal = repository.getTotal(evmKit.receiveAddress.hex)
                canWithdrawAll = localRecordTotal >= allRecordTotal && repository.getEnableReleaseVoteTotal(evmKit.receiveAddress.hex, evmKit.lastBlockHeight ?: 0) > 0
                emitState()
            } catch (e: Exception) {
                android.util.Log.e("LockedInfoViewModel", "checkWithdraw all state error=$e")
            }
        }
    }

    fun onBottomReached() {
        /*viewModelScope.launch(Dispatchers.IO) {
            lockedVoteService.loadNext()
        }*/
        viewModelScope.launch(Dispatchers.IO) {
            lockRecordTotal = repository.getEnableReleaseVoteTotal(evmKit.receiveAddress.hex, evmKit.lastBlockHeight?: 0L)
            if (lockRecordTotal != (withdrawList?.size ?: 0)) {
                getData()
            }

        }
    }

    fun check(lockId: Long) {
        withdrawList = withdrawList?.map {
            it.copy(
                checked = if (it.id == lockId) !it.checked else it.checked
            )
        } as MutableList<WithdrawModule.WithDrawInfo>?
        emitState()
    }

    fun selectAll(select: Boolean) {
        withdrawList?.forEach {
            if (it.enable) {
                it.checked = select
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
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            withdrawList?.let { list ->
                val checkedList = list.filter { it.checked }.map { it.id }
                try {
                    val result = service.removeVoteOrApproval(checkedList)
                    sendResult = SendResult.Sent
                    withdrawList = list.filter { !it.checked } as MutableList
                    emitState()
                    LockRecordManager.updateVoteStatus()
                } catch (e: Exception) {
                    Log.d("withdraw", "release withdraw error=$e")
                    sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
                }
            }
        }
    }


    fun withdrawAllEnable() {
        closeDialog()
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var ids = repository.getEnableReleaseVoteRecordIds(
                    evmKit.receiveAddress.hex,
                    evmKit.lastBlockHeight ?: 0
                )
                ids?.let {
                    service.removeVoteOrApproval(it)

                    LockRecordManager.updateVoteStatus()
                    sendResult = SendResult.Sent
                }
            } catch (e: Exception) {
                android.util.Log.e("LockedInfoViewModel", "withdraw all record error=$e")
                sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

