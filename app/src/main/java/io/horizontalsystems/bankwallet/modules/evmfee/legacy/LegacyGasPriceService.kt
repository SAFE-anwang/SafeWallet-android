package io.horizontalsystems.bankwallet.modules.evmfee.legacy

import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.Bound
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsWarning
import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal
import kotlin.math.max

class LegacyGasPriceService(
    private val gasPriceProvider: LegacyGasPriceProvider,
    private val minRecommendedGasPrice: Long? = DEFAULT_GAS_PRICE,
    /**
     * 初始显示的 gasPrice。
     *
     * 默认 100_000_000 wei（0.1 Gwei）：部分链（如 SAFE4）节点返回的 gasPrice 偏低，
     * 且获取推荐值需要一次网络请求，为避免界面初始为 Loading 且值偏低，
     * 这里直接以 0.1 Gwei 作为初始默认值，用户点「推荐」后仍会取节点真实值覆盖。
     */
    private val initialGasPrice: Long? = DEFAULT_GAS_PRICE,
) : IEvmGasPriceService() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var setGasPriceJob: Job? = null
    private var recommendedGasPrice: Long? = null

    private val overpricingBound = Bound.Multiplied(BigDecimal(1.5))
    private val riskOfStuckBound = Bound.Multiplied(BigDecimal(0.9))

    private var state: DataState<GasPriceInfo> = DataState.Loading

    override fun createState() = state


    private val recommendedGasPriceSingle
        get() = recommendedGasPrice?.let { Single.just(it) }
            ?: gasPriceProvider.gasPriceSingle()
                .map { it }
                .doOnSuccess { gasPrice ->
                    val adjustedGasPrice = max(gasPrice.toLong(), minRecommendedGasPrice ?: 0)
                    recommendedGasPrice = adjustedGasPrice
                }

    init {
        start()
    }

    override fun start() {
        if (initialGasPrice != null) {
            setGasPrice(initialGasPrice)
        } else {
            setRecommended()
        }
    }

    private suspend fun getRecommendedGasPriceSingle(): Long {
        recommendedGasPrice?.let {
            return it
        }

        val gasPrice = gasPriceProvider.gasPriceSingle().await()
        val adjustedGasPrice = gasPrice.coerceAtLeast(minRecommendedGasPrice ?: 0)

        recommendedGasPrice = adjustedGasPrice

        return adjustedGasPrice
    }

    override fun setRecommended() {
        setGasPriceInternal(null)
    }

    fun setGasPrice(value: Long) {
        setGasPriceInternal(value)
    }

    private fun setGasPriceInternal(value: Long?) {
        setGasPriceJob?.cancel()
        setGasPriceJob = coroutineScope.launch {
            try {
                val recommended = getRecommendedGasPriceSingle()

                val gasPriceInfo = if (value == null) {
                    GasPriceInfo(
                        gasPrice = GasPrice.Legacy(recommended),
                        gasPriceDefault = GasPrice.Legacy(recommended),
                        default = true,
                        warnings = listOf<FeeSettingsWarning>(),
                        errors = listOf()
                    )
                } else {
                    val warnings = buildList {
                        if (value < riskOfStuckBound.calculate(recommended)) {
                            add(FeeSettingsWarning.RiskOfGettingStuckLegacy)
                        }

                        if (value >= overpricingBound.calculate(recommended)) {
                            add(FeeSettingsWarning.Overpricing)
                        }
                    }

                    GasPriceInfo(
                        gasPrice = GasPrice.Legacy(value),
                        gasPriceDefault = GasPrice.Legacy(recommended),
                        default = false,
                        warnings = warnings,
                        errors = listOf()
                    )
                }

                state = DataState.Success(gasPriceInfo)
                emitState()
            } catch (e: Throwable) {
                state = DataState.Error(e)
                emitState()
            }
        }
    }

    companion object {
        /** 默认 gasPrice：100_000_000 wei（0.1 Gwei），用作初始化时的初始值 */
        const val DEFAULT_GAS_PRICE = 100_000_000L
    }
}
