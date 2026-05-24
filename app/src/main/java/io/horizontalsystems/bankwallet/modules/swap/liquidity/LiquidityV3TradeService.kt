package io.horizontalsystems.bankwallet.modules.swap.liquidity

import android.util.Log
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ExactType
import io.horizontalsystems.bankwallet.modules.swap.UniversalSwapTradeData
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapResultState
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapData.UniswapData
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.liquidity.PancakeSwapKit
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.v3.FeeAmount
import io.horizontalsystems.uniswapkit.v3.pool.PoolManager
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class LiquidityV3TradeService(
    private val pancakeKit: PancakeSwapKit,
    private val evmKit: EthereumKit,
    private val rpcSourceHttp: RpcSource.Http
) : ILiquidityTradeService {

    private var swapDataDisposable: Disposable? = null
    private var swapData: SwapData? = null
    private var currentSqrtPriceX96: BigInteger = BigInteger.ZERO
    private var poolToken0Decimals: Int = 0
    private var poolToken1Decimals: Int = 0
    private var currentPoolFee: FeeAmount = FeeAmount.MEDIUM_PANCAKESWAP

    override val swapDataFetched: Boolean
        get() = currentSqrtPriceX96 > BigInteger.ZERO

    override var state: SwapResultState = SwapResultState.NotReady()
        private set(value) {
            field = value
            _stateFlow.update { value }
        }

    override val recipient: Address?
        get() = tradeOptions.recipient
    override val slippage: BigDecimal
        get() = tradeOptions.allowedSlippage
    override val ttl: Long
        get() = tradeOptions.ttl

    private val _stateFlow = MutableStateFlow(state)
    override val stateFlow: StateFlow<SwapResultState>
        get() = _stateFlow

    override var tradeOptions: SwapTradeOptions = SwapTradeOptions()
        set(value) {
            field = value
        }

    override val currentPoolPrice: BigDecimal?
        get() {
            if (currentSqrtPriceX96 == BigInteger.ZERO) return null
            val q96 = BigDecimal(BigInteger.ONE.shiftLeft(96))
            val sqrtPrice = BigDecimal(currentSqrtPriceX96).divide(q96, 18, RoundingMode.HALF_UP)
            if (sqrtPrice <= BigDecimal.ZERO) return null
            return sqrtPrice.multiply(sqrtPrice)
        }

    override val currentPoolPriceHuman: BigDecimal?
        get() {
            val rawPrice = currentPoolPrice ?: return null
            if (poolToken0Decimals == 0 && poolToken1Decimals == 0) return null
            val factor = computeConversionFactor()
            return rawPrice.multiply(factor)
        }

    override fun toRawPrice(humanPrice: BigDecimal): BigDecimal {
        val factor = computeConversionFactor()
        return humanPrice.divide(factor, 18, RoundingMode.HALF_UP)
    }

    private fun computeConversionFactor(): BigDecimal {
        val exp = poolToken0Decimals - poolToken1Decimals
        return if (exp >= 0) BigDecimal.TEN.pow(exp)
        else BigDecimal.ONE.divide(BigDecimal.TEN.pow(-exp), 18, RoundingMode.HALF_UP)
    }

    override fun stop() {
        clearDisposables()
        swapData = null
        currentSqrtPriceX96 = BigInteger.ZERO
        poolToken0Decimals = 0
        poolToken1Decimals = 0
        currentPoolFee = FeeAmount.MEDIUM_PANCAKESWAP
    }

    override fun fetchSwapData(
        tokenFrom: Token?,
        tokenTo: Token?,
        amountFrom: BigDecimal?,
        amountTo: BigDecimal?,
        exactType: ExactType
    ) {
        if (tokenFrom == null || tokenTo == null) {
            state = SwapResultState.NotReady()
            swapData = null
            currentSqrtPriceX96 = BigInteger.ZERO
            poolToken0Decimals = 0
            poolToken1Decimals = 0
            currentPoolFee = FeeAmount.MEDIUM_PANCAKESWAP
            return
        }

        state = SwapResultState.Loading

        swapDataDisposable?.dispose()
        swapDataDisposable = null

        swapDataDisposable = fetchPoolPrice(tokenFrom, tokenTo)
            .subscribeOn(Schedulers.io())
            .subscribe({ result ->
                currentSqrtPriceX96 = result.sqrtPriceX96
                poolToken0Decimals = result.token0Decimals
                poolToken1Decimals = result.token1Decimals
                currentPoolFee = result.fee
                val isInverted = result.isInverted

                // Convert raw pool price to human-readable price for amount calculation
                // amounts from ViewModel are human-readable (not raw with decimals)
                val rawPrice = result.price
                val humanPrice = if (rawPrice != null) {
                    rawPrice.multiply(computeConversionFactor())
                } else null

                val amount = if (exactType == ExactType.ExactFrom) amountFrom else amountTo

                if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0 || humanPrice == null || humanPrice <= BigDecimal.ZERO) {
                    state = SwapResultState.NotReady()
                    return@subscribe
                }

                val (amountIn, amountOut, executionPrice) = if (exactType == ExactType.ExactFrom) {
                    val out = if (isInverted) {
                        amount.divide(humanPrice, tokenTo.decimals, RoundingMode.HALF_UP)
                    } else {
                        amount.multiply(humanPrice).setScale(tokenTo.decimals, RoundingMode.HALF_UP)
                    }
                    Triple(amount, out, humanPrice)
                } else {
                    val inn = if (isInverted) {
                        amount.multiply(humanPrice).setScale(tokenFrom.decimals, RoundingMode.HALF_UP)
                    } else {
                        amount.divide(humanPrice, tokenFrom.decimals, RoundingMode.HALF_UP)
                    }
                    Triple(inn, amount, humanPrice)
                }

                val tradeData = UniversalSwapTradeData(
                    amountIn = amountIn,
                    amountOut = amountOut,
                    executionPrice = executionPrice,
                    priceImpact = null
                )
                state = SwapResultState.Ready(UniswapData(tradeData))
            }, { error ->
                Log.e("LiquidityV3TradeService", "fetchSwapData error: ${error.message}")
                state = SwapResultState.NotReady(listOf(error))
            })
    }

    override fun updateSwapSettings(recipient: Address?, slippage: BigDecimal?, ttl: Long?) {
        tradeOptions = SwapTradeOptions(
            slippage ?: TradeOptions.defaultAllowedSlippage,
            ttl ?: TradeOptions.defaultTtl,
            recipient
        )
    }

    @Throws
    override fun transactionData(
        tokenIn: Token,
        tokenOut: Token,
        recipient: io.horizontalsystems.ethereumkit.models.Address?,
        tokenInAmount: BigInteger,
        tokenOutAmount: BigInteger,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
    ): TransactionData {
        return pancakeKit.transactionLiquidityV3Data(
            evmKit.receiveAddress, evmKit.chain,
            uniswapToken(tokenIn), uniswapToken(tokenOut), recipient,
            tokenInAmount, tokenOutAmount,
            currentSqrtPriceX96,
            minPrice, maxPrice,
            currentPoolFee
        )
    }

    private fun clearDisposables() {
        swapDataDisposable?.dispose()
        swapDataDisposable = null
    }

    /**
     * Fetches pool price by trying each available fee tier for the given dex type.
     * For V3 pools, [PoolManager.getSqrtPriceX96] is used instead of the
     * V2 [getReserves] method, which is not supported on V3 pool contracts.
     */
    private fun fetchPoolPrice(tokenFrom: Token, tokenTo: Token): Single<PoolPriceResult> {
        return Single.fromCallable {
            val dexType = if (evmKit.chain == Chain.BinanceSmartChain) DexType.PancakeSwap else DexType.Uniswap
            val poolManager = PoolManager(dexType)
            val uniswapTokenIn = uniswapToken(tokenFrom)
            val uniswapTokenOut = uniswapToken(tokenTo)

            var sqrtPriceX96 = BigInteger.ZERO
            var foundFee: FeeAmount? = null
            for (fee in FeeAmount.sorted(dexType)) {
                try {
                    sqrtPriceX96 = kotlinx.coroutines.rx2.rxSingle {
                        poolManager.getSqrtPriceX96(rpcSourceHttp, evmKit.chain, uniswapTokenIn.address, uniswapTokenOut.address, fee)
                    }.blockingGet()
                    if (sqrtPriceX96 > BigInteger.ZERO) {
                        foundFee = fee
                        Log.d("LiquidityV3TradeService", "Found pool at fee=${fee.value} tickSpacing=${fee.tickSpacing} sqrtPriceX96=$sqrtPriceX96")
                        break
                    }
                } catch (e: Exception) {
                    Log.d("LiquidityV3TradeService", "No pool at fee=${fee.value}: ${e.message}")
                }
            }

            if (sqrtPriceX96 <= BigInteger.ZERO) {
                throw Exception("No V3 pool found for ${tokenFrom.coin.code}/${tokenTo.coin.code}")
            }

            val q96 = BigDecimal(BigInteger.ONE.shiftLeft(96))
            val sqrtPrice = BigDecimal(sqrtPriceX96).divide(q96, 18, RoundingMode.HALF_UP)
            val price = sqrtPrice.multiply(sqrtPrice)

            // tokenIn is token1: price is token1/token0, so actual price for tokenInInTokenOut needs adjustment
            val token0 = if (uniswapTokenIn.sortsBefore(uniswapTokenOut)) uniswapTokenIn else uniswapTokenOut
            val token1 = if (uniswapTokenIn.sortsBefore(uniswapTokenOut)) uniswapTokenOut else uniswapTokenIn
            val isInverted = uniswapTokenIn != token0

            PoolPriceResult(sqrtPriceX96, price, isInverted, token0.decimals, token1.decimals, foundFee!!)
        }
    }

    private data class PoolPriceResult(
        val sqrtPriceX96: BigInteger,
        val price: BigDecimal?,
        val isInverted: Boolean,
        val token0Decimals: Int,
        val token1Decimals: Int,
        val fee: FeeAmount
    )

    @Throws
    private fun uniswapToken(token: Token?) = when (val tokenType = token?.type) {
        TokenType.Native -> when (token.blockchainType) {
            BlockchainType.SafeFour,
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne -> pancakeKit.etherToken(evmKit.chain)
            else -> throw Exception("Invalid coin for swap: $token")
        }
        is TokenType.Eip20 -> pancakeKit.token(
            io.horizontalsystems.ethereumkit.models.Address(
                tokenType.address
            ), token.decimals)
        else -> throw Exception("Invalid coin for swap: $token")
    }

    private val TokenType.isWeth: Boolean
        get() = this is TokenType.Eip20 && address.equals(pancakeKit.etherToken(evmKit.chain).address.hex, true)
    private val Token.isWeth: Boolean
        get() = type.isWeth
    private val Token.isNative: Boolean
        get() = type == TokenType.Native

    private fun isEthWrapping(tokenFrom: Token?, tokenTo: Token?) =
        when {
            tokenFrom == null || tokenTo == null -> false
            else -> {
                tokenFrom.isNative && tokenTo.isWeth || tokenTo.isNative && tokenFrom.isWeth
            }
        }

    sealed class TradeServiceError : Throwable() {
        object WrapUnwrapNotAllowed : TradeServiceError()
    }

}
