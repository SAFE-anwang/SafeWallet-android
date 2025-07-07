package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.proposal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawConfirmationDialog
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawModule
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawNodeViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawUi.WithdrawItem
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.LightGrey50
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class WithdrawAvailableFragment(): BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<WithdrawModule.Input>()
        val wallet = input?.wallet ?: return
        val isSuperNode = input.isSuperNode
        val viewModel by viewModels<WithdrawAvailableViewModel> { WithdrawModule.Factory(isSuperNode, wallet) }
        WithdrawVoteScreen(navController, viewModel)
    }
}


@Composable
fun WithdrawVoteScreen(
    navController: NavController,
    viewModel: WithdrawAvailableViewModel
) {
    val uiState = viewModel.uiState
    val proceedEnabled = uiState.enableWithdraw
    val nodeList = uiState.list
    val sendResult = viewModel.sendResult
    val view = LocalView.current
    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                view,
                R.string.SAFE4_Withdraw_Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        }

        SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                view,
                R.string.SAFE4_Withdraw_Send_Success,
                SnackbarDuration.LONG
            )
            viewModel.sendResult = null
        }

        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
            viewModel.sendResult = null
        }

        null -> Unit
    }
    Column(modifier = Modifier
        .background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(id = R.string.SAFE4_Withdraw_Proposal),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )
        if (nodeList.isNullOrEmpty()) {
            Column() {
                if (nodeList == null) {
                    ListEmptyView(
                        text = stringResource(R.string.Transactions_WaitForSync),
                        icon = R.drawable.ic_clock
                    )
                } else {
                    ListEmptyView(
                        text = stringResource(R.string.SAFE4_Withdraw_No_Data),
                        icon = R.drawable.ic_no_data
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            Scaffold(
                backgroundColor = ComposeAppTheme.colors.tyler,
                bottomBar = {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        title = stringResource(R.string.SAFE4_Withdraw),
                        onClick = {
                            if (viewModel.hasConnection()) {
                                viewModel.showConfirmation()
                            } else {
                                HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                            }
                        },
                        enabled = proceedEnabled
                    )
                }
            ) {
                LazyColumn(modifier = Modifier.padding(it)
                    .padding(start = 16.dp, end = 16.dp), state = listState) {
                    WithdrawList(
                        lockIdsList = nodeList,
                        onCheckedChange = { id, checked ->
                            viewModel.check(id)
                        },
                        onBottomReached = {
                            viewModel.onBottomReached()
                        }
                    )
                }
            }
        }
    }
    if (uiState.showConfirmDialog) {
        WithdrawConfirmationDialog(
            content = stringResource(R.string.SAFE4_Withdraw_Proposal_Hint),
            {
                viewModel.withdraw()
            }, {
                viewModel.closeDialog()
            }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.WithdrawList(
    lockIdsList: List<WithdrawModule.WithDrawInfo>,
    onCheckedChange: (Int, Boolean) -> Unit,
    onBottomReached: () -> Unit,
) {
    val bottomReachedRank = getBottomReachedRank(lockIdsList)
    items(lockIdsList) {
        WithdrawItem(
            it.id,
            it.amount,
            it.enable,
            it.checked,
            it.height
        ) { lockId, checked ->
            onCheckedChange.invoke(lockId, checked)
        }
        if (it.id == bottomReachedRank) {
            onBottomReached.invoke()
        }
    }
}


private fun getBottomReachedRank(nodeList: List<WithdrawModule.WithDrawInfo>): Int? {
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (nodeList.size > 4) nodeList.size - 4 else 0

    return nodeList.getOrNull(index)?.id
}