package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.fiat.FiatService
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.swap.*
import io.horizontalsystems.bankwallet.modules.swap.allowance.*
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityPendingAllowanceService
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.math.absoluteValue

object LiquidityListModule {


    class Factory() : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return when (modelClass) {
                LiquidityListViewModel::class.java -> {

                    LiquidityListViewModel(
                        App.accountManager,
                        App.walletStorage,
                        App.adapterManager,
                        App.marketKit,
                        LiquidityViewItemFactory()
                    ) as T
                }

                else -> throw IllegalArgumentException()
            }
        }

    }

    data class LiquidityItem(
        val walletA: Wallet,
        val walletB: Wallet,
        val addressA: String,
        val addressB: String,
        val walletAmount: BigDecimal,
        val walletBmount: BigDecimal,
        val liquidity: BigInteger,
        val shareRate: BigDecimal,
        val poolTokenTotalSupply: BigInteger
    )

    @Parcelize
    data class PriceImpactViewItem(val level: PriceImpactLevel, val value: String) : Parcelable

    sealed class AmountTypeItem : WithTranslatableTitle {
        object Coin : AmountTypeItem()
        class Currency(val name: String) : AmountTypeItem()

        override val title: TranslatableString
            get() = when (this) {
                Coin -> TranslatableString.ResString(R.string.Swap_AmountTypeCoin)
                is Currency -> TranslatableString.PlainString(name)
            }

        override fun equals(other: Any?): Boolean {
            return other is Coin && this is Coin || other is Currency && this is Currency && other.name == this.name
        }

        override fun hashCode() = when (this) {
            Coin -> javaClass.hashCode()
            is Currency -> name.hashCode()
        }
    }

    sealed class SwapResultState {
        object Loading : SwapResultState()
        class Ready(val swapData: SwapData) : SwapResultState()
        class NotReady(val errors: List<Throwable> = listOf()) : SwapResultState()
    }

    sealed class SwapData {
        data class OneInchData(val data: OneInchSwapParameters) : SwapData()
        data class UniswapData(val data: UniversalSwapTradeData) : SwapData() {
            private val normalPriceImpact = BigDecimal(1)
            private val warningPriceImpact = BigDecimal(5)
            private val forbiddenPriceImpact = BigDecimal(20)

            val priceImpactLevel: PriceImpactLevel? = data.priceImpact?.let {
                when {
                    it >= BigDecimal.ZERO && it < normalPriceImpact -> PriceImpactLevel.Negligible
                    it >= normalPriceImpact && it < warningPriceImpact -> PriceImpactLevel.Normal
                    it >= warningPriceImpact && it < forbiddenPriceImpact -> PriceImpactLevel.Warning
                    else -> PriceImpactLevel.Forbidden
                }
            }
        }
    }

    data class TradeViewX(
        val providerTradeData: ProviderTradeData,
        val expired: Boolean = false
    )

    sealed class ProviderTradeData {
        class OneInchTradeViewItem(
            val primaryPrice: String? = null,
            val secondaryPrice: String? = null,
        ) : ProviderTradeData()

        class UniswapTradeViewItem(
            val primaryPrice: String? = null,
            val secondaryPrice: String? = null,
            val priceImpact: PriceImpactViewItem? = null,
        ) : ProviderTradeData()
    }

    /*@Parcelize
    class Dex(val blockchain: Blockchain, val provider: ILiquidityProvider) : Parcelable {
        val blockchainType get() = blockchain.type
    }*/

    /*interface ILiquidityProvider : Parcelable {
        val id: String
        val title: String
        val url: String
        val supportsExactOut: Boolean

        fun supports(blockchainType: BlockchainType): Boolean
    }*/

    @Parcelize
    object PancakeLiquidityProvider : SwapMainModule.ISwapProvider {
        override val id get() = "pancake_liquidity"
        override val title get() = "PancakeSwap"
        override val url get() = "https://pancakeswap.finance/"
        override val supportsExactOut get() = true

        override fun supports(blockchainType: BlockchainType): Boolean {
            return blockchainType == BlockchainType.BinanceSmartChain
        }
    }

/*    @Parcelize
    data class ApproveData(
        val dex: SwapMainModule.Dex,
        val token: Token,
        val spenderAddress: String,
        val amount: BigDecimal,
        val allowance: BigDecimal
    ) : Parcelable*/

    @Parcelize
    enum class PriceImpactLevel : Parcelable {
        Negligible, Normal, Warning, Forbidden
    }

    abstract class UniswapWarnings : Warning() {
        object PriceImpactWarning : UniswapWarnings()
        class PriceImpactForbidden(val providerName: String) : UniswapWarnings()
    }

    @Parcelize
    data class OneInchSwapParameters(
        val tokenFrom: Token,
        val tokenTo: Token,
        val amountFrom: BigDecimal,
        val amountTo: BigDecimal,
        val slippage: BigDecimal,
        val recipient: Address? = null
    ) : Parcelable

    sealed class SwapError : Throwable() {
        object InsufficientBalanceFrom : SwapError()
        object InsufficientAllowance : SwapError()
        object RevokeAllowanceRequired : SwapError()
        object ForbiddenPriceImpactLevel : SwapError()
    }

    @Parcelize
    data class CoinBalanceItem(
        val token: Token,
        val balance: BigDecimal?,
        val fiatBalanceValue: CurrencyValue?,
    ) : Parcelable

    enum class ExactType {
        ExactFrom, ExactTo
    }

/*    sealed class SwapActionState {
        object Hidden : SwapActionState()
        class Enabled(val buttonTitle: String) : SwapActionState()
        class Disabled(val buttonTitle: String, val loading: Boolean = false) : SwapActionState()

        val title: String
            get() = when (this) {
                is Enabled -> this.buttonTitle
                is Disabled -> this.buttonTitle
                else -> ""
            }

        val showProgress: Boolean
            get() = this is Disabled && loading
    }*/

}

fun BigDecimal.scaleUp(scale: Int): BigInteger {
    val exponent = scale - scale()

    return if (exponent >= 0) {
        unscaledValue() * BigInteger.TEN.pow(exponent)
    } else {
        unscaledValue() / BigInteger.TEN.pow(exponent.absoluteValue)
    }
}
