package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourLockedVoteService
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class LockedInfoViewModel(
    val wallet: Wallet,
    val evmKit: EthereumKit,
    val service: WithdrawService,
    val lockedVoteService: SafeFourLockedVoteService,
    private val connectivityManager: ConnectivityManager
): ViewModelUiState<WithdrawModule.WithDrawLockInfoUiState>() {


    private val disposables = CompositeDisposable()
    private var withdrawList : List<WithdrawModule.WithDrawLockedInfo>? = null
    private var withdrawListVote : List<WithdrawModule.WithDrawLockedInfo>? = null
    private var withdrawListProposal : List<WithdrawModule.WithDrawLockedInfo>? = null
    private var withdrawAvailable : List<WithdrawModule.WithDrawLockedInfo>? = null

    private var showConfirmationDialog = false

    private var isWithdrawing = AtomicBoolean(false)
    var sendResult by mutableStateOf<SendResult?>(null)

    var clickWithdrawInfo: WithdrawModule.WithDrawLockedInfo? = null
    var clickAddLockDayId: Int = 0

    override fun createState(): WithdrawModule.WithDrawLockInfoUiState {
        return getUiState()
    }

    private fun getUiState(): WithdrawModule.WithDrawLockInfoUiState {
        /*val list = if (withdrawList != null && withdrawListVote != null) {
            withdrawList!! + withdrawListVote!! + withdrawListProposal!!
        } else if (withdrawList != null) {
            withdrawList
        } else {
            withdrawListVote
        }*/
        val list = (withdrawList ?: listOf()) + (withdrawListVote ?: listOf()) + (withdrawListProposal ?: listOf()) + (withdrawAvailable ?: listOf())
        return WithdrawModule.WithDrawLockInfoUiState(
            list?.sortedBy { it.id }?.distinctBy { it.id },
            showConfirmationDialog
        )
    }

    init {
        lockedVoteService.itemsObservable
            .subscribeIO {
                withdrawList =
                    it.map {
                        WithdrawModule.WithDrawLockedInfo(it.lockId,
                            it.unlockHeight.toLong(),
                            it.releaseHeight.toLong(),
                            NodeCovertFactory.formatSafe(it.lockValue),
                            if (it.address == service.zeroAddress) null else it.address,
                            it.address2,
                            it.unlockHeight == BigInteger.ZERO || it.unlockHeight.toLong() < (evmKit.lastBlockHeight
                                ?: 0),
                                    if (it.address == service.zeroAddress) null else it.unlockHeight > BigInteger.ZERO
                        )
                    }
                emitState()
            }.let {
                disposables.add(it)
            }
        lockedVoteService.itemsObservableLocked
            .subscribeIO {
                withdrawListVote =
                    it.map {
                        WithdrawModule.WithDrawLockedInfo(it.lockId,
                            it.unlockHeight.toLong(),
                            it.releaseHeight.toLong(),
                            NodeCovertFactory.formatSafe(it.lockValue),  it.address, it.address2,
                            (it.unlockHeight.toLong() < (evmKit.lastBlockHeight ?: 0))
                                    || (it.releaseHeight.toLong() < (evmKit.lastBlockHeight ?: 0)),
                            if (it.address == service.zeroAddress) null else it.unlockHeight > BigInteger.ZERO
                        )
                    }
                emitState()
            }.let {
                disposables.add(it)
            }

        lockedVoteService.mineItemsObservable
            .subscribeIO {
                withdrawListProposal =
                    it.map {
                        WithdrawModule.WithDrawLockedInfo(it.lockId,
                            it.unlockHeight.toLong(),
                            it.releaseHeight.toLong(),
                            NodeCovertFactory.formatSafe(it.lockValue),  it.address, it.address2,
                            (it.releaseHeight == BigInteger.ZERO && it.unlockHeight.toLong() < (evmKit.lastBlockHeight ?: 0))
                                    || (it.unlockHeight == BigInteger.ZERO && it.releaseHeight.toLong() < (evmKit.lastBlockHeight ?: 0)),
                            if (it.address == service.zeroAddress) null else it.unlockHeight > BigInteger.ZERO
                        )
                    }
                emitState()
            }.let {
                disposables.add(it)
            }
        service.itemsObservableAvailable
            .subscribeIO {
                withdrawAvailable = it
                emitState()
            }
        start()
    }

    fun start() {
        viewModelScope.launch(Dispatchers.Default) {
               lockedVoteService.loadItems(0)
               lockedVoteService.loadItemsLocked(0)
            lockedVoteService.getMinProposalNum()
            service.loadItemsAvailable(0)
        }
    }

    fun onBottomReached() {
        viewModelScope.launch(Dispatchers.IO) {
            lockedVoteService.loadNext()
            service.loadNext()
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
                service.withdraw(listOf(clickWithdrawInfo!!.id))
                sendResult = SendResult.Sent
                val isOnlyWithdraw = clickWithdrawInfo!!.unlockHeight == 0L
                if (isOnlyWithdraw) {
                    withdrawList = withdrawList?.filter { it.id != clickWithdrawInfo?.id }
                }
                emitState()
            } catch (e: Exception) {
                sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

