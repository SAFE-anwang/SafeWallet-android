package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.safe4.src20.SyncSafe4Tokens
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.marketkit.SafeExtend.isSafeCoin
import java.math.BigDecimal
import kotlin.math.log

@Composable
fun RateColor(diff: BigDecimal?) =
    if ((diff ?: BigDecimal.ZERO) >= BigDecimal.ZERO) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian

@Composable
fun diffColor(value: BigDecimal) =
    if (value.signum() >= 0) {
        ComposeAppTheme.colors.remus
    } else {
        ComposeAppTheme.colors.lucian
    }

@Composable
fun formatValueAsDiff(value: Value): String =
    App.numberFormatter.formatValueAsDiff(value)

@Composable
fun RateText(diff: BigDecimal?): String {
    if (diff == null) return ""
    val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
    return App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
}

@Composable
fun CoinImage(
    iconUrl: String?,
    placeholder: Int? = null,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) {
    val fallback = placeholder ?: R.drawable.coin_placeholder
    when {
        iconUrl != null -> Image(
            painter = rememberAsyncImagePainter(
                model = iconUrl,
                error = painterResource(fallback),
                onError = {

                }
            ),
            contentDescription = null,
            modifier = modifier
                .background(Color.White, CircleShape)
                .clip(CircleShape),
            colorFilter = colorFilter,
            contentScale = ContentScale.FillBounds,
        )
        else -> Image(
            painter = painterResource(fallback),
            contentDescription = null,
            modifier = modifier,
            colorFilter = colorFilter
        )
    }
}

@Composable
fun CoinImageSafe(
    uid: String,
    iconUrl: String?,
    placeholder: Int? = null,
    colorFilter: ColorFilter? = null,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    if (uid.isSafeCoin()) {
        val logo = SyncSafe4Tokens.getLogo(uid)
        if (!logo.isNullOrEmpty()) {
            CoinImage(
                iconUrl = logo,
                placeholder = placeholder,
                modifier = modifier
                    .size(size),
                colorFilter = colorFilter
            )
        } else if (iconUrl?.isNotEmpty() == true && !iconUrl.endsWith("/safe4-coin@3x.png") && !iconUrl.endsWith("/safe-coin@3x.png")) {
            CoinImage(
                iconUrl = iconUrl,
                placeholder = placeholder,
                modifier = modifier
                    .size(size).clip(CircleShape),
                colorFilter = colorFilter
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo_safe_24),
                contentDescription = null,
                modifier = modifier
                    .size(size),
                colorFilter = colorFilter
            )
        }
    } else {
        CoinImage(
            iconUrl = iconUrl,
            placeholder = placeholder,
            modifier = Modifier
                .size(size),
            colorFilter = colorFilter,
        )
    }
}

@Composable
fun CoinImageSafe(
    uid: String,
    painter: Painter,
    placeholder: Int? = null,
    colorFilter: ColorFilter? = null,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    if (uid.isSafeCoin()) {
        val logo = SyncSafe4Tokens.getLogo(uid)
        if (!logo.isNullOrEmpty()) {
            CoinImage(
                iconUrl = logo,
                placeholder = placeholder,
                modifier = modifier
                    .size(size),
                colorFilter = colorFilter
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo_safe_24),
                contentDescription = null,
                modifier = modifier
                    .size(size),
                colorFilter = colorFilter
            )
        }
    } else {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier
                .size(size),
            colorFilter = colorFilter
        )
    }
}

@Composable
fun NftIcon(
    modifier: Modifier = Modifier,
    iconUrl: String?,
    placeholder: Int? = null,
    colorFilter: ColorFilter? = null
) {
    val fallback = placeholder ?: R.drawable.ic_platform_placeholder_24
    when {
        iconUrl != null -> Image(
            painter = rememberAsyncImagePainter(
                model = iconUrl,
                error = painterResource(fallback)
            ),
            contentDescription = null,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .size(32.dp),
            colorFilter = colorFilter,
            contentScale = ContentScale.Crop
        )
        else -> Image(
            painter = painterResource(fallback),
            contentDescription = null,
            modifier = modifier.size(32.dp),
            colorFilter = colorFilter
        )
    }
}