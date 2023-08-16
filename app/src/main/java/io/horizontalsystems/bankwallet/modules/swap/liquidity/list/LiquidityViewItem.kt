package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
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
    val poolTokenTotalSupply: BigInteger
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
            BigDecimal(item.liquidity).divide(BigDecimal.TEN.pow(18), 5, RoundingMode.DOWN).toString(),
            df.format(item.shareRate),
            item.poolTokenTotalSupply
        )
    }
}
