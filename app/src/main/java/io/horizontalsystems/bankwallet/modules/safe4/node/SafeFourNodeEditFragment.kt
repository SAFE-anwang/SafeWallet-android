package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.marketkit.models.BlockchainType
import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create.SingleButtonDatePickerView
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create.TwoButtonDatePickerView
import io.horizontalsystems.bankwallet.modules.safe4.node.supernode.RangeSliderScreen
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteModule
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteRecordViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.VoteRecordView
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.confirmation.SafeFourVoteConfirmationModule
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AdditionalDataCell2
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class SafeFourNodeEditFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val navController = findNavController()
        val input = navController.requireInput<Input>()
        val wallet = input.wallet
        val name = input.name
        val enode = input.enode
        val address = input.nodeAddress
        val desc = input.desc
        val nodeType = input.nodeType
        val viewModel by viewModels<SafeFourNodeEditViewModel> {
            SafeFourModule.FactoryEdit(wallet, nodeType, name, enode, address, desc, input.nodeId, input.incentivePlan)
        }
        EditScreen(navController, viewModel, nodeType, input.incentivePlan)
    }

    @Parcelize
    data class Input(
            val wallet: Wallet,
            val name: String,
            val desc: String,
            val enode: String,
            val nodeType: Int,
            val nodeAddress: String,
            val nodeId: Int,
            val incentivePlan: NodeIncentivePlan,
    ) : Parcelable
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditScreen(
        navController: NavController,
        viewModel: SafeFourNodeEditViewModel,
        nodeType: Int,
        incentivePlan: NodeIncentivePlan,
) {
    val uiState = viewModel.uiState
    val wallet = uiState.wallet
    val addressError = uiState.addressError
    val isSuperNode = NodeType.SuperNode == NodeType.getType(nodeType)
    val title = if (isSuperNode) {
        R.string.Safe_Four_Node_Edit_Super_Title
    } else {
        R.string.Safe_Four_Node_Edit_Master_Title
    }

    var nameErrorState by remember{ mutableStateOf(false) }
    var descErrorState by remember{ mutableStateOf(false) }

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

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
                title = stringResource(id = title),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
        )
        HintView(
                if (isSuperNode)
                R.string.Safe_Four_Node_Edit_Super_Hint
            else
                R.string.Safe_Four_Node_Edit_Master_Hint
        )
        Column(modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())) {

            if (isSuperNode) {
                Spacer(modifier = Modifier.height(16.dp))
                body_bran(text = stringResource(id = R.string.Safe_Four_Proposal_Create_Title))

                FormsInput(
                        enabled = true,
                        pasteEnabled = false,
                        initial = uiState.name,
                        hint = "",
                ) {
                    viewModel.onEnterName(it)
                    nameErrorState = it.length < 8
                }

                if (nameErrorState) {
                    Text(
                            modifier = Modifier.padding(start = 16.dp),
                            text = stringResource(id = R.string.Safe_Four_Register_Mode_Length_Error, 8),
                            color = ComposeAppTheme.colors.redD,
                            style = ComposeAppTheme.typography.caption,
                            maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    ButtonPrimaryYellow(
                            modifier = Modifier
                                    .wrapContentSize(),
                            title = stringResource(R.string.Safe_Four_Node_Update_Button),
                            enabled = uiState.nameCanUpdate,
                            onClick = {
                                viewModel.updateName()
                            },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val addressHint = if (isSuperNode) {
                R.string.Safe_Four_Register_Mode_Super_Node_Address
            } else {
                R.string.Safe_Four_Register_Mode_Master_Node_Address
            }
            Text(
                    text = stringResource(id = addressHint),
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )

            Spacer(modifier = Modifier.height(10.dp))
            HSAddressInput(
                    initial = uiState.address?.let { Address(it) },
                    tokenQuery = wallet.token.tokenQuery,
                    coinCode = wallet.coin.code,
                    error = addressError,
                    navController = navController
            ) {
                viewModel.onEnterAddress(it)
            }
            if (uiState.existsNode || uiState.isFounder) {
                Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(
                                id = if (isSuperNode) R.string.Safe_Four_Register_Mode_Exists_Super_Node
                                else R.string.Safe_Four_Register_Mode_Exists_Master_Node
                        ),
                        color = ComposeAppTheme.colors.redD,
                        style = ComposeAppTheme.typography.caption,
                        maxLines = 1,
                )
            }
            if (uiState.isInputCurrentWalletAddress) {
                Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(
                                id =  R.string.Safe_Four_Register_Mode_Dont_User_Current_Wallet_Address
                        ),
                        color = ComposeAppTheme.colors.redD,
                        style = ComposeAppTheme.typography.caption,
                        maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                ButtonPrimaryYellow(
                        modifier = Modifier
                                .wrapContentSize(),
                        title = stringResource(R.string.Safe_Four_Node_Update_Button),
                        enabled = uiState.addressCanUpdate,
                        onClick = {
                            viewModel.updateAddress()
                        },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            body_bran( text = stringResource(id = R.string.Safe_Four_Node_Info_ENode))

            FormsInput(
                    enabled = true,
                    pasteEnabled = false,
                    initial = uiState.enode,
                    hint = "",
            ) {
                viewModel.onEnterENODE(it)
            }
            if (uiState.existsEnode) {
                Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(id = R.string.Safe_Four_Register_Mode_Exists_Enode),
                        color = ComposeAppTheme.colors.redD,
                        style = ComposeAppTheme.typography.caption,
                        maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row {
                ButtonPrimaryYellow(
                        modifier = Modifier
                                .wrapContentSize(),
                        title = stringResource(R.string.Safe_Four_Node_Update_Button),
                        enabled = uiState.enodeCanUpdate,
                        onClick = {
                            viewModel.updateEnode()
                        },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            body_bran( text = stringResource(id = R.string.Safe_Four_Node_Info_Desc))

            FormsInput(
                    enabled = true,
                    pasteEnabled = false,
                    initial = uiState.desc,
                    hint = "",
            ) {
                viewModel.onEnterDesc(it)
                descErrorState = it.length < 12
            }

            if (descErrorState) {
                Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(id = R.string.Safe_Four_Register_Mode_Length_Error, 12),
                        color = ComposeAppTheme.colors.redD,
                        style = ComposeAppTheme.typography.caption,
                        maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row {
                ButtonPrimaryYellow(
                        modifier = Modifier
                                .wrapContentSize(),
                        title = stringResource(R.string.Safe_Four_Node_Update_Button),
                        enabled = uiState.descCanUpdate,
                        onClick = {
                            viewModel.updateDesc()
                        },
                )
            }
            if (isSuperNode) {
                Spacer(modifier = Modifier.height(10.dp))
                RangeSliderScreen(
                    initValue = incentivePlan.partner.toFloat() .. (incentivePlan.partner + incentivePlan.creator).toFloat()
                ) { t1, t2, t3 ->
                    viewModel.onEnterIncentive(t1, t2, t3)
                }
                Row {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .wrapContentSize(),
                        title = stringResource(R.string.Safe_Four_Node_Update_Button),
                        enabled = uiState.incentiveCanUpdate,
                        onClick = {
                            viewModel.updateDescIncentive()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))
        }
    }
}
