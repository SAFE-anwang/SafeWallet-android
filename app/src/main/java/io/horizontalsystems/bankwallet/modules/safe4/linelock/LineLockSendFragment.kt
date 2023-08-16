package io.horizontalsystems.bankwallet.modules.safe4.linelock

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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ISendSafeAdapter
import io.horizontalsystems.bankwallet.databinding.ActivitySendBinding
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ReceiveViewModel
import io.horizontalsystems.bankwallet.modules.safe4.linelock.address.DefaultAddressFragment
import io.horizontalsystems.bankwallet.modules.safe4.linelock.fee.LineLockFeeFragment
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendPresenter
import io.horizontalsystems.bankwallet.modules.send.SendPresenter.ActionState
import io.horizontalsystems.bankwallet.modules.send.SendRouter
import io.horizontalsystems.bankwallet.modules.send.SendView
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
import io.horizontalsystems.marketkit.models.FullCoin

class LineLockSendFragment : BaseFragment() {

    private val wallet by lazy { requireArguments().getParcelable<Wallet>(WALLET)!! }
    private val safeAdapter by lazy { App.adapterManager.getAdapterForWallet(wallet) as ISendSafeAdapter }
    private val safeInteractor by lazy { LineLockSendInteractor(safeAdapter) }
    private val safeConvertHandler by lazy { LineLockSendHandler(safeInteractor) }
    private val vmFactory by lazy { SendModule.LineLockFactory2(safeAdapter, safeConvertHandler, safeInteractor) }
    private val mainPresenter by viewModels<SendPresenter>() { vmFactory }

    private lateinit var binding: ActivitySendBinding

    private var proceedButtonView: ProceedButtonView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        // prevent fragment recreations by passing null to onCreate
        super.onCreate(savedInstanceState)

        binding = ActivitySendBinding.inflate(layoutInflater)
        val view = binding.root
        setToolbar()

        subscribeToViewEvents(mainPresenter.view as SendView, wallet)
        subscribeToRouterEvents(mainPresenter.router as SendRouter)

        mainPresenter.onViewDidLoad()
        return view
    }

    private fun setToolbar() {
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.toolbarCompose.setContent {
            ComposeAppTheme {
                AppBar(
                    title = TranslatableString.ResString(R.string.Safe4_Line_Locked),
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
            addToBackStack(null)
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
                        sendAmountFragment.setLockedTitle()
                    }
                }
                is SendModule.Input.Address -> {
                    //add address view
                    mainPresenter.addressModuleDelegate?.let {
                        val receiveAdapter = App.adapterManager.getReceiveAdapterForWallet(wallet) ?: throw ReceiveViewModel.NoReceiverAdapter()
                        val sendAddressFragment =
                            DefaultAddressFragment(receiveAdapter.receiveAddress, wallet.token, it, mainPresenter.handler)
                        fragments.add(sendAddressFragment)
                        childFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, sendAddressFragment)
                            .commitNow()
                    }
                }
                SendModule.Input.Hodler -> {
                    mainPresenter.hodlerModuleDelegate?.let {
                        val sendAddressFragment = SendHodlerFragment(it, mainPresenter.handler)
                        fragments.add(sendAddressFragment)
                        childFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, sendAddressFragment)
                            .commitNow()
                    }
                }
                is SendModule.Input.Fee -> {
                    //add fee view
                    mainPresenter.feeModuleDelegate?.let {
                        val sendFeeFragment = LineLockFeeFragment(
                            wallet.token,
                            it,
                            mainPresenter.handler,
                            mainPresenter.customPriorityUnit
                        )
                        fragments.add(sendFeeFragment)
                        childFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, sendFeeFragment)
                            .commitNow()
                    }
                }
                is SendModule.Input.Memo -> {
                    //add memo view
                    val sendMemoFragment =
                        SendMemoFragment(input.maxLength, input.hidden, mainPresenter.handler)
                    fragments.add(sendMemoFragment)
                    childFragmentManager.beginTransaction()
                        .add(R.id.sendLinearLayout, sendMemoFragment).commitNow()
                }
                SendModule.Input.ProceedButton -> {
                    //add send button
                    proceedButtonView = ProceedButtonView(requireActivity())
                    proceedButtonView?.bind {
                        if ((mainPresenter.handler as LineLockSendHandler).checkLineLock()){
                            mainPresenter.onProceedClicked()
                        }
                    }
                    binding.sendLinearLayout.addView(proceedButtonView)
                }
            }
        }

        fragments.forEach { it.init() }

        mainPresenter.onModulesDidLoad()
    }

/*    override fun finish() {
        super.finish()

        overridePendingTransition(0, R.anim.slide_to_bottom)
    }*/

    companion object {
        const val WALLET = "wallet_key"
    }

}
