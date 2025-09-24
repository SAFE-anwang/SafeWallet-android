package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfoRepository
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordManager
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourLockedVoteService
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class LockedInfoViewModel(
    val wallet: Wallet,
    val evmKit: EthereumKit,
    val service: WithdrawService,
    private val repository: LockRecordInfoRepository,
    private val connectivityManager: ConnectivityManager
): ViewModelUiState<WithdrawModule.WithDrawLockInfoUiState>() {


    private val disposables = CompositeDisposable()
    private var withdrawList : MutableList<WithdrawModule.WithDrawLockedInfo>? = null
    private var withdrawAvailable : List<WithdrawModule.WithDrawLockedInfo>? = null

    private var showConfirmationDialog = false

    private var isWithdrawing = AtomicBoolean(false)
    var sendResult by mutableStateOf<SendResult?>(null)

    var clickWithdrawInfo: WithdrawModule.WithDrawLockedInfo? = null
    var clickAddLockDayId: Int = 0
    var lockRecordTotal = 0
    var offset = 0
    var page = 0
    val limit = 20

    override fun createState(): WithdrawModule.WithDrawLockInfoUiState {
        return getUiState()
    }

    private fun getUiState(): WithdrawModule.WithDrawLockInfoUiState {
        Log.d("LockedInfoViewModel", "withdrawList= ${withdrawList?.size}, ${withdrawAvailable?.size}")
        val list = mutableListOf<WithdrawModule.WithDrawLockedInfo>()
        withdrawAvailable?.let {
            list.addAll(it)
        }
        withdrawList?.let {
            list.addAll(it.sortedBy { it.unlockHeight ?: 0 < evmKit.lastBlockHeight?: 0 })
        }
        return WithdrawModule.WithDrawLockInfoUiState(
            list,
            showConfirmationDialog
        )
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            LockRecordManager.recordState.collect {
                getTotal()
            }
        }

        start()
    }

    private fun getTotal() {
        val old = lockRecordTotal
        lockRecordTotal = repository.getTotal(evmKit.receiveAddress.hex)
        Log.d("LockedInfoViewModel", "total nun=$lockRecordTotal, old=$old}")
        if (page == 1) {
            getData()
        }
        getWithdrawEnableRecord()
    }

    fun start() {
        viewModelScope.launch(Dispatchers.IO) {
            getTotal()
            getData()
            getWithdrawEnableRecord()
            queryNeedUpdateRecords()
            service.deleteLockedInfo()
        }
    }

    private fun initIfNeed() {
        if (withdrawList == null) {
            withdrawList = mutableListOf()
        }
    }

    private fun getWithdrawEnableRecord() {
        if (lockRecordTotal == 0)   return
        try {
            val result = repository.getRecordsForEnableWithdraw(evmKit.receiveAddress.hex,evmKit.lastBlockHeight ?: 0)
//            if (records.isEmpty())  return
            result?.let { records ->
                val lockInfo = records.filter { it.id != 0L } .map {
                    WithdrawModule.WithDrawLockedInfo(it.id,
                        it.unlockHeight,
                        it.releaseHeight,
                        NodeCovertFactory.formatSafe(it.value),
                        it.value,
                        it.address,
                        it.address2,
                        it.frozenAddr,
                        (it.releaseHeight == 0L && (it.unlockHeight ?: 0)< (evmKit.lastBlockHeight ?: 0))
                                || (it.unlockHeight == 0L && (it.releaseHeight ?: 0) < (evmKit.lastBlockHeight ?: 0)),
                        if (it.address2 == service.zeroAddress || it.type > 0) null else (it.unlockHeight ?: 0) > 0L,
                        it.contact
                    )
                }
                withdrawAvailable = lockInfo
                emitState()
            }
        } catch (e: Exception) {

        }
    }

    private fun getData() {
        Log.d("LockedInfoViewModel", "lockRecordTotal=$lockRecordTotal, size=${(withdrawList?.size ?: 0)}, ${ (withdrawAvailable?.size ?: 0)}")
        if (lockRecordTotal == 0 || lockRecordTotal == (withdrawList?.size ?: 0) + (withdrawAvailable?.size ?: 0))   return
        try {
            offset = page * limit
            val records = repository.getRecordsPaged(evmKit.receiveAddress.hex, evmKit.lastBlockHeight?: 0L, limit, offset)
                .filter { it.id != 0L }
            Log.d("LockedInfoViewModel", "get cache data: page=$page, offset=$offset, result=${records.map { it.id }}")
            if (records.isNotEmpty()) {
                val lockInfo = records.map {
                    WithdrawModule.WithDrawLockedInfo(it.id,
                        it.unlockHeight,
                        it.releaseHeight,
                        NodeCovertFactory.formatSafe(it.value),
                        it.value,
                        it.address,
                        it.address2,
                        it.frozenAddr,
                        (it.releaseHeight == 0L && (it.unlockHeight ?: 0)< (evmKit.lastBlockHeight ?: 0))
                                || ((it.releaseHeight ?: 0) > 0L && (it.releaseHeight ?: 0) < (evmKit.lastBlockHeight ?: 0)),
                        if (it.address == service.zeroAddress || it.type > 0) null else (it.unlockHeight ?: 0) > 0L,
                        it.contact
                    )
                }
                initIfNeed()
                withdrawList?.addAll(lockInfo)
            }
            if (records.isNotEmpty() && records.size == limit && lockRecordTotal > (withdrawList?.size ?: 0)) {
                page ++
            }
            emitState()
        } catch (e: Exception) {
            Log.e("LockedInfoViewModel", "get record error=$e")
        }
    }

    fun onBottomReached() {
        viewModelScope.launch(Dispatchers.IO) {
            lockRecordTotal = repository.getTotal(evmKit.receiveAddress.hex)
            if (lockRecordTotal == (withdrawList?.size ?: 0) +( withdrawAvailable?.size ?: 0)) {
                return@launch
            }
            getData()
        }
    }

    private fun queryNeedUpdateRecords() {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val records = repository.queryNeedUpdateRecords(evmKit.receiveAddress.hex)
                val temp = records.map {
                    val info = service.getRecordInfo(it.id)
                    it.copy(releaseHeight = info.recordInfo?.releaseHeight?.toLong())
                }
                repository.save(temp)
            }
        } catch (e: Exception) {

        }
    }

    fun check(info: WithdrawModule.WithDrawLockedInfo) {
        clickWithdrawInfo = info
    }

    fun getHintText(): Int {
        return if (clickWithdrawInfo?.unlockHeight == 0L) {
            R.string.SAFE4_Withdraw_Local_Hint
        } else {
            R.string.SAFE4_Withdraw_Vote_Hint
        }
    }

    fun addLockDay(lockedId: Int) {
        clickAddLockDayId = lockedId
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
        if (clickWithdrawInfo == null)  return
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if ((clickWithdrawInfo?.releaseHeight ?: 0) > 0) {
                    service.withdraw(
                        listOf(clickWithdrawInfo!!.id),
                        getType(clickWithdrawInfo!!.value)
                    )
                } else {
                    service.removeVoteOrApproval(
                        listOf(clickWithdrawInfo!!.id)
                    )
                }
                sendResult = SendResult.Sent
                val isOnlyWithdraw = clickWithdrawInfo!!.unlockHeight == 0L
                if (isOnlyWithdraw) {
                    withdrawList = withdrawList?.filter { it.id != clickWithdrawInfo?.id } as MutableList<WithdrawModule.WithDrawLockedInfo>?

                    clickWithdrawInfo?.let {
                        repository.delete(it.id, it.contract)
                    }

                }
                emitState()
            } catch (e: Exception) {
                sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
            }
        }
    }

    fun getType(value: BigInteger): Int {
        val tempValue = NodeCovertFactory.valueConvert(value)
        return if (tempValue >= BigDecimal.valueOf(0.1) && tempValue < BigDecimal.valueOf(1)) {
            2
        } else if (tempValue >= BigDecimal.valueOf(0.01) && tempValue < BigDecimal.valueOf(0.1)) {
            1
        } else {
            0
        }
    }

    fun withdrawEnable(recordInfo: LockRecordInfo): Boolean {
        return (recordInfo.releaseHeight == 0L && (recordInfo.unlockHeight
            ?: 0) < (evmKit.lastBlockHeight
            ?: 0L)) || (recordInfo.unlockHeight == 0L && (recordInfo.releaseHeight
            ?: 0) < (evmKit.lastBlockHeight ?: 0))
    }

    fun addLockEnable(recordInfo: LockRecordInfo): Boolean? {
        return if (recordInfo.address == service.zeroAddress || recordInfo.type > 0) null else (recordInfo.unlockHeight
            ?: 0L) > 0L
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

