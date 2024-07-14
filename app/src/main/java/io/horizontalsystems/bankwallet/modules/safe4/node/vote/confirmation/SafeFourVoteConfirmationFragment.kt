package io.horizontalsystems.bankwallet.modules.safe4.node.vote.confirmation

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
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

class SafeFourCreateNodeConfirmationFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SafeFourConfirmationModule.Input>()
        if (input?.data == null) {
            navController.popBackStack(R.id.createSuperNodeFragment, true)
            return
        }

        val viewModel by viewModels<SafeFourCreateNodeConfirmationViewModel> { SafeFourConfirmationModule.Factory(
                input.title,
                input.title == getString(R.string.Safe_Four_Register_Super_Node),
                input.wallet,
                input.data,
        ) }
        SafeFourCreateNodeConfirmationScreen(viewModel, navController)
    }
}

@Composable
fun SafeFourCreateNodeConfirmationScreen(
        viewModel: SafeFourCreateNodeConfirmationViewModel,
        navController: NavController
) {
    val uiState = viewModel.uiState
    val data = viewModel.createNodeData
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
                title = uiState.title,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            Column(
                    modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                            .background(ComposeAppTheme.colors.lawrence)
            ) {
                Row(
                        modifier = Modifier
                                .padding(16.dp)) {
                    Text(
                            text = stringResource(id = R.string.Safe_Four_Register_Lock),
                            style = ComposeAppTheme.typography.body,
                            color = ComposeAppTheme.colors.grey,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                            text = "5,000 SAFE",
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.body,
                            maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))


            Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Register_Mode_Super_Node_Address),
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )

            Spacer(modifier = Modifier.height(12.dp))


            Column(
                    modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                            .background(ComposeAppTheme.colors.lawrence)
            ) {

                body_grey(modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(id = R.string.Safe_Four_Register_Mode_Name))

                body_bran(modifier = Modifier.padding(start = 16.dp),
                        text = data.name)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                    modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                            .background(ComposeAppTheme.colors.lawrence)
            ) {
                body_bran(modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(id = R.string.Safe_Four_Register_ENODE))

                body_bran(modifier = Modifier.padding(start = 16.dp),
                        text = data.enode)
            }

            body_grey(modifier = Modifier.padding(start = 16.dp), text = stringResource(id = R.string.Safe_Four_Register_ENODE_Hint))

            Spacer(modifier = Modifier.height(12.dp))

            body_bran(modifier = Modifier.padding(start = 16.dp), text = stringResource(id = R.string.Safe_Four_Register_Introduction))
            Column(
                    modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                            .background(ComposeAppTheme.colors.lawrence)
            ) {

                body_grey(modifier = Modifier.padding(start = 16.dp), text = stringResource(id = R.string.Safe_Four_Register_Introduction))

                body_bran(modifier = Modifier.padding(start = 16.dp), text = data.description)

            }
            Spacer(modifier = Modifier.height(12.dp))

            Row {
                ButtonPrimaryYellow(
                        modifier = Modifier
                                .weight(1f)
                                .padding(16.dp)
                                .height(40.dp),
                        title = stringResource(R.string.Safe_Four_Register_Node_Send),
                        onClick = {
                            viewModel.send()
                        }
                )
            }
        }
    }
}