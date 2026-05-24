package io.horizontalsystems.bankwallet.modules.swap.liquidity.add.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.*
import io.horizontalsystems.bankwallet.modules.swap.liquidity.add.AddLiquidityModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.add.AddLiquidityViewModel
import io.horizontalsystems.bankwallet.modules.swap.ui.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Keyboard
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.compose.observeKeyboardState
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddLiquidityScreen(
    navController: NavController,
    viewModel: AddLiquidityViewModel,
    onTapRevoke1: () -> Unit,
    onTapApprove1: () -> Unit,
    onTapRevoke2: () -> Unit,
    onTapApprove2: () -> Unit,
    onTapProceed: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val providerViewItems = viewModel.state.providerViewItems
    val state = viewModel.state

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = modalBottomSheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                BottomSheetProviderSelector(
                    items = providerViewItems,
                    onSelect = { viewModel.setProvider(it) }
                ) {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }
                }
            },
        ) {
            Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                AppBar(
                    title = stringResource(R.string.liquidity_add_title),
                    navigationIcon = {
                        HsBackButton(onClick = navController::popBackStack)
                    },
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    TopMenu(
                        state = state,
                        viewModel = viewModel,
                        showProviderSelector = {
                            coroutineScope.launch { modalBottomSheetState.show() }
                        },
                    )
                    AddLiquidityCards(
                        navController = navController,
                        viewModel = viewModel,
                        onTapRevoke1 = onTapRevoke1,
                        onTapApprove1 = onTapApprove1,
                        onTapRevoke2 = onTapRevoke2,
                        onTapApprove2 = onTapApprove2,
                        onTapProceed = onTapProceed,
                    )
                }
            }
        }
    }
}

@Composable
fun AddLiquidityCards(
    navController: NavController,
    viewModel: AddLiquidityViewModel,
    onTapRevoke1: () -> Unit,
    onTapApprove1: () -> Unit,
    onTapRevoke2: () -> Unit,
    onTapApprove2: () -> Unit,
    onTapProceed: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardState by observeKeyboardState()
    var showSuggestions by remember { mutableStateOf(false) }

    val state = viewModel.state
    val tokenAState = state.tokenAState
    val tokenBState = state.tokenBState
    val swapError = state.error
    val buttons = state.buttons
    val hasNonZeroBalance = state.hasNonZeroBalance

    LaunchedEffect(state.refocusKey) {
        focusRequester.requestFocus()
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)

            // Token selection cards (same style as original LiquidityMainFragment)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                // Version badge
                VersionBadge(
                    version = state.version,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )

                SwapCoinCardView(
                    dex = state.dex,
                    cardState = tokenAState,
                    navController = navController,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    focusRequester = focusRequester,
                    onCoinSelect = { viewModel.onSelectFromCoin(it) },
                    onAmountChange = { viewModel.onFromAmountChange(it) },
                ) { isFocused ->
                    showSuggestions = isFocused
                }

                VSpacer(8.dp)
                SwitchCoinsSection { viewModel.onTapSwitch() }
                VSpacer(8.dp)

                SwapCoinCardView(
                    dex = state.dex,
                    cardState = tokenBState,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp),
                    navController = navController,
                    onCoinSelect = { viewModel.onSelectToCoin(it) },
                    onAmountChange = { viewModel.onToAmountChange(it) },
                )
            }

            // V3 Price Range Card
            if (state.showPriceRange) {
                Spacer(modifier = Modifier.height(12.dp))
                PriceRangeCard(
                    viewModel = viewModel,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Error display
            if (swapError != null) {
                SwapError(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    text = swapError
                )
            } else {
                // Available balance info
                val infoItems = mutableListOf<@Composable () -> Unit>()

                if (infoItems.isEmpty()) {
                    state.availableBalance?.let {
                        infoItems.add { AvailableBalance(it) }
                    }
                }

                if (infoItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleLineGroup(infoItems)
                }
            }

            // Approve warnings (revoke info)
            if (buttons.revoke1 is SwapMainModule.SwapActionState.Enabled && buttons.revoke1 != SwapMainModule.SwapActionState.Hidden) {
                Spacer(modifier = Modifier.height(12.dp))
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.Approve_RevokeAndApproveInfo, "" /*allowance info*/)
                )
            }

            if (buttons.revoke2 is SwapMainModule.SwapActionState.Enabled && buttons.revoke2 != SwapMainModule.SwapActionState.Hidden) {
                Spacer(modifier = Modifier.height(12.dp))
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.Approve_RevokeAndApproveInfo, "")
                )
            }

            VSpacer(32.dp)

            // Action buttons
            ActionButtons2(
                buttons = buttons,
                onTapRevoke1 = onTapRevoke1,
                onTapApprove1 = onTapApprove1,
                onTapRevoke2 = onTapRevoke2,
                onTapApprove2 = onTapApprove2,
                onTapProceed = onTapProceed,
            )
        }

        VSpacer(32.dp)

        // Suggestions bar for quick percentage selection
        if (showSuggestions && keyboardState == Keyboard.Opened) {
            SuggestionsBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                onDelete = {
                    viewModel.onFromAmountChange(null)
                },
                onSelect = {
                    focusManager.clearFocus()
                    viewModel.onSetAmountInBalancePercent(it)
                },
                selectEnabled = hasNonZeroBalance ?: false,
                deleteEnabled = tokenAState.inputState.amount.isNotBlank()
            )
        }
    }
}

@Composable
private fun VersionBadge(
    version: AddLiquidityModule.Version,
    modifier: Modifier = Modifier
) {
    val label = when (version) {
        AddLiquidityModule.Version.V2 -> "V2"
        AddLiquidityModule.Version.V3 -> "V3"
    }
    val color = when (version) {
        AddLiquidityModule.Version.V2 -> ComposeAppTheme.colors.jacob
        AddLiquidityModule.Version.V3 -> ComposeAppTheme.colors.remus
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = ComposeAppTheme.typography.micro,
            color = color
        )
    }
}

@Composable
private fun TopMenu(
    state: AddLiquidityModule.AddLiquidityState,
    viewModel: AddLiquidityViewModel,
    showProviderSelector: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            ButtonSecondaryTransparent(
                title = state.dex.provider.title,
                iconRight = R.drawable.ic_down_arrow_20,
                onClick = showProviderSelector
            )
        }
        ButtonSecondaryToggle(
            modifier = Modifier.padding(end = 16.dp),
            select = state.amountTypeSelect,
            onSelect = {
                viewModel.onToggleAmountType()
            },
            enabled = state.amountTypeSelectEnabled
        )
    }
}

@Composable
private fun BottomSheetProviderSelector(
    items: List<LiquidityMainModule.ProviderViewItem>,
    onSelect: (SwapMainModule.ISwapProvider) -> Unit,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_swap_24),
        title = stringResource(R.string.Swap_SelectSwapProvider_Title),
        onCloseClick = onCloseClick,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(items, showFrame = true) { item ->
            RowUniversal(
                onClick = {
                    onSelect.invoke(item.provider)
                    onCloseClick.invoke()
                },
            ) {
                Image(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(32.dp),
                    painter = painterResource(
                        id = getDrawableResource(item.provider.id, context)
                            ?: R.drawable.coin_placeholder
                    ),
                    contentDescription = null
                )
                body_leah(
                    modifier = Modifier.weight(1f),
                    text = item.provider.title
                )
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.selected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_checkmark_20),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

private fun getDrawableResource(name: String, context: android.content.Context): Int? {
    val resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
    return if (resourceId == 0) null else resourceId
}
