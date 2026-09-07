package io.horizontalsystems.bankwallet.modules.nftv2.src721

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import java.math.BigDecimal
import java.math.RoundingMode

class SRC721MintFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SRC721Module.MintInput>()
        if (input == null) {
            navController.popBackStack()
            return
        }
        val viewModel by viewModels<SRC721MintViewModel> { SRC721Module.MintFactory(input) }
        SRC721MintScreen(viewModel = viewModel, navController = navController)
    }
}

@Composable
fun SRC721MintScreen(
    viewModel: SRC721MintViewModel,
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

    LaunchedEffect(sendResult) {
        if (sendResult is SendResult.Sent) {
            navController.popBackStack()
        }
    }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(id = R.string.Nft_Mint_Title),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            body_bran(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.Nft_Mint_To)
            )
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = true,
                pasteEnabled = true,
                hint = ""
            ) {
                viewModel.onAddressChange(it)
            }

            Spacer(modifier = Modifier.height(12.dp))

            body_bran(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.Nft_Mint_Amount)
            )
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = true,
                pasteEnabled = false,
                hint = "",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            ) {
                viewModel.onAmountChange(it)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isOwner) {
                body_grey(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = "Owner"
                )
            } else {
                uiState.mintPrice?.let { price ->
                    body_grey(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.Nft_Mint_Price, weiToSafe(price))
                    )
                }
                uiState.allowedAmount?.let { allowed ->
                    Spacer(modifier = Modifier.height(4.dp))
                    body_grey(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.Nft_Mint_Allowed, allowed.toString())
                    )
                }
                uiState.cost?.let { cost ->
                    Spacer(modifier = Modifier.height(4.dp))
                    body_grey(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.Nft_Mint_Cost, weiToSafe(cost))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(40.dp),
                title = stringResource(R.string.Nft_Mint_Action),
                onClick = {
                    viewModel.mint()
                },
                enabled = uiState.proceedEnabled
            )
        }
    }
}

private fun weiToSafe(wei: java.math.BigInteger): String {
    return BigDecimal(wei)
        .divide(BigDecimal.TEN.pow(18), 8, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
