package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.TextButton
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HsTextButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val AppRippleTheme = RippleConfiguration(color = if (isSystemInDarkTheme()) Color.White else Color.Black,
            rippleAlpha = RippleAlpha(
                    pressedAlpha = 0.24f,
                    focusedAlpha = 0.24f,
                    draggedAlpha = 0.16f,
                    hoveredAlpha = 0.08f
            ))
    CompositionLocalProvider(LocalRippleConfiguration provides AppRippleTheme) {
        TextButton(
            onClick = onClick
        ) {
            content()
        }
    }
}