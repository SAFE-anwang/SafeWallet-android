package io.horizontalsystems.bankwallet.modules.safe4.linelocksafe

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel

class LineLockSendSafeFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SafeFourModule.LineLockInput>()
        val wallet = input?.wallet
        if (wallet == null) {
            navController.popBackStack(R.id.sendSafe4LockFragment, true)
            return
        }
        val viewModel by viewModels<LineLockSendSafeViewModel> { LineLockSafeModule.Factory(wallet) }
        val amountInputModeViewModel by viewModels<AmountInputModeViewModel> {
            AmountInputModeModule.Factory(wallet.coin.uid)
        }
        val adapter by lazy { App.adapterManager.getAdapterForWallet<ISendEthereumAdapter>(wallet) as ISendEthereumAdapter }
        val factory = SendEvmModule.Factory(wallet, Address(viewModel.receiveAddress()), false, adapter)
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
