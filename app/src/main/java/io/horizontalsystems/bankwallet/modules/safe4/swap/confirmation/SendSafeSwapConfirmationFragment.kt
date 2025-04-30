package io.horizontalsystems.bankwallet.modules.safe4.swap.confirmation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.getInputX
import io.horizontalsystems.bankwallet.databinding.FragmentConfirmationSendEvmBinding
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.safe4.swap.Safe4SwapViewModel
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.CustomSnackbar
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData

class SendSafeSwapConfirmationFragment : BaseFragment() {

    private val logger = AppLogger("send-evm")
    private val sendWsafeViewModel by navGraphViewModels<Safe4SwapViewModel>(R.id.safe4SwapFragment)

    private val vmFactory by lazy {
        SendEvmConfirmationModule.Factory(
            sendWsafeViewModel.adapter.evmKitWrapper,
            SendEvmData(transactionData, additionalInfo),
            100000
        )
    }
    private val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(R.id.safe4SwapConfirmationFragment) { vmFactory }
    private val nonceServiceViewModel by viewModels< SendEvmNonceViewModel> { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.safe4SwapConfirmationFragment) { vmFactory }

    private var snackbarInProcess: CustomSnackbar? = null

    private val transactionData: TransactionData
        get() {
            val transactionDataParcelable =
                arguments?.getParcelable<SendEvmModule.TransactionDataParcelable>(SendEvmModule.transactionDataKey)!!
            return TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input,
                safe4Swap = transactionDataParcelable.safe4Swap
            )
        }
    private val additionalInfo: SendEvmData.AdditionalInfo?
        get() = arguments?.getParcelable(SendEvmModule.additionalInfoKey)

    private var _binding: FragmentConfirmationSendEvmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmationSendEvmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snackbarInProcess?.dismiss()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack(R.id.safe4SwapFragment, true)
                    true
                }
                else -> false
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        sendEvmTransactionViewModel.sendEnabledLiveData.observe(viewLifecycleOwner) { enabled ->
            setSendButton(enabled)
        }

        sendEvmTransactionViewModel.sendingLiveData.observe(viewLifecycleOwner) {
            snackbarInProcess = HudHelper.showInProcessMessage(
                requireView(),
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) {
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.safe4SwapFragment, true)
            }, 1200)
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)

            findNavController().popBackStack()
        }

        /*binding.sendEvmTransactionView.init(
            sendEvmTransactionViewModel,
            feeViewModel,
            viewLifecycleOwner,
            findNavController(),
            R.id.sendWsafeConfirmationFragment
        )*/

        binding.buttonSendCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setSendButton()
    }

    private fun setSendButton(enabled: Boolean = false) {
        binding.sendEvmTransactionView.setContent {
            SendEvmTransactionView(
                sendEvmTransactionViewModel,
                feeViewModel,
                nonceServiceViewModel,
                findNavController()
                /*R.id.sendWsafeConfirmationFragment,*/
            )
        }
        binding.buttonSendCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(R.string.Send_Confirmation_Send_Button),
                    onClick = {
                        logger.info("click send button")
                        sendEvmTransactionViewModel.send(logger)
                    },
                    enabled = enabled
                )
            }
        }
    }

}
