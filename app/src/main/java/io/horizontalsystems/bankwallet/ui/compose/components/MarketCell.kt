package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.marketkit.models.Analytics.TechnicalAdvice.Advice
import io.horizontalsystems.marketkit.SafeExtend.isSafeIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketCoin(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    coinUid: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    value: String? = null,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
    advice: Advice? = null,
    isTop: Boolean = false,
    isBottom: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .combinedClickable(
                enabled = onClick != null || onLongClick != null,
                onClick = onClick ?: { },
                onLongClick = onLongClick
            )
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        CellPrimary(
            left = {
                CoinImageSafe(
                    uid = coinUid,
                    iconUrl = coinIconUrl,
                    placeholder = coinIconPlaceholder,
                    modifier = Modifier.padding(horizontal = 16.dp).size(32.dp)
                )
            },
            middle = {
                CellMiddleInfo(
                    title = title.hs,
                    badge = advice?.name?.hs,
                    subtitle = subtitle.hs,
                    subtitleBadge = label?.hs,
                )
            },
            right = {
                CellRightInfo(
                    title = value?.hs ?: "n/a".hs,
                    subtitle = marketDataValueComponent(marketDataValue)
                )
            },
        )
    }
}

@Composable
fun SignalBadge(advice: Advice) {
    val textColor = when (advice) {
        Advice.Buy -> ComposeAppTheme.colors.remus
        Advice.Sell -> ComposeAppTheme.colors.lucian
        Advice.StrongBuy -> ComposeAppTheme.colors.tyler
        Advice.StrongSell -> ComposeAppTheme.colors.tyler
        Advice.Neutral -> ComposeAppTheme.colors.leah
        else -> ComposeAppTheme.colors.jacob
    }

    val backgroundColor = when (advice) {
        Advice.Buy -> ComposeAppTheme.colors.green20
        Advice.Sell -> ComposeAppTheme.colors.red20
        Advice.StrongBuy -> ComposeAppTheme.colors.remus
        Advice.StrongSell -> ComposeAppTheme.colors.lucian
        Advice.Neutral -> ComposeAppTheme.colors.blade
        else -> ComposeAppTheme.colors.yellow20
    }

    val text = when (advice) {
        Advice.Buy -> stringResource(R.string.Coin_Analytics_Indicators_Buy)
        Advice.Sell -> stringResource(R.string.Coin_Analytics_Indicators_Sell)
        Advice.StrongBuy -> stringResource(R.string.Coin_Analytics_Indicators_StrongBuy)
        Advice.StrongSell -> stringResource(R.string.Coin_Analytics_Indicators_StrongSell)
        Advice.Neutral -> stringResource(R.string.Coin_Analytics_Indicators_Neutral)
        else -> stringResource(R.string.Coin_Analytics_Indicators_Risky)
    }

    BadgeText(
        text = text,
        textColor = textColor,
        background = backgroundColor
    )
}

@Composable
fun marketDataValueComponent(marketDataValue: MarketDataValue?): HSString {
    return when (marketDataValue) {
        is MarketDataValue.MarketCap -> marketDataValue.value.hs

        is MarketDataValue.Volume -> marketDataValue.value.hs

        is MarketDataValue.Diff -> formatValueAsDiff(marketDataValue.value).hs(
            diffColor(marketDataValue.value.raw())
        )

        null -> "---".hs
    }
}

@Preview
@Composable
fun PreviewMarketCoin() {
    ComposeAppTheme {
        MarketCoin(
            title = "ETH",
            subtitle = "Ethereum With very long name for token",
            coinUid = "safe-coin",
            coinIconUrl = "eth.png",
            coinIconPlaceholder = R.drawable.logo_ethereum_24,
            value = "$2600",
        )
    }
}
