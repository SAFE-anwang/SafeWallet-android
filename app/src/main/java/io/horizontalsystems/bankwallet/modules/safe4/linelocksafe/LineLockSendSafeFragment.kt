package io.horizontalsystems.bankwallet.modules.safe4.linelocksafe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.ISendSafeAdapter
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.databinding.ActivitySendBinding
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule
import io.horizontalsystems.bankwallet.modules.safe4.linelock.LineLockSendHandler
import io.horizontalsystems.bankwallet.modules.safe4.linelock.LineLockSendInteractor
import io.horizontalsystems.bankwallet.modules.safe4.linelock.address.DefaultAddressFragment
import io.horizontalsystems.bankwallet.modules.safe4.linelock.fee.LineLockFeeFragment
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourCreateNodeModule
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.safe4.node.supernode.SafeFourCreateNodeScreen
import io.horizontalsystems.bankwallet.modules.safe4.node.supernode.SafeFourCreateNodeViewModel
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendPresenter
import io.horizontalsystems.bankwallet.modules.send.SendPresenter.ActionState
import io.horizontalsystems.bankwallet.modules.send.SendRouter
import io.horizontalsystems.bankwallet.modules.send.SendView
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.ConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeInfoFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.sendbutton.ProceedButtonView
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class LineLockSendSafeFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SafeFourModule.LineLockInput>()
        val wallet = input?.wallet
        if (wallet == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.sendSafe4LockFragment, true)
            return
        }
        val viewModel by viewModels<LineLockSendSafeViewModel> { LineLockSafeModule.Factory(wallet) }
        val amountInputModeViewModel by viewModels<AmountInputModeViewModel> {
            AmountInputModeModule.Factory(wallet.coin.uid)
        }
        val factory = SendEvmModule.Factory(wallet, viewModel.receiveAddress())
        val evmKitWrapperViewModel by navGraphViewModels<EvmKitWrapperHoldingViewModel>(
            R.id.sendSafe4LockFragment
        ) { factory }
        @Suppress("UNUSED_VARIABLE")
        val initiateLazyViewModel = evmKitWrapperViewModel //needed in SendEvmConfirmationFragment
        LineLockSendSafeScreen(
            amountInputModeViewModel,
            viewModel = viewModel,
            navController = navController)
    }
}
