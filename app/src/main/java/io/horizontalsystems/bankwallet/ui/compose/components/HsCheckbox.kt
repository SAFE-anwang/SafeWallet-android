package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.LightGrey50

@Composable
fun HsCheckbox(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = { onCheckedChange?.invoke(!checked) }
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_checkbox_frame),
            contentDescription = null,
            tint = if (App.localStorage.currentTheme == ThemeType.Blue) {
                if (enabled) ComposeAppTheme.colors.grey  else LightGrey50
            } else {
                ComposeAppTheme.colors.grey
            }
        )
        if (checked) {
            Icon(
                painter = painterResource(id = R.drawable.ic_checkbox_check),
                contentDescription = null,
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }
}



@Composable
fun HsCheckbox2(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = { onCheckedChange?.invoke(!checked) }
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_checkbox_frame),
            contentDescription = null,
            tint = if (App.localStorage.currentTheme == ThemeType.Blue) {
                if (enabled) ComposeAppTheme.colors.bran  else LightGrey50
            } else {
                if (enabled) ComposeAppTheme.colors.bran  else ComposeAppTheme.colors.grey
            }
        )
        if (checked) {
            Icon(
                painter = painterResource(id = R.drawable.ic_checkbox_check),
                contentDescription = null,
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }
}
