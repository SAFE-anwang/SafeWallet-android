package io.horizontalsystems.bankwallet.modules.dapp

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Base64
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navGraphViewModels
import android.util.Log
import android.webkit.WebViewClient.ERROR_CONNECT
import android.webkit.WebViewClient.ERROR_HOST_LOOKUP
import android.webkit.WebViewClient.ERROR_PROXY_AUTHENTICATION
import android.webkit.WebViewClient.ERROR_TIMEOUT
import com.tencent.mmkv.MMKV
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.databinding.FragmentDappBrowseBinding
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.bankwallet.modules.walletconnect.AuthEvent
import io.horizontalsystems.bankwallet.modules.walletconnect.SignEvent
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionViewModel
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import java.net.URLDecoder

class DAppBrowseFragment: BaseFragment(){

//    private lateinit  var baseViewModel : WalletConnectViewModel
    /*private val wc2MainViewModel by viewModels<WC2MainViewModel> {
        WC2MainViewModel.Factory()
    }*/
/*    private val viewModel by viewModels<WCSessionViewModel> {
        Log.e("connectWallet", "DAppBrowseFragment")
        val input = arguments?.getInputX<WCSessionModule.Input>()
        WCSessionModule.Factory(input?.sessionTopic)
    }*/

    private var viewModel: WCSessionViewModel? = null

    private val walletConnectListViewModel by viewModels<WalletConnectListViewModel> {
        WalletConnectListModule.Factory()
    }

    private val HISTORY_KEY = "dapp_history"
    private var _binding: FragmentDappBrowseBinding? = null
    private val binding get() = _binding!!

    private lateinit var webView: WebView
    private lateinit var urlString: String


    private val disposables = CompositeDisposable()
    private val errorLiveEvent = SingleLiveEvent<String?>()

    private var autoConnect = true
    private var isConnecting = false
    private var isShowWarning = false

    /** Web3 JS Bridge for window.ethereum injection */
    private var web3Bridge: DAppWeb3Bridge? = null
    private var isProviderInjected = false

    var adapter: DAppHistoryAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MainModule.isOpenDapp = true
        _binding = FragmentDappBrowseBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()

        val wcViewModel = WCViewModel()
        wcViewModel.walletEvents
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .onEach { event ->
                    when (event) {
                        is SignEvent.SessionProposal -> {
                            val input = arguments?.getInputX<WCSessionModule.Input>()
                            viewModel = WCSessionViewModel(
                                App.wcSessionManager,
                                App.connectivityManager,
                                App.accountManager.activeAccount,
                                input?.sessionTopic,
                                wcManager = App.wcManager,
                                networkManager = App.networkManager,
                                appConfigProvider = App.appConfigProvider,
                                paidActionSettingsManager = App.paidActionSettingsManager
                            )
                            viewModel?.connect()
                        }
                        is SignEvent.SessionRequest -> {
//                            findNavController().slideFromBottom(R.id.wcRequestFragment,)
                        }

                        is SignEvent.Disconnect -> {
                            viewModel?.disconnect()
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
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.refreshView.setOnClickListener {
            var inputContent = binding.inputWebUrl.text.toString()
            if (inputContent.isEmpty()) return@setOnClickListener
            if (!isShowWarning) {
                isShowWarning = true
                showAlert(inputContent)
                return@setOnClickListener
            }

            webView.loadUrl(checkUrl(inputContent))
        }
        binding.inputWebUrl.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_GO || i == EditorInfo.IME_ACTION_DONE) {
                var inputContent = binding.inputWebUrl.text.toString()
                if (inputContent.isEmpty()) return@setOnEditorActionListener false
                if (!isShowWarning) {
                    isShowWarning = true
                    showAlert(inputContent)
                } else {
                    webView.loadUrl(checkUrl(inputContent))
                    saveHistory(checkUrl(inputContent))
                    hideHistory()
                }
            }
            false
        }
        val input = findNavController().requireInput<Input>()
        val url = input.url
        val name = input.name
        val isInput = input.isInput?.let {
            binding.dappToolbar.visibility = if (it) View.GONE else View.VISIBLE
            binding.layoutInput.visibility = if (it) View.VISIBLE else View.GONE
            binding.layoutHistory.visibility = if (it) View.VISIBLE else View.GONE
        }
        binding.inputWebUrl.setText(url)
        binding.dappToolbar.title = name
        binding.progressBar.progress = 0

        // Initialize Web3 bridge for JS wallet injection
        if (url != null) {
            initWeb3Bridge(url)
        }

        addWebView()
        url?.let {
            urlString = it
            webView.loadUrl(url)
        }

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

        errorLiveEvent.observe(viewLifecycleOwner, Observer { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        })

        initHistoryView()
    }

    private fun checkUrl(url: String):String {
        var newUrl = url
        if (newUrl.startsWith("http://")) {
            newUrl = url.replace("http://", "https://")
        }
        if (!newUrl.startsWith("https://") && newUrl.startsWith("www.")) {
            newUrl = "https://$newUrl"
        }
        return newUrl
    }

    private fun showAlert(url: String) {

        ConfirmationDialog.show(
            icon = R.drawable.ic_attention_24,
            title = getString(R.string.Access_Websites_Warning_Title),
            warningText = getString(R.string.Access_Websites_Warning),
            actionButtonTitle = getString(R.string.Access_Websites_Warning_Proceed),
            transparentButtonTitle = getString(R.string.Alert_fallback_Cancel),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    webView.loadUrl(checkUrl(url))
                    saveHistory(checkUrl(url))
                    hideHistory()
                }

                override fun onTransparentButtonClick() {

                }

                override fun onCancelButtonClick() {

                }
            }
        )
    }

    private fun addWebView() {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        webView = WebView(requireActivity().applicationContext)
        binding.webRootView.addView(webView, layoutParams)
        // clear webview cache
        if (MMKV.defaultMMKV()?.decodeBool("isClearCache", false) == false) {
            MMKV.defaultMMKV()?.encode("isClearCache", true)
            webView.clearCache(true)
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        setting()
    }

    private fun initWeb3Bridge(url: String) {
        web3Bridge = DAppWeb3Bridge(
            url = url,
            onRequestAccounts = {
                // Try to get accounts from active wallet
                try {
                    val account = App.accountManager.activeAccount ?: return@DAppWeb3Bridge null
                    val bridge = web3Bridge ?: return@DAppWeb3Bridge null
                    val evmKitManager = App.evmBlockchainManager.getEvmKitManager(bridge.defaultBlockchainType)
                    val wrapper = evmKitManager.getEvmKitWrapper(account, bridge.defaultBlockchainType)
                    val addr = wrapper?.evmKit?.receiveAddress?.hex
                    if (addr != null) {
                        "[${"\""}${addr}${"\""}]"
                    } else null
                } catch (e: Exception) {
                    Log.e("Web3Bridge", "onRequestAccounts error: ${e.message}")
                    null
                }
            },
            onSendTransaction = { requestId, txObj ->
                Log.d("Web3Bridge", "onSendTransaction: id=$requestId, tx=$txObj")
                val bridge = web3Bridge ?: return@DAppWeb3Bridge

                try {
                    val paramsStr = JSONArray().apply { put(txObj) }.toString()

                    activity?.runOnUiThread {
                        WCDelegate.dappRequestEvent = WCDelegate.DAppRequest(
                            id = requestId.toLong(),
                            method = "eth_sendTransaction",
                            params = paramsStr,
                            chainId = "eip155:${bridge.defaultChain.id}",
                            peerName = url,
                            peerUrl = url,
                            peerIcon = "",
                            blockchainType = bridge.defaultBlockchainType,
                            chainName = bridge.defaultChain.name,
                            onRespond = { id, result -> bridge.resolveRequest(id.toInt(), result) },
                            onReject = { id -> bridge.rejectRequest(id.toInt()) },
                        )

                        try {
                            findNavController().slideFromBottom(R.id.wcRequestFragment)
                        } catch (e: Exception) {
                            Log.e("Web3Bridge", "slideFromBottom error: ${e.message}")
                            bridge.rejectRequest(requestId, e.message ?: "Navigation error")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Web3Bridge", "onSendTransaction error: ${e.message}")
                    bridge.rejectRequest(requestId, e.message ?: "Internal error")
                }
            },
            onSign = { requestId, method, params ->
                Log.d("Web3Bridge", "onSign: id=$requestId, method=$method")
                val bridge = web3Bridge ?: return@DAppWeb3Bridge

                try {
                    // Get active address for forming proper params
                    val account = App.accountManager.activeAccount
                    val evmKitManager = App.evmBlockchainManager.getEvmKitManager(bridge.defaultBlockchainType)
                    val wrapper = account?.let { evmKitManager.getEvmKitWrapper(it, bridge.defaultBlockchainType) }
                    val address = wrapper?.evmKit?.receiveAddress?.hex ?: "0x0"

                    // Build params JSON array string matching WC format
                    val paramsStr = when (method) {
                        "personal_sign" -> JSONArray().apply { put(params); put(address) }.toString()
                        "eth_sign" -> JSONArray().apply { put(address); put(params) }.toString()
                        "eth_signTypedData", "eth_signTypedData_v4" -> JSONArray().apply { put(address); put(params) }.toString()
                        else -> JSONArray().apply { put(params) }.toString()
                    }

                    activity?.runOnUiThread {
                        WCDelegate.dappRequestEvent = WCDelegate.DAppRequest(
                            id = requestId.toLong(),
                            method = method,
                            params = paramsStr,
                            chainId = "eip155:${bridge.defaultChain.id}",
                            peerName = url,
                            peerUrl = url,
                            peerIcon = "",
                            blockchainType = bridge.defaultBlockchainType,
                            chainName = bridge.defaultChain.name,
                            onRespond = { id, result -> bridge.resolveRequest(id.toInt(), result) },
                            onReject = { id -> bridge.rejectRequest(id.toInt()) },
                        )

                        try {
                            findNavController().slideFromBottom(R.id.wcRequestFragment)
                        } catch (e: Exception) {
                            Log.e("Web3Bridge", "slideFromBottom error: ${e.message}")
                            bridge.rejectRequest(requestId, e.message ?: "Navigation error")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Web3Bridge", "onSign error: ${e.message}")
                    bridge.rejectRequest(requestId, e.message ?: "Internal error")
                }
            },
            onSwitchChain = { chainId ->
                Log.d("Web3Bridge", "onSwitchChain: $chainId")
                val requestedChain = io.horizontalsystems.ethereumkit.models.Chain.values()
                    .firstOrNull { it.id == chainId }
                requestedChain != null
            }
        )
        Log.d("Web3Bridge", "Bridge initialized for $url")
    }

    private fun injectWeb3Provider() {
        if (isProviderInjected) return
        isProviderInjected = true  // set immediately to prevent duplicate injection from multiple onPageFinished calls
        val bridge = web3Bridge ?: return

        // Set up response callback - pushes results back to JS
        bridge.setResponseCallback { jsonResponse ->
            Log.d("Web3Bridge", "sendResponse: $jsonResponse")
            webView.post {
                val escaped = jsonResponse.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "")
                webView.evaluateJavascript(
                    "window._safeWalletOnResponse && window._safeWalletOnResponse('$escaped');",
                    { evalResult -> Log.d("Web3Bridge", "Response JS evaluated: $evalResult") }
                )
            }
        }

        // Pre-set account info BEFORE injecting script to avoid race condition.
        // The script uses these synchronized values for chainId/networkVersion/selectedAddress.
        val chainIdHex = "0x" + bridge.defaultChain.id.toString(16)
        val networkVersion = bridge.defaultChain.id.toString()
        var selectedAddress = ""

        try {
            val account = App.accountManager.activeAccount
            if (account != null) {
                val evmKitManager = App.evmBlockchainManager.getEvmKitManager(bridge.defaultBlockchainType)
                val wrapper = evmKitManager.getEvmKitWrapper(account, bridge.defaultBlockchainType)
                val addr = wrapper?.evmKit?.receiveAddress?.hex
                if (addr != null) {
                    bridge.setAccounts(listOf(addr))
                    selectedAddress = addr
                }
            }
        } catch (e: Exception) {
            Log.e("Web3Bridge", "setAccounts error: ${e.message}")
        }

        // Inject the provider script with synchronized state
        val script = bridge.buildProviderScript(chainIdHex, networkVersion, selectedAddress)
        webView.evaluateJavascript(script) { injectResult ->
            // injectResult is null for IIFE (no explicit return), which is normal
            Log.d("Web3Bridge", "Provider evaluated: $injectResult (null is expected for IIFE)")

            // Verify that window.ethereum was actually set up
            webView.evaluateJavascript("window.ethereum && window.ethereum._safeWalletInjected") { hasProvider ->
                val success = "true" == hasProvider
                Log.d("Web3Bridge", "Provider verified: _safeWalletInjected=$hasProvider, success=$success, chainId=$chainIdHex, networkVersion=$networkVersion, address=$selectedAddress")
                if (!success) {
                    Log.e("Web3Bridge", "Provider injection failed! window.ethereum._safeWalletInjected not set")
                    isProviderInjected = false  // allow retry on next onPageFinished
                }
            }
        }
    }

    private var retryCount = 0
    private var lastFailedUrl: String? = null
    @SuppressLint("SetJavaScriptEnabled")
    private fun setting() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.e("connectWallet", "shouldOverrideUrlLoading: $url")
                if (url?.contains("requestId") == true || url?.startsWith("data:text/html") == true) return true
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
                    // Re-inject on navigation
                    isProviderInjected = false
                }
                return false
            }

            /**
             * Intercept main page response to strip CSP headers.
             * Uniswap's CSP blocks privy.app.uniswap.org iframe which is needed for wallet connection.
             */
            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (request == null || !request.isForMainFrame) return null
                val url = request.url?.toString() ?: return null
                if (!url.contains("app.uniswap.org")) return null

                try {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("User-Agent", request.requestHeaders?.get("User-Agent") ?: "Mozilla/5.0")
                    // Forward cookies from WebView
                    val cookies = CookieManager.getInstance().getCookie(url)
                    if (!cookies.isNullOrEmpty()) {
                        connection.setRequestProperty("Cookie", cookies)
                    }
                    connection.connect()

                    val contentType = connection.contentType ?: "text/html; charset=utf-8"
                    val mimeType = contentType.split(";")[0].trim()
                    val encoding = connection.contentEncoding ?: "utf-8"
                    var body = connection.inputStream.use { it.readBytes() }

                    // Sync cookies back to WebView
                    connection.headerFields?.get("Set-Cookie")?.forEach { cookie ->
                        CookieManager.getInstance().setCookie(url, cookie)
                    }

                    // Strip CSP from both HTTP headers and HTML <meta> tags to allow privy iframe
                    val headers = mutableMapOf<String, String>()
                    connection.headerFields?.forEach { (key, values) ->
                        if (key != null && values != null && values.isNotEmpty()) {
                            if (!key.equals("Content-Security-Policy", ignoreCase = true) &&
                                !key.equals("Content-Security-Policy-Report-Only", ignoreCase = true)) {
                                headers[key] = values.joinToString(", ")
                            }
                        }
                    }

                    // Also strip <meta> CSP tags from HTML body (Uniswap may set CSP via meta tags)
                    if (mimeType.contains("html", ignoreCase = true)) {
                        val bodyStr = String(body, charset(encoding))
                        val stripped = bodyStr
                            .replace(Regex("<meta\\s+http-equiv\\s*=\\s*[\"']Content-Security-Policy[\"'][^>]*>", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("<meta\\s+http-equiv\\s*=\\s*[\"']Content-Security-Policy-Report-Only[\"'][^>]*>", RegexOption.IGNORE_CASE), "")
                        body = stripped.toByteArray(charset(encoding))
                    }

                    Log.d("Web3Bridge", "CSP stripped for: $url")
                    return WebResourceResponse(mimeType, encoding,
                        connection.responseCode, connection.responseMessage, headers, body.inputStream())
                } catch (e: Exception) {
                    Log.e("Web3Bridge", "CSP strip error: ${e.message}")
                }
                return null
            }

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                return null
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                Log.d("connectWallet", "onReceivedSslError= $error")
                handler?.proceed()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.d("connectWallet", "onReceivedError error=${error?.errorCode}, ${error?.description}")
                // 只处理主框架的错误，不处理 iframe 等子资源
                if (request?.isForMainFrame == true) {
                    val errorCode = error?.errorCode
                    val url = request.url.toString()

                    if (isRetryableError(errorCode)) {
                        retryCount++
                        lastFailedUrl = url

                        Log.d("WebView", "加载失败，第${retryCount}次重试: $url")

                        // 重置 autoConnect 以便下次页面加载完成后重新执行 getSession
                        autoConnect = true

                        // 延迟后重试
                        view?.postDelayed({
                            view.loadUrl(url)
                        }, 2000)
                    } else {
                        // 非可重试错误，显示错误页面
                        retryCount = 0
                        showErrorPage(view, errorCode ?: -1)
                    }
                } else {
//                    super.onReceivedError(view, request, error)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {

            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 页面加载成功，重置重试计数
                retryCount = 0
                lastFailedUrl = null
                // Inject Web3 provider JS
                injectWeb3Provider()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Log.e("connectWallet", "progress: $newProgress")
                binding.progressBar.progress = newProgress
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100 && autoConnect) {
                    autoConnect = false
                    getSession()
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                if (consoleMessage?.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    Log.e("Web3JS", "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                } else {
                    Log.d("Web3JS", "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()}")
                }
                return true
            }
        }
        val webViewSettings = webView.settings
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.loadWithOverviewMode = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.databaseEnabled = true
        webViewSettings.loadWithOverviewMode = true
        webViewSettings.useWideViewPort = true
        webViewSettings.allowFileAccess = true
        webViewSettings.allowContentAccess = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webViewSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Register Web3 bridge as JS interface
        web3Bridge?.let { bridge ->
            webView.addJavascriptInterface(bridge, "_safeWalletBridge")
        }
    }

    private fun isRetryableError(errorCode: Int?): Boolean {
        return errorCode == ERROR_TIMEOUT ||           // ERR_CONNECTION_TIMED_OUT
                errorCode == ERROR_CONNECT ||           // 连接失败
                errorCode == ERROR_HOST_LOOKUP ||       // DNS 解析失败
                errorCode == ERROR_PROXY_AUTHENTICATION // 代理认证
    }

    private fun showErrorPage(view: WebView?, errorCode: Int) {
        val html = """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    text-align: center;
                    padding: 50px;
                    font-family: system-ui, -apple-system, sans-serif;
                }
                h2 {
                    font-size: 48px;
                    margin-bottom: 20px;
                }
                p {
                    font-size: 32px;
                    margin-bottom: 40px;
                    color: #666;
                }
                button {
                    font-size: 36px;
                    padding: 16px 48px;
                    background-color: #007aff;
                    color: white;
                    border: none;
                    border-radius: 12px;
                    cursor: pointer;
                }
                button:active {
                    background-color: #0051d5;
                }
                #retryHint {
                    font-size: 28px;
                    color: #999;
                    margin-top: 30px;
                }
            </style>
        </head>
        <body>
            <h2>加载失败</h2>
            <p>错误码: $errorCode</p>
            <button onclick="location.reload()">点击重试</button>
            <div id="retryHint">3 秒后自动重试...</div>
            <script>
                var countdown = 3;
                var el = document.getElementById('retryHint');
                var timer = setInterval(function() {
                    countdown--;
                    if (countdown <= 0) {
                        clearInterval(timer);
                        location.reload();
                    } else {
                        el.textContent = countdown + ' 秒后自动重试...';
                    }
                }, 1000);
            </script>
        </body>
        </html>
    """.trimIndent()
        view?.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        // Also schedule a native-level retry after 4 seconds as fallback
        view?.postDelayed({
            autoConnect = true
            view.loadUrl(lastFailedUrl ?: urlString)
        }, 4000)
    }

    private fun getSession() {
        val accountId = App.accountManager.activeAccount?.id ?: return
        val cacheConnectLink = App.preferences.getString(getKey(urlString), null) ?: return
        Log.e("connectWallet", "auto connect $cacheConnectLink")

        /*App.wc2SessionManager.sessions.forEach {
            Log.e("connectWallet", "auto connect v2 ${it.topic}, ${it.metaData?.url}, $cacheConnectLink")
            if (cacheConnectLink == it.metaData?.url) {
                Log.e("connectWallet", "auto connect v2")
                connectSession(it.topic, false)
            }
        }*/
    }

    private fun getKey(linkString: String): String {
        return Base64.encodeToString(linkString.toByteArray(), Base64.DEFAULT)
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

    private fun connectSession(session: String, isV1: Boolean) {
        if (isConnecting) return
        isConnecting = true
        if (isV1) {

        } else {
            wc2Connect(session, null)
        }
    }

    private fun wc2Connect(topic: String?, connectionLink: String?) {
        walletConnectListViewModel.setConnectionUri(connectionLink ?: "")
    }

    override fun onDestroy() {
//        viewModel.cancel()
        webView?.let {
            (webView.parent as ViewGroup).removeView(webView)
            it.destroy()
        }
        disposables.dispose()
        MainModule.isOpenDapp = false
        super.onDestroy()
    }

    private fun initHistoryView() {
        val historyList = getHistory()
        if (historyList.isEmpty()) {
            hideHistory()
            return
        }
        adapter = DAppHistoryAdapter(historyList, object : DAppHistoryAdapter.HistoryClickListener {
            override fun onClick(url: String) {
                hideHistory()
                binding.inputWebUrl.setText(url)
                if (!isShowWarning) {
                    isShowWarning = true
                    showAlert(url)
                } else {
                    webView.loadUrl(checkUrl(url))
                }
            }
        })
        binding.rvHistory.adapter = adapter
        binding.ivDelete.setOnClickListener {
            deleteHistory()
        }
    }

    private fun getHistory(): List<String> {
        val sp = App.preferences
        val historyList = sp.getStringSet(HISTORY_KEY, mutableSetOf())
        return historyList?.map {
            it
        } ?: emptyList()
    }

    private fun saveHistory(url: String) {
        val sp = App.preferences
        val historyList = sp.getStringSet(HISTORY_KEY, mutableSetOf())
        historyList?.let {
            historyList.add(url)
            sp.edit().putStringSet(HISTORY_KEY, historyList).commit()
        }
    }

    private fun deleteHistory() {
        App.preferences.edit().remove(HISTORY_KEY).commit()
        adapter?.updateData(emptyList())
        hideHistory()
    }

    private fun hideHistory() {
        binding.layoutHistory.visibility = View.GONE
    }

    @Parcelize
    data class Input(
            val url: String,
            val name: String,
            val isInput: Boolean? = false
    ) : Parcelable
}
