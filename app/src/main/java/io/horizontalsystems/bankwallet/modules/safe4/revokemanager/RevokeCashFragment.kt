package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

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
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.util.Log
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentRevokeBinding
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType
import java.net.URLDecoder

class RevokeCashFragment: BaseFragment() {

    private val TAG = "RevokeCash"
    private var _binding: FragmentRevokeBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentRevokeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        load()
    }

    private fun initUI() {
        binding.dappToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
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
                Log.d(TAG, "url=$url")
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

}