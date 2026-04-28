package io.horizontalsystems.bankwallet.modules.txsource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkViewModel
import io.horizontalsystems.marketkit.models.Blockchain

object EvmTransactionSourceModule {

    class Factory() : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EvmTransactionViewModel() as T
        }
    }

}
