package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SafeFourCreateProposalConfirmationFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {

        val address = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit?.receiveAddress
        if (address == null) {
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val input = navController.getInput<SafeFourProposalModule.CreateProposalInput>()
        val wallet = input?.wallet ?: return
        val proposalInfo = input?.data ?: return
        val viewModel by viewModels<SafeFourCreateProposalConfirmationViewModel> { SafeFourProposalModule.FactoryCreateProposal(wallet, proposalInfo) }
        ProposalInfoScreen(navController = navController, viewModel = viewModel)
    }


}


@Composable
fun ProposalInfoScreen(
        navController: NavController,
        viewModel: SafeFourCreateProposalConfirmationViewModel
) {
    val uiState = viewModel.uiState
    val view = LocalView.current

    val sendResult = viewModel.sendResult

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
            navController.popBackStack(R.id.createProposalFragment, true)
        }
    }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
                title = stringResource(id = R.string.Safe_Four_Proposal_Create),
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
                .padding(top = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState())) {


            body_grey(
                    modifier = Modifier
                            .padding(start = 16.dp),
                        text = stringResource(id = R.string.Safe_Four_Proposal_Create_Title))
            Spacer(modifier = Modifier.height(5.dp))

            body_bran(
                    modifier = Modifier
                            .padding(start = 16.dp),
                        text = uiState.title)
            Divider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )

            body_grey(
                    modifier = Modifier
                            .padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Proposal_Create_Desc))
            Spacer(modifier = Modifier.height(5.dp))
            body_bran(
                    modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp),
                    text = uiState.desc)

            Divider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )

            body_grey(
                    modifier = Modifier
                            .padding(start = 16.dp),
                    text = stringResource(id = R.string.Safe_Four_Proposal_Create_Apply))
            Spacer(modifier = Modifier.height(5.dp))
            body_bran(
                    modifier = Modifier
                            .padding(start = 16.dp),
                    text = uiState.amount)
            Divider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )

            Column(
                    modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp),
            ) {
                subhead1_grey(
                        modifier = Modifier
                                .fillMaxWidth(),
                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_Issuance_Method),
                        maxLines = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    subhead1_grey(
                            text = stringResource(id = R.string.Safe_Four_Proposal_Info_In),
                            maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (uiState.isOncePay) {
                        subhead1_leah(
                                text = uiState.endDate,
                                maxLines = 1,
                        )
                    } else {
                        subhead1_leah(
                                text = uiState.startDate,
                                maxLines = 1,
                        )
                        subhead1_grey(
                                text = stringResource(id = R.string.Safe_Four_Proposal_Info_To),
                                maxLines = 1,
                        )
                        subhead1_leah(
                                text = uiState.endDate,
                                maxLines = 1,
                        )
                    }
                }

                Row {
                    Spacer(modifier = Modifier.width(4.dp))
                    val res = if (uiState.isOncePay)
                        R.string.Safe_Four_Proposal_Info_Once
                    else
                        R.string.Safe_Four_Proposal_Info_Instalments_Times
                    subhead1_leah(
                            text = stringResource(id = res, uiState.payTimes),
                            maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (uiState.isOncePay) {
                        subhead1_grey(
                                text = stringResource(id = R.string.Safe_Four_Proposal_Info_Total),
                                maxLines = 1,
                        )
                    }
                    subhead1_grey(
                            text = stringResource(id = R.string.Safe_Four_Proposal_Info_Issuance),
                            maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    subhead1_leah(
                            text = uiState.amount,
                            maxLines = 1,
                    )
                }
            }
        }
        ButtonPrimaryYellow(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(40.dp),
                title = stringResource(R.string.Safe_Four_Register_Node_Send),
                onClick = {
                    viewModel.send()
                }
        )
    }
}