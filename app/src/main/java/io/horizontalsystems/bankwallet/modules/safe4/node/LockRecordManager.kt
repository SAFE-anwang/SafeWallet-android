package io.horizontalsystems.bankwallet.modules.safe4.node

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalRecordRepository
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalService
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object LockRecordManager {

    private val _recordState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recordState = _recordState.asStateFlow()

    private val _newProposalRecordState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val newProposalRecordState = _newProposalRecordState.asStateFlow()
    private var proposalRecordRepository: ProposalRecordRepository? = null

    var service: WithdrawService? = null
    var service1: WithdrawService? = null
    var service2: WithdrawService? = null

    fun getAllLockRecord() {
        GlobalScope.launch(Dispatchers.IO) {
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
                while(adapterEvm == null) {
                    adapterEvm = (App.adapterManager.getAdapterForWallet(it) as? BaseEvmAdapter)
                    delay(1000)
                }
                adapterEvm?.let { adapter ->
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
                        service?.deleteLockedInfo()
                        service1?.deleteLockedInfo()
                        service2?.deleteLockedInfo()

                        delay(3000)
                    } catch (e: Exception) {

                    }
                }

            }
        }
    }

    suspend fun switchWallet() {
        service?.cancel()
        service1?.cancel()
        service2?.cancel()
        delay(2000)
        getAllLockRecord()
        getAllProposalRecord()
    }

    fun emit() {
        _recordState.update { true }
    }


    fun getAllProposalRecord() {
        GlobalScope.launch(Dispatchers.IO) {
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
        GlobalScope.launch(Dispatchers.IO) {

            proposalRecordRepository?.updateStatus()
        }
    }

    fun exit() {

    }
}