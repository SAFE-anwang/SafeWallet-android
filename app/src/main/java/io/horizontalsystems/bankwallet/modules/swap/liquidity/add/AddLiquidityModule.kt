package io.horizontalsystems.bankwallet.modules.swap.liquidity.add

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm
import io.horizontalsystems.bankwallet.core.fiat.FiatService
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.multiswap.TimerService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.*
import io.horizontalsystems.bankwallet.modules.swap.liquidity.*
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.AmountTypeItem
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.PriceImpactLevel
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.ProviderViewItem
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapAmountInputState
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapCoinCardViewState
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapState
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.TradeViewX
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID

object AddLiquidityModule {

    private const val tokenFromKey = "token_from_key"
    private const val tokenToKey = "token_to_key"
    const val resultKey = "add_liquidity_settings_result"

    enum class Version {
        V2, V3
    }

    sealed class FeeTier(val label: String, val fee: Int) : Parcelable {
        @Parcelize
        object Low : FeeTier("0.01%", 100)

        @Parcelize
        object Medium : FeeTier("0.05%", 500)

        @Parcelize
        object Medium25 : FeeTier("0.25%", 2500)

        @Parcelize
        object UltraHigh : FeeTier("1%", 10000)

        companion object {
            val all = listOf(Low, Medium, Medium25, UltraHigh)
            val default = Medium25
        }
    }

    data class PriceRange(
        val minPrice: String,
        val maxPrice: String,
        val currentPrice: String?
    )

    data class AddLiquidityState(
        val version: Version,
        val dex: SwapMainModule.Dex,
        val providerViewItems: List<ProviderViewItem>,
        val tokenAState: SwapCoinCardViewState,
        val tokenBState: SwapCoinCardViewState,
        val availableBalance: String?,
        val availableBalanceB: String?,
        val amountTypeSelect: Select<AmountTypeItem>,
        val amountTypeSelectEnabled: Boolean,
        val error: String?,
        val buttons: SwapMainModule.SwapButtons2,
        val hasNonZeroBalance: Boolean?,
        val refocusKey: Long,
        // V3 specific
        val feeTiers: List<FeeTier>,
        val selectedFeeTier: FeeTier?,
        val priceRange: PriceRange?,
        val showPriceRange: Boolean,
    )

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {

        // slideFromRight wraps the bundle as { "input": originalBundle }
        private val input: Bundle? = arguments.getParcelable("input")
        private val tokenFrom: Token? = input?.getParcelable(tokenFromKey)
        private val tokenTo: Token? = input?.getParcelable(tokenToKey)

        private val swapProviders: List<SwapMainModule.ISwapProvider> = listOf(
            LiquidityMainModule.PancakeLiquidityProvider,
            LiquidityMainModule.PancakeV3LiquidityProvider,
            LiquidityMainModule.UniswapLiquidityProvider,
            LiquidityMainModule.UniswapV3LiquidityProvider,
            LiquidityMainModule.Safe4LiquidityProvider
        )

        private val switchService by lazy { AmountTypeSwitchServiceSendEvm() }
        private val swapMainXService by lazy {
            LiquidityMainService(tokenFrom, swapProviders, App.localStorage)
        }
        private val evmKitWrapper by lazy {
            App.evmBlockchainManager.getEvmKitManager(swapMainXService.dex.blockchainType).evmKitWrapper
        }
        private val evmKit: EthereumKit
            get() = evmKitWrapper?.evmKit ?: throw Exception("EvmKit is not initialized")

        private val allowanceServiceA by lazy { LiquidityAllowanceService(App.adapterManager, evmKit) }
        private val allowanceServiceB by lazy { LiquidityAllowanceService(App.adapterManager, evmKit) }
        private val pendingAllowanceServiceA by lazy {
            LiquidityPendingAllowanceService(App.adapterManager, allowanceServiceA)
        }
        private val pendingAllowanceServiceB by lazy {
            LiquidityPendingAllowanceService(App.adapterManager, allowanceServiceB)
        }
        private val errorShareService by lazy { ErrorShareService() }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                AddLiquidityViewModel::class.java -> {
                    val fromFiatService =
                        FiatService(switchService, App.currencyManager, App.marketKit)
                    val toFiatService =
                        FiatService(switchService, App.currencyManager, App.marketKit)

                    val fromTokenService = LiquidityTokenService(
                        switchService = switchService,
                        fiatService = fromFiatService,
                        resetAmountOnCoinSelect = true,
                        initialToken = tokenFrom
                    )
                    val toTokenService = LiquidityTokenService(
                        switchService = switchService,
                        fiatService = toFiatService,
                        resetAmountOnCoinSelect = false,
                        initialToken = tokenTo
                    )

                    val formatter = LiquidityViewItemHelper(App.numberFormatter)

                    AddLiquidityViewModel(
                        formatter,
                        swapMainXService,
                        switchService,
                        fromTokenService,
                        toTokenService,
                        allowanceServiceA,
                        allowanceServiceB,
                        pendingAllowanceServiceA,
                        pendingAllowanceServiceB,
                        errorShareService,
                        TimerService(),
                        App.currencyManager,
                        App.adapterManager,
                        evmKitWrapper
                    ) as T
                }

                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    fun prepareParams(tokenFrom: Token, tokenTo: Token? = null) = bundleOf(tokenFromKey to tokenFrom, tokenToKey to tokenTo)

    fun isV3Provider(provider: SwapMainModule.ISwapProvider): Boolean {
        return provider.id.contains("v3", ignoreCase = true)
    }

    fun getVersionFromProvider(provider: SwapMainModule.ISwapProvider): Version {
        return if (isV3Provider(provider)) Version.V3 else Version.V2
    }
}
