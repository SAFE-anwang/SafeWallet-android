package io.horizontalsystems.bankwallet.modules.main

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.findActivity
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceScreen
import io.horizontalsystems.bankwallet.modules.dapp.DAppBrowseFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.market.MarketScreen
import io.horizontalsystems.bankwallet.modules.rateapp.RateApp
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesFragment
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceModule
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceScreen
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceViewModel
import io.horizontalsystems.bankwallet.modules.safe4.Safe4Module
import io.horizontalsystems.bankwallet.modules.safe4.Safe4Screen
import io.horizontalsystems.bankwallet.modules.safe4.Safe4ViewModel
import io.horizontalsystems.bankwallet.modules.sendtokenselect.SendTokenSelectFragment
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsScreen
import io.horizontalsystems.bankwallet.modules.tor.TorStatusView
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsScreen
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.tg.StartTelegramsService
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager.SupportState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeText
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.HsNavigationBarItem
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.HsNavigationBarItemDefaults
import kotlinx.coroutines.delay

class MainFragment : BaseComposeFragment() {
    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()

//    private val transactionsViewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }
    private val safe4ViewModel by viewModels<Safe4ViewModel> { Safe4Module.Factory() }
    private var startTelegramService: StartTelegramsService? = null
    private var intentUri: Uri? = null

    @Composable
    override fun GetContent(navController: NavController) {
        val backStackEntry = navController.safeGetBackStackEntry(R.id.mainFragment)

        backStackEntry?.let {
            val viewModel =
                ViewModelProvider(backStackEntry.viewModelStore, TransactionsModule.Factory())
                    .get(TransactionsViewModel::class.java)
            MainScreenWithRootedDeviceCheck(
//                transactionsViewModel = viewModel,
                deepLink = intentUri,
                navController = navController,
                mainActivityViewModel = mainActivityViewModel,
                clearActivityData = { activity?.intent?.data = null },
                safe4ViewModel = safe4ViewModel,
                openLink = {
                    openLink(it)
                }
            )
        } ?: run {
            // Back stack entry doesn't exist, restart activity
            val intent = Intent(context, MainActivity::class.java)
            requireActivity().startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().moveTaskToBack(true)
                }
            })
    }

    private fun joinTelegramGroup(groupLink: String) {
        if (startTelegramService == null) {
            startTelegramService = StartTelegramsService(requireActivity())
        }
        startTelegramService?.join(groupLink)
    }

    private fun openLink(url: String) {
        context?.let {
            when(url) {
                App.appConfigProvider.appTelegramLink -> {
                    joinTelegramGroup(url)
                }
                App.appConfigProvider.supportEmail -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    requireActivity().startActivity(intent)
                }
                App.appConfigProvider.safeBSCPancakeswap,
                App.appConfigProvider.safeEthUniswap -> {
                    findNavController().slideFromRight(R.id.dappBrowseFragment,
                            DAppBrowseFragment.Input(url, url.subSequence(8, url.length).toString())
                    )
                }
                else -> {
                    startActivity(Intent(requireActivity(), WebViewActivity::class.java).apply {
                        putExtra("url", url)
                        putExtra("name", url.subSequence(8, url.length))
                    })
                }
            }
        }

    }

    /*fun openBalanceFragment() {
        binding.viewPager.setCurrentItem(1, false)
    }*/
}

@Composable
private fun MainScreenWithRootedDeviceCheck(
//    transactionsViewModel: TransactionsViewModel,
    deepLink: Uri?,
    navController: NavController,
    rootedDeviceViewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory()),
    mainActivityViewModel: MainActivityViewModel,
    clearActivityData: () -> Unit,
    safe4ViewModel: Safe4ViewModel,
    openLink: (String) -> Unit
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(mainActivityViewModel, /*transactionsViewModel, */deepLink, navController, clearActivityData, safe4ViewModel = safe4ViewModel, openLink = openLink)
    }
}

@Composable
private fun MainScreen(
    mainActivityViewModel: MainActivityViewModel,
    deepLink: Uri?,
    fragmentNavController: NavController,
    clearActivityData: () -> Unit,
    viewModel: MainViewModel = viewModel(factory = MainModule.Factory()),
    safe4ViewModel: Safe4ViewModel,
    openLink: (String) -> Unit
) {
    val activityIntent by mainActivityViewModel.intentLiveData.observeAsState()
    LaunchedEffect(activityIntent) {
        activityIntent?.data?.let {
            mainActivityViewModel.intentHandled()
            viewModel.handleDeepLink(it)
        }
    }

    val uiState = viewModel.uiState

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(ComposeAppTheme.colors.blade)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if (uiState.torEnabled) {
                    TorStatusView()
                }
                NavigationBar(
                    modifier = Modifier.height(56.dp),
                    containerColor = ComposeAppTheme.colors.blade,
                ) {
                    uiState.mainNavItems.forEach { destination ->
                        HsNavigationBarItem(
                            selected = destination.selected,
                            onClick = {
                                if (destination.mainNavItem == MainNavigation.Tg) {
                                    openLink.invoke(App.appConfigProvider.appTelegramLink)
                                } else {
                                    viewModel.onSelect(destination.mainNavItem)
                                    stat(
                                        page = StatPage.Main,
                                        event = StatEvent.SwitchTab(destination.mainNavItem.statTab)
                                    )
                                }
                            },
                            onLongClick = if (destination.selected && destination.mainNavItem == MainNavigation.Balance) {
                                {
                                    fragmentNavController.slideFromBottom(R.id.walletSwitchDialog)
                                    stat(
                                        page = StatPage.Main,
                                        event = StatEvent.Open(StatPage.SwitchWallet)
                                    )
                                }
                            } else null,
                            enabled = destination.enabled,
                            colors = HsNavigationBarItemDefaults.colors(
                                selectedIconColor = ComposeAppTheme.colors.jacob,
                                unselectedIconColor = ComposeAppTheme.colors.grey,
                                indicatorColor = ComposeAppTheme.colors.transparent,
                                selectedTextColor = ComposeAppTheme.colors.jacob,
                                unselectedTextColor = ComposeAppTheme.colors.grey,
                            ),
                            icon = {
                                BadgedIcon(destination.badge) {
                                    Icon(
                                        painter = painterResource(destination.mainNavItem.iconRes),
                                        contentDescription = stringResource(destination.mainNavItem.titleRes)
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column {
            Crossfade(uiState.selectedTabItem) { navItem ->
                when (navItem) {
                    MainNavigation.Market -> MarketScreen(fragmentNavController)
                    MainNavigation.Balance -> BalanceScreen(fragmentNavController)
                    /*MainNavigation.Transactions -> TransactionsScreen(
                        fragmentNavController,
                        transactionsViewModel
                    )*/
                    MainNavigation.Safe4 -> Safe4Screen(safe4ViewModel, fragmentNavController, openLink)
                    MainNavigation.Tg -> {
                        null
                    }

                    MainNavigation.Settings -> SettingsScreen(fragmentNavController, mainViewModel = viewModel)
                }
            }
        }
    }

    if (uiState.showWhatsNew) {
        LaunchedEffect(Unit) {
            fragmentNavController.slideFromBottom(
                R.id.releaseNotesFragment,
                ReleaseNotesFragment.Input(true)
            )
            viewModel.whatsNewShown()
        }
    }

    if (uiState.showDonationPage) {
        LaunchedEffect(Unit) {
            fragmentNavController.slideFromBottom(R.id.whyDonateFragment)
            viewModel.donationShown()
        }
    }

    if (uiState.showRateAppDialog) {
        val context = LocalContext.current
        RateApp(
            onRateClick = {
                RateAppManager.openPlayMarket(context)
                viewModel.closeRateDialog()
            },
            onCancelClick = { viewModel.closeRateDialog() }
        )
    }

    if (uiState.wcSupportState != null) {
        when (val wcSupportState = uiState.wcSupportState) {
            SupportState.NotSupportedDueToNoActiveAccount -> {
                fragmentNavController.slideFromBottom(R.id.wcErrorNoAccountFragment)
            }

            is SupportState.NotSupportedDueToNonBackedUpAccount -> {
                val text = stringResource(R.string.WalletConnect_Error_NeedBackup)
                fragmentNavController.slideFromBottom(
                    R.id.backupRequiredDialog,
                    BackupRequiredDialog.Input(wcSupportState.account, text)
                )

                stat(page = StatPage.Main, event = StatEvent.Open(StatPage.BackupRequired))
            }

            is SupportState.NotSupported -> {
                fragmentNavController.slideFromBottom(
                    R.id.wcAccountTypeNotSupportedDialog,
                    WCAccountTypeNotSupportedDialog.Input(wcSupportState.accountTypeDescription)
                )
            }

            else -> {}
        }
        viewModel.wcSupportStateHandled()
    }

    uiState.deeplinkPage?.let { deepLinkPage ->
        LaunchedEffect(Unit) {
            delay(500)
            fragmentNavController.slideFromRight(
                deepLinkPage.navigationId,
                deepLinkPage.input
            )
            viewModel.deeplinkPageHandled()
        }
    }

    uiState.openSend?.let { openSend ->
        fragmentNavController.slideFromRight(
            R.id.sendTokenSelectFragment,
            SendTokenSelectFragment.Input(
                openSend.blockchainTypes,
                openSend.tokenTypes,
                openSend.address,
                openSend.amount,
                openSend.memo,
            )
        )
        viewModel.onSendOpened()
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }
}

@Composable
private fun BadgedIcon(
    badge: MainModule.BadgeType?,
    icon: @Composable BoxScope.() -> Unit,
) {
    when (badge) {
        is MainModule.BadgeType.BadgeNumber ->
            BadgedBox(
                badge = {
                    BadgeText(
                        text = badge.number.toString(),
                    )
                },
                content = icon
            )

        MainModule.BadgeType.BadgeDot ->
            BadgedBox(
                badge = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = 7.dp, y = (-9).dp)
                            .background(
                                ComposeAppTheme.colors.lucian,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) { }
                },
                content = icon
            )

        else -> {
            Box {
                icon()
            }
        }
    }
}

fun NavController.safeGetBackStackEntry(destinationId: Int): NavBackStackEntry? {
    return try {
        this.getBackStackEntry(destinationId)
    } catch (e: IllegalArgumentException) {
        null
    }
}