package io.horizontalsystems.bankwallet.modules.swap.liquidity.uniswap

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.providers.UniswapProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.uniswapkit.Extensions
import io.horizontalsystems.uniswapkit.UniswapKit
import kotlinx.parcelize.Parcelize

object UniswapLiquidityModule {

    data class GuaranteedAmountViewItem(val title: String, val value: String)

    @Parcelize
    data class PriceImpactViewItem(val level: UniswapLiquidityTradeService.PriceImpactLevel, val value: String) : Parcelable

    abstract class UniswapWarnings : Warning() {
        object PriceImpactWarning : UniswapWarnings()
    }

    class AllowanceViewModelFactory(
        private val service: UniswapLiquidityService
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapAllowanceViewModel::class.java -> {
                    SwapAllowanceViewModel(
                        service,
                        service.allowanceService,
                        service.pendingAllowanceService,
                        SwapViewItemHelper(App.numberFormatter)
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    class Factory(
        val dex: SwapMainModule.Dex
    ) : ViewModelProvider.Factory {

        private val evmKit: EthereumKit by lazy { App.evmBlockchainManager.getEvmKitManager(dex.blockchainType).evmKitWrapper?.evmKit!! }
        private val uniswapKit by lazy { UniswapKit.getInstance(evmKit) }
        private val uniswapProvider by lazy { UniswapProvider(uniswapKit) }
        private val allowanceService by lazy {
            SwapAllowanceService(
                uniswapProvider.routerAddress,
                App.adapterManager,
                evmKit
            )
        }
        private val pendingAllowanceService by lazy {
            SwapPendingAllowanceService(
                App.adapterManager,
                allowanceService
            )
        }
        private val service by lazy {
            UniswapLiquidityService(
                dex,
                tradeService,
                allowanceService,
                pendingAllowanceService,
                App.adapterManager
            )
        }
        private val tradeService by lazy {
            UniswapLiquidityTradeService(evmKit, uniswapProvider)
        }
        private val formatter by lazy {
            SwapViewItemHelper(App.numberFormatter)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            Extensions.isSafeSwap = dex.provider.id == "safe"
            return when (modelClass) {
                UniswapLiquidityViewModel::class.java -> {
                    UniswapLiquidityViewModel(service, tradeService, pendingAllowanceService, formatter) as T
                }
                SwapAllowanceViewModel::class.java -> {
                    SwapAllowanceViewModel(service, allowanceService, pendingAllowanceService, formatter) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
