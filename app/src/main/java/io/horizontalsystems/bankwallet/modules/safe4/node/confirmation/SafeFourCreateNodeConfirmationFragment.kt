package io.horizontalsystems.bankwallet.modules.safe4.node.confirmation

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.safe4.node.supernode.SafeFourCreateNodeViewModel
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class SafeFourCreateNodeConfirmationFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SafeFourConfirmationModule.Input>()
        if (input?.data == null) {
            navController.popBackStack(R.id.createSuperNodeFragment, true)
            return
        }

        val viewModel by viewModels<SafeFourCreateNodeConfirmationViewModel> { SafeFourConfirmationModule.Factory(
                input.isSuper,
                input.wallet,
                input.data,
        ) }
        SafeFourCreateNodeConfirmationScreen(viewModel, navController, input.isSuper)
    }
}

@Composable
fun SafeFourCreateNodeConfirmationScreen(
        viewModel: SafeFourCreateNodeConfirmationViewModel,
        navController: NavController,
        isSuperNode: Boolean
) {
    val uiState = viewModel.uiState
    val data = viewModel.createNodeData

    val sendResult = viewModel.sendResult
    val view = LocalView.current
    val title = if (isSuperNode) R.string.Safe_Four_Register_Super_Node else R.string.Safe_Four_Register_Master_Node
    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                    view,
                    R.string.Send_Sending,
                    SnackbarDuration.INDEFINITE
            )
        }

        SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                    view,
                    R.string.Send_Success,
                    SnackbarDuration.LONG
            )
        }

        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
        }

        null -> Unit
    }

    LaunchedEffect(sendResult) {
        if (sendResult == SendResult.Sent) {
            delay(1200)
            navController.popBackStack(R.id.createSuperNodeFragment, true)
        }
    }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
                title = stringResource(id = title),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
        )
        Column(modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .verticalScroll(rememberScrollState())) {

            Text(
                    modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Register_Lock_Confirmation),
                    style = ComposeAppTheme.typography.subhead1,
                    color = ComposeAppTheme.colors.bran,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                    modifier = Modifier
                            .padding(start = 16.dp)) {
                Text(
                        text = stringResource(id = R.string.Safe_Four_Register_Lock),
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.grey,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Text(
                        modifier = Modifier.padding(end = 16.dp),
                        text = uiState.lockAmount,
                        color = ComposeAppTheme.colors.bran,
                        style = ComposeAppTheme.typography.body,
                        maxLines = 1,
                )
            }
            Divider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )
            if (isSuperNode) {

                body_grey(
                        modifier = Modifier
                                .padding(start = 16.dp),
                        text = stringResource(id = R.string.Safe_Four_Register_Mode_Name))
                Spacer(modifier = Modifier.height(5.dp))

                body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        text = data.name)

                Divider(
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                )
            }


            body_grey(modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Node_Info_Address))

            Spacer(modifier = Modifier.height(5.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        text = data.address)

            Divider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )



            body_grey(modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Node_Info_Creator))

            Spacer(modifier = Modifier.height(5.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        text = uiState.creator)

            Divider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )


            body_grey(modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Register_ENODE))

            Spacer(modifier = Modifier.height(5.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        text = data.enode)

            Divider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )

            body_grey(modifier = Modifier.padding(start = 16.dp), text = stringResource(id = R.string.Safe_Four_Register_Introduction))

            Spacer(modifier = Modifier.height(5.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), text = data.description)


        }
        Spacer(modifier = Modifier.height(16.dp))
        ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth()
                        .padding(16.dp)
                        .height(40.dp),
                title = stringResource(R.string.Safe_Four_Register_Node_Send),
                onClick = {
                    viewModel.send()
                }
        )
    }
}