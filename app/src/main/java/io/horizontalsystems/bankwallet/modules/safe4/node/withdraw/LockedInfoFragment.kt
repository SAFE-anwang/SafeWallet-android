package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
//import androidx.paging.LoadState
//import androidx.paging.compose.LazyPagingItems
//import androidx.paging.compose.collectAsLazyPagingItems
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawUi.WithdrawLockItem
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class LockedInfoFragment(): BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<WithdrawModule.Input>()
        val wallet = input?.wallet ?: return
        val isSuperNode = input.isSuperNode
        val viewModel by viewModels<LockedInfoViewModel> { WithdrawModule.Factory(isSuperNode, wallet) }
        WithdrawVoteScreen(navController, viewModel)
    }
}


@Composable
fun WithdrawVoteScreen(
    navController: NavController,
    viewModel: LockedInfoViewModel
) {
    val uiState = viewModel.uiState
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
            title = stringResource(id = R.string.Safe_Four_Lock),
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

                }
            ) {
                LazyColumn(modifier = Modifier.padding(it)
                    .padding(start = 16.dp, end = 16.dp), state = listState) {
                    WithdrawList(
                        lockIdsList = nodeList,
                        onWithdraw = { info ->
                            if (viewModel.hasConnection()) {
                                viewModel.check(info)
                                viewModel.showConfirmation()
                            } else {
                                HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                            }
                        },
                        onAddLockDay = {
                            navController.slideFromBottom(
                                R.id.addLockDayFragment,
                                SafeFourModule.AddLockDayInput(
                                    listOf(it),
                                    viewModel.wallet
                                )
                            )
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
            content = stringResource(viewModel.getHintText()),
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
    lockIdsList: List<WithdrawModule.WithDrawLockedInfo>,
    onWithdraw: (WithdrawModule.WithDrawLockedInfo) -> Unit,
    onAddLockDay: (Long) -> Unit,
    onBottomReached: () -> Unit,
) {
    val bottomReachedRank = getBottomReachedRank(lockIdsList)
    items(lockIdsList) {
        WithdrawLockItem(
            it.id,
            it.amount,
            it.withdrawEnable,
            it.addLockDayEnable,
            it.unlockHeight,
            it.releaseHeight,
            it.address,
            it.address2,
            {
                onWithdraw.invoke(it)
            }, { lockId ->
                onAddLockDay.invoke(it.id)
            }
        )
        if (it.id == bottomReachedRank) {
            onBottomReached.invoke()
        }
    }
}


private fun getBottomReachedRank(nodeList: List<WithdrawModule.WithDrawLockedInfo>): Long? {
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (nodeList.size > 4) nodeList.size - 4 else 0

    return nodeList.getOrNull(index)?.id
}