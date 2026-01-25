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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteModule
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteRecordViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.VoteRecordView
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.confirmation.SafeFourVoteConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AdditionalDataCell2
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_green50
import io.horizontalsystems.bankwallet.ui.compose.components.body_issykBlue
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class SafeFourNodeInfoFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val address = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit?.receiveAddress
        if (address == null) {
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }

        val navController = findNavController()
        val input = navController.requireInput<Input>()
        val wallet = input.wallet
        val nodeType = input.nodeType
        val nodeId = input.nodeId
        val viewModel by viewModels<SafeFourNodeInfoViewModel> { SafeFourVoteModule.FactoryInfo(wallet, nodeId, address, nodeType) }

        val voteRecordViewModel by viewModels<SafeFourVoteRecordViewModel> {
            SafeFourVoteModule.FactoryRecord(input.wallet, input.nodeAddress, NodeType.getType(nodeType) == NodeType.SuperNode, nodeId)
        }
        TabInfoScreen(navController, viewModel, nodeType, voteRecordViewModel, input)
    }

    @Parcelize
    data class Input(
            val wallet: Wallet,
            val nodeId: Int,
            val nodeType: Int,
            val nodeAddress: String,
    ) : Parcelable
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabInfoScreen(
        navController: NavController,
        viewModel: SafeFourNodeInfoViewModel,
        nodeType: Int,
        voteRecordViewModel: SafeFourVoteRecordViewModel,
        input: SafeFourNodeInfoFragment.Input
) {

    val tabs = viewModel.tabs
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val isSuperNode = NodeType.getType(nodeType) == NodeType.SuperNode

    Column(modifier = Modifier
            .background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
                title = stringResource(id =
                if (isSuperNode)
                    R.string.Safe_Four_Node_Info_Title
                else
                    R.string.Safe_Four_Node_Info_Title_Master
                ),
                navigationIcon = {
                    HsBackButton(onClick = { navController.navigateUp() })
                }
        )
        Column(modifier = Modifier
                .verticalScroll(rememberScrollState())) {

            NodeBaseInfoScreen(navController, viewModel)
            Spacer(modifier = Modifier.height(10.dp))
            val selectedTab = tabs[pagerState.currentPage]
            val tabItems = tabs.map {
                TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
            }
            Tabs(tabItems, onClick = { tab ->
                coroutineScope.launch {
                    pagerState.scrollToPage(tab.ordinal)
                }
            })

            HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false
            ) { page ->
                when (tabs[page]) {
                    SafeFourVoteModule.TabInfo.Creator -> {
                        CreatorScreen(viewModel)
                    }

                    SafeFourVoteModule.TabInfo.Voters -> {
                        VoterRecordScreen(viewModel = voteRecordViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun NodeBaseInfoScreen(
        navController: NavController,
        viewModel: SafeFourNodeInfoViewModel
) {
    ComposeAppTheme {

        Column(modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ComposeAppTheme.colors.lawrence)) {
                NodeInfoScreen(viewModel, isSuperNode = viewModel.isSuper)
        }
    }
}


@Composable
fun NodeInfoScreen(
        viewModel: SafeFourNodeInfoViewModel,
        isSuperNode: Boolean
) {
    val nodeInfo = viewModel.uiState.nodeInfo ?: return
    val uiState = viewModel.uiState

    val view = LocalView.current

    Column {
        Spacer(Modifier.height(16.dp))
        Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = R.string.Safe_Four_Node_Info),
                style = ComposeAppTheme.typography.subhead1,
                color = ComposeAppTheme.colors.bran,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                maxLines = 1,
        )
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),) {
            Text(
                    text = stringResource(id = R.string.Safe_Four_Node_Info_Id),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.grey,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                    text = nodeInfo.id.toString(),
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )
        }
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),){
            Text(
                    text = stringResource(id = R.string.Safe_Four_Node_Info_Status),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.grey,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                    text = nodeInfo.status.title().getString(),
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )
        }
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        Text(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                text = stringResource(id = R.string.Safe_Four_Node_Info_Address),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.grey,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                        .clickable {
                            TextHelper.copyText(nodeInfo.address.hex)
                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                        },
                text = nodeInfo.address.hex,
                color = ComposeAppTheme.colors.issykBlue,
                style = ComposeAppTheme.typography.body,
        )
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = R.string.Safe_Four_Node_Info_Creator),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.grey,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                        .clickable {
                            TextHelper.copyText(nodeInfo.creator.hex)
                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                        },
                text = nodeInfo.creator.hex,
                color = ComposeAppTheme.colors.issykBlue,
                style = ComposeAppTheme.typography.body,
        )
        if (isSuperNode) {
            Divider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )
            Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(id = R.string.Safe_Four_Node_Info_Name),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.grey,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
            )
            Text(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                    text = nodeInfo.name,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )
        }
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),){
            Text(
                    text = stringResource(id = R.string.Safe_Four_Node_Info_Pledge),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.grey,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                    text = nodeInfo.createPledge,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )
        }
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),){
            Text(
                    text = stringResource(id = R.string.Safe_Four_Node_Info_Vote_Pledge),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.grey,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                    text = "${nodeInfo.voteCompleteCount} SAFE",
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )
        }
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = R.string.Safe_Four_Node_Info_ENode),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.grey,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                        .clickable {
                            TextHelper.copyText(nodeInfo.enode)
                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                        },
                text = nodeInfo.enode,
                color = ComposeAppTheme.colors.issykBlue,
                style = ComposeAppTheme.typography.body
        )
        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = R.string.Safe_Four_Node_Info_Desc),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.grey,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                text = nodeInfo.desc,
                color = ComposeAppTheme.colors.bran,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
        )

        Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        body_bran(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = R.string.Safe_Four_Register_Reward))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                    text = stringResource(id = R.string.Safe_Four_Register_Creator),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.bran,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
            )
            Text(
                    text = uiState.creatorText,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            LinearProgressIndicator(
                    modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                            .background(ComposeAppTheme.colors.lawrence),
                    progress = uiState.creator,
                    color = ComposeAppTheme.colors.issykBlue,
                    backgroundColor = ComposeAppTheme.colors.grey50)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                    text = stringResource(id = R.string.Safe_Four_Register_Partner),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.bran,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
            )
            Text(
                    text = uiState.partnerText,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            LinearProgressIndicator(
                    modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                            .background(ComposeAppTheme.colors.lawrence),
                    progress = uiState.partner,
                    color = ComposeAppTheme.colors.issykBlue,
                    backgroundColor = ComposeAppTheme.colors.grey50)
        }
        if (isSuperNode) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(id = R.string.Safe_Four_Register_Voters),
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.bran,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                )
                Text(
                        text = uiState.voterText,
                        color = ComposeAppTheme.colors.bran,
                        style = ComposeAppTheme.typography.body,
                        maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                LinearProgressIndicator(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                                .background(ComposeAppTheme.colors.lawrence),
                        progress = uiState.voter,
                        color = ComposeAppTheme.colors.issykBlue,
                        backgroundColor = ComposeAppTheme.colors.grey50)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun CreatorScreen(
        viewModel: SafeFourNodeInfoViewModel
) {
    val uiState = viewModel.uiState
    val recordList = uiState.creatorList
    Column(modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)) {
        Row(modifier = Modifier
                .wrapContentHeight()
                .padding(start = 10.dp, top = 16.dp, end = 10.dp, bottom = 16.dp)) {
            body_bran(text = stringResource(R.string.Safe_Four_Node_Vote_Record_ID),
                    modifier = Modifier.weight(1.1f))
            Spacer(modifier = Modifier.width(10.dp))
            body_bran(text = stringResource(R.string.Safe_Four_Node_Vote_Record_Address),
                    modifier = Modifier.weight(4f))
            Spacer(modifier = Modifier.width(10.dp))
            body_bran(text = stringResource(R.string.Safe_Four_Node_Vote_Record_Num),
                    modifier = Modifier.weight(2.5f))
        }
        Divider(
                modifier = Modifier
                        .wrapContentHeight()
                        .padding(bottom = 16.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
        )
        recordList.forEach { item ->
            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)) {

                body_bran(text = item.id,
                        modifier = Modifier.weight(1.1f))
                Spacer(modifier = Modifier.width(10.dp))
                if (item.isMine) {
                    body_issykBlue(text = item.address,
                            modifier = Modifier.weight(4f))
                } else {
                    body_bran(text = item.address,
                            modifier = Modifier.weight(4f))
                }
                Spacer(modifier = Modifier.width(10.dp))
                body_bran(text = item.amount,
                        modifier = Modifier.weight(2.5f))
            }
            Divider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
            )
        }
    }
}



@Composable
fun VoterRecordScreen(
        viewModel: SafeFourVoteRecordViewModel
) {
    val uiState = viewModel.uiState
    val recordList = uiState.voteRecords
    if (recordList.isNullOrEmpty()) {
        Column() {
            Spacer(modifier = Modifier.height(16.dp))
            if (recordList == null) {
                ListEmptyView2(
                        text = stringResource(R.string.Transactions_WaitForSync),
                        icon = R.drawable.ic_clock
                )
            } else {
                ListEmptyView2(
                        text = stringResource(R.string.Safe_Four_No_Vote_Record),
                        icon = R.drawable.ic_no_data
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        Column(modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ComposeAppTheme.colors.lawrence)) {
            Column {
                Row(modifier = Modifier
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 16.dp)
                ) {
                    subhead1_leah(text = stringResource(R.string.Safe_Four_Node_Vote_Record_Address),
                            modifier = Modifier.weight(4f))

                    subhead1_leah(text = stringResource(R.string.Safe_Four_Node_Vote_Record_Num),
                            modifier = Modifier.weight(2f))
                }
                Divider(
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                )
            }
            recordList.forEach { item ->
                Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp, bottom = 10.dp)) {
                    if (item.isMine) {
                        body_issykBlue(text = item.address,
                                modifier = Modifier.weight(4f))
                    } else {
                        body_bran(text = item.address,
                                modifier = Modifier.weight(4f))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    body_bran(text = item.lockValue,
                            modifier = Modifier.weight(2f))
                }

                Divider(
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                )
            }
        }
    }
}


private fun getRecordBottomReachedRank(nodeList: List<VoteRecordView>): Int? {
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (nodeList.size > 4) nodeList.size - 4 else 0

    return nodeList.getOrNull(index)?.index
}