package io.horizontalsystems.bankwallet.modules.nftv2.src721

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.safe4.src20.DeployConfirmationDialog
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

class SRC721EditFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SRC721Module.EditInput>()
        if (input == null) {
            navController.popBackStack()
            return
        }
        val viewModel by viewModels<SRC721EditViewModel> { SRC721Module.EditFactory(input) }
        SRC721EditScreen(viewModel = viewModel, navController = navController)
    }
}

@Composable
fun SRC721EditScreen(
    viewModel: SRC721EditViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    val view = LocalView.current
    val context = LocalContext.current
    val sendResult = viewModel.sendResult

    val logoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.size <= 128 * 1024) {
                    viewModel.updateLogo(bytes)
                } else {
                    HudHelper.showErrorMessage(view, R.string.Nft_Edit_Logo_MaxSize)
                }
            }
        }
    }

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
            title = stringResource(id = R.string.Nft_Edit_Title),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )
        if (uiState.loading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = ComposeAppTheme.colors.grey)
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                EditField(
                    label = stringResource(R.string.Nft_Edit_OrgName),
                    value = uiState.orgName ?: "",
                    onValueChange = viewModel::setOrgName
                )
                EditField(
                    label = stringResource(R.string.Nft_Edit_OfficialUrl),
                    value = uiState.officialUrl ?: "",
                    onValueChange = viewModel::setOfficialUrl
                )
                EditField(
                    label = stringResource(R.string.Nft_Edit_WhitePaperUrl),
                    value = uiState.whitePaperUrl ?: "",
                    onValueChange = viewModel::setWhitePaperUrl
                )
                EditField(
                    label = stringResource(R.string.Nft_Description),
                    value = uiState.description ?: "",
                    onValueChange = viewModel::setDescription
                )
                EditField(
                    label = stringResource(R.string.Nft_Deploy_BaseURI),
                    value = uiState.baseURI ?: "",
                    onValueChange = viewModel::setBaseURI
                )
                EditField(
                    label = stringResource(R.string.Nft_Deploy_MintPrice),
                    value = uiState.mintPrice ?: "",
                    onValueChange = viewModel::setMintPrice
                )
                EditField(
                    label = stringResource(R.string.Nft_Deploy_MaxSupply),
                    value = uiState.maxSupply ?: "",
                    onValueChange = viewModel::setMaxSupply
                )

                // Logo
                Spacer(modifier = Modifier.height(12.dp))
                body_bran(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(R.string.Nft_Edit_Logo)
                )
                uiState.logoFee?.let { fee ->
                    body_grey(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(R.string.Nft_Edit_Logo_Fee, fee)
                    )
                }
                body_grey(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(R.string.Nft_Edit_Logo_MaxSize)
                )
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(40.dp),
                    title = stringResource(R.string.Nft_Edit_Logo),
                    onClick = { logoPicker.launch("image/*") }
                )

                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(40.dp),
                    title = stringResource(R.string.Button_Continue),
                    onClick = { viewModel.update() },
                    enabled = uiState.hasUpdate
                )
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))
    body_bran(
        modifier = Modifier.padding(start = 16.dp),
        text = label
    )
    FormsInput(
        modifier = Modifier.padding(horizontal = 16.dp),
        enabled = true,
        pasteEnabled = true,
        hint = "",
        initial = value
    ) {
        onValueChange(it)
    }
}
