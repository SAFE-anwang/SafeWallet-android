package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.phrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.transactions.Filter
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class SelectWalletFragment: BaseFragment() {

    private val viewModel = SelectWalletViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    WalletsScreen(
                        findNavController(),
                        viewModel
                    ) {
                        select(it)
                    }
                }
            }
        }
    }

    private fun select(name: String) {
        setNavigationResult("walletName", bundleOf("name" to name))
        findNavController().popBackStack()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun WalletsScreen(
    findNavController: NavController,
    viewModel: SelectWalletViewModel,
    onSelect: (String) -> Unit
) {
    val walletItems by viewModel.walletItemsLiveData.observeAsState()
    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
    ) {
        SearchBar(
            title = stringResource(R.string.Restore_Import_Choose_Wallet),
            searchHintText = stringResource(R.string.Restore_Import_Wallet_Name_Hint),
            onClose = { findNavController.popBackStack() },
            onSearchTextChanged = { text ->
                viewModel.updateFilter(text)
            }
        )
        filterTypes?.let { filterTypes ->
            FilterTypeTabs(
                filterTypes,
                { viewModel.setFilterWalletType(it) },
                { scrollToTopAfterUpdate = true })
        }

        walletItems?.let {
            if (it.isEmpty()) {
                ListEmptyView(
                    text = stringResource(R.string.ManageCoins_NoResults),
                    icon = R.drawable.ic_not_found
                )
            } else {
                LazyColumn {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(
                            thickness = 1.dp,
                            color = if (App.localStorage.currentTheme == ThemeType.Blue) ComposeAppTheme.colors.dividerLine else ComposeAppTheme.colors.steel10,
                        )
                    }
                    items(it) { viewItem ->
                        CoinCell(
                            viewItem = viewItem,
                            onItemClick = { onSelect.invoke(viewItem.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinCell(
    viewItem: WalletInfo,
    onItemClick: () -> Unit
) {
    CellMultilineClear(
        borderBottom = true,
        onClick = onItemClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = viewItem.name,
                    color = ComposeAppTheme.colors.leah,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
                )

                val type = when(viewItem.wallet) {
                    0 -> TranslatableString.ResString(R.string.Restore_Import_Wallet_Type_Software)
                    1 -> TranslatableString.ResString(R.string.Restore_Import_Wallet_Type_Hardware)
                    2 -> TranslatableString.ResString(R.string.Restore_Import_Wallet_Type_Both)
                    else -> TranslatableString.ResString(R.string.Restore_Import_Wallet_Type_Unkonwn)
                }
                Text(
                    text = type.getString(),
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterTypeTabs(
    filterTypes: List<Filter<FilterWalletType>>,
    onWalletTypeClick: (FilterWalletType) -> Unit,
    scrollToTopAfterUpdate: () -> Unit
) {
    val tabItems = filterTypes.map {
        TabItem(stringResource(it.item.title), it.selected, it.item)
    }

    ScrollableTabs(tabItems) { walletType ->
        onWalletTypeClick.invoke(walletType)
        scrollToTopAfterUpdate.invoke()
    }
}