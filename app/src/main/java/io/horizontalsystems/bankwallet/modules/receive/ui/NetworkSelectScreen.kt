package io.horizontalsystems.bankwallet.modules.receive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.description
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.NetworkSelectViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImageSafe
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.SafeExtend.isSafeCoin
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.launch

@Composable
fun NetworkSelectScreen(
    navController: NavController,
    activeAccount: Account,
    fullCoin: FullCoin,
    closeModule: () -> Unit,
    onSelect: (Wallet) -> Unit
) {
    val viewModel = viewModel<NetworkSelectViewModel>(
        factory = NetworkSelectViewModel.Factory(
            activeAccount,
            fullCoin
        )
    )
    val coroutineScope = rememberCoroutineScope()

    HSScaffold(
        title = stringResource(R.string.Balance_Network),
        onBack = { navController.popBackStack() },
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = closeModule
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .background(ComposeAppTheme.colors.tyler)
                    .fillMaxWidth()
            ) {
                TextBlock(
                    stringResource(R.string.Balance_NetworkSelectDescription)
                )
                VSpacer(20.dp)
            }
            viewModel.eligibleTokens.forEach { token ->
                val blockchain = token.blockchain
                NetworkCell(
                    title = blockchain.name,
                    subtitle = blockchain.description,
                    imageUrl = blockchain.type.imageUrl,
                    uid = fullCoin.coin.uid,
                    onClick = {
                        coroutineScope.launch {
                            onSelect.invoke(viewModel.getOrCreateWallet(token))
                        }
                    }
                )
                HsDivider()
            }
            VSpacer(32.dp)
        }
    }
}

@Composable
fun NetworkCell(
    title: String,
    subtitle: String,
    imageUrl: String,
    uid: String,
    onClick: (() -> Unit)? = null
) {
    CellPrimary(
        left = {
            CoinImageSafe(
                uid = uid,
                iconUrl = imageUrl,
                placeholder = R.drawable.ic_platform_placeholder_32,
            )
        },
        middle = {
            CellMiddleInfo(
                title = title.hs,
                subtitle = subtitle.hs,
            )
        },
        right = {
            CellRightNavigation()
        },
        onClick = onClick
    )
}
