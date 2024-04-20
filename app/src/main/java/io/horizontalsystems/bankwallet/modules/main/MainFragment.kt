package io.horizontalsystems.bankwallet.modules.main

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
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
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsScreen
import io.horizontalsystems.bankwallet.modules.tor.TorStatusView
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsScreen
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.tg.StartTelegramsService
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager.SupportState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import io.horizontalsystems.bankwallet.ui.compose.components.HsBottomNavigation
import io.horizontalsystems.bankwallet.ui.compose.components.HsBottomNavigationItem
import io.horizontalsystems.bankwallet.ui.extensions.WalletSwitchBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.telegram.ui.LaunchActivity

//import org.drinkless.td.libcore.telegram.TdAuthManager

class MainFragment : BaseComposeFragment() {

//    private val transactionsViewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }
    private val safe4ViewModel by viewModels<Safe4ViewModel> { Safe4Module.Factory() }
    private var startTelegramService: StartTelegramsService? = null
    private var intentUri: Uri? = null

    @Composable
    override fun GetContent(navController: NavController) {
        ComposeAppTheme {
            MainScreenWithRootedDeviceCheck(
//                transactionsViewModel = transactionsViewModel,
                deepLink = intentUri,
                navController = navController,
                clearActivityData = { activity?.intent?.data = null },
                safe4ViewModel = safe4ViewModel,
                openLink = {
                    openLink(it)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentUri = activity?.intent?.data
        activity?.intent?.data = null //clear intent data

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
    clearActivityData: () -> Unit,
    rootedDeviceViewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory()),
    safe4ViewModel: Safe4ViewModel,
    openLink: (String) -> Unit
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(/*transactionsViewModel, */deepLink, navController, clearActivityData, safe4ViewModel = safe4ViewModel, openLink = openLink)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
//    transactionsViewModel: TransactionsViewModel,
    deepLink: Uri?,
    fragmentNavController: NavController,
    clearActivityData: () -> Unit,
    viewModel: MainViewModel = viewModel(factory = MainModule.Factory(deepLink)),
    safe4ViewModel: Safe4ViewModel,
    openLink: (String) -> Unit
) {

    val uiState = viewModel.uiState
    val selectedPage = uiState.selectedTabIndex
    val pagerState = rememberPagerState(initialPage = selectedPage) { uiState.mainNavItems.size }

    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            WalletSwitchBottomSheet(
                wallets = viewModel.wallets,
                watchingAddresses = viewModel.watchWallets,
                selectedAccount = uiState.activeWallet,
                onSelectListener = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                        viewModel.onSelect(it)
                    }
                },
                onCancelClick = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }
                }
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                backgroundColor = ComposeAppTheme.colors.tyler,
                bottomBar = {
                    Column {
                        if (uiState.torEnabled) {
                            TorStatusView()
                        }
                        HsBottomNavigation(
                            backgroundColor = ComposeAppTheme.colors.tyler,
                            elevation = 10.dp
                        ) {
                            uiState.mainNavItems.forEach { item ->
                                HsBottomNavigationItem(
                                    icon = {
                                        BadgedIcon(item.badge) {
                                            Icon(
                                                painter = painterResource(item.mainNavItem.iconRes),
                                                contentDescription = stringResource(item.mainNavItem.titleRes)
                                            )
                                        }
                                    },
                                    selected = item.selected,
                                    enabled = item.enabled,
                                    selectedContentColor = ComposeAppTheme.colors.jacob,
                                    unselectedContentColor = if (item.enabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.grey50,
                                    onClick = {
                                        if (item.mainNavItem == MainNavigation.Tg) {
                                            openLink.invoke(App.appConfigProvider.appTelegramLink)
                                        } else {
                                            viewModel.onSelect(item.mainNavItem)
                                        }
                                    },
                                    onLongClick = {
                                        if (item.mainNavItem == MainNavigation.Balance) {
                                            coroutineScope.launch {
                                                modalBottomSheetState.show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) {
                BackHandler(enabled = modalBottomSheetState.isVisible) {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }
                }
                Column(modifier = Modifier.padding(it)) {
                    LaunchedEffect(key1 = selectedPage, block = {
                        pagerState.scrollToPage(selectedPage)
                    })

                    HorizontalPager(
                        modifier = Modifier.weight(1f),
                        state = pagerState,
                        userScrollEnabled = false,
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        when (uiState.mainNavItems[page].mainNavItem) {
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
            HideContentBox(uiState.contentHidden)
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

    DisposableLifecycleCallbacks(
        onResume = viewModel::onResume,
    )
}

@Composable
private fun HideContentBox(contentHidden: Boolean) {
    val backgroundModifier = if (contentHidden) {
        Modifier.background(ComposeAppTheme.colors.tyler)
    } else {
        Modifier
    }
    Box(Modifier.fillMaxSize().then(backgroundModifier))
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
                    Badge(
                        backgroundColor = ComposeAppTheme.colors.lucian
                    ) {
                        Text(
                            text = badge.number.toString(),
                            style = ComposeAppTheme.typography.micro,
                            color = ComposeAppTheme.colors.white,
                        )
                    }
                },
                content = icon
            )

        MainModule.BadgeType.BadgeDot ->
            BadgedBox(
                badge = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
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
