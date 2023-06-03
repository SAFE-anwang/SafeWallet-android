package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import androidx.compose.runtime.Immutable
import io.horizontalsystems.marketkit.models.Token

@Immutable
data class LiquidityViewItem(
    val token: Token,
    val title: String,
    val value: String,
    val coinCode: String,
    val coinTitle: String,
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val secondaryValue: DeemedValue<String>,
    val expanded: Boolean,
)


data class DeemedValue<T>(val value: T, val dimmed: Boolean = false, val visible: Boolean = true)