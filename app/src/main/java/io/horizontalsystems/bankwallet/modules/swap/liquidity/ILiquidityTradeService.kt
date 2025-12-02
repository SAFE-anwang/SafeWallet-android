package io.horizontalsystems.bankwallet.modules.swap.liquidity

import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.UniversalSwapTradeData
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import java.math.BigInteger

interface ILiquidityTradeService : LiquidityMainModule.ISwapTradeService {
    var tradeOptions: SwapTradeOptions
    @Throws
    fun transactionData(
        tokenIn: Token,
        tokenOut: Token,
        recipient: Address?,
        tokenInAmount: BigInteger,
        tokenOutAmount: BigInteger
    ): TransactionData

}