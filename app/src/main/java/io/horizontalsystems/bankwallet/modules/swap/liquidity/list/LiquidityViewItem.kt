package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.NumberRounding
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.swappable
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat

@Immutable
data class LiquidityViewItem(
    val walletA: Wallet,
    val walletB: Wallet,
    val addressA: String,
    val addressB: String,
    val amountA: String,
    val amountB: String,
    val liquidity: String,
    val shareRate: String,
    val poolTokenTotalSupply: BigInteger,
    val feeTier: String? = null,       // V3 only: fee tier display like "0.05%"
    val tickRange: String? = null,     // V3 only: tick range display
    val tokenId: BigInteger? = null,   // V3 only: NFT token ID
    val isV3: Boolean = false          // true if this is a V3 position
)

data class DeemedValue<T>(val value: T)

class LiquidityViewItemFactory {

    private fun coinValue(
        balance: BigDecimal,
        full: Boolean,
        coinDecimals: Int
    ): DeemedValue<String> {
        val formatted = if (full) {
            App.numberFormatter.formatCoinFull(balance, null, coinDecimals)
        } else {
            App.numberFormatter.formatCoinShort(balance, null, coinDecimals)
        }

        return DeemedValue(formatted)
    }

    fun viewItem(
        item: LiquidityListModule.LiquidityItem,
    ): LiquidityViewItem {
        val walletA = item.walletA
        val walletB = item.walletB


        val df = DecimalFormat("##.########")
        return LiquidityViewItem(
            walletA,
            walletB,
            item.addressA,
            item.addressB,
            df.format(item.walletAmount),
            df.format(item.walletBmount),
            App.numberFormatter.formatCoinFull(BigDecimal(item.liquidity).divide(BigDecimal.TEN.pow(18), 8, RoundingMode.FLOOR), null, 18),
            df.format(item.shareRate),
            item.poolTokenTotalSupply
        )
    }

    fun viewItemV3(
        item: LiquidityListModule.V3PositionItem,
    ): LiquidityViewItem {
        val df = DecimalFormat("##.########")
        val feeTier = formatFeeTier(item.fee)
        val tickRange = formatTickRange(item.tickLower, item.tickUpper)

        return LiquidityViewItem(
            walletA = item.walletA,
            walletB = item.walletB,
            addressA = item.addressA,
            addressB = item.addressB,
            amountA = df.format(item.token0Amount),
            amountB = df.format(item.token1Amount),
            liquidity = App.numberFormatter.formatCoinFull(
                BigDecimal(item.liquidity), null, 0
            ),
            shareRate = feeTier,
            poolTokenTotalSupply = item.tokenId,
            feeTier = feeTier,
            tickRange = tickRange,
            tokenId = item.tokenId,
            isV3 = true
        )
    }

    private fun formatFeeTier(fee: BigInteger): String {
        // fee is in hundredths of a basis point: 500 = 0.05%, 3000 = 0.3%, 10000 = 1%
        val feePercent = BigDecimal(fee).divide(BigDecimal("10000"), 4, RoundingMode.HALF_UP)
        return "${feePercent.stripTrailingZeros().toPlainString()}%"
    }

    private fun formatTickRange(tickLower: BigInteger, tickUpper: BigInteger): String {
        // Tick range is display-only for now
        val lowerPrice = BigDecimal("1.0001").pow(tickLower.toInt())
        val upperPrice = BigDecimal("1.0001").pow(tickUpper.toInt())
        val df = DecimalFormat("#.#####")
        return "${df.format(lowerPrice)} - ${df.format(upperPrice)}"
    }
}
