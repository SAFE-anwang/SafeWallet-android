package io.horizontalsystems.bankwallet.modules.swap.liquidity

import android.util.Log
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.GetReserves
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.PairAddress
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Token
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.TokenAmount
import org.web3j.protocol.Web3j

object LiquidityPair {

    @Throws(Exception::class)
    fun getPairReservesForPancakeSwap(web3j: Web3j?, tokenA: Token, tokenB: Token): Array<Any>? {
        val pairAddress = PairAddress.getPairAddressForPancakeSwap(tokenA.address, tokenB.address)
        Log.i(
            "getPairReservesForPancakeSwap",
            "${tokenA.symbol} ${tokenB.symbol} ${pairAddress}"
        )
        val reserves = GetReserves.getReserves(web3j, pairAddress)
        val r0 = reserves[0]
        val r1 = reserves[1]
        val sortedTokenAddresses = PairAddress.sort(tokenA.address, tokenB.address)
        val token0 = if (sortedTokenAddresses[0] == tokenA.address) tokenA else tokenB
        val token1 = if (sortedTokenAddresses[1] == tokenB.address) tokenB else tokenA
        Log.i(
            "R0:{} ({})",
            "${TokenAmount.toBigDecimal(token0, r0, token0.decimals)} ${token0.symbol}"
        )
        Log.i(
            "R1:{} ({})",
            "${TokenAmount.toBigDecimal(token1, r1, token1.decimals)} ${token1.symbol}"
        )
        return arrayOf(
            pairAddress,
            token0, token1,
            r0, r1
        )
    }

}