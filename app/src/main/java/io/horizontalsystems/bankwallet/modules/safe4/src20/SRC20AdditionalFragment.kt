package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.safe4.node.HintView
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlin.contracts.contract

class SRC20AdditionalFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SRC20Module.InputEdit>()
        val wallet = input?.wallet
        val customToken = input?.customToken
        if (wallet == null || customToken == null) {
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val viewModel by viewModels<SRC20AdditionalViewModel> { SRC20Module.FactoryEdit(wallet, customToken) }
        SRC20AdditionalScreen(customToken, viewModel = viewModel, navController = navController)
    }
}



@Composable
fun SRC20AdditionalScreen(
    tokenInfo: CustomToken,
    viewModel: SRC20AdditionalViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    val proceedEnabled = uiState.canUpdate

    val view = LocalView.current
    val sendResult = viewModel.sendResult

    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                view,
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )
            viewModel.sendResult = null
        }

        SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                view,
                R.string.Send_Success,
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

    LaunchedEffect(sendResult) {
        if (sendResult == SendResult.Sent) {
            navController.popBackStack()
        }
    }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(id = R.string.SRC20_Issuance),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                    .background(ComposeAppTheme.colors.lawrence)
                    .padding(16.dp)
            ) {
                Row {
                    body_leah(
                        modifier = Modifier.weight(2f),
                        text = stringResource(R.string.SRC20_Edit_Name),
                        maxLines = 1,
                    )
                    body_bran(
                        modifier = Modifier.padding(start = 16.dp).weight(5f),
                        text = tokenInfo.name,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    body_leah(
                        modifier = Modifier.weight(2f),
                        text = stringResource(R.string.SRC20_Edit_Symbol),
                        maxLines = 1,
                    )
                    body_bran(
                        modifier = Modifier.padding(start = 16.dp).weight(5f),
                        text = tokenInfo.symbol,
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    body_leah(
                        modifier = Modifier.padding(start = 16.dp).weight(2f),
                        text = stringResource(R.string.SRC20_Deploy_Supply),
                        maxLines = 1,
                    )
                    body_bran(
                        modifier = Modifier.padding(start = 16.dp).weight(5f),
                        text = uiState.totalSupply,
                        maxLines = 1,
                    )
                }

            }

            Spacer(modifier = Modifier.height(12.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.SRC20_Additional_Address)
            )

            FormsInput(
                initial = tokenInfo.creator,
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = false,
                pasteEnabled = false,
                hint = ""
            ) {

            }

            Spacer(modifier = Modifier.height(12.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.SRC20_Additional_Number)
            )

            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = true,
                pasteEnabled = false,
                hint = "",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            ) {
                viewModel.setAdditionalNumber(it)
            }


            Spacer(modifier = Modifier.height(12.dp))

            Row {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .height(40.dp),
                    title = stringResource(R.string.SRC20_Info_Add),
                    onClick = {
                        viewModel.showConfirm()
                    },
                    enabled = proceedEnabled
                )
            }
        }
    }

    if (uiState.showConfirmationDialog) {
        EditConfirmationDialog(
            content = stringResource(R.string.SRC20_Additional_Hint),
            onOKClick = {
                viewModel.update()
            },
            onCancelClick = {
                viewModel.cancel()
            }
        )
    }
}