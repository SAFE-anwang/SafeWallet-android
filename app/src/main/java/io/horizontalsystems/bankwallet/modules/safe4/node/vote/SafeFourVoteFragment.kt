package io.horizontalsystems.bankwallet.modules.safe4.node.vote

import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance
import io.horizontalsystems.bankwallet.modules.safe4.node.ListEmptyView2
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeType
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.confirmation.SafeFourVoteConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.ui.SuggestionsBar
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.LightGrey50
import io.horizontalsystems.bankwallet.ui.compose.components.AdditionalDataCell2
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.BoxTyler44
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.SecondaryButtonDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.SecondaryButtonDefaults.buttonColors
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_green50
import io.horizontalsystems.bankwallet.ui.compose.components.body_issykBlue
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class SafeFourVoteFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val address = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit?.receiveAddress
        if (address == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }

        val navController = findNavController()
        val input = navController.requireInput<Input>()
        val wallet = input.wallet
        val title = input.title
        val nodeType = input.nodeType
        val nodeId = input.nodeId
        val nodeAddress = input.nodeAddress
        val viewModel by viewModels<SafeFourVoteViewModel> { SafeFourVoteModule.Factory(wallet, nodeId, address, nodeAddress, nodeType, input.isJoin) }
        val amountInputModeViewModel by viewModels<AmountInputModeViewModel> {
            AmountInputModeModule.Factory(wallet.coin.uid)
        }

        val voteRecordViewModel by viewModels<SafeFourVoteRecordViewModel> {
            SafeFourVoteModule.FactoryRecord(input.wallet, input.nodeAddress, NodeType.getType(nodeType) == NodeType.SuperNode, nodeId)
        }
//        if (NodeType.getType(nodeType) == NodeType.SuperNode) {
            TabScreen(title, navController, viewModel, amountInputModeViewModel, nodeType, voteRecordViewModel, input)
//        } else {
//            MasterVoteScreen(title, navController, viewModel, amountInputModeViewModel)
//        }
    }

    @Parcelize
    data class Input(
            val wallet: Wallet,
            val title: String,
            val nodeId: Int,
            val nodeType: Int,
            val nodeAddress: String,
            val isJoin: Boolean = false,
            val sendEntryPointDestId: Int = 0
    ) : Parcelable
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabScreen(
        title: String,
        navController: NavController,
        viewModel: SafeFourVoteViewModel,
        amountInputModeViewModel: AmountInputModeViewModel,
        nodeType: Int,
        voteRecordViewModel: SafeFourVoteRecordViewModel,
        input: SafeFourVoteFragment.Input
) {

    val tabs = viewModel.tabs
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val isJoin = input.isJoin
    val isSuperNode = NodeType.getType(nodeType) == NodeType.SuperNode

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
                title = title,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
        )
        if (isJoin) {
            MasterVoteScreen(title, navController, viewModel, amountInputModeViewModel)
        } else {
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
                    SafeFourVoteModule.Tab.SafeVote -> {
//                        if (isSuperNode) {
                            viewModel.setIsLockVote(false)
                            VoteScreen(title, navController, viewModel, amountInputModeViewModel, voteRecordViewModel)
                        /*} else {
                            MasterVoteScreen(title, navController, viewModel, amountInputModeViewModel)
                        }*/
                    }

                    SafeFourVoteModule.Tab.LockVote -> {
                        viewModel.setIsLockVote(true)
                        LockVoteScreen(navController, title, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun VoteScreen(
        title: String,
        navController: NavController,
        viewModel: SafeFourVoteViewModel,
        amountInputModeViewModel: AmountInputModeViewModel,
        voteRecordViewModel: SafeFourVoteRecordViewModel,
) {

    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val amountCaution = uiState.amountCaution
    val proceedEnabled = uiState.canBeSend
    val amountInputType = amountInputModeViewModel.inputType
    val view = LocalView.current

    val tabs2 = viewModel.tabs2
    val pagerState = rememberPagerState(initialPage = 0) { tabs2.size }
    val coroutineScope = rememberCoroutineScope()

    ComposeAppTheme {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(12.dp))
            AvailableBalance(
                    coinCode = wallet.coin.code,
                    coinDecimal = viewModel.coinMaxAllowedDecimals,
                    fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                    availableBalance = availableBalance,
                    amountInputType = amountInputType,
                    rate = viewModel.coinRate
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAmountInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    focusRequester = focusRequester,
                    availableBalance = availableBalance,
                    caution = amountCaution,
                    coinCode = wallet.coin.code,
                    coinDecimal = viewModel.coinMaxAllowedDecimals,
                    fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                    onClickHint = {
                        amountInputModeViewModel.onToggleInputType()
                    },
                    onValueChange = {
                        viewModel.onEnterAmount(it)
                    },
                    inputType = amountInputType,
                    rate = viewModel.coinRate
            )

            ButtonPrimaryYellow(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                    title = stringResource(R.string.Send_DialogProceed),
                    onClick = {
                        if (viewModel.hasConnection()) {
                            viewModel.getSafeVoteData()?.let {
                                navController.slideFromRight(
                                        R.id.voteConfirmationFragment,
                                        SafeFourVoteConfirmationModule.Input(title, viewModel.isSuper, it, wallet, R.id.voteFragment)
                                )
                            }
                        } else {
                            HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                        }
                    },
                    enabled = proceedEnabled
            )

            Column(modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeAppTheme.colors.lawrence)) {


                NodeInfoScreen(viewModel, isSuperNode = viewModel.isSuper)
            }

            val selectedTab = tabs2[pagerState.currentPage]
            val tabItems = tabs2.map {
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
                when (tabs2[page]) {

                    SafeFourVoteModule.Tab2.Creator -> {
                        CreatorScreen2(viewModel)
                    }

                    SafeFourVoteModule.Tab2.Voters -> {
                        VoterRecordScreen2(viewModel = voteRecordViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun LockVoteScreen(
        navController: NavController,
        title: String,
        viewModel: SafeFourVoteViewModel
) {
    val uiState = viewModel.uiState
    val proceedEnabled = uiState.recordVoteCanSend
    val nodeList = uiState.lockIdInfo
    val view = LocalView.current
    if (nodeList.isNullOrEmpty()) {
        Column() {
            if (nodeList == null) {
                ListEmptyView(
                        text = stringResource(R.string.Transactions_WaitForSync),
                        icon = R.drawable.ic_clock
                )
            } else {
                ListEmptyView(
                        text = stringResource(R.string.Safe_Four_No_Locked_Record),
                        icon = R.drawable.ic_no_data
                )
            }
        }
    } else {
        val listState = rememberLazyListState()
        var checked by rememberSaveable { mutableStateOf(false) }
        Scaffold(
                backgroundColor = ComposeAppTheme.colors.tyler,
                topBar = {
                    Column {
                        Row(modifier = Modifier
                                .wrapContentHeight()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
                            HsCheckbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        checked = it
                                        viewModel.selectAllLock(it)
                                    }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            subhead2_grey(text = stringResource(R.string.Safe_Four_Node_Vote_Lock_All))
                        }
                        Column(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ComposeAppTheme.colors.lawrence)
                        ) {
                            subhead2_grey(
                                    modifier = Modifier
                                            .padding(16.dp),
                                    text = stringResource(R.string.Safe_Four_Node_Vote_Lock_hint))
                        }
                    }
                },
                bottomBar = {
                    ButtonPrimaryYellow(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                            title = stringResource(R.string.Send_DialogProceed),
                            onClick = {
                                if (viewModel.hasConnection()) {
                                    viewModel.getLockVoteData()?.let {
                                        navController.slideFromRight(
                                                R.id.voteConfirmationFragment,
                                                SafeFourVoteConfirmationModule.Input(title, viewModel.isSuper, it, viewModel.wallet, R.id.voteFragment)
                                        )
                                    }
                                } else {
                                    HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                                }
                            },
                            enabled = proceedEnabled
                    )
                }
        ) {
            LazyColumn(modifier = Modifier.padding(it), state = listState) {
                lockedList(
                        lockIdsList = nodeList,
                        onCheckedChange = { id, checked ->
                            viewModel.checkLockVote(id, checked)
                        },
                        onBottomReached = {
                            viewModel.onBottomReached()
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.lockedList(
        lockIdsList: List<LockIdsView>,
        onCheckedChange: (Int, Boolean) -> Unit,
        onBottomReached: () -> Unit,
) {
    val bottomReachedRank = getBottomReachedRank(lockIdsList)
    val nColumns = 3
    val itemsCount = lockIdsList.size
    val rows = (itemsCount + nColumns - 1) / nColumns

    items(rows) { rowIndex ->
        Row(modifier = Modifier.fillMaxWidth()) {
            for(columnIndex in 0 until nColumns) {
                val itemIndex = rowIndex * 3 + columnIndex
                if (itemIndex < lockIdsList.size) {
                    val item = lockIdsList[itemIndex]

                    Column(
                            modifier = Modifier.weight(1f, fill = true),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HsCheckbox(
                                checked = item.checked,
                                enabled = item.enable,
                                onCheckedChange = {
                                    onCheckedChange.invoke(item.lockIds.toInt(), it)
                                }
                        )
                        val color = if (item.enable) {
                            ComposeAppTheme.colors.bran
                        } else {
                            if (App.localStorage.currentTheme == ThemeType.Blue) {
                                if (item.enable) ComposeAppTheme.colors.grey  else LightGrey50
                            } else {
                                ComposeAppTheme.colors.grey
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = stringResource(R.string.Safe_Four_Node_Vote_Lock_Id, item.lockIds),
                                style = ComposeAppTheme.typography.body,
                                color = color,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = item.lockValue,
                                style = ComposeAppTheme.typography.body,
                                color = color,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f, fill = true))
                }
                if (itemIndex == bottomReachedRank) {
                    onBottomReached.invoke()
                }
            }
        }
    }
}


private fun getBottomReachedRank(nodeList: List<LockIdsView>): Int? {
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (nodeList.size > 4) nodeList.size - 4 else 0

    return nodeList.getOrNull(index)?.index
}

@Composable
fun MasterVoteScreen(
        title: String,
        navController: NavController,
        viewModel: SafeFourVoteViewModel,
        amountInputModeViewModel: AmountInputModeViewModel
) {

    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val proceedEnabled = uiState.canBeSend
    val remainingShares = uiState.remainingShares
    val amountInputType = amountInputModeViewModel.inputType
    val joinAmountList = uiState.joinAmountList
    val view = LocalView.current

    ComposeAppTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            AvailableBalance(
                    coinCode = wallet.coin.code,
                    coinDecimal = viewModel.coinMaxAllowedDecimals,
                    fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                    availableBalance = availableBalance,
                    amountInputType = amountInputType,
                    rate = viewModel.coinRate
            )

            Spacer(modifier = Modifier.height(16.dp))
            AdditionalDataCell2 {
                subhead2_grey(text = stringResource(R.string.Safe_Four_Node_Remaining_Shares))

                Spacer(modifier = Modifier.weight(1f))

                subhead2_leah(text = remainingShares.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            subhead2_grey(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(R.string.Safe_Four_Node_Register_Num))

            JoinAmountBar(
                    percents = joinAmountList,
                    onSelect = {
                        viewModel.selectJoinAmount(it)
                    },
                    selectEnabled = true,
            )

            ButtonPrimaryYellow(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                    title = stringResource(R.string.Safe_Four_Node_Join),
                    onClick = {
                        if (viewModel.hasConnection()) {
                            viewModel.getAppendRegisterData()?.let {
                                navController.slideFromRight(
                                        R.id.voteConfirmationFragment,
                                        SafeFourVoteConfirmationModule.Input(title, viewModel.isSuper, it, wallet, R.id.voteFragment)
                                )
                            }
                        } else {
                            HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                        }
                    },
                    enabled = proceedEnabled
            )


            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeAppTheme.colors.lawrence)) {

                NodeInfoScreen(viewModel, viewModel.isSuper)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JoinAmountBar(
        modifier: Modifier = Modifier,
        percents: List<JoinAmount>,
        onSelect: (Int) -> Unit,
        selectEnabled: Boolean,
) {
    Column(modifier = modifier) {
        Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceAround
        ) {
            percents.forEachIndexed { index, joinAmount  ->
                if (index <= 2) JoinAmountView(modifier, joinAmount, onSelect, selectEnabled)
            }
        }
        if (percents.size > 3) {
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceAround
            ) {
                percents.forEachIndexed { index, joinAmount ->
                    if (index > 2) JoinAmountView(modifier, joinAmount, onSelect, selectEnabled)
                }
            }
        }
    }
}

@Composable
private fun JoinAmountView(
        modifier: Modifier = Modifier,
        percent: JoinAmount,
        onSelect: (Int) -> Unit,
        selectEnabled: Boolean,
) {
    ButtonSecondary(
            modifier = Modifier
                    .border(1.dp,
                            if (percent.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey,
                            RoundedCornerShape(14.dp))
            ,
            buttonColors = ButtonDefaults.buttonColors(
                    backgroundColor = ComposeAppTheme.colors.transparent,
                    contentColor = if (percent.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey
            ),
            enabled = selectEnabled,
            onClick = { onSelect.invoke(percent.value) }
    ) {
        Text(
                text = "${percent.value} SAFE",
                modifier = modifier,
                style = ComposeAppTheme.typography.subhead2,
                color = if (percent.selected) {
                    ComposeAppTheme.colors.jacob
                } else {
                    ComposeAppTheme.colors.grey
                },
        )
    }
}

@Composable
fun NodeInfoScreen(
        viewModel: SafeFourVoteViewModel,
        isSuperNode: Boolean
) {
    val nodeInfo = viewModel.uiState.nodeInfo ?: return
    val uiState = viewModel.uiState
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
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                text = nodeInfo.address.hex,
                color = ComposeAppTheme.colors.bran,
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
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                text = nodeInfo.creator.hex,
                color = ComposeAppTheme.colors.bran,
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
                    text = nodeInfo.voteCompleteCount,
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
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                text = nodeInfo.enode,
                color = ComposeAppTheme.colors.bran,
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
fun CreatorScreen2(
        viewModel: SafeFourVoteViewModel
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
fun VoterRecordScreen2(
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