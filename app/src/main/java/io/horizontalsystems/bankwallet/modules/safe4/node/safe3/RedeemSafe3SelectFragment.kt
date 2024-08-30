package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.safe4.Safe4Module
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.SearchBar
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_bran
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class RedeemSafe3SelectFragment(): BaseComposeFragment() {
	@Composable
	override fun GetContent(navController: NavController) {
		val input = navController.getInput<RedeemSafe3Module.Input>()
		val wallet = input?.wallet ?: return
		val safe3Wallet = input?.safe3Wallet ?: return
		val viewModelSelect by viewModels<RedeemSafe3SelectViewModel> { RedeemSafe3Module.Factory(wallet, safe3Wallet) }
		RedeemSafe3SelectScreen(navController = navController, viewModelSelect,  wallet, safe3Wallet)
	}
}

@Composable
fun RedeemSafe3SelectScreen(
		navController: NavController,
		viewModelSelect: RedeemSafe3SelectViewModel,
		wallet: Wallet,
		safe3Wallet: Wallet,
) {

	Column(modifier = Modifier
			.background(color = ComposeAppTheme.colors.tyler)) {
		AppBar(
				title = stringResource(id = R.string.Redeem_Safe3_Title),
				showSpinner = viewModelSelect.uiState.syncing,
				navigationIcon = {
					HsBackButton(onClick = { navController.popBackStack() })
				}
		)

		TabScreen(navController = navController, viewModelSelect, wallet, safe3Wallet)
	}
}

@Composable
fun TabScreen(
		navController: NavController,
		viewModelSelect: RedeemSafe3SelectViewModel,
		wallet: Wallet,
		safe3Wallet: Wallet,
) {

	val tabs = viewModelSelect.tabs

	val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
	val coroutineScope = rememberCoroutineScope()
	val selectedTab = tabs[pagerState.currentPage]

	val tabItems = tabs.map {
		TabItem(stringResource(id = it.second), it == selectedTab, it)
	}

	Tabs(tabItems, onClick = { tab ->
		coroutineScope.launch {
			pagerState.scrollToPage(tab.first)
		}
	})
	Spacer(modifier = Modifier.height(2.dp))
	HorizontalPager(
			state = pagerState,
			userScrollEnabled = false
	) { page ->
		when (page) {
			0 -> {
				val viewModel = viewModel<RedeemSafe3ViewModel>(factory = RedeemSafe3Module.Factory(wallet, safe3Wallet) )
				RedeemSafe3Screen(viewModel = viewModel, navController = navController)
				viewModelSelect.updateSyncStatus(false)
			}
			1 -> {
				val viewModel = viewModel<RedeemSafe3LocalViewModel> (factory = RedeemSafe3Module.Factory(wallet, safe3Wallet))
				RedeemSafe3LocalScreen(viewModel = viewModel) {
					viewModelSelect.updateSyncStatus(it)
				}
			}
		}
	}
}