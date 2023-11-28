package io.horizontalsystems.bankwallet.modules.send

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.databinding.ActivitySendBinding
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.safesend.SafeSendActivity
import io.horizontalsystems.bankwallet.modules.send.SendPresenter.ActionState
import io.horizontalsystems.bankwallet.modules.send.safe.SendSafeHandler
import io.horizontalsystems.bankwallet.modules.send.safe.SendSafeInteractor
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.ConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeInfoFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.sendbutton.ProceedButtonView
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.FullCoin

class SendActivity : BaseFragment() {

    private val wallet by lazy { requireArguments().getParcelable<Wallet>(WALLET)!! }
    val safeAdapter by lazy { App.adapterManager.getAdapterForWallet(wallet) as ISendSafeAdapter }
    val safeInteractor by lazy { SendSafeInteractor(safeAdapter) }
    val safeConvertHandler by lazy { SendSafeHandler(safeInteractor) }
    private val vmFactory by lazy { SendModule.Factory2(safeAdapter, safeConvertHandler, safeInteractor) }
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
//        setContentView(view)
//
//        overridePendingTransition(R.anim.slide_from_bottom, R.anim.slide_to_top)

//        val wallet: Wallet = intent.getParcelableExtra(WALLET) ?: run { finish(); return }

        setToolbar(wallet.token.fullCoin)

//        mainPresenter =
//            ViewModelProvider(this, SendModule.Factory(wallet)).get(SendPresenter::class.java)

        subscribeToViewEvents(mainPresenter.view as SendView, wallet)
        subscribeToRouterEvents(mainPresenter.router as SendRouter)

        mainPresenter.onViewDidLoad()
        return view
    }

    private fun setToolbar(fullCoin: FullCoin) {
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.toolbarCompose.setContent {
            ComposeAppTheme {
                AppBar(
                    title = stringResource(R.string.Send_Title, fullCoin.coin.code),
                    navigationIcon = {
                        if (fullCoin.coin.uid == "safe-coin"
                            || fullCoin.coin.uid == "custom_safe-erc20-SAFE"
                            || fullCoin.coin.uid == "custom_safe-bep20-SAFE") {
                            Image(painter = painterResource(id = R.drawable.logo_safe_24),
                                contentDescription = null,
                                modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
                            )
                        } else {
                            CoinImage(
                                iconUrl = fullCoin.coin.imageUrl,
                                placeholder = fullCoin.iconPlaceholder,
                                modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
                            )
                        }
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
            HudHelper.showSuccessMessage(
                binding.root.findViewById(android.R.id.content),
                R.string.Send_Success,
                SnackbarDuration.LONG
            )

            //Delay 1200 millis to properly show message
            Handler(Looper.getMainLooper()).postDelayed({ findNavController().popBackStack() }, 1200)
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
                addToBackStack(null)
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
                    }
                }
                is SendModule.Input.Address -> {
                    //add address view
                    mainPresenter.addressModuleDelegate?.let {
                        val sendAddressFragment =
                            SendAddressFragment(wallet.token, it, mainPresenter.handler)
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
                        val sendFeeFragment = SendFeeFragment(
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
                    proceedButtonView?.bind { mainPresenter.onProceedClicked() }
                    binding.sendLinearLayout.addView(proceedButtonView)
                }
            }
        }

        fragments.forEach { it.init() }

        mainPresenter.onModulesDidLoad()
    }

    companion object {
        const val WALLET = "wallet_key"
    }

}
