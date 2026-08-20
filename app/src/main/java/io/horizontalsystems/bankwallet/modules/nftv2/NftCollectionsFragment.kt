package io.horizontalsystems.bankwallet.modules.nftv2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nftv2.ui.NftCollectionList
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class NftCollectionsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        NftCollectionsScreen(navController)
    }
}

@Composable
private fun NftCollectionsScreen(navController: NavController) {
    HSScaffold(
        title = stringResource(R.string.Balance_TabNft),
        onBack = { navController.popBackStack() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NftCollectionList(navController)
        }
    }
}
