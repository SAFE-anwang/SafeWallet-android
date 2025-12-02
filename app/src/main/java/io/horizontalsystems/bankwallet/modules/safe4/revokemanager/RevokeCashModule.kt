package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType

object RevokeCashModule {

    class Factory(val blockchainType: BlockchainType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val chain = App.evmBlockchainManager.getChain(blockchainType)
            val evmKitWrapper = App.evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper ?: throw IllegalArgumentException("$blockchainType is not support")
            return when (modelClass) {
                RevokeCashViewModel::class.java -> {
                    val chain = App.evmBlockchainManager.getChain(blockchainType)
                    val address = evmKitWrapper.evmKit.receiveAddress.hex
                    val account = App.accountManager.activeAccount ?: throw IllegalArgumentException("account is not null")
                    RevokeCashViewModel(chain, address, account) as T
                }

                EvmKitWrapperHoldingViewModel::class.java -> {
                    EvmKitWrapperHoldingViewModel(evmKitWrapper) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

}