package io.horizontalsystems.bankwallet.modules.safe4.node

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalRecordRepository
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalService
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object LockRecordManager {

    private val _recordState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recordState = _recordState.asStateFlow()

    private val _refreshTrigger: MutableStateFlow<Long> = MutableStateFlow(0L)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    private val _newProposalRecordState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val newProposalRecordState = _newProposalRecordState.asStateFlow()
    private var proposalRecordRepository: ProposalRecordRepository? = null

    var service: WithdrawService? = null
    var service1: WithdrawService? = null
    var service2: WithdrawService? = null

    var job: Job? = null

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getAllLockRecord() {
        scope.launch(Dispatchers.IO) {
            getAdapter()?.let { adapter ->
                    try {
                        Log.d("WithdrawService", "address=${adapter.evmKit.receiveAddress.hex}")
                        val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                        service = WithdrawService(rpcBlockchainSafe4, adapter.evmKitWrapper)
                        service1 = WithdrawService(rpcBlockchainSafe4, adapter.evmKitWrapper, 1)
                        service2 = WithdrawService(rpcBlockchainSafe4, adapter.evmKitWrapper, 2)
                        val repository = LockRecordInfoRepository(App.appDatabase.lockRecordDao())
                        service?.setLockRecordRepository(repository)
                        service1?.setLockRecordRepository(repository)
                        service2?.setLockRecordRepository(repository)
                        service?.start()
                        service1?.start()
                        service2?.start()
                        scope.launch {
                            service?.updateLockedInfo()
                        }
                        scope.launch {
                            service1?.updateLockedInfo()
                        }
                        scope.launch {
                            service2?.updateLockedInfo()
                        }
                    } catch (e: Exception) {
                        Log.e("LockRecordManager", "getAllLockRecord error", e)
                        // 即使同步失败，也通知 ViewModel 刷新（使用 DB 已有数据）
                        _recordState.update { true }
                    }
                } ?: run {
                    // getAdapter 返回 null，没有钱包可用，也通知 ViewModel
                    _recordState.update { true }
                }

//            }
        }
    }

    private suspend fun getAdapter(): BaseEvmAdapter? {
        var safeWallet: Wallet? = null
        while(safeWallet == null) {
            try {
                val walletList: List<Wallet> = App.walletManager.activeWallets
                walletList.forEach {
                    if (it.token.blockchain.type is BlockchainType.SafeFour && it.coin.uid == "safe4-coin") {
                        safeWallet = it
                        return@forEach
                    }
                }
            } catch (e: Exception) {

            }
        }
        safeWallet?.let {
            var adapterEvm = (App.adapterManager.getAdapterForWallet(it) as? BaseEvmAdapter)
            while (adapterEvm == null) {
                adapterEvm = (App.adapterManager.getAdapterForWallet(it) as? BaseEvmAdapter)
                delay(1000)
            }
            return adapterEvm
        }
        return null
    }

    suspend fun switchWallet() {
        cancelAllSyncTasks()
        _refreshTrigger.update { it + 1 }
        delay(2000)
        getAllLockRecord()
        getAllProposalRecord()
        updateVoteStatus()
    }

    fun switchNetwork() {
        cancelAllSyncTasks()
        _refreshTrigger.update { it + 1 }
        scope.launch {
            delay(1000)
            getAllLockRecord()
            getAllProposalRecord()
            updateVoteStatus()
        }
    }

    fun cancelAllSyncTasks() {
        Log.d("LockRecordManager", "cancelAllSyncTasks: cancelling all sync tasks")

        // 取消投票状态更新任务
        job?.cancel()
        job = null

        // 取消并清理 WithdrawService 实例
        service?.cancel()
        service?.clear()
        service1?.cancel()
        service1?.clear()
        service2?.cancel()
        service2?.clear()

        service = null
        service1 = null
        service2 = null

        // 取消整个协程作用域并创建新的
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    /**
     * 更新本地记录状态。
     * @param withRetry 是否为提现后调用。提现交易链上确认需要时间，单次更新
     * 可能读到旧状态，因此采用多次重试：每次从链上读取最新状态保存到本地，
     * 若检测到状态已发生变化（交易已确认）则提前结束，否则按递增间隔重试。
     * 切换钱包/网络等场景传 false，仅单次更新。
     */
    fun updateVoteStatus(withRetry: Boolean = false) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            if (withRetry) {
                val maxAttempts = 5
                val retryDelays = listOf(5000L, 10000L, 15000L, 20000L, 30000L)
                for (attempt in 0 until maxAttempts) {
                    if (!isActive) break
                    delay(retryDelays[attempt])
                    try {
                        val confirmed = updateVoteStatusFromChain()
                        Log.d("updateVoteStatus", "attempt ${attempt + 1}/$maxAttempts, confirmed=$confirmed")
                        // 每次更新后通知 UI 刷新，DB 一旦更新成功界面即可显示正确数据
                        emit()
                        if (confirmed) {
                            Log.d("updateVoteStatus", "vote status confirmed on chain, stop retrying")
                            break
                        }
                    } catch (e: Exception) {
                        Log.e("updateVoteStatus", "attempt ${attempt + 1} error=$e")
                    }
                }
            } else {
                delay(5000)
                try {
                    updateVoteStatusFromChain()
                } catch (e: Exception) {
                    Log.e("updateVoteStatus", "error=$e")
                }
            }
        }
    }

    /**
     * 从链上读取投票锁仓记录最新状态并保存到本地 DB。
     * @return true 表示检测到至少一条记录的状态相比本地已发生变化（交易已确认），
     *         false 表示链上状态与本地一致（交易可能尚未确认）。
     */
    private suspend fun updateVoteStatusFromChain(): Boolean {
        val adapter = getAdapter() ?: return true
        val safe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
        val repository = LockRecordInfoRepository(App.appDatabase.lockRecordDao())
        val voteLocked =
            repository.getRecordsVoteLockRecord(adapter.evmKit.receiveAddress.hex)
        if (voteLocked.isEmpty()) return true

        val updateLocked = mutableListOf<LockRecordInfo>()
        var anyChanged = false
        voteLocked.forEach { record ->
            if (!scope.isActive) return@forEach
            val info = safe4.getRecordByID(record.id, 0)
            val recordUseInfo = safe4.getRecordUseInfo(record.id.toInt())
            val newUnlockHeight = info.unlockHeight.toLong()
            val newReleaseHeight = recordUseInfo.releaseHeight.toLong()
            val newVotedAddr = recordUseInfo.votedAddr.value
            val newFrozenAddr = recordUseInfo.frozenAddr.value

            if (newUnlockHeight != record.unlockHeight ||
                newReleaseHeight != record.releaseHeight ||
                newVotedAddr != record.address2 ||
                newFrozenAddr != record.frozenAddr
            ) {
                anyChanged = true
            }

            updateLocked.add(
                record.copy(
                    unlockHeight = newUnlockHeight,
                    releaseHeight = newReleaseHeight,
                    address2 = newVotedAddr,
                    frozenAddr = newFrozenAddr
                )
            )
        }
        if (updateLocked.isNotEmpty()) {
            repository.save(updateLocked)
        }
        return anyChanged
    }

    fun emit() {
        // toggle 确保重复调用也能触发 collector（StateFlow 相同值不重复发射）
        _recordState.update { !it }
    }


    fun getAllProposalRecord() {
        scope.launch(Dispatchers.IO) {
            var safeWallet: Wallet? = null
            while (safeWallet == null) {
                try {
                    val walletList: List<Wallet> = App.walletManager.activeWallets
                    walletList.forEach {
                        if (it.token.blockchain.type is BlockchainType.SafeFour && it.coin.uid == "safe4-coin") {
                            safeWallet = it
                            return@forEach
                        }
                    }
                } catch (e: Exception) {
                    Log.e("getAllProposalRecord", "error=$e")
                }
            }
            safeWallet?.let {
                var adapterEvm = (App.adapterManager.getAdapterForWallet(it) as? BaseEvmAdapter)
                while(adapterEvm == null) {
                    adapterEvm = (App.adapterManager.getAdapterForWallet(it) as? BaseEvmAdapter)
                    delay(1000)
                }
                adapterEvm?.let { adapter ->
                    val rpcBlockchainSafe4 =
                        adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    var repository = proposalRecordRepository
                    if (repository == null) {
                        repository =
                            ProposalRecordRepository(App.appDatabase.proposalRecordDao())
                    }
                    proposalRecordRepository = repository
                    val service = SafeFourProposalService(rpcBlockchainSafe4, adapter.evmKitWrapper, repository)
                    service.start()
                    delay(5000)
                    _newProposalRecordState.value = repository.getNewProposalRecordNum() > 0
                }
            }
        }
    }

    fun updateProposalStatus() {
        scope.launch(Dispatchers.IO) {

            proposalRecordRepository?.updateStatus()
        }
    }

    fun exit() {
        cancelAllSyncTasks()
    }
}