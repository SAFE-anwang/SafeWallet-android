package io.horizontalsystems.bankwallet.modules.swap.liquidity.add.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.liquidity.add.AddLiquidityModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.add.AddLiquidityViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

/**
 * V3 Price Range selector component
 */
@Composable
fun PriceRangeCard(
    viewModel: AddLiquidityViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(16.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            subhead2_grey(text = stringResource(R.string.Liquidity_PriceRange))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fee Tier Selection
        subhead2_grey(text = stringResource(R.string.Liquidity_FeeTier))
        Spacer(modifier = Modifier.height(8.dp))
        FeeTierSelector(
            feeTiers = state.feeTiers,
            selected = state.selectedFeeTier,
            onSelect = { viewModel.onSelectFeeTier(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Price Range Inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PriceInput(
                label = stringResource(R.string.Liquidity_MinPrice),
                value = state.priceRange?.minPrice ?: "",
                onValueChange = { viewModel.onPriceRangeMinChange(it) },
                modifier = Modifier.weight(1f)
            )
            PriceInput(
                label = stringResource(R.string.Liquidity_MaxPrice),
                value = state.priceRange?.maxPrice ?: "",
                onValueChange = { viewModel.onPriceRangeMaxChange(it) },
                modifier = Modifier.weight(1f)
            )
        }

        // Current price hint
        state.priceRange?.currentPrice?.let { currentPrice ->
            Spacer(modifier = Modifier.height(8.dp))
            caption_grey(
                text = "Current price: $currentPrice",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeeTierSelector(
    feeTiers: List<AddLiquidityModule.FeeTier>,
    selected: AddLiquidityModule.FeeTier?,
    onSelect: (AddLiquidityModule.FeeTier) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        feeTiers.forEach { feeTier ->
            val isSelected = feeTier == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) ComposeAppTheme.colors.jacob.copy(alpha = 0.15f)
                        else ComposeAppTheme.colors.tyler
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) ComposeAppTheme.colors.jacob
                        else ComposeAppTheme.colors.steel20,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(feeTier) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = feeTier.label,
                    style = ComposeAppTheme.typography.subheadR,
                    color = if (isSelected) ComposeAppTheme.colors.jacob
                    else ComposeAppTheme.colors.grey
                )
            }
        }
    }
}

@Composable
private fun PriceInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        caption_grey(text = label)
        Spacer(modifier = Modifier.height(4.dp))
        var textValue by remember(value) { mutableStateOf(value) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.tyler)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    onValueChange(newValue)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = ComposeAppTheme.typography.subhead.copy(
                    color = ComposeAppTheme.colors.leah
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (textValue.isEmpty()) {
                            Text(
                                text = "0.0",
                                style = ComposeAppTheme.typography.subhead,
                                color = ComposeAppTheme.colors.grey50
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun caption_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.grey,
        modifier = modifier,
        textAlign = textAlign
    )
}
