package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.vote

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import com.google.android.exoplayer2.util.Log
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
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class WithdrawVoteFragment(): BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<WithdrawModule.Input>()
        val wallet = input?.wallet ?: return
        val isSuperNode = input.isSuperNode
        val viewModel by viewModels<WithdrawVoteViewModel> { WithdrawModule.Factory(isSuperNode, wallet) }
        WithdrawVoteScreen(navController, viewModel)
    }
}


@Composable
fun WithdrawVoteScreen(
    navController: NavController,
    viewModel: WithdrawVoteViewModel
) {
    var withdrawAll by remember { mutableStateOf(false) }
    var isAll by remember { mutableStateOf(false) }

    var selectAllState by remember { mutableStateOf(false) }
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
            if (!isAll) {
                viewModel.sendResult = null
            }
        }

        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
            viewModel.sendResult = null
        }

        null -> Unit
    }

    LaunchedEffect(sendResult) {
        if (sendResult == SendResult.Sent) {
            delay(1200)
            navController.popBackStack(R.id.safe4WithdrawVoteFragment, true)
        }
    }


    Column(modifier = Modifier
        .background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(id = R.string.SAFE4_Withdraw_Locked),
            menuItems = listOf(
                /*MenuItem(
                    title = TranslatableString.ResString(
                        if (selectAllState) R.string.Menu_Item_Select_All_Cancel else R.string.Menu_Item_Select_All),
                    onClick = {
                        if (uiState.list?.isNotEmpty() == true) {
                            selectAllState = !selectAllState
                            viewModel.selectAll(selectAllState)
                        }
                    }
                )*/
                MenuItem(
                    title = TranslatableString.ResString(R.string.Withdraw_All),
                    onClick = {
                        withdrawAll = true
                        viewModel.showConfirmation()
                    },
                    enabled = uiState.enableReleaseAll
                )
            ),
            navigationIcon = {
                HsBackButton(onClick = { navController.navigateUp() })
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

                    // 添加列表结束提示
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            body_grey(
                                text = stringResource(R.string.list_footer),
                            )
                        }
                    }
                }
            }
        }
    }
    if (uiState.showConfirmDialog) {
        WithdrawConfirmationDialog(
            content = stringResource(R.string.SAFE4_Withdraw_Vote_Hint),
            {
                if (withdrawAll) {
                    isAll = true
                    viewModel.withdrawAllEnable()
                } else {
                    isAll = false
                    viewModel.withdraw()
                }
                withdrawAll = false
            }, {
                viewModel.closeDialog()
                withdrawAll = false
            }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.WithdrawList(
    lockIdsList: List<WithdrawModule.WithDrawInfo>,
    onCheckedChange: (Long, Boolean) -> Unit,
    onBottomReached: () -> Unit,
) {
    val bottomReachedRank = getBottomReachedRank(lockIdsList)
    items(lockIdsList) {
        WithdrawItem(
            it.id,
            it.amount,
            it.enable,
            it.checked,
            it.height,
            it.releaseHeight,
            it.address
        ) { lockId, checked ->
            onCheckedChange.invoke(lockId, checked)
        }
        if (it.id == bottomReachedRank) {
            onBottomReached.invoke()
        }
    }
}


private fun getBottomReachedRank(nodeList: List<WithdrawModule.WithDrawInfo>): Long? {
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (nodeList.size > 4) nodeList.size - 4 else 0

    return nodeList.getOrNull(index)?.id
}