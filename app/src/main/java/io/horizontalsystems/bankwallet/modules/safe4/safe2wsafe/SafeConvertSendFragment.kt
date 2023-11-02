package io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.databinding.ActivitySendBinding
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.address.ReceiveAddressModule
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.address.WsafeAddressFragment
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.fee.WsafeFeeFragment
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendPresenter
import io.horizontalsystems.bankwallet.modules.send.SendPresenter.ActionState
import io.horizontalsystems.bankwallet.modules.send.SendRouter
import io.horizontalsystems.bankwallet.modules.send.SendView
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.ConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeInfoFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.sendbutton.ProceedButtonView
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.FullCoin

class SafeConvertSendFragment : BaseFragment() {

    private val wsafeWallet by lazy { requireArguments().getParcelable<Wallet>(SafeConvertSendActivity.WALLET_WSAFE)!! }
    private val safeWallet by lazy { requireArguments().getParcelable<Wallet>(SafeConvertSendActivity.WALLET_SAFE)!! }
    private val isETH by lazy { requireArguments().getBoolean(SafeConvertSendActivity.IS_ETH)!! }
    private val isMatic by lazy { requireArguments().getBoolean(SafeConvertSendActivity.IS_MATIC)!! }

    val wsafeAdapter by lazy { App.adapterManager.getAdapterForWallet(wsafeWallet) as ISendEthereumAdapter }
    val safeAdapter by lazy { App.adapterManager.getAdapterForWallet(safeWallet) as ISendSafeAdapter }
    val safeInteractor by lazy { SendSafeConvertInteractor(safeAdapter) }
    val safeConvertHandler by lazy { SendSafeConvertHandler(safeInteractor, wsafeAdapter) }
    private val vmFactory by lazy { SendModule.SafeConvertFactory2(safeAdapter, safeConvertHandler, safeInteractor) }
    private val mainPresenter by viewModels<SendPresenter>() { vmFactory }

    private lateinit var binding: ActivitySendBinding

    private var proceedButtonView: ProceedButtonView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        // prevent fragment recreations by passing null to onCreate
        super.onCreate(null)

        binding = ActivitySendBinding.inflate(layoutInflater)
        val view = binding.root

        setToolbar()

        subscribeToViewEvents(mainPresenter.view as SendView, safeWallet)
        subscribeToRouterEvents(mainPresenter.router as SendRouter)

        mainPresenter.onViewDidLoad()

        SafeInfoManager.startNet()
        return view
    }

    private fun setToolbar() {
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.toolbarCompose.setContent {
            var titleRes = R.string.Safe4_Title_safe2wsafe_erc20
            if (!isETH) {
                titleRes = R.string.Safe4_Title_safe2wsafe_bep20
            }
            if (isMatic) {
                titleRes = R.string.Safe4_Title_safe2wsafe_matic
            }
            ComposeAppTheme {
                AppBar(
                        title = stringResource(titleRes),
                        navigationIcon = {
                            Image(painter = painterResource(id = R.drawable.logo_safe_24),
                                    contentDescription = null,
                                    modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .size(24.dp)
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

    private fun subscribeToRouterEvents(router: SendRouter) {
        router.closeWithSuccess.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                HudHelper.showSuccessMessage(
                    binding.root,
                    R.string.Send_Success,
                    SnackbarDuration.LONG
                )

                //Delay 1200 millis to properly show message
                Handler(Looper.getMainLooper()).postDelayed(
                    { findNavController().popBackStack() },
                    1200
                )
            }
        })
    }

    private fun subscribeToViewEvents(presenterView: SendView, wallet: Wallet) {
        presenterView.inputItems.observe(viewLifecycleOwner, Observer { inputItems ->
            addInputItems(wallet, inputItems)
        })


        presenterView.showSendConfirmation.observe(viewLifecycleOwner, Observer {
            hideKeyboard()

            childFragmentManager.commit {
                setCustomAnimations(
                    R.anim.slide_from_right,
                    R.anim.slide_to_left,
                    R.anim.slide_from_left,
                    R.anim.slide_to_right
                )
                add(R.id.rootView, ConfirmationFragment(mainPresenter))
            }
        })

        presenterView.sendButtonEnabled.observe(viewLifecycleOwner, Observer { actionState ->
            val defaultTitle = getString(R.string.Send_DialogProceed)

            when (actionState) {
                is ActionState.Enabled -> {
                    proceedButtonView?.updateState(true)
                    proceedButtonView?.setTitle(defaultTitle)
                }
                is ActionState.Disabled -> {
                    proceedButtonView?.updateState(false)
                    proceedButtonView?.setTitle(actionState.title ?: defaultTitle)
                }
            }
        })
    }

    fun showFeeInfo() {
        hideKeyboard()

        childFragmentManager.commit {
            setCustomAnimations(
                R.anim.slide_from_right,
                R.anim.slide_to_left,
                R.anim.slide_from_left,
                R.anim.slide_to_right
            )
            add(R.id.rootView, SendFeeInfoFragment())
        }
    }

    private fun addInputItems(wallet: Wallet, inputItems: List<SendModule.Input>) {
        val fragments = mutableListOf<SendSubmoduleFragment>()

        inputItems.forEach { input ->
            when (input) {
                SendModule.Input.Amount -> {
                    //add amount view
                    mainPresenter.amountModuleDelegate?.let {
                        val sendAmountFragment =
                            SendAmountFragment(wallet, it, mainPresenter.handler)
                        fragments.add(sendAmountFragment)
                        childFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, sendAmountFragment).commitNow()
                    }
                }
                is SendModule.Input.Address -> {
                    //add address view
                    mainPresenter.addressModuleDelegate?.let {
                        val receiveAdapter = App.adapterManager.getReceiveAdapterForWallet(wsafeWallet) ?: throw ReceiveAddressModule.NoReceiverAdapter()
                        val wsafeAddressFragment =
                            WsafeAddressFragment(receiveAdapter.receiveAddress, wsafeWallet.token, it, mainPresenter.handler)
                        fragments.add(wsafeAddressFragment)
                        childFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, wsafeAddressFragment)
                            .commitNow()
                    }
                }
                is SendModule.Input.Fee -> {
                    //add fee view
                    mainPresenter.feeModuleDelegate?.let {
                        val wsafeFeeFragment = WsafeFeeFragment(
                            wallet.token,
                            it,
                            mainPresenter.handler,
                            mainPresenter.customPriorityUnit
                        )
                        fragments.add(wsafeFeeFragment)
                        childFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, wsafeFeeFragment)
                            .commitNow()
                    }
                }
                SendModule.Input.ProceedButton -> {
                    //add send button
                    proceedButtonView = ProceedButtonView(requireActivity())
                    proceedButtonView?.bind { mainPresenter.validMinAmount() }
                    binding.sendLinearLayout.addView(proceedButtonView)
                }
                else -> {}
            }
        }

        fragments.forEach { it.init() }

        mainPresenter.onModulesDidLoad()
    }

    /*override fun finish() {
        super.finish()

        overridePendingTransition(0, R.anim.slide_to_bottom)
    }*/
    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val WALLET_SAFE = "wallet_safe_key"
        const val WALLET_WSAFE = "wallet_wsafe_key"
        const val IS_ETH = "eth_Transaction"
        const val IS_MATIC = "is_matic"


        fun prepareParams(
            safeWallet: Wallet,
            wsafeWallet: Wallet,
            isETH: Boolean,
            isMatic: Boolean,
        ) = bundleOf(
            WALLET_SAFE to safeWallet,
            WALLET_WSAFE to wsafeWallet,
            IS_ETH to isETH,
            IS_MATIC to isMatic,
        )
    }
}
