package io.horizontalsystems.bankwallet.modules.nftv2.src721

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.safe4.src20.EditConfirmationDialog
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class SRC721ManagerFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SRC721Module.Input>()
        val wallet = input?.wallet
        if (wallet == null) {
            navController.popBackStack()
            return
        }
        val viewModel by viewModels<SRC721ManagerViewModel> { SRC721Module.Factory(wallet) }
        SRC721ManagerScreen(viewModel = viewModel, wallet = wallet, navController = navController)
    }
}

@Composable
fun SRC721ManagerScreen(
    viewModel: SRC721ManagerViewModel,
    wallet: io.horizontalsystems.bankwallet.entities.Wallet,
    navController: NavController
) {
    val uiState = viewModel.uiState
    val view = LocalView.current
    val sendResult = viewModel.sendResult

    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(view, R.string.Send_Sending, SnackbarDuration.INDEFINITE)
            viewModel.sendResult = null
        }
        is SendResult.Sent -> {
            HudHelper.showSuccessMessage(view, R.string.Send_Success, SnackbarDuration.LONG)
            viewModel.sendResult = null
        }
        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
            viewModel.sendResult = null
        }
        null -> Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            title = stringResource(id = R.string.Nft_Manager_Title),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )

        if (uiState.list.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                body_grey(text = stringResource(R.string.Nft_Manager_Empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.list, key = { it.info.address }) { item ->
                    SRC721ItemCard(
                        item = item,
                        onMint = {
                            navController.slideFromRight(
                                R.id.src721MintFragment,
                                SRC721Module.MintInput(wallet, item.info)
                            )
                        },
                        onBurn = { viewModel.showBurnDialog(item.info) },
                        onRemove = { viewModel.showRemoveDialog(item.info) },
                        onEdit = {
                            navController.slideFromRight(
                                R.id.src721EditFragment,
                                SRC721Module.EditInput(wallet, item.info)
                            )
                        }
                    )
                }
            }
        }
    }

    when (val dialog = viewModel.activeDialog) {
        is SRC721ManagerViewModel.Dialog.Burn -> {
            BurnTokenDialog(
                tokenId = viewModel.burnTokenId,
                onTokenIdChange = viewModel::onEnterBurnTokenId,
                onConfirm = { viewModel.burn(dialog.info) },
                onCancel = viewModel::dismissDialog
            )
        }
        is SRC721ManagerViewModel.Dialog.Remove -> {
            EditConfirmationDialog(
                content = stringResource(R.string.Nft_Remove_Confirm),
                onOKClick = { viewModel.remove(dialog.info) },
                onCancelClick = viewModel::dismissDialog
            )
        }
        null -> Unit
    }
}

@Composable
private fun SRC721ItemCard(
    item: SRC721ManagerItem,
    onMint: () -> Unit,
    onBurn: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            body_leah(
                modifier = Modifier.weight(1f),
                text = "${item.info.name} (${item.info.symbol})",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.info.burnable) {
                Text(
                    text = "Burnable",
                    style = ComposeAppTheme.typography.microSB,
                    color = ComposeAppTheme.colors.grey,
                    modifier = Modifier
                        .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = item.info.address.shorten(),
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.jacob,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    TextHelper.copyText(item.info.address)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                }
                .padding(horizontal = 2.dp, vertical = 1.dp)
        )

        Spacer(Modifier.height(8.dp))
        Row {
            body_grey(
                text = stringResource(
                    R.string.Nft_Minted_Count,
                    item.totalSupply ?: "--"
                )
            )
            Spacer(Modifier.width(16.dp))
            body_grey(
                text = stringResource(
                    R.string.Nft_Remain_Count,
                    item.remainSupply ?: "--"
                )
            )
        }

        Spacer(Modifier.height(12.dp))
        Row {
            ActionText(text = stringResource(R.string.Nft_Mint_Action), onClick = onMint)
            if (item.info.burnable) {
                Spacer(Modifier.width(24.dp))
                ActionText(text = stringResource(R.string.Nft_Burn_Action), onClick = onBurn)
            }
            Spacer(Modifier.weight(1f))
            ActionText(
                text = stringResource(R.string.Button_Delete),
                color = ComposeAppTheme.colors.lucian,
                onClick = onRemove
            )
        }
    }
}

@Composable
private fun ActionText(
    text: String,
    color: androidx.compose.ui.graphics.Color = ComposeAppTheme.colors.jacob,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = ComposeAppTheme.typography.subhead,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun BurnTokenDialog(
    tokenId: String,
    onTokenIdChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color = ComposeAppTheme.colors.lawrence)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            body_bran(text = stringResource(R.string.Nft_Burn_TokenId_Hint))

            Spacer(Modifier.height(12.dp))

            FormsInput(
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
                pasteEnabled = true,
                hint = "",
                initial = tokenId,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            ) {
                onTokenIdChange(it)
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ButtonPrimaryTransparent(
                    onClick = onCancel,
                    title = stringResource(R.string.Safe_Four_Proposal_Cancel)
                )
                Spacer(Modifier.width(8.dp))
                ButtonPrimaryYellow(
                    onClick = onConfirm,
                    title = stringResource(R.string.Button_Ok),
                    enabled = tokenId.isNotEmpty()
                )
            }
        }
    }
}
