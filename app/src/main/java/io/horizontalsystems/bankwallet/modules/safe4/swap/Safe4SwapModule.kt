package io.horizontalsystems.bankwallet.modules.safe4.swap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.marketkit.models.Token

object Safe4SwapModule {

    class Factory(
        private val wallet1: Wallet,
        private val wallet2: Wallet
    ) : ViewModelProvider.Factory {
        private val adapter by lazy { App.adapterManager.getAdapterForWallet(wallet2) as ISendEthereumAdapter }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return when (modelClass) {
                Safe4SwapViewModel::class.java -> {
                    Safe4SwapViewModel(
                        wallet1.token,
                        wallet2.token,
                        adapter,
                        App.adapterManager,
                        App.connectivityManager
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    data class Safe4SwapUiState(
        val canSend: Boolean,
        val fromToken: Token,
        val toToken: Token,
        val balance1: String,
        val balance2: String,
        val caution: Caution?,
        val inputAmount: String? = null
    )

}