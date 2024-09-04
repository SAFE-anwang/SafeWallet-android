package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.bankwallet.modules.balance.BalanceCache
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch

class RedeemSafe3SelectFragment(): BaseComposeFragment() {
	@Composable
	override fun GetContent(navController: NavController) {
		val input = navController.getInput<RedeemSafe3Module.Input>()
		val wallet = input?.wallet ?: return
		val safe3Wallet = input.safe3Wallet
		val viewModelSelect by viewModels<RedeemSafe3SelectViewModel> { RedeemSafe3Module.Factory(wallet) }
		RedeemSafe3SelectScreen(navController = navController, viewModelSelect,  wallet, safe3Wallet)
	}
}

@Composable
fun RedeemSafe3SelectScreen(
		navController: NavController,
		viewModelSelect: RedeemSafe3SelectViewModel,
		wallet: Wallet,
		safe3Wallet: Wallet?,
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
		safe3Wallet: Wallet?,
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
				val viewModel = viewModel<RedeemSafe3ViewModel>(factory = RedeemSafe3Module.Factory(wallet) )
				RedeemSafe3Screen(viewModel = viewModel, navController = navController)
				viewModelSelect.updateSyncStatus(false)
			}
			1 -> {
				var safeWallet = safe3Wallet
				if (safe3Wallet == null) {
					val walletList: List<Wallet> = App.walletManager.activeWallets
					for (it in walletList) {
						if (it.token.blockchain.type is BlockchainType.Safe && it.coin.uid == "safe-coin") {
							safeWallet = it
						}
					}
				}
				if (safeWallet == null) {
					Toast.makeText(navController.context, Translator.getString(R.string.Safe4_Wallet_Tips, "Safe3"), Toast.LENGTH_SHORT).show()
					coroutineScope.launch {
						pagerState.scrollToPage(0)
					}
					return@HorizontalPager
				}
				val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
                val state =  balanceAdapterRepository.state(safeWallet)
				if (state !is AdapterState.Synced) {
					Toast.makeText(navController.context, Translator.getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
					coroutineScope.launch {
						pagerState.scrollToPage(0)
					}
					return@HorizontalPager
				}
				val viewModel = viewModel<RedeemSafe3LocalViewModel> (factory = RedeemSafe3Module.Factory2(wallet, safe3Wallet!!))
				RedeemSafe3LocalScreen(viewModel = viewModel) {
					viewModelSelect.updateSyncStatus(it)
				}
			}
		}
	}
}