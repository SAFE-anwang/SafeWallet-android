package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info

import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalStatus
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType

class SafeFourProposalInfoFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {

        val address = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit?.receiveAddress
        if (address == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val input = navController.getInput<SafeFourProposalModule.InfoInput>()
        val wallet = input?.wallet ?: return
        val proposalInfo = input?.proposalInfo ?: return
        val viewModel by viewModels<SafeFourProposalInfoViewModel> { SafeFourProposalModule.FactoryInfo(wallet, proposalInfo) }
        ProposalInfoScreen(navController = navController, viewModel = viewModel)
    }


}


@Composable
fun ProposalInfoScreen(
        navController: NavController,
        viewModel: SafeFourProposalInfoViewModel
) {
    val uiState = viewModel.uiState
    val proposalInfo = viewModel.uiState.proposalInfo
    val voteList = viewModel.uiState.voteList
    val voteEnable = uiState.voteEnable
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

        Column() {
            AppBar(
                    title = stringResource(id = R.string.Safe_Four_Proposal_Title),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    }
            )
            Surface(color = ComposeAppTheme.colors.tyler,
                    modifier = Modifier.padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))) {
            Scaffold(
            ) { paddingValues ->

                val listState = rememberLazyListState()
                LazyColumn(Modifier.padding(start = 16.dp, end = 16.dp), state = listState) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            subhead1_grey(
                                    modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                    text = stringResource(id = R.string.Safe_Four_Proposal_Info_ID),
                                    maxLines = 1,
                            )
                            subhead1_leah(
                                    modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(2f),
                                    text = proposalInfo.id.toString(),
                                    maxLines = 1,
                            )
                        }
                        Divider(
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                                thickness = 1.dp,
                                color = ComposeAppTheme.colors.steel10,
                        )
                        ContentScreen(viewModel)

                        Divider(
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                                thickness = 1.dp,
                                color = ComposeAppTheme.colors.steel10,
                        )
                        Column {
                            Row {
                                subhead1_grey(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_Voting_Status),
                                        maxLines = 1,
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                val color = when (proposalInfo.status) {
                                    is ProposalStatus.Voting -> ComposeAppTheme.colors.tgBlue
                                    is ProposalStatus.Lose -> ComposeAppTheme.colors.grey50
                                    is ProposalStatus.Adopt -> ComposeAppTheme.colors.greenD
                                }
                                Text(
                                        text = proposalInfo.status.title().getString(),
                                        style = ComposeAppTheme.typography.body,
                                        color = color,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                )
                            }
                            Row {
                                subhead1_grey(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_Total),
                                        maxLines = 1,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_Votes, voteList?.size
                                                ?: 0),
                                        style = ComposeAppTheme.typography.body,
                                        color = ComposeAppTheme.colors.leah,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                )

                                Spacer(modifier = Modifier.width(6.dp))
                                subhead1_grey(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Vote_Agree),
                                        maxLines = 1,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_Votes, proposalInfo.agreeNum),
                                        style = ComposeAppTheme.typography.body,
                                        color = ComposeAppTheme.colors.greenL,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                )

                                Spacer(modifier = Modifier.width(6.dp))
                                subhead1_grey(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Vote_Refuse),
                                        maxLines = 1,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_Votes, proposalInfo.rejectNmu),
                                        style = ComposeAppTheme.typography.body,
                                        color = ComposeAppTheme.colors.redD,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                )

                                Spacer(modifier = Modifier.width(6.dp))
                                subhead1_grey(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_Abstain),
                                        maxLines = 1,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_Votes, proposalInfo.abstentionNum),
                                        style = ComposeAppTheme.typography.body,
                                        color = ComposeAppTheme.colors.grey,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                )
                            }
                        }
                        val backgroundColor = if (voteEnable) {
                            ComposeAppTheme.colors.laguna
                        } else {
                            ComposeAppTheme.colors.yellow50
                        }
                    if (proposalInfo.status == ProposalStatus.Voting) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val text = if (voteEnable) {
                                R.string.Safe_Four_Proposal_Enable
                            } else {
                                R.string.Safe_Four_Proposal_Disable
                            }
                            Column(
                                    modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                                            .background(backgroundColor)
                                            .padding(16.dp)
                            ) {
                                subhead1_leah(
                                        text = stringResource(id = text),
                                )
                            }
                            if (uiState.isVoted) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row {
                                    body_leah(
                                            text = stringResource(id = R.string.Safe_Four_Proposal_Already_Vote),
                                            maxLines = 1,
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                            modifier = Modifier
                                                    .fillMaxWidth(),
                                            text = ProposalVoteStatus.Agree.title().toString(),
                                            style = ComposeAppTheme.typography.body,
                                            color = ComposeAppTheme.colors.greenD,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                            textAlign = TextAlign.End
                                    )
                                }
                            } else if (uiState.voteEnable) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                ) {

                                    ButtonSecondary(
                                            modifier = Modifier
                                                    .width(80.dp)
                                                    .height(40.dp)
                                                    .border(1.dp,
                                                            ComposeAppTheme.colors.green50,
                                                            RoundedCornerShape(14.dp)),
                                            buttonColors = ButtonDefaults.buttonColors(
                                                    backgroundColor = ComposeAppTheme.colors.transparent,
                                                    contentColor = ComposeAppTheme.colors.jacob
                                            ),
                                            onClick = {
                                                viewModel.vote(0)
                                            }
                                    ) {
                                        Text(
                                                text = stringResource(id = R.string.Safe_Four_Proposal_Vote_Agree),
                                                style = ComposeAppTheme.typography.subhead1,
                                                color = ComposeAppTheme.colors.greenD,
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))

                                    ButtonSecondary(
                                            modifier = Modifier
                                                    .width(80.dp)
                                                    .height(40.dp)
                                                    .border(1.dp,
                                                            ComposeAppTheme.colors.red50,
                                                            RoundedCornerShape(14.dp)),
                                            buttonColors = ButtonDefaults.buttonColors(
                                                    backgroundColor = ComposeAppTheme.colors.transparent,
                                                    contentColor = ComposeAppTheme.colors.jacob
                                            ),
                                            onClick = {
                                                viewModel.vote(1)
                                            }
                                    ) {
                                        Text(
                                                text = stringResource(id = R.string.Safe_Four_Proposal_Vote_Refuse),
                                                style = ComposeAppTheme.typography.subhead1,
                                                color = ComposeAppTheme.colors.redD,
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))
                                    ButtonSecondary(
                                            modifier = Modifier
                                                    .width(80.dp)
                                                    .height(40.dp)
                                                    .border(1.dp,
                                                            ComposeAppTheme.colors.grey50,
                                                            RoundedCornerShape(14.dp)),
                                            buttonColors = ButtonDefaults.buttonColors(
                                                    backgroundColor = ComposeAppTheme.colors.transparent,
                                                    contentColor = ComposeAppTheme.colors.jacob
                                            ),
                                            onClick = {
                                                viewModel.vote(2)
                                            }
                                    ) {
                                        Text(
                                                text = stringResource(id = R.string.Safe_Four_Proposal_Vote_Give_up),
                                                style = ComposeAppTheme.typography.subhead1,
                                                color = ComposeAppTheme.colors.grey,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    if (uiState.voteList?.isNotEmpty() == true) {
                        item {
                            Divider(
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                                    thickness = 1.dp,
                                    color = ComposeAppTheme.colors.steel10,
                            )
                            subhead1_grey(
                                    text = stringResource(id = R.string.Safe_Four_Proposal_Info_Votes_Record),
                                    maxLines = 1,
                            )
                            Divider(
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                            )
                        }
                        itemsIndexed(
                                items = uiState.voteList
                        ) { index, item ->
                            Column(
                            ) {
                                Row {
                                    body_leah(
                                            modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(4f),
                                            text = item.address,
                                            maxLines = 1,
                                    )

                                    val color = when (item.state) {
                                        is ProposalVoteStatus.Agree -> ComposeAppTheme.colors.greenD
                                        is ProposalVoteStatus.Refuse -> ComposeAppTheme.colors.redD
                                        is ProposalVoteStatus.Abstain -> ComposeAppTheme.colors.grey50
                                    }

                                    /*val icon = when (item.state) {
                                    is ProposalVoteStatus.Agree -> R.drawable.ic_check_circle_24
                                    is ProposalVoteStatus.Refuse -> R.drawable.ic_highlight_off_24
                                    is ProposalVoteStatus.Abstain -> R.drawable.ic_help_outline_24
                                }

                                Icon(painter = painterResource(id = icon), contentDescription = null)*/
                                    Text(
                                            modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                            text = item.state.title().toString(),
                                            style = ComposeAppTheme.typography.body,
                                            color = color,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                            textAlign = TextAlign.End
                                    )
                                }
                            }

                            Divider(
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                                    thickness = 1.dp,
                                    color = ComposeAppTheme.colors.steel10,
                            )
                        }
                    }
                }

            }
        }
    }
    if (uiState.showConfirmationDialog) {
        ProposalConfirmationDialog(
                viewModel,
                onOKClick = {
                    viewModel.send()
                },
                onCancelClick = { viewModel.closeDialog() }
        )
    }
}


@Composable
fun ContentScreen(
        viewModel: SafeFourProposalInfoViewModel,
        isConfirmation: Boolean = false
) {
    val uiState = viewModel.uiState
    val proposalInfo = viewModel.uiState.proposalInfo
    val voteEnable = uiState.voteEnable

    Row {
        subhead1_grey(
                modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                text = stringResource(id = R.string.Safe_Four_Proposal_Info_Title),
                maxLines = 1,
        )
        subhead1_leah(
                modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f),
                text = proposalInfo.title,
                maxLines = 1,
        )
    }
    Divider(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
    )
    if (!isConfirmation) {
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            subhead1_grey(
                    modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    text = stringResource(id = R.string.Safe_Four_Proposal_Info_Creator),
                    maxLines = 1,
            )
            subhead1_leah(
                    modifier = Modifier
                            .fillMaxWidth()
                            .weight(2f),
                    text = proposalInfo.creator
            )
        }
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row {
        subhead1_grey(
                modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                text = stringResource(id = R.string.Safe_Four_Proposal_Info_Amount),
                maxLines = 1,
        )
        subhead1_leah(
                modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f),
                text = proposalInfo.amount,
                maxLines = 1,
        )
    }
    Divider(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Column {
        subhead1_grey(
                modifier = Modifier
                        .fillMaxWidth(),
                text = stringResource(id = R.string.Safe_Four_Proposal_Info_Des),
                maxLines = 1,
        )
        Spacer(modifier = Modifier.height(6.dp))
        subhead1_leah(
                modifier = Modifier
                        .fillMaxWidth(),
                text = proposalInfo.desc,
        )
    }
    Divider(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
    )
    Spacer(modifier = Modifier.height(12.dp))

    Column {
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
            if (proposalInfo.payTimes == 1) {
                subhead1_leah(
                        text = proposalInfo.endDate,
                        maxLines = 1,
                )
            } else {
                subhead1_leah(
                        text = proposalInfo.startDate,
                        maxLines = 1,
                )
                subhead1_grey(
                        text = stringResource(id = R.string.Safe_Four_Proposal_Info_To),
                        maxLines = 1,
                )
                subhead1_leah(
                        text = proposalInfo.endDate,
                        maxLines = 1,
                )
            }
        }

        Row {
            Spacer(modifier = Modifier.width(4.dp))
            val res = if (proposalInfo.payTimes == 1)
                R.string.Safe_Four_Proposal_Info_Once
            else
                R.string.Safe_Four_Proposal_Info_Instalments_Times
            subhead1_leah(
                    text = stringResource(id = res, proposalInfo.payTimes),
                    maxLines = 1,
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (proposalInfo.payTimes == 1) {
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
                    text = proposalInfo.amount,
                    maxLines = 1,
            )
        }
    }
}