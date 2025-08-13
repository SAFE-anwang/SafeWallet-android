package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.google.android.exoplayer2.util.Log
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.databinding.FragmentRevokeBinding
import io.horizontalsystems.bankwallet.modules.dapp.DAppBrowseFragment.Input
import io.horizontalsystems.core.findNavController
import java.net.URLDecoder

class RevokeCashFragment: BaseFragment() {

    private val TAG = "RevokeCashFragment"
    private var _binding: FragmentRevokeBinding? = null
    private val binding get() = _binding!!

    private lateinit var webView: WebView

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
        load(null)
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
                WebViewConfiguration.make(0, "", webView)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {

            }
        }
        val webViewSettings = webView.settings
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webViewSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        val messageHandler = RevokeCashMessageHandler()
        webView.addJavascriptInterface(messageHandler, Web3Method.transactionHandler.methodName)
        webView.addJavascriptInterface(messageHandler, Web3Method.walletSwitchChain.methodName)
        webView.addJavascriptInterface(messageHandler, Web3Method.ethSendTransaction.methodName)
        webView.addJavascriptInterface(messageHandler, Web3Method.ethChainId.methodName)
    }

    private fun load(connectInfo: RevokeConnectInfo?) {
        val url =  if (connectInfo != null) {
            "https://revoke.cash/zh/address/${connectInfo.walletAddress}?chainId=${connectInfo.chainId}"
        }else {
            "https://revoke.cash"
        }
        webView.loadUrl(url)
    }

}