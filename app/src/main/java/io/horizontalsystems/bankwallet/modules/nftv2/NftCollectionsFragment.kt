package io.horizontalsystems.bankwallet.modules.nftv2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nftv2.ui.NftCollectionList
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.TabButtonSecondaryTransparent

class NftCollectionsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        NftCollectionsScreen(navController)
    }
}

@Composable
private fun NftCollectionsScreen(navController: NavController) {
    val viewModel: NftCollectionListViewModel = viewModel(factory = NftCollectionListViewModel.Factory())
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Balance_TabNft),
        onBack = { navController.popBackStack() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab 栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ComposeAppTheme.colors.tyler)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabButtonSecondaryTransparent(
                    title = stringResource(R.string.Nft_Tab_All),
                    selected = uiState.tab == NftListTab.All,
                    onSelect = { viewModel.onTabChange(NftListTab.All) }
                )
                TabButtonSecondaryTransparent(
                    title = stringResource(R.string.Nft_Tab_Favorites),
                    selected = uiState.tab == NftListTab.Favorites,
                    onSelect = { viewModel.onTabChange(NftListTab.Favorites) }
                )
            }

            NftCollectionList(navController, viewModel)
        }
    }
}
