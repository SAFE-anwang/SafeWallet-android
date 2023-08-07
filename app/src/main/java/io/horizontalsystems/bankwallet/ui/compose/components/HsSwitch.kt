package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
){
    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
        Switch(
            modifier = modifier,
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ComposeAppTheme.colors.white,
                uncheckedThumbColor = ComposeAppTheme.colors.lightGrey,
                checkedTrackColor = ComposeAppTheme.colors.yellowD,
                uncheckedTrackColor = if (App.localStorage.currentTheme == ThemeType.Blue) ComposeAppTheme.colors.raina else ComposeAppTheme.colors.elenaD,
                checkedTrackAlpha = 1f,
                uncheckedTrackAlpha = 0.2f,
            ),
            enabled = enabled
        )
    }
}
