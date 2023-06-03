package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.marketkit.models.Token

object LiquidityModule {
    const val TOKEN_KEY = "token"

    class Factory(private val token: Token) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = LiquidityService(token)

            return LiquidityViewModel(service, listOf(service)) as T
        }
    }

    fun prepareParams(token: Token) = bundleOf(TOKEN_KEY to token)

}