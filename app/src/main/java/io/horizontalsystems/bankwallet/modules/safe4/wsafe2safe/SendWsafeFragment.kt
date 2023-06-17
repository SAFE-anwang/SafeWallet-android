package io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.databinding.FragmentSendEvmBinding
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.receive.ReceiveViewModel
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.SafeConvertSendActivity
import io.horizontalsystems.bankwallet.modules.sendevm.AmountInputViewModel
import io.horizontalsystems.bankwallet.modules.sendevm.SendAvailableBalanceViewModel
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.FullCoin

class SendWsafeFragment : BaseFragment() {

    private val wsafeWallet by lazy { requireArguments().getParcelable<Wallet>(SafeConvertSendActivity.WALLET_WSAFE)!! }
    private val safeWallet by lazy { requireArguments().getParcelable<Wallet>(SafeConvertSendActivity.WALLET_SAFE)!! }
    private val isETH by lazy { requireArguments().getBoolean(SafeConvertSendActivity.IS_ETH)!! }
    private val isMatic by lazy { requireArguments().getBoolean(SafeConvertSendActivity.IS_MATIC)!! }

    private val vmFactory by lazy { SendEvmModule.WsafeFactory(wsafeWallet) }
    private val viewModel by navGraphViewModels<SendWsafeViewModel>(R.id.sendWsafeFragment) { vmFactory }
    private val availableBalanceViewModel by viewModels<SendAvailableBalanceViewModel> { vmFactory }
    private val amountViewModel by viewModels<AmountInputViewModel> { vmFactory }

    private var _binding: FragmentSendEvmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendEvmBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setToolbar()

        availableBalanceViewModel.viewStateLiveData.observe(viewLifecycleOwner, { state ->
            binding.availableBalanceSpinner.isVisible =
                state is SendAvailableBalanceViewModel.ViewState.Loading
            binding.availableBalanceValue.text =
                (state as? SendAvailableBalanceViewModel.ViewState.Loaded)?.value
        })

        binding.amountInput.onTextChangeCallback =
            { _, new -> amountViewModel.onChangeAmount(new ?: "") }
        binding.amountInput.onTapSecondaryCallback = { amountViewModel.onSwitch() }
        binding.amountInput.onTapMaxCallback = { amountViewModel.onClickMax() }
        binding.amountInput.postDelayed({ binding.amountInput.setFocus() }, 200)


        amountViewModel.amountLiveData.observe(viewLifecycleOwner, { amount ->
            if (binding.amountInput.getAmount() != amount && !amountViewModel.areAmountsEqual(
                    binding.amountInput.getAmount(),
                    amount
                )
            )
                binding.amountInput.setAmount(amount)
        })

        amountViewModel.revertAmountLiveData.observe(viewLifecycleOwner, { revertAmount ->
            binding.amountInput.revertAmount(revertAmount)
        })

        amountViewModel.maxEnabledLiveData.observe(viewLifecycleOwner, { maxEnabled ->
            binding.amountInput.maxButtonVisible = maxEnabled
        })

        amountViewModel.secondaryTextLiveData.observe(viewLifecycleOwner, { secondaryText ->
            binding.amountInput.setSecondaryText(secondaryText)
        })

        amountViewModel.inputParamsLiveData.observe(viewLifecycleOwner, {
            binding.amountInput.setInputParams(it)
        })

        viewModel.amountCautionLiveData.observe(viewLifecycleOwner, { caution ->
            setAmountInputCaution(caution)
        })

        viewModel.proceedLiveEvent.observe(viewLifecycleOwner, { sendData ->
            findNavController().slideFromRight(
                R.id.sendEvmFragment_to_sendWsafeConfirmationFragment,
                SendEvmConfirmationModule.prepareParams(sendData)
            )
        })

        binding.buttonProceedCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setProceedButton(viewModel)

        SafeInfoManager.startNet()
    }

    private fun setToolbar() {
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.toolbarCompose.setContent {
            var titleRes = R.string.Safe4_Title_wsafe2safe_erc20
            if (!isETH) {
                titleRes = R.string.Safe4_Title_wsafe2safe_bep20
            }
            if (isMatic) {
                viewModel.isMatic = isMatic
                titleRes = R.string.Safe4_Title_wsafe2safe_matic
            }
            ComposeAppTheme {
                AppBar(
                    title = TranslatableString.ResString(titleRes),
                    navigationIcon = {
                        Image(painter = painterResource(id = R.drawable.logo_safe_24),
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
                        )
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                findNavController().popBackStack()
                            }
                        )
                    )
                )
            }
        }
    }

    private fun setAmountInputCaution(caution: Caution?) {
        binding.txtHintError.isVisible = caution != null
        binding.txtHintError.text = caution?.text
        binding.background.hasError = caution != null

        when (caution?.type) {
            Caution.Type.Error -> {
                binding.background.hasError = true
                binding.txtHintError.setTextColor(requireContext().getColor(R.color.red_d))
            }
            Caution.Type.Warning -> {
                binding.background.hasWarning = true
                binding.txtHintError.setTextColor(requireContext().getColor(R.color.yellow_d))
            }
            else -> {
                binding.background.clearStates()
            }
        }
    }

    private fun setProceedButton(viewModel: SendWsafeViewModel) {
       binding.buttonProceedCompose.setContent {
            ComposeAppTheme {
                val proceedEnabled by viewModel.proceedEnabledLiveData.observeAsState(false)
                val receiveAdapter = App.adapterManager.getReceiveAdapterForWallet(safeWallet) ?: throw ReceiveViewModel.NoReceiverAdapter()
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.Safe4_Wsafe_Receive_Address),
                        style = ComposeAppTheme.typography.subhead1,
                        color = ComposeAppTheme.colors.leah,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HSAddressInput(
                        initial = Address(receiveAdapter.receiveAddress),
                        tokenQuery = safeWallet.token.tokenQuery,
                        navController = findNavController(),
                        coinCode = safeWallet.coin.code,
                        error = viewModel.error,
                        onValueChange = {
                            viewModel.onEnterAddress(wsafeWallet, safeWallet, it)
                        }
                    )
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 24.dp,
                                bottom = 24.dp
                            ),
                        title = getString(R.string.Send_DialogProceed),
                        onClick = {
                            viewModel.onClickProceed()
                        },
                        enabled = proceedEnabled
                    )
                }

            }
        }
    }

}
