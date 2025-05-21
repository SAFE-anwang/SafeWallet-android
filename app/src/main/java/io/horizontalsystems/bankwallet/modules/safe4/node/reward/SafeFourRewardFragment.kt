package io.horizontalsystems.bankwallet.modules.safe4.node.reward

import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawConfirmationDialog
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch

class SafeFourRewardFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {

        val address = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit?.receiveAddress
        if (address == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val input = navController.getInput<SafeFourRewardModule.Input>()
        val wallet = input?.wallet ?: return
        val viewModel by viewModels<SafeFourRewardViewModel> { SafeFourRewardModule.Factory(wallet) }
        RewardInfoScreen(navController = navController, viewModel = viewModel)
    }


}


@Composable
fun RewardInfoScreen(
        navController: NavController,
        viewModel: SafeFourRewardViewModel
) {
    val uiState = viewModel.uiState
    val rewardList = uiState.rewardList
    val proceedEnabled = uiState.canWithdraw
    val sendResult = viewModel.sendResult
    val view = LocalView.current
    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                view,
                R.string.SAFE4_Withdraw_Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        }

        SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                view,
                R.string.SAFE4_Withdraw_Send_Success,
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
        Column() {
            AppBar(
                    title = stringResource(id = R.string.Safe_Four_Profit_Title),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    }
            )
            Surface(color = ComposeAppTheme.colors.lawrence,
                    modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))) {
            Scaffold(
                    backgroundColor = ComposeAppTheme.colors.lawrence,
                    topBar = {
                        Column(

                        ) {
                            Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                                body_leah(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                        text = stringResource(id = R.string.Safe_Four_Profit_Title_Date),
                                        maxLines = 1,
                                )
                                body_leah(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                        text = stringResource(id = R.string.Safe_Four_Profit_Title_Amount),
                                        maxLines = 1,
                                        textAlign = TextAlign.End
                                )
                            }
                            Divider(
                                    thickness = 1.dp,
                                    color = ComposeAppTheme.colors.steel10,
                            )
                        }
                    },
                bottomBar = {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        title = stringResource(R.string.SAFE4_Withdraw),
                        onClick = {
                            if (viewModel.hasConnection()) {
//                                viewModel.showConfirmation()
                                viewModel.withdraw()
                            } else {
                                HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                            }
                        },
                        enabled = proceedEnabled
                    )
                }
            ) { paddingValues ->
                if (rewardList.isNullOrEmpty()) {
                    Column(Modifier.padding(paddingValues)) {
                        if (rewardList == null) {
                            ListEmptyView(
                                    text = stringResource(R.string.Transactions_WaitForSync),
                                    icon = R.drawable.ic_clock
                            )
                        } else {
                            ListEmptyView(
                                    text = stringResource(R.string.Safe_Four_No_Profit),
                                    icon = R.drawable.ic_no_data
                            )
                        }
                    }
                } else {
                    val listState = rememberLazyListState()

                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(Modifier.padding(paddingValues), state = listState) {

                        itemsIndexed(
                                items = rewardList
                        ) { index, item ->
                            Column(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                            ) {
                                Row {
                                    val color = if (item.rewarded) {
                                        ComposeAppTheme.colors.leah
                                    } else {
                                        ComposeAppTheme.colors.issykBlue
                                    }
                                    Text(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                        text = item.date,
                                        maxLines = 1,
                                        style = ComposeAppTheme.typography.body,
                                        color = color
                                    )
                                    Text(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                        text = item.amount,
                                        maxLines = 1,
                                        textAlign = TextAlign.End,
                                        style = ComposeAppTheme.typography.body,
                                        color = color
                                    )
                                }
                            }

                            Divider(
                                    thickness = 1.dp,
                                    color = ComposeAppTheme.colors.steel10,
                            )
                        }
                    }
                }

            }
        }
        if (uiState.showConfirmDialog) {
            WithdrawConfirmationDialog(
                content = stringResource(R.string.SAFE4_Withdraw_Profit_Hint),
                {
                    viewModel.withdraw()
                }, {
                    viewModel.closeDialog()
                }
            )
        }
    }
}