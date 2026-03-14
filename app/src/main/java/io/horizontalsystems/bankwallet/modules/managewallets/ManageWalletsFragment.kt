package io.horizontalsystems.bankwallet.modules.managewallets

import android.os.Bundle
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.slideFromRightForResult
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.restoreconfig.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.tokenselect.SelectChainTab
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.RestoreBlockchainsFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.SafeExtend.isSafeCoin
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottom.BottomSearchBar
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTop
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTopType
import io.horizontalsystems.marketkit.models.Token
import kotlin.collections.map

class ManageWalletsFragment : BaseComposeFragment() {

    private val vmFactory by lazy {
        val input = findNavController().getInput<RestoreBlockchainsFragment.Input>()
        ManageWalletsModule.Factory(
                input?.accountType
    ) }
    private val viewModel by viewModels<ManageWalletsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }

    @Composable
    override fun GetContent(navController: NavController) {
        ManageWalletsScreen(
            navController,
            viewModel,
            restoreSettingsViewModel
        )
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ManageWalletsScreen(
    navController: NavController,
    viewModel: ManageWalletsViewModel,
    restoreSettingsViewModel: RestoreSettingsViewModel
) {
    val uiState = viewModel.uiState
    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var isSearchActive by remember { mutableStateOf(false) }

    val lazyListState = rememberSaveable(
        uiState.items.size,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    val filterBlockchains by viewModel.filterBlockchainsLiveData.observeAsState()

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            if (isSearchActive) {
                isSearchActive = false
            }
        }
    }

    restoreSettingsViewModel.openBirthdayHeightConfig?.let { token ->
        restoreSettingsViewModel.birthdayHeightConfigOpened()

        navController.slideFromRightForResult<BirthdayHeightConfig.Result>(
            resId = R.id.zcashConfigure,
            input = token
        ) {
            if (it.config != null) {
                restoreSettingsViewModel.onEnter(it.config)
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }

        stat(page = StatPage.CoinManager, event = StatEvent.Open(StatPage.BirthdayInput))
    }

    HSScaffold(
        title = stringResource(id = R.string.ManageCoins_title),
        onBack = { navController.popBackStack() },
        menuItems = if (viewModel.addTokenEnabled) {
            listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.ManageCoins_AddToken),
                    icon = R.drawable.ic_add_24,
                    onClick = {
                        navController.slideFromRight(R.id.addTokenFragment)

                        stat(
                            page = StatPage.CoinManager,
                            event = StatEvent.Open(StatPage.AddToken)
                        )
                    }
                ))
        } else {
            listOf()
        },
    ) {
        filterBlockchains?.let { filterBlockchains ->
            CellHeaderSorting(borderBottom = true) {
                var showFilterBlockchainDialog by remember { mutableStateOf(false) }
                if (showFilterBlockchainDialog) {
                    SelectorDialogCompose(
                        title = stringResource(R.string.Transactions_Filter_Blockchain),
                        items = filterBlockchains.map {
                            SelectorItem(it.item?.name ?: stringResource(R.string.Transactions_Filter_AllBlockchains), it.selected, it)
                        },
                        onDismissRequest = {
                            showFilterBlockchainDialog = false
                        },
                        onSelectItem = viewModel::onEnterFilterBlockchain
                    )
                }

                val filterBlockchain = filterBlockchains.firstOrNull { it.selected }?.item
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ButtonSecondaryTransparent(
                        title = filterBlockchain?.name
                            ?: stringResource(R.string.Transactions_Filter_AllBlockchains),
                        iconRight = R.drawable.ic_arrow_drop_down,
                        onClick = {
                            showFilterBlockchainDialog = true
                        }
                    )
                }
            }
        }
        Column {
            val tabItems: List<TabItem<SelectChainTab>> = uiState.tabs.map { chainTab ->
                TabItem(
                    title = chainTab.title,
                    selected = chainTab == uiState.selectedTab,
                    item = chainTab,
                )
            }
            if (tabItems.isNotEmpty()) {
                TabsTop(TabsTopType.Scrolled, tabItems) {
                    viewModel.onTabSelected(it)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                if (uiState.items.isEmpty()) {
                    ListEmptyView(
                        text = stringResource(R.string.Search_NotFounded),
                        icon = R.drawable.warning_filled_24
                    )
                } else {
                    LazyColumn(
                        state = lazyListState
                    ) {
                        items(uiState.items) { viewItem ->
                            CoinCell(
                                viewItem = viewItem,
                                onItemClick = {
                                    if (viewItem.enabled) {
                                        viewModel.disable(viewItem.item)

                                        stat(
                                            page = StatPage.CoinManager,
                                            event = StatEvent.DisableToken(viewItem.item)
                                        )
                                    } else {
                                        viewModel.enable(viewItem.item)

                                        stat(
                                            page = StatPage.CoinManager,
                                            event = StatEvent.EnableToken(viewItem.item)
                                        )
                                    }
                                },
                                onInfoClick = {
                                    navController.slideFromBottom(
                                        R.id.configuredTokenInfo,
                                        viewItem.item
                                    )

                                    stat(
                                        page = StatPage.CoinManager,
                                        event = StatEvent.OpenTokenInfo(viewItem.item)
                                    )
                                }
                            )
                            HsDivider()
                        }
                        item {
                            VSpacer(88.dp)
                        }
                    }
                }
                BottomSearchBar(
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    onActiveChange = { active ->
                        isSearchActive = active
                    },
                    onSearchQueryChange = { query ->
                        viewModel.updateFilter(query)
                        searchQuery = query
                    }
                )
            }
        }
    }
}

@Composable
private fun CoinCell(
    viewItem: CoinViewItem<Token>,
    onItemClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    CellPrimary(
        left = {
            /*CellLeftImage(
                painter = viewItem.imageSource.painter(),
                type = ImageType.Ellipse,
                size = 32
            )*/
            val uid = if (viewItem.isSafe4Deploy) {
                viewItem.item.tokenQuery.customCoinUid
            } else {
                viewItem.item.coin.uid
            }
            CoinImageSafe(
                uid = uid,
                painter = viewItem.imageSource.painter(),
                placeholder = R.drawable.ic_platform_placeholder_32,
                modifier = Modifier
                    .padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                    .size(32.dp)
            )
        },
        middle = {
            CellMiddleInfo(
                title = viewItem.title.hs,
                badge = viewItem.label?.hs,
                subtitle = viewItem.subtitle.hs,
            )
        },
        right = {
            CellRightControlsSwitcher(
                checked = viewItem.enabled,
                onInfoClick = if (viewItem.hasInfo) onInfoClick else null,
                onCheckedChange = { onItemClick.invoke() }
            )
        },
        onClick = onItemClick
    )
}
