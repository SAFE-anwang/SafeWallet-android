package io.horizontalsystems.bankwallet.modules.txsource

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BtcBlockchainSettingsModule.BlockchainSettingsIcon
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class TransactionSourceSettingsFragment: BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        EvmNetworkScreen(navController)
    }

}

@Composable
private fun EvmNetworkScreen(
    navController: NavController,
) {
    val viewModel = viewModel<EvmTransactionViewModel>(
        factory = EvmTransactionSourceModule.Factory()
    )
    if (viewModel.closeScreen) {
        navController.popBackStack()
    }

    HSScaffold(
        title = stringResource(R.string.TransactionSourceSettings_Title),
        onBack = navController::popBackStack,
    ) {
        Column {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 数据源选择：全局生效，适用于所有链
                item {
                    CellUniversalLawrenceSection(viewModel.viewState.defaultItems) { item ->
                        TransactionSettingCell(item.name, item.selected, null) {
                            viewModel.onSelectSyncSource(item.id)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(36.dp))
                    ButtonsGroupWithShade {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.Button_Save),
                            enabled = viewModel.viewState.saveButtonEnabled,
                            onClick = { viewModel.save() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionSettingCell(
    title: Int,
    checked: Boolean,
    icon: BlockchainSettingsIcon?,
    isSafe: Boolean = false,
    onClick: () -> Unit
) {
    RowUniversal(
        onClick = onClick
    ) {
        icon?.let {
            HSpacer(width = 16.dp)
            Image(
                modifier = Modifier
                    .size(32.dp),
                painter = when (icon) {
                    is BlockchainSettingsIcon.ApiIcon -> painterResource(icon.resId)
                    is BlockchainSettingsIcon.BlockchainIcon -> if (isSafe) {
                        painterResource(R.drawable.logo_safe_24)
                    } else {
                        rememberAsyncImagePainter(
                            model = icon.url,
                            error = painterResource(R.drawable.ic_platform_placeholder_32)
                        )
                    }
                },
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            body_leah(
                text = stringResource(title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }
    }
}
