package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.glance.layout.Alignment
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.RedeemSafe3Module
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.RedeemSafe3SelectViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.TabScreen
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawUi.WithdrawItem
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class WithdrawFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<WithdrawModule.Input>()
        val wallet = input?.wallet ?: return
        val isSuperNode = input.isSuperNode
        val viewModel by viewModels<WithdrawNodeViewModel> { WithdrawModule.Factory(isSuperNode, wallet) }
        WithDrawScreen(navController = navController, viewModel, isSuperNode, wallet)
    }
}

@Composable
fun WithDrawScreen(
    navController: NavController,
    viewModel: WithdrawNodeViewModel,
    isSuperNode: Boolean,
    wallet: Wallet,
) {

    val uiState = viewModel.uiState
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
            title = stringResource(id =
                if (isSuperNode)
                    R.string.SAFE4_Withdraw_Super_Node
                else
                    R.string.SAFE4_Withdraw_Master_Node
            ),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )

        Column(modifier = Modifier
            .background(color = ComposeAppTheme.colors.tyler)) {

            if (uiState.list == null) {
                ListEmptyView(
                    text = stringResource(R.string.SAFE4_Withdraw_Loading),
                    icon = R.drawable.ic_clock
                )
            } else {
                if (uiState.list.isEmpty()) {
                    ListEmptyView(
                        text = stringResource(R.string.SAFE4_Withdraw_No_Data),
                        icon = R.drawable.ic_no_data
                    )
                }
                Column {

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .weight(5f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        uiState.list.forEach { info ->
                            WithdrawItem(
                                info.id,
                                info.amount,
                                info.checked,
                                info.enable,
                                info.height,
                                info.address
                            ) { lockId, checked ->
                                viewModel.check(lockId)
                            }
                        }
                    }

                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .weight(0.4f)
                            .fillMaxWidth()
                            .height(40.dp),
                        title = stringResource(id = R.string.SAFE4_Withdraw),
                        enabled = uiState.enableWithdraw,
                        onClick = {
                            if (viewModel.hasConnection()) {
                                viewModel.showConfirmation()
                            } else {
                                HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                            }
                        }
                    )
                }
            }
        }

    }
    if (uiState.showConfirmDialog) {
        WithdrawConfirmationDialog(
            content = stringResource(R.string.SAFE4_Withdraw_Node_Hint,
                Translator.getString(if (isSuperNode) R.string.SAFE4_Withdraw_Node_Hint_Super else R.string.SAFE4_Withdraw_Node_Hint_Master)),
            {
                viewModel.withdraw()
            }, {
                viewModel.closeDialog()
            }
        )
    }
}

