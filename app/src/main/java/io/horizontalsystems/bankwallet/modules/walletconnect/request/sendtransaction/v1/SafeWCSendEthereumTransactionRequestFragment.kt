package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.SafeWalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.ui.SendEthRequestScreen
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class SafeWCSendEthereumTransactionRequestFragment : BaseFragment() {
    private val logger = AppLogger("wallet-connect")
    private val baseViewModel by navGraphViewModels<SafeWalletConnectViewModel>(R.id.mainFragment)
    val vmFactory by lazy {
        WCRequestModule.Factory(
            baseViewModel.sharedSendEthereumTransactionRequest!!, baseViewModel.service
        )
    }
    private val viewModel by viewModels<WCSendEthereumTransactionRequestViewModel> { vmFactory }
    private val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.mainWcSendEthereumTransactionRequestFragment) { vmFactory }

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
                SendEthRequestScreen(
                    findNavController(),
                    viewModel,
                    sendEvmTransactionViewModel,
                    feeViewModel,
                    logger,
                    R.id.mainWcSendEthereumTransactionRequestFragment
                ) { close() }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
            close()
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) { transactionHash ->
            viewModel.approve(transactionHash)
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            close()
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

    }

    private fun close() {
        findNavController().popBackStack()
    }

    override fun onDestroy() {
        super.onDestroy()
        baseViewModel.sharedSendEthereumTransactionRequest = null
    }
}
