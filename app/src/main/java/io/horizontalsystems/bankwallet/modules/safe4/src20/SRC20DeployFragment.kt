package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessorImpl
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class SRC20DeployFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SRC20Module.Input>()
        val wallet = input?.wallet
        if (wallet == null) {
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val viewModel by viewModels<SRC20DeployViewModel> { SRC20Module.Factory(wallet) }
        SRC20DeployScreen(viewModel = viewModel, navController = navController)
    }
}



@Composable
fun SRC20DeployScreen(
    viewModel: SRC20DeployViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    val proceedEnabled = uiState.proceedEnabled

    val options = listOf(
        stringResource(id = R.string.SRC20_Deploy_Type_Normal),
        stringResource(id = R.string.SRC20_Deploy_Type_Mintable),
        stringResource(id = R.string.SRC20_Deploy_Type_Burnable)
    )
    var selectedOption by remember{ mutableStateOf(0) }
    var nameErrorState by remember{ mutableStateOf(false) }
    var symbolErrorState by remember{ mutableStateOf(false) }

    var lastValidValue by remember { mutableStateOf("") }

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
            title = stringResource(id = R.string.SRC20_Deploy_Title),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            body_leah(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(R.string.SRC20_Deploy_Mode),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    options.forEachIndexed { index, option ->
                        Row(modifier = Modifier
                            .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = index == selectedOption,
                                onClick = {
                                    selectedOption = index
                                    viewModel.onSelectType(index)
                                }
                            )
                            Text(
                                text = option,
                                style = ComposeAppTheme.typography.body,
                                color = ComposeAppTheme.colors.grey,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ComposeAppTheme.colors.lawrence, RoundedCornerShape(8.dp))
                    .background(ComposeAppTheme.colors.lawrence)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = uiState.deplopDesc),
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.SRC20_Deploy_Name)
            )

            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = true,
                pasteEnabled = false,
                hint = "",
                maxLength = 20
            ) {
                viewModel.onEnterName(it)
                nameErrorState = it.length < 3
            }

            Spacer(modifier = Modifier.height(12.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.SRC20_Deploy_Symbol))
            Spacer(modifier = Modifier.height(6.dp))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = true,
                pasteEnabled = false,
                hint = "",
                qrScannerEnabled = false
            ) {
                viewModel.onEnterSymbol(it)
                symbolErrorState = it.length < 3
            }
            /*if (symbolErrorState) {
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Register_Mode_Exists_Enode),
                    color = ComposeAppTheme.colors.redD,
                    style = ComposeAppTheme.typography.caption,
                    maxLines = 1,
                )
            }*/

            Spacer(modifier = Modifier.height(12.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.SRC20_Deploy_Supply))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = true,
                pasteEnabled = false,
                hint = "",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textPreprocessor = object : TextPreprocessor {
                    override fun process(text: String): String {
                        val value = if (viewModel.isValidDecimalInput(text, lastValidValue)) {
                            lastValidValue = text
                            text
                        } else {
                            // 如果输入无效，恢复到最后一次有效的值
                            lastValidValue
                        }
                        return value
                    }

                }
            ) {
                viewModel.onEnterSupply(it)
            }

            /*if (descErrorState) {
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Register_Mode_Length_Error, 12),
                    color = ComposeAppTheme.colors.redD,
                    style = ComposeAppTheme.typography.caption,
                    maxLines = 1,
                )
            }*/

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .height(40.dp),
                    title = stringResource(R.string.Button_Continue),
                    onClick = {
                        viewModel.showConfirm()
                    },
                    enabled = proceedEnabled
                )
            }
        }
    }

    if (uiState.showConfirmationDialog) {
        DeployConfirmationDialog(
            content = options.get(uiState.type.type),
            onOKClick = {
                viewModel.deploy()
            },
            onCancelClick = {
                viewModel.cancel()
            }
        )
    }
}