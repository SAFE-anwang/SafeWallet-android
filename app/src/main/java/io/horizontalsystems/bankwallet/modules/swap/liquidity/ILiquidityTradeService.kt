package io.horizontalsystems.bankwallet.modules.swap.liquidity

import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.BigInteger

interface ILiquidityTradeService : LiquidityMainModule.ISwapTradeService {
    var tradeOptions: SwapTradeOptions
    /** Raw pool price (token1/token0 from sqrtPriceX96 without decimal adjustment), null if not fetched yet or V2 */
    val currentPoolPrice: BigDecimal? get() = null
    /** Human-readable pool price (with decimal adjustment: rawPrice * 10^(dec0-dec1) = token1/token0 in display units) */
    val currentPoolPriceHuman: BigDecimal? get() = null
    /** Whether swapData has been fetched at least once for the current pair (not stale from singleton) */
    val swapDataFetched: Boolean get() = false
    /** Convert a human-readable price to raw pool price for SDK consumption */
    fun toRawPrice(humanPrice: BigDecimal): BigDecimal = humanPrice
    @Throws
    fun transactionData(
        tokenIn: Token,
        tokenOut: Token,
        recipient: Address?,
        tokenInAmount: BigInteger,
        tokenOutAmount: BigInteger,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null,
    ): TransactionData

}