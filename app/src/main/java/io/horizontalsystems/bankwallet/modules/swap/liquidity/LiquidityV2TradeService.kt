package io.horizontalsystems.bankwallet.modules.swap.liquidity

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.UniversalSwapTradeData
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapResultState
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.ExactType
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapData.UniswapData
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.liquidity.PancakeSwapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.BigInteger

class LiquidityV2TradeService(
    private val pancakeKit: PancakeSwapKit,
    private val evmKit: EthereumKit,
    private val rpcSourceHttp: RpcSource.Http
) : ILiquidityTradeService {

    private var swapDataDisposable: Disposable? = null
    private var swapData: SwapData? = null

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

    override fun stop() {
        clearDisposables()
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
            return
        }

        state = SwapResultState.Loading

        swapDataDisposable?.dispose()
        swapDataDisposable = null

        swapDataDisposable = swapDataSingle(tokenFrom, tokenTo)
            .subscribeOn(Schedulers.io())
            .subscribe({
                swapData = it
                syncTradeData(/*exactType,*/ amountFrom, amountTo, tokenFrom, tokenTo)
            }, { error ->
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
        tokenOutAmount: BigInteger
    ): TransactionData {
        return pancakeKit.transactionLiquidityData(evmKit.receiveAddress, evmKit.chain,
            uniswapToken(tokenIn), uniswapToken(tokenOut), recipient, tokenInAmount, tokenOutAmount)
    }

    private fun clearDisposables() {
        swapDataDisposable?.dispose()
        swapDataDisposable = null
    }

    private fun syncTradeData(amountFrom: BigDecimal?, amountTo: BigDecimal?, tokenFrom: Token, tokenTo: Token) {
        val swapData = swapData ?: return

        val amount = amountFrom

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            state = SwapResultState.NotReady()
            return
        }

        try {
            val tradeData = tradeData(swapData, amount, tradeOptions.tradeOptions)
            state = SwapResultState.Ready(UniswapData(tradeData))
        } catch (e: Throwable) {
            val error = when {
                e is TradeError.TradeNotFound && isEthWrapping(tokenFrom, tokenTo) -> TradeServiceError.WrapUnwrapNotAllowed
                else -> e
            }
            state = SwapResultState.NotReady(listOf(error))
        }
    }

    private fun swapDataSingle(tokenIn: Token?, tokenOut: Token?): Single<SwapData> {
        return try {
            val uniswapTokenIn = uniswapToken(tokenIn)
            val uniswapTokenOut = uniswapToken(tokenOut)

            pancakeKit.swapData(rpcSourceHttp, evmKit.chain, uniswapTokenIn, uniswapTokenOut)
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    private fun tradeData(swapData: SwapData, amount: BigDecimal, /*tradeType: TradeType,*/ tradeOptions: TradeOptions): UniversalSwapTradeData {
        val tradeData = /*when (tradeType) {
            TradeType.ExactIn -> {*/
                pancakeKit.bestTradeExactIn(swapData, amount, tradeOptions)
            /*}
            TradeType.ExactOut -> {
                pancakeKit.bestTradeExactOut(swapData, amount, tradeOptions)
            }
        }*/
        return UniversalSwapTradeData.buildFromTradeDataV2(tradeData)
    }

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
