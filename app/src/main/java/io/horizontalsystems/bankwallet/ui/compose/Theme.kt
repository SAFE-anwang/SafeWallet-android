package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.theme.ThemeType


val lightPalette = Colors(
    jacob = YellowL,
    remus = GreenL,
    lucian = RedL,
    tyler = Light,
    bran = Dark,
    leah = SteelDark,
    claude = Color.White,
    lawrence = Color.White,
    jeremy = SteelLight,
    laguna = LagunaL,
    purple = PurpleL,
    raina = White50,
    andy = Steel20,
)

val darkPalette = Colors(
    jacob = YellowD,
    remus = GreenD,
    lucian = RedD,
    tyler = Dark,
    bran = LightGrey,
    leah = SteelLight,
    claude = Dark,
    lawrence = SteelDark,
    jeremy = Steel20,
    laguna = LagunaD,
    purple = PurpleD,
    raina = Steel10,
    andy = Black50,
)

val bluePalette = Colors(
    jacob = YellowL,
    remus = GreenL,
    lucian = RedL,
    tyler = Blue,
    bran = Color.Black,
    leah = Color.Black,
    claude = Color.Black,
    lawrence = Color.White,
    jeremy = SteelLight,
    laguna = LagunaL,
    purple = PurpleD,
    raina = White50,
    andy = Black50
)

@Composable
fun ComposeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {

    val colors = if (App.localStorage.currentTheme == ThemeType.Blue) bluePalette
        else {
            if (darkTheme) {
                darkPalette
            } else {
                lightPalette
            }
        }

    //custom styles
    ProvideLocalAssets(colors = colors, typography = Typography()) {
        //material styles
        MaterialTheme(
            content = content
        )
    }

}

/*object AppRippleTheme : RippleTheme {
    // Here you should return the ripple color you want
    // and not use the defaultRippleColor extension on RippleTheme.
    // Using that will override the ripple color set in DarkMode
    // or when you set light parameter to false
    @Composable
    override fun defaultColor(): Color = RippleTheme.defaultRippleColor(
        if (isSystemInDarkTheme()) Color.White else Color.Black,
        lightTheme = true
    )

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
        Color.Black,
        lightTheme = true
    )
}*/

object ComposeAppTheme {
    val colors: Colors
        @Composable
        get() = LocalColors.current

    val typography: Typography
        @Composable
        get() = LocalTypography.current
}

@Composable
fun ProvideLocalAssets(
    colors: Colors,
    typography: Typography,
    content: @Composable () -> Unit
) {

    val colorPalette = remember {
        // Explicitly creating a new object here so we don't mutate the initial [colors]
        // provided, and overwrite the values set in it.
        colors.copy()
    }
    colorPalette.update(colors)
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalColors provides colorPalette,
        LocalTypography provides typography,
        LocalDensity provides Density(currentDensity.density, fontScale = 1f),
        content = content
    )
}

val LocalColors = compositionLocalOf<Colors> {
//    error("No Colors provided")
    lightPalette
}
