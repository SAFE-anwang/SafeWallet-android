package io.horizontalsystems.bankwallet.modules.dapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.modules.dapp.service.DAppApiService
import io.horizontalsystems.bankwallet.modules.dapp.service.DAppService
import io.horizontalsystems.bankwallet.net.SafeNetService
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal
import java.util.*

object DAppModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return DAppViewModel(
                DAppService(
                    DAppApiService()
                )
            ) as T
        }
    }

    class FactorySearch : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return DAppSearchViewModel(
                DAppService(
                    DAppApiService()
                )
            ) as T
        }
    }
}

data class TransactionLockInfo(
    val lockedUntil: Date,
    val originalAddress: String,
    val amount: BigDecimal?,
    val unlockedHeight: Long?
)

sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Double) : TransactionStatus() //progress in 0.0 .. 1.0
    object Completed : TransactionStatus()
    object Failed : TransactionStatus()
}

data class TransactionWallet(
    val platformCoin: PlatformCoin?,
    val source: TransactionSource,
    val badge: String?
)

data class TransactionSource(
    val blockchain: Blockchain,
    val account: Account,
    val coinSettings: CoinSettings
) {

    sealed class Blockchain {
        object Bitcoin : Blockchain()
        object Litecoin : Blockchain()
        object BitcoinCash : Blockchain()
        object Dash : Blockchain()
        object Safe : Blockchain()
        object Ethereum : Blockchain()
        object Zcash : Blockchain()
        object BinanceSmartChain : Blockchain()
        class Bep2(val symbol: String) : Blockchain(){
            override fun hashCode(): Int {
                return this.symbol.hashCode()
            }
            override fun equals(other: Any?): Boolean {
                return when(other){
                    is Bep2 -> this.symbol == other.symbol
                    else -> false
                }
            }
        }

        fun getTitle(): String {
            return when(this){
                Bitcoin -> "Bitcoin"
                Litecoin -> "Litecoin"
                BitcoinCash -> "BitcoinCash"
                Dash -> "Dash"
                Safe -> "Safe"
                Ethereum -> "Ethereum"
                Zcash -> "Zcash"
                BinanceSmartChain -> "Binance Smart Chain"
                is Bep2 -> "Binance Chain"
            }
        }
    }

}