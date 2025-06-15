package io.horizontalsystems.bankwallet.modules.safe4.linelocksafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun LineLockSendSafeScreen(
    amountInputModeViewModel: AmountInputModeViewModel,
    viewModel: LineLockSendSafeViewModel,
    navController: NavController
) {
    val wallet = viewModel.wallet
    val predefinedAddress = viewModel.receiveAddress()
    val uiState = viewModel.uiState
    val addressError = viewModel.uiState.addressError

    val amountCaution = uiState.amountCaution
    val amountInputType = amountInputModeViewModel.inputType
    val view = LocalView.current

    val availableBalance = uiState.availableBalance
    val proceedEnabled = uiState.sendEnable
    val tips = uiState.tips

    val focusRequester = remember { FocusRequester() }
    var inputAmountErrorState by remember{ mutableStateOf(false) }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(id = R.string.Safe4_Line_Locked),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            body_leah(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(R.string.Safe_Four_Register_Node_Create_Mode),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(4.dp))

            Spacer(modifier = Modifier.height(12.dp))
            AvailableBalance(
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = availableBalance,
                amountInputType = AmountInputType.COIN,
                rate = viewModel.coinRate
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAmountInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                focusRequester = focusRequester,
                availableBalance = availableBalance,
                caution = amountCaution,
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                onClickHint = {
                    amountInputModeViewModel.onToggleInputType()
                },
                onValueChange = {
                    it?.let {
                        inputAmountErrorState = it.toInt() < 1
                    }
                    viewModel.onEnterAmount(it)
                },
                inputType = amountInputType,
                rate = viewModel.coinRate
            )

            if (inputAmountErrorState) {
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Vote_Amount_Error),
                    color = ComposeAppTheme.colors.redD,
                    style = ComposeAppTheme.typography.caption,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HSAddressInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = predefinedAddress?.let { Address(it) },
                tokenQuery = wallet.token.tokenQuery,
                coinCode = wallet.coin.code,
                error = addressError,
                navController = navController
            ) {
                viewModel.onEnterAddress(it)
            }

            Spacer(modifier = Modifier.height(12.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.Safe4_Every_Time_Lock_Amount))

            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = uiState.lockAmount,
                enabled = true,
                pasteEnabled = false,
                hint = "",
                maxLength = 20
            ) {
                viewModel.onEnterLockAmount(it)
            }
            Spacer(modifier = Modifier.height(8.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.Safe4_Starting_Month))

            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = uiState.startMonth,
                enabled = true,
                pasteEnabled = false,
                hint = "",
                maxLength = 20
            ) {
                viewModel.onEnterLockMonth(it)
            }
            Spacer(modifier = Modifier.height(8.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.Safe4_Interval_Month))

            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = uiState.intervalMonth,
                enabled = true,
                pasteEnabled = false,
                hint = "",
                maxLength = 20
            ) {
                viewModel.onEnterLockInterval(it)
            }
            Spacer(modifier = Modifier.height(8.dp))
            tips?.let {
                body_bran(modifier = Modifier.padding(start = 16.dp),
                    text = it)
            }

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {
                    if (viewModel.hasConnection()) {
                        viewModel.getSendData()?.let {
                            navController.slideFromRight(
                                R.id.sendLineLockConfirmationFragment,
                                SendEvmConfirmationModule.Input(it, R.id.sendSafe4LockFragment)
                            )
                        }
                    } else {
                        HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                    }
                },
                enabled = proceedEnabled
            )
        }
    }
}