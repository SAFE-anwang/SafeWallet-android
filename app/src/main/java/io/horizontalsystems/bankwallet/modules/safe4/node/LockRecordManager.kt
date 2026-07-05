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

    fun updateVoteStatus() {
        job = scope.launch (Dispatchers.IO){
            delay(10000)
            try {
                getAdapter()?.let { adapter ->
                    val safe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val repository = LockRecordInfoRepository(App.appDatabase.lockRecordDao())
                    val voteLocked =
                        repository.getRecordsVoteLockRecord(adapter.evmKit.receiveAddress.hex)
                    val updateLocked = mutableListOf<LockRecordInfo>()
                    voteLocked.forEach {
                        if (job?.isActive == true) {
                            val info = safe4.getRecordByID(it.id, 0)
                            val recordUseInfo = safe4.getRecordUseInfo(it.id.toInt())
                            updateLocked.add(
                                it.copy(
                                    unlockHeight = info.unlockHeight.toLong(),
                                    releaseHeight = recordUseInfo.releaseHeight.toLong(),
                                    address2 = recordUseInfo.votedAddr.value,
                                    frozenAddr = recordUseInfo.frozenAddr.value
                                )
                            )
                        }
                    }
                    repository.save(updateLocked)
                }
            } catch (e: Exception) {
                android.util.Log.d("updateVoteStatus", "error=$e")
            }
        }
    }

    fun emit() {
        _recordState.update { true }
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