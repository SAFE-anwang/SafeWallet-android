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

    private val _recordState: MutableStateFlow<Int> = MutableStateFlow(0)
    val recordState = _recordState.asStateFlow()

    private val _newProposalRecordState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val newProposalRecordState = _newProposalRecordState.asStateFlow()
    private var proposalRecordRepository: ProposalRecordRepository? = null

    var exit = false

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
                    val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val service = WithdrawService(rpcBlockchainSafe4, adapter.evmKitWrapper)
                    val service1 = WithdrawService(rpcBlockchainSafe4, adapter.evmKitWrapper, 1)
                    val service2 = WithdrawService(rpcBlockchainSafe4, adapter.evmKitWrapper, 2)
                    val repository = LockRecordInfoRepository(App.appDatabase.lockRecordDao())
                    service.setLockRecordRepository(repository)
                    service1.setLockRecordRepository(repository)
                    service2.setLockRecordRepository(repository)
//                    while(!exit) {
                        service.start()
                        service1.start()
                        service2.start()
                        _recordState.update { _recordState.value ++ }
                        delay(3000)
//                    }
                }

            }
        }
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
        proposalRecordRepository?.updateStatus()
    }

}