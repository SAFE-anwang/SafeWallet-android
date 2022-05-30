package io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.databinding.ActivitySendBinding
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ReceiveViewModel
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
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.snackbar.SnackbarDuration

class SafeConvertSendActivity : BaseActivity() {

    private lateinit var mainPresenter: SendPresenter
    private lateinit var binding: ActivitySendBinding

    private var proceedButtonView: ProceedButtonView? = null

    lateinit var safeWallet: Wallet
    lateinit var wsafeWallet: Wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        // prevent fragment recreations by passing null to onCreate
        super.onCreate(null)

        binding = ActivitySendBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        overridePendingTransition(R.anim.slide_from_bottom, R.anim.slide_to_top)

        safeWallet = intent.getParcelableExtra(WALLET_SAFE) ?: run { finish(); return }

        wsafeWallet = intent.getParcelableExtra(WALLET_WSAFE) ?: run { finish(); return }

        setToolbar(safeWallet.platformCoin.fullCoin)

        val adapter by lazy { App.adapterManager.getAdapterForWallet(wsafeWallet) as ISendEthereumAdapter }

        mainPresenter =
            ViewModelProvider(this, SendModule.SafeConvertFactory(safeWallet, adapter)).get(SendPresenter::class.java)

        subscribeToViewEvents(mainPresenter.view as SendView, safeWallet)
        subscribeToRouterEvents(mainPresenter.router as SendRouter)

        mainPresenter.onViewDidLoad()

        SafeInfoManager.startNet()

    }

    private fun setToolbar(fullCoin: FullCoin) {
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.toolbarCompose.setContent {
            var titleRes = R.string.Safe4_Title_wsafe2safe_erc20
            if ("custom_safe-bep20-SAFE" == wsafeWallet.coin.uid) {
                titleRes = R.string.Safe4_Title_wsafe2safe_bep20
            }
            ComposeAppTheme {
                AppBar(
                    title = TranslatableString.ResString(titleRes),
                    navigationIcon = {
                        CoinImage(
                            iconUrl = fullCoin.coin.iconUrl,
                            placeholder = fullCoin.iconPlaceholder,
                            modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
                        )
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                finish()
                            }
                        )
                    )
                )
            }
        }
    }

    private fun subscribeToRouterEvents(router: SendRouter) {
        router.closeWithSuccess.observe(this, Observer {
            HudHelper.showSuccessMessage(
                findViewById(android.R.id.content),
                R.string.Send_Success,
                SnackbarDuration.LONG
            )

            //Delay 1200 millis to properly show message
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1200)
        })
    }

    private fun subscribeToViewEvents(presenterView: SendView, wallet: Wallet) {
        presenterView.inputItems.observe(this, Observer { inputItems ->
            addInputItems(wallet, inputItems)
        })


        presenterView.showSendConfirmation.observe(this, Observer {
            hideSoftKeyboard()

            supportFragmentManager.commit {
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

        presenterView.sendButtonEnabled.observe(this, Observer { actionState ->
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
        hideSoftKeyboard()

        supportFragmentManager.commit {
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
                        supportFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, sendAmountFragment).commitNow()
                    }
                }
                is SendModule.Input.Address -> {
                    //add address view
                    mainPresenter.addressModuleDelegate?.let {
                        val receiveAdapter = App.adapterManager.getReceiveAdapterForWallet(wsafeWallet) ?: throw ReceiveViewModel.NoReceiverAdapter()
                        val wsafeAddressFragment =
                            WsafeAddressFragment(receiveAdapter.receiveAddress, wsafeWallet.platformCoin, it, mainPresenter.handler)
                        fragments.add(wsafeAddressFragment)
                        supportFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, wsafeAddressFragment)
                            .commitNow()
                    }
                }
                is SendModule.Input.Fee -> {
                    //add fee view
                    mainPresenter.feeModuleDelegate?.let {
                        val wsafeFeeFragment = WsafeFeeFragment(
                            wallet.platformCoin,
                            it,
                            mainPresenter.handler,
                            mainPresenter.customPriorityUnit
                        )
                        fragments.add(wsafeFeeFragment)
                        supportFragmentManager.beginTransaction()
                            .add(R.id.sendLinearLayout, wsafeFeeFragment)
                            .commitNow()
                    }
                }
                SendModule.Input.ProceedButton -> {
                    //add send button
                    proceedButtonView = ProceedButtonView(this)
                    proceedButtonView?.bind { mainPresenter.validMinAmount() }
                    binding.sendLinearLayout.addView(proceedButtonView)
                }
            }
        }

        fragments.forEach { it.init() }

        mainPresenter.onModulesDidLoad()
    }

    override fun finish() {
        super.finish()

        overridePendingTransition(0, R.anim.slide_to_bottom)
    }

    companion object {
        const val WALLET_SAFE = "wallet_safe_key"
        const val WALLET_WSAFE = "wallet_wsafe_key"
    }

}
