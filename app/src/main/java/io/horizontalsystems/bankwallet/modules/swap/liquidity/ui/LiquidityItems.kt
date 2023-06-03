package io.horizontalsystems.bankwallet.modules.swap.liquidity.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppModule
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppViewModel
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.LiquidityViewItem
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.LiquidityViewModel
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh

@Composable
fun LiquidityItems(
    balanceViewItems: List<LiquidityViewItem>,
    viewModel: LiquidityViewModel,
    navController: NavController,
    uiState: LiquidityViewModel.LiquidityUiState
) {
    val rateAppViewModel = viewModel<RateAppViewModel>(factory = RateAppModule.Factory())
    DisposableEffect(true) {
        rateAppViewModel.onBalancePageActive()
        onDispose {
            rateAppViewModel.onBalancePageInactive()
        }
    }

    Column {
        val context = LocalContext.current

        /*when (val totalState = uiState.totalState) {
            TotalUIState.Hidden -> {
                DoubleText(
                    title = "*****",
                    body = "*****",
                    dimmed = false,
                    onClickTitle = {
                        viewModel.onBalanceClick()
                        HudHelper.vibrate(context)
                    },
                    onClickBody = {

                    }
                )
            }
            is TotalUIState.Visible -> {
                DoubleText(
                    title = totalState.currencyValueStr,
                    body = totalState.coinValueStr,
                    dimmed = totalState.dimmed,
                    onClickTitle = {
                        viewModel.onBalanceClick()
                        HudHelper.vibrate(context)
                    },
                    onClickBody = {
                        viewModel.toggleTotalType()
                        HudHelper.vibrate(context)
                    }
                )
            }
        }*/

        /*HeaderSorting(borderTop = true) {
            var showSortTypeSelectorDialog by remember { mutableStateOf(false) }

            ButtonSecondaryTransparent(
                title = stringResource(viewModel.sortType.getTitleRes()),
                iconRight = R.drawable.ic_down_arrow_20,
                onClick = {
                    showSortTypeSelectorDialog = true
                }
            )

            if (showSortTypeSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.Balance_Sort_PopupTitle),
                    items = viewModel.sortTypes.map {
                        TabItem(stringResource(it.getTitleRes()), it == viewModel.sortType, it)
                    },
                    onDismissRequest = {
                        showSortTypeSelectorDialog = false
                    },
                    onSelectItem = {
                        viewModel.sortType = it
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (accountViewItem.isWatchAccount) {
                Image(
                    painter = painterResource(R.drawable.icon_binocule_24),
                    contentDescription = "binoculars icon"
                )
            }


            Spacer(modifier = Modifier.weight(1f))

            ButtonSecondaryCircle(
                icon = R.drawable.ic_transactions,
                onClick = {
                    navController.slideFromRight(
                        R.id.transactionFragment
                    )
                }
            )

            if (accountViewItem.manageCoinsAllowed) {
                Spacer(modifier = Modifier.padding(start = 16.dp))

                ButtonSecondaryCircle(
                    icon = R.drawable.ic_manage_2,
                    onClick = {
                        navController.slideFromRight(R.id.manageWalletsFragment,
                            ManageWalletsModule.prepareParams(accountViewItem.accountType)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
        }*/

        Wallets(balanceViewItems, viewModel, navController, uiState)
    }
}


@Composable
fun Wallets(
    balanceViewItems: List<LiquidityViewItem>,
    viewModel: LiquidityViewModel,
    navController: NavController,
    uiState: LiquidityViewModel.LiquidityUiState
) {
    var revealedCardId by remember { mutableStateOf<Int?>(null) }

    /*val listState = rememberSaveable(
        accountId,
        sortType,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }*/

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(uiState.isRefreshing),
        onRefresh = {
            viewModel.onRefresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
//            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(balanceViewItems, key = { item -> item.token.hashCode() }) { item ->
                LiquidityCardSwipable(
                    viewItem = item,
                    viewModel = viewModel,
                    navController = navController,
                    revealed = revealedCardId == item.token.hashCode(),
                    onReveal = { walletHashCode ->
                        if (revealedCardId != walletHashCode) {
                            revealedCardId = walletHashCode
                        }
                    },
                    onConceal = {
                        revealedCardId = null
                    },
                )

            }
        }
    }
}


