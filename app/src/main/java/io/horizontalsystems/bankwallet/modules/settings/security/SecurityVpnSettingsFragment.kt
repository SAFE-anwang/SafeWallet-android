package io.horizontalsystems.bankwallet.modules.settings.security

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import com.v2ray.ang.util.Utils
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.settings.security.fallbackblock.BottomSheetFallbackBlockSelectDialog
import io.horizontalsystems.bankwallet.modules.settings.security.fallbackblock.FallbackBlockModule
import io.horizontalsystems.bankwallet.modules.settings.security.fallbackblock.FallbackBlockViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecurityPasscodeSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecuritySettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.tor.SecurityTorSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.tor.SecurityTorSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.ui.PasscodeBlock
import io.horizontalsystems.bankwallet.modules.settings.security.ui.TorBlock
import io.horizontalsystems.bankwallet.modules.settings.security.ui.*
import io.horizontalsystems.bankwallet.modules.settings.security.vpn.SecurityVpnSettingsViewModel
import io.horizontalsystems.bankwallet.net.VpnConnectService
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.marketkit.models.Blockchain
import kotlin.system.exitProcess

class SecuritySettingsFragment : BaseComposeFragment() {

    private val torViewModel by viewModels<SecurityTorSettingsViewModel> {
        SecurityTorSettingsModule.Factory()
    }

    private val securitySettingsViewModel by viewModels<SecuritySettingsViewModel> {
        SecurityPasscodeSettingsModule.Factory()
    }

    private val fallbackBlockViewModel by viewModels<FallbackBlockViewModel> {
        FallbackBlockModule.Factory()
    }

    @Composable
    override fun GetContent() {
        val vpnViewModel = SecurityVpnSettingsViewModel(requireContext().getSharedPreferences("vpnSetting", Context.MODE_PRIVATE)) { connectState ->
            context?.let {
                if (connectState) {
                    VpnConnectService.startVpn(requireActivity())
                } else {
                    Utils.stopVService(it)
                }
            }
        }
        ComposeAppTheme {
            SecurityCenterScreen(
                securitySettingsViewModel = securitySettingsViewModel,
                torViewModel = torViewModel,
                vpnViewModel = vpnViewModel,
                fallbackBlockViewModel = fallbackBlockViewModel,
                navController = findNavController(),
                showAppRestartAlert = { showAppRestartAlert() },
                restartApp = { restartApp() },fallbackClick = {
                    showFallbackSelectorDialog(it)
                }
            )
        }
    }

    private fun showAppRestartAlert() {
        val warningTitle = if (torViewModel.torCheckEnabled) {
            getString(R.string.Tor_Connection_Enable)
        } else {
            getString(R.string.Tor_Connection_Disable)
        }

        val actionButton = if (torViewModel.torCheckEnabled) {
            getString(R.string.Button_Enable)
        } else {
            getString(R.string.Button_Disable)
        }

        ConfirmationDialog.show(
            icon = R.drawable.ic_tor_connection_24,
            title = getString(R.string.Tor_Alert_Title),
            warningTitle = warningTitle,
            warningText = getString(R.string.SettingsSecurity_AppRestartWarning),
            actionButtonTitle = actionButton,
            transparentButtonTitle = getString(R.string.Alert_Cancel),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    torViewModel.setTorEnabled()
                }

                override fun onTransparentButtonClick() {
                    torViewModel.resetSwitch()
                }

                override fun onCancelButtonClick() {
                    torViewModel.resetSwitch()
                }
            }
        )
    }

    private fun showFallbackBlockAlert(blockchain: Blockchain, year: Int, month: Int) {
        val warningTitle = getString(R.string.fallback_block_title)

        ConfirmationDialog.show(
            icon = R.drawable.ic_safe_20,
            title = getString(R.string.fallback_block_title),
            warningTitle = warningTitle,
            warningText = getString(R.string.fallback_Warning),
            actionButtonTitle = getString(R.string.Alert_fallback),
            transparentButtonTitle = getString(R.string.Alert_fallback_Cancel),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    fallbackBlockViewModel.fallback(blockchain, year, month)
                    setNavigationResult("fallbackHeight", bundleOf("isFallback" to true))
                    findNavController().popBackStack()

                }

                override fun onTransparentButtonClick() {

                }

                override fun onCancelButtonClick() {

                }
            }
        )
    }

    private fun showFallbackSelectorDialog(blockchain: Blockchain) {
        val dialog = BottomSheetFallbackBlockSelectDialog()
        dialog.items = fallbackBlockViewModel.itemsTime
        dialog.onSelectListener = {
            showFallbackBlockAlert(blockchain, it.year, it.month)
        }

        dialog.show(childFragmentManager, "selector_dialog")
    }

    private fun restartApp() {
        activity?.let {
            MainModule.startAsNewTask(it)
            exitProcess(0)
        }
    }
}

@Composable
private fun SecurityCenterScreen(
    securitySettingsViewModel: SecuritySettingsViewModel,
    torViewModel: SecurityTorSettingsViewModel,
    vpnViewModel: SecurityVpnSettingsViewModel,
    fallbackBlockViewModel: FallbackBlockViewModel,
    navController: NavController,
    showAppRestartAlert: () -> Unit,
    restartApp: () -> Unit,
    fallbackClick: (Blockchain) -> Unit
) {

    DisposableLifecycleCallbacks(
            onResume = {
                securitySettingsViewModel.update()
            },
    )

    if (torViewModel.restartApp) {
        restartApp()
        torViewModel.appRestarted()
    }

    val uiState = securitySettingsViewModel.uiState
    Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                        title = stringResource(R.string.Settings_SecurityCenter),
                        navigationIcon = {
                            HsBackButton(onClick = { navController.popBackStack() })
                        },
                )
            }
    ) {
        Column(
                Modifier
                        .padding(it)
                        .verticalScroll(rememberScrollState())
        ) {
            PasscodeBlock(
                    securitySettingsViewModel,
                    navController
            )

            VSpacer(height = 32.dp)

            CellUniversalLawrenceSection {
                SecurityCenterCell(
                        start = {
                            Icon(
                                    painter = painterResource(id = R.drawable.ic_off_24),
                                    tint = ComposeAppTheme.colors.grey,
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = null
                            )
                        },
                        center = {
                            body_leah(
                                    text = stringResource(id = R.string.Appearance_BalanceAutoHide),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                            )
                        },
                        end = {
                            HsSwitch(
                                    checked = uiState.balanceAutoHideEnabled,
                                    onCheckedChange = {
                                        securitySettingsViewModel.onSetBalanceAutoHidden(it)
                                    }
                            )
                        }
                )
            }
            InfoText(
                    text = stringResource(R.string.Appearance_BalanceAutoHide_Description),
                    paddingBottom = 32.dp
            )

//            item {
//                VSpacer(height = 24.dp)
                HeaderText(stringResource(R.string.SecurityCenter_Internet))
                VpnBlock(
                        vpnViewModel,
                        showAppRestartAlert,
                )
                VSpacer(height = 5.dp)
                TorBlock(
                        torViewModel,
                        showAppRestartAlert,
                )

                DuressPasscodeBlock(
                        securitySettingsViewModel,
                        navController
                )
                InfoText(text = stringResource(R.string.SettingsSecurity_DuressPinDescription))

//                VSpacer(height = 32.dp)
//            }

//            item {
                VSpacer(height = 24.dp)
                HeaderText(stringResource(R.string.fallback_block_title))
                FallBlockBlock(fallbackBlockViewModel, fallbackClick)
//            }

        }
    }
}

@Composable
fun SecurityCenterCell(
    start: @Composable RowScope.() -> Unit,
    center: @Composable RowScope.() -> Unit,
    end: @Composable() (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        start.invoke(this)
        Spacer(Modifier.width(16.dp))
        center.invoke(this)
        end?.let {
            Spacer(
                Modifier
                    .defaultMinSize(minWidth = 8.dp)
                    .weight(1f)
            )
            end.invoke(this)
        }
    }
}