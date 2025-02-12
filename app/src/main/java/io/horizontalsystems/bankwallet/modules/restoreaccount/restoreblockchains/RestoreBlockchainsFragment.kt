package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.getInputX
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.ZCashConfig
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsFragment
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.WalletType
import io.horizontalsystems.bankwallet.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.zcashconfigure.ZcashConfigure
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.delay
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class RestoreBlockchainsFragment : BaseFragment() {

    val vmFactory by lazy {
        val input = findNavController().getInput<Input>()
        RestoreBlockchainsModule.Factory(
            input!!.accountName!!,
            input.accountType!!,
            true,
            true
        )
    }

    var purpose: Int? = null

    private val viewModel by viewModels<RestoreBlockchainsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    val input = findNavController().getInput<Input>()
                    val popUpToInclusiveId = input?.popUpToInclusiveId ?: -1
                    ManageWalletsScreen(
                        findNavController(),
                        viewModel,
                        restoreSettingsViewModel,
                        popUpToInclusiveId
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observe()
        val input = findNavController().getInput<Input>()
        purpose = input?.purposeType
        purpose?.let {
            val viewItems = viewModel.viewItemsLiveData.value
            viewItems?.find {
                it.item.uid == "bitcoin"
            }?.let { blockchain ->
                viewModel.enable(blockchain.item, purpose)
            }

        }
    }

    private fun observe() {
        viewModel.successLiveEvent.observe(viewLifecycleOwner) {
            val input = findNavController().getInput<Input>()
            val popUpToInclusiveId = input?.popUpToInclusiveId ?: R.id.restoreAccountFragment

            val inclusive = input?.popOffInclusiveKey ?: false
            findNavController().popBackStack(
                popUpToInclusiveId,
                inclusive
            )

        }
    }

    private fun showBottomSelectorDialog(
        config: BottomSheetSelectorMultipleDialog.Config,
        onSelect: (indexes: List<Int>) -> Unit,
        onCancel: () -> Unit
    ) {
        BottomSheetSelectorMultipleDialog.show(
            fragmentManager = childFragmentManager,
            title = config.title,
            icon = config.icon,
            items = config.viewItems,
            selected = config.selectedIndexes,
            onItemSelected = { onSelect(it) },
            onCancelled = { onCancel() },
            warningTitle = config.descriptionTitle,
            warning = config.description,
            notifyUnchanged = true,
            allowEmpty = config.allowEmpty
        )
    }

    companion object {
        const val ACCOUNT_NAME_KEY = "account_name_key"
        const val ACCOUNT_TYPE_KEY = "account_type_key"
        const val PURPOSE_TYPE_KEY = "purpose_type_key"
    }

    @Parcelize
    data class Input(
            val accountName: String?,
            val accountType: AccountType?,
            val purposeType: Int = 0,
            val popUpToInclusiveId: Int = 0,
            val popOffInclusiveKey: Boolean = false,
            val walletType: WalletType? = null
    ) : Parcelable

}

@Composable
private fun ManageWalletsScreen(
        navController: NavController,
        viewModel: RestoreBlockchainsViewModel,
        restoreSettingsViewModel: RestoreSettingsViewModel,
        popUpToInclusiveId: Int
) {
    val view = LocalView.current

    val coinItems by viewModel.viewItemsLiveData.observeAsState()
    val doneButtonEnabled by viewModel.restoreEnabledLiveData.observeAsState(false)
    val restored = viewModel.restored

    if (restoreSettingsViewModel.openZcashConfigure != null) {
        restoreSettingsViewModel.zcashConfigureOpened()

        navController.getNavigationResult(ZcashConfigure.resultBundleKey) { bundle ->
            val requestResult = bundle.getInt(ZcashConfigure.requestResultKey)

            if (requestResult == ZcashConfigure.RESULT_OK) {
                val zcashConfig = bundle.getParcelable<ZCashConfig>(ZcashConfigure.zcashConfigKey)
                zcashConfig?.let { config ->
                    restoreSettingsViewModel.onEnter(config)
                }
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }

        navController.slideFromBottom(R.id.zcashConfigure)
    }

    LaunchedEffect(restored) {
        if (restored) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Restored,
                icon = R.drawable.icon_add_to_wallet_2_24,
                iconTint = R.color.white
            )
            delay(300)
            if (popUpToInclusiveId != -1) {
                navController.popBackStack(popUpToInclusiveId, true)
            } else {
                navController.popBackStack()
            }
        }
    }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(R.string.Restore_Title),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            },
            menuItems = listOf(
                    MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Add),
                            onClick = viewModel::onRestore,
                            enabled = doneButtonEnabled
                    )
            )
        )

        LazyColumn {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(
                    thickness = 1.dp,
                    color = if (App.localStorage.currentTheme == ThemeType.Blue) ComposeAppTheme.colors.dividerLine else ComposeAppTheme.colors.steel10,
                )
            }
            coinItems?.let {
                items(it) { viewItem ->
                    CellMultilineClear(
                        borderBottom = true,
                        onClick = { onItemClick(viewItem, viewModel) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Image(
                                painter = viewItem.imageSource.painter(),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                body_leah(
                                    text = viewItem.title,
                                    maxLines = 1,
                                )
                                subhead2_grey(
                                    text = viewItem.subtitle,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            if (viewItem.hasSettings) {
                                HsIconButton(
                                    onClick = { viewModel.onClickSettings(viewItem.item) }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit_20),
                                        contentDescription = null,
                                        tint = ComposeAppTheme.colors.grey
                                    )
                                }
                            }
                            HsSwitch(
                                checked = viewItem.enabled,
                                onCheckedChange = { onItemClick(viewItem, viewModel) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun onItemClick(viewItem: CoinViewItem<Blockchain>, viewModel: RestoreBlockchainsViewModel) {
    if (viewItem.enabled) {
        viewModel.disable(viewItem.item)
    } else {
        viewModel.enable(viewItem.item)
    }
}
