package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
        modifier = Modifier
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange?.invoke(!checked) }
            )
            .size(24.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.checkbox_inactive_24),
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
