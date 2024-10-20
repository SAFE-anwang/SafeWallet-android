package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ButtonPrimaryCircle(
    @DrawableRes icon: Int = R.drawable.ic_arrow_down_left_24,
    contentDescription: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Dp = 50.dp
) {
    val shape = CircleShape
    HsIconButton(
        onClick = { onClick() },
        modifier = Modifier
            .size(height)
            .clip(shape)
            .background(if (enabled) {
                if (App.localStorage.currentTheme == ThemeType.Blue)
                    ComposeAppTheme.colors.tyler
                else
                    ComposeAppTheme.colors.leah
            } else ComposeAppTheme.colors.steel20),
        enabled = enabled,
        rippleColor = ComposeAppTheme.colors.claude
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = if (enabled) ComposeAppTheme.colors.claude else ComposeAppTheme.colors.grey50
        )
    }
}

@Composable
fun ButtonSecondaryCircle(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int = R.drawable.ic_arrow_down_20,
    contentDescription: String? = null,
    tint: Color = ComposeAppTheme.colors.leah,
    onClick: () -> Unit,
) {
    HsIconButton(
        onClick = onClick,
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(ComposeAppTheme.colors.steel20),
        enabled = enabled,
        rippleColor = tint
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Composable
fun ButtonThirdCircle(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        @DrawableRes icon: Int = R.drawable.ic_arrow_down_20,
        contentDescription: String? = null,
        tint: Color = ComposeAppTheme.colors.leah,
        height: Dp = 40.dp,
        onClick: () -> Unit,
) {
    HsIconButton(
        onClick = onClick,
        modifier = modifier
            .size(height)
            .clip(CircleShape)
            .background(ComposeAppTheme.colors.steel20),
        enabled = enabled,
        rippleColor = tint,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = tint
        )
    }
}
