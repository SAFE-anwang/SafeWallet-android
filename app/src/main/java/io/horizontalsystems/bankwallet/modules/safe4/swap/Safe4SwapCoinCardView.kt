package io.horizontalsystems.bankwallet.modules.safe4.swap

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapAmountInputState
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapCoinCardViewState
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinFragment
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.SafeExtend.isSafeCoin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal


@Composable
fun Safe4SwapCoinCardView(
    init: String?,
    token: Token,
    enabled: Boolean,
    max: Int,
    navController: NavController,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onAmountChange: ((String) -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {

    Row(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        SwapAmountInput(
            init ?: "",
            enabled,
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp),
            focusRequester = focusRequester,
            onFocusChanged = onFocusChanged,
            onChangeAmount = {
                onAmountChange?.invoke(it)
            }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(id = R.drawable.logo_safe_24),
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            val title = token.coin.code
            Column {
                subhead1_leah(text = title)
                token.badge?.let {
                    subhead2_grey(text = it)
                }
            }
            Icon(
                modifier = Modifier.padding(start = 4.dp),
                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
    }

}

@Composable
private fun SwapAmountInput(
    init: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    onChangeAmount: (String) -> Unit,
    onFocusChanged: ((Boolean) -> Unit)?
) {
    var focused by remember { mutableStateOf(false) }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    if (!amountsEqual(init.toBigDecimalOrNull(), textState.text.toBigDecimalOrNull())) {
        if (init.isNotEmpty()) {
//            val amount = SwapMainModule.format(init)
            textState = textState.copy(text = init, selection = TextRange(init.length))
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        BasicTextField(
            modifier = Modifier
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                    onFocusChanged?.invoke(focusState.isFocused)

                    if (!focusState.isFocused) {
                        textState = textState.copy(selection = TextRange(0))
                    }
                }
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            value = textState,
            enabled = enabled,
            singleLine = true,
            onValueChange = { textFieldValue ->
                textState = textFieldValue
                onChangeAmount.invoke(textFieldValue.text)

            },
            textStyle = ColoredTextStyle(
                color = ComposeAppTheme.colors.leah,
                textStyle = ComposeAppTheme.typography.headline1,
                textAlign = TextAlign.Start
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.15f to Color.Transparent,
                0.15f to ComposeAppTheme.colors.jacob,
                0.85f to ComposeAppTheme.colors.jacob,
                0.85f to Color.Transparent,
                1.00f to Color.Transparent
            ),
            /*visualTransformation = { text ->
                if (text.isEmpty() || state.primaryPrefix == null) {
                    TransformedText(text, OffsetMapping.Identity)
                } else {
                    val out = state.primaryPrefix + text
                    val prefixOffset = state.primaryPrefix.length

                    val offsetTranslator = object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int {
                            return offset + prefixOffset
                        }

                        override fun transformedToOriginal(offset: Int): Int {
                            if (offset <= prefixOffset - 1) return prefixOffset
                            return offset - prefixOffset
                        }
                    }
                    TransformedText(AnnotatedString(out), offsetTranslator)
                }
            },*/
            /*decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textState.text.isEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = state.primaryPrefix ?: "0",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.headline1,
                            textAlign = TextAlign.Start
                        )
                    }
                    innerTextField()
                }
            }*/
        )
    }
}

private fun isValid(amount: String?, validDecimals: Int): Boolean {
    val newAmount = amount?.toBigDecimalOrNull()

    return when {
        amount.isNullOrBlank() -> true
        newAmount != null && newAmount.scale() > validDecimals -> false
        else -> true
    }
}

private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
    return when {
        amount1 == null && amount2 == null -> true
        amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
        else -> false
    }
}
