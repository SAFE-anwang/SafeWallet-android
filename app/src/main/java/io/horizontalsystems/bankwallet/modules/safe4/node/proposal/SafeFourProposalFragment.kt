package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.safe4.node.HintView
import io.horizontalsystems.bankwallet.modules.safe4.node.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch

class SafeFourProposalFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {

        val address = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit?.receiveAddress
        if (address == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val input = navController.getInput<SafeFourProposalModule.Input>()
        val wallet = input?.wallet ?: return
        val viewModel by viewModels<SafeFourProposalViewModel> { SafeFourProposalModule.Factory( wallet) }


        TabScreen(navController, viewModel)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabScreen(
        navController: NavController,
        viewModel: SafeFourProposalViewModel,
) {

    val tabs = viewModel.tabs
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
                title = stringResource(id = R.string.Safe_Four_Proposal_Title),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf(
                        MenuItem(
                                title = TranslatableString.ResString(R.string.Safe_Four_Proposal_Create_Button),
                                onClick = {
                                    navController.slideFromBottom(
                                            R.id.createProposalFragment,
                                            viewModel.wallet
                                    )
                                }
                        )
                )

        )

        HintView(textId = R.string.Safe_Four_Proposal_Hint)

        val selectedTab = tabs[pagerState.currentPage]
        val tabItems = tabs.map {
            TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
        }
        Tabs(tabItems, onClick = { tab ->
            coroutineScope.launch {
                pagerState.scrollToPage(tab.ordinal)
            }
        })
        Spacer(modifier = Modifier.height(2.dp))
        SearchBar(
                searchHintText = stringResource(R.string.Proposal_Search),
                focusRequester = focusRequester,
                onClose = { viewModel.clearQuery() },
                onSearchTextChanged = { query -> viewModel.searchByQuery(query) }
        )
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalPager(
                state = pagerState,
                userScrollEnabled = false
        ) { page ->
            when (tabs[page]) {
                SafeFourProposalModule.Tab.AllProposal -> {
                    viewModel.currentScreen = 0
                    SafeFourProposalScreen(viewModel, navController)
                }

                SafeFourProposalModule.Tab.MineProposal -> {
                    viewModel.currentScreen = 1
                    SafeFourProposalScreen(viewModel, navController, true)
                }
            }
        }
    }
}