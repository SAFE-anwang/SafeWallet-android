package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HsDivider(modifier: Modifier = Modifier) {
    Divider(
        thickness = 0.5.dp,
        color = if (App.localStorage.currentTheme == ThemeType.Blue) ComposeAppTheme.colors.dividerLine else ComposeAppTheme.colors.blade,
        modifier = modifier
    )
}
