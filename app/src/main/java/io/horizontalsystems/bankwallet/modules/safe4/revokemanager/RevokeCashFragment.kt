package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.util.Log
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.getInputX
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentRevokeBinding
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.modules.walletconnect.AuthEvent
import io.horizontalsystems.bankwallet.modules.walletconnect.SignEvent
import io.horizontalsystems.bankwallet.modules.walletconnect.WCViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.net.URLDecoder

class RevokeCashFragment: BaseFragment() {

    private val TAG = "RevokeCash"
    private var _binding: FragmentRevokeBinding? = null
    private val binding get() = _binding!!

    private var isConnecting = false
    private val walletConnectListViewModel by viewModels<WalletConnectListViewModel> {
        WalletConnectListModule.Factory()
    }
    private var wcSessionViewModel: WCSessionViewModel? = null

    private lateinit var webView: WebView

    private val viewModel by navGraphViewModels<RevokeCashViewModel>(R.id.dappRevokeFragment) {
        RevokeCashModule.Factory(BlockchainType.BinanceSmartChain)
    }
    val evmKitWrapperViewModel by navGraphViewModels<EvmKitWrapperHoldingViewModel>(
        R.id.dappRevokeFragment
    ) { RevokeCashModule.Factory(BlockchainType.BinanceSmartChain) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MainModule.isOpenDapp = true
        _binding = FragmentRevokeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        load()
        val wcViewModel = WCViewModel()
        wcViewModel.walletEvents
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { event ->
                when (event) {
                    is SignEvent.SessionProposal -> {
                        val input = arguments?.getInputX<WCSessionModule.Input>()
                        wcSessionViewModel = WCSessionViewModel(
                            App.wcSessionManager,
                            App.connectivityManager,
                            App.accountManager.activeAccount,
                            input?.sessionTopic,
                            App.evmBlockchainManager
                        )
                        wcSessionViewModel?.connect()
                    }
                    is SignEvent.SessionRequest -> {
                    }

                    is SignEvent.Disconnect -> {
                        wcSessionViewModel?.disconnect()
                    }

                    is AuthEvent.OnRequest -> {
                    }

                    else -> Unit
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun initUI() {
        binding.dappToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.dappToolbar.setOnMenuItemClickListener {
            val dialog = Dialog(requireActivity())
            dialog.setContentView(R.layout.revoke_dialog_layout)
            dialog.findViewById<ImageView>(R.id.close).setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
            return@setOnMenuItemClickListener true
        }
        addWebView()

        //监听返回键
        var callback = object: OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    findNavController().popBackStack()
                }
            }

        }
        //获取Activity的返回键分发器添加回调
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            callback)

    }


    private fun addWebView() {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        webView = WebView(requireActivity().applicationContext)
        binding.webRootView.addView(webView, layoutParams)
        setting()
    }

    private fun setting() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d(TAG, "connectWallet url=$url")
                if (url?.contains("requestId") == true) return true
                Log.e("connectWallet", "shouldOverrideUrlLoading: $url")
                if (url?.contains("/wc?uri=") == true) {
                    val connectLink = url.substring(url.indexOf("wc?uri=") + 7)
                    val decode = URLDecoder.decode(connectLink)
                    Log.d("connectWallet", "shouldOverrideUrlLoading: ${decode}")
                    connectWallet(decode)
                    return true
                }
                if (url?.startsWith("wc:") == true) {
                    connectWallet(url)
                    return true
                }
                url?.let {
                    view?.loadUrl(url)
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                WebViewConfiguration.make(viewModel.chain.id, viewModel.address, webView)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {

            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("CustomProvider", "message:${consoleMessage?.message()}")
                return super.onConsoleMessage(consoleMessage)
            }
        }
        val webViewSettings = webView.settings
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webViewSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        val messageHandler = RevokeCashMessageHandler(
            onTransactionData = { transactionData ->
                view?.post {
                    val info = SendEvmData.AdditionalInfo.DAppInfo(SendEvmData.DappInfo("RevokeCash"))
                    val sendData = SendEvmData(transactionData, info)
                    val initiateLazyViewModel = evmKitWrapperViewModel
                    findNavController().slideFromRight(
                        R.id.sendEvmRevokeConfirmationFragment,
                        SendEvmConfirmationModule.Input(sendData, R.id.dappRevokeFragment, 0)
                    )
                }
            },
            onSwitchChain = { chainId ->
                viewModel.switchChain(chainId)
            }
        )
        webView.addJavascriptInterface(messageHandler, "HandlerMessage")
    }

    private fun load() {
        webView.loadUrl(viewModel.getUrl())
    }


    private fun connectWallet(connectionLink: String) {
        if (connectionLink.endsWith("@1") || connectionLink.endsWith("@2")) return
        if (isConnecting) return
        isConnecting = true
        when {
            connectionLink.contains("@1?") -> {}
            connectionLink.contains("@2?") -> wc2Connect(null, connectionLink)
        }
    }

    private fun wc2Connect(topic: String?, connectionLink: String?) {
        walletConnectListViewModel.setConnectionUri(connectionLink ?: "")
    }

    override fun onDestroy() {
        webView?.let {
            (webView.parent as ViewGroup).removeView(webView)
            it.destroy()
        }
        MainModule.isOpenDapp = false
        super.onDestroy()
    }
}