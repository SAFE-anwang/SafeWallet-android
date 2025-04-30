package io.horizontalsystems.bankwallet.modules.safe4.swap

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import io.horizontalsystems.bankwallet.modules.sendevm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.ui.SwitchCoinsSection
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize

class SAFE4SwapFragment: BaseFragment() {


    @Parcelize
    data class Input(
        val wallet1: Wallet,
        val wallet2: Wallet,
        val swapEntryPointDestId: Int = 0
    ) : Parcelable


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            val navController = findNavController()
            try {

                val input = navController.requireInput<Input>()
                val swapEntryPointDestId = input.swapEntryPointDestId
                val factory = Safe4SwapModule.Factory(input.wallet1, input.wallet2)
                val swapViewModel : Safe4SwapViewModel by navGraphViewModels<Safe4SwapViewModel>(R.id.safe4SwapFragment) { factory }

                setContent {
                    ComposeAppTheme {
                        SwapMainScreen(
                            navController,
                            swapViewModel,
                            swapEntryPointDestId
                        )
                    }
                }
            } catch (t: Throwable) {
                navController.popBackStack()
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwapMainScreen(
    navController: NavController,
    viewModel: Safe4SwapViewModel,
    swapEntryPointDestId: Int,

) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val uiState = viewModel.swapState
    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = stringResource(R.string.SAFE4_Swap),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeAppTheme.colors.lawrence)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.SAFE4_Swap_From),
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.microSB,
                        )
                        Spacer(
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.SAFE4_Swap_Balance, uiState.balance1),
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.microSB,
                        )
                    }
                    Safe4SwapCoinCardView(
                        uiState.inputAmount,
                        uiState.fromToken,
                        true,
                        8,
                        navController,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
                        onAmountChange = {
                            viewModel.onChangeAmount(it)
                        }
                    )

                    VSpacer(8.dp)
                    SwitchCoinsSection { viewModel.onTapSwitch() }
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.SAFE4_Swap_To),
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.microSB,
                        )
                        Spacer(
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.SAFE4_Swap_Balance, uiState.balance2),
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.microSB,
                        )
                    }
                    Safe4SwapCoinCardView(
                        uiState.inputAmount,
                        uiState.toToken,
                        true,
                        8,
                        navController,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                        onAmountChange = {

                        }
                    )
                }
                VSpacer(10.dp)
                uiState.caution?.let { caution ->
                    androidx.compose.material.Text(
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                        text = caution.text,
                        style = ComposeAppTheme.typography.caption,
                        color = ComposeAppTheme.colors.redD,
                    )
                }
                VSpacer(16.dp)
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    title = stringResource(R.string.Send_DialogProceed),
                    onClick = {
                        if (viewModel.hasConnection()) {
                            viewModel.getSendEvmData()?.let {
                                navController.navigate(
                                    R.id.sendEvmFragment_to_sendSafe4SwapConfirmationFragment,
                                    SendEvmConfirmationModule.prepareParams(it)
                                )
                            }
                        } else {
                            HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                        }
                    },
                    enabled = uiState.canSend
                )
            }
        }
    }
}