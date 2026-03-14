package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.TextButton
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.horizontalsystems.bankwallet.ui.compose.MyRippleConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HsTextButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalRippleConfiguration provides MyRippleConfiguration) {
        TextButton(
            onClick = onClick
        ) {
            content()
        }
    }
}