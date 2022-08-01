package io.horizontalsystems.bankwallet.modules.dapp

import android.os.Bundle
import android.util.Base64
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.databinding.FragmentDappBrowseBinding
import io.horizontalsystems.bankwallet.modules.walletconnect.SafeWalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2.WC2SignMessageRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionService
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Request
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SignMessageRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2PingService
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Request
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SignMessageRequest
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.findNavController
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.net.URLDecoder

class DAppBrowseFragment: BaseFragment(){

//    private lateinit  var baseViewModel : WalletConnectViewModel

    private var _binding: FragmentDappBrowseBinding? = null
    private val binding get() = _binding!!

    private lateinit var webView: WebView
    /*private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var webRootView: LinearLayout*/
    private lateinit var urlString: String

    private var wc1Service: WC1Service? = null
    private var wc2Service: WC2SessionService? = null

    private val disposables = CompositeDisposable()
    private val openRequestLiveEvent = SingleLiveEvent<WC1Request>()
    val openWalletConnectRequestLiveEvent = SingleLiveEvent<WC2Request>()

    private var autoConnect = true
    private var isConnecting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDappBrowseBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        /*toolbar = findViewById(R.id.dappToolbar)
        progressBar = findViewById(R.id.progressBar)
        webRootView = findViewById(R.id.webRootView)*/
        binding.dappToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        val url = arguments?.getString("url")
        val name = arguments?.getString("name")
        binding.dappToolbar.title = name
        binding.progressBar.progress = 0
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
        openRequestLiveEvent.observe(viewLifecycleOwner, Observer {
            /*val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.mainFragment) {
                WalletConnectModule.Factory2(
                    wc1Service!!
                )
            }*/
            when (it) {
                is WC1SendEthereumTransactionRequest -> {
//                    baseViewModel.sharedSendEthereumTransactionRequest = it

                    findNavController().slideFromRight(
                        R.id.mainFragment_to_wcSendEthereumTransactionRequestFragment
                    )
                }
                is WC1SignMessageRequest -> {
                    Log.e("connectWallet", "navigation sign message")
//                    baseViewModel.sharedSignMessageRequest = it

                    findNavController().slideFromRight(
                        R.id.mainFragment_to_wcSignMessageRequestFragment
                    )
                }
            }
        })
        openWalletConnectRequestLiveEvent.observe(viewLifecycleOwner, Observer {
            when (it) {
                is WC2SignMessageRequest -> {
                    findNavController().slideFromBottom(
                        R.id.wc2SignMessageRequestFragment,
                        WC2SignMessageRequestFragment.prepareParams(it.id)
                    )
                }
                is WC2SendEthereumTransactionRequest -> {
                    findNavController().slideFromBottom(
                        R.id.wc2SendEthereumTransactionRequestFragment,
                        WC2SendEthereumTransactionRequestFragment.prepareParams(it.id)
                    )
                }
            }
        })
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
                Log.e("connectWallet", "shouldOverrideUrlLoading: $url")
                if (url?.startsWith("wc:") == true) {
                    connectWallet(url)
                    return true
                }
                url?.let {
                    view?.loadUrl(url)
                }
                return false
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
        }
        val webViewSettings = webView.settings
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.loadWithOverviewMode = true
    }

    private fun getSession() {
        val accountId = App.accountManager.activeAccount?.id ?: return
        val cacheConnectLink = App.preferences.getString(getKey(urlString), null) ?: return
        Log.e("connectWallet", "auto connect $cacheConnectLink")
        App.wc1SessionManager.sessions.forEach {
            if (it.accountId == accountId && cacheConnectLink == it.session.toUri()) {
                Log.e("connectWallet", "auto connect v1 $cacheConnectLink")
                connectSession(it.remotePeerId, true)
            }
        }
        App.wc2SessionManager.sessions.forEach {
            if (it.accounts.contains(accountId) && cacheConnectLink == it.peerAppMetaData?.url) {
                Log.e("connectWallet", "auto connect v2")
                connectSession(it.topic, false)
            }
        }
    }

    private fun getKey(linkString: String): String {
        return Base64.encodeToString(linkString.toByteArray(), Base64.DEFAULT)
    }

    private fun connectWallet(connectionLink: String) {
        if (connectionLink.endsWith("@1") || connectionLink.endsWith("@2")) return
        if (isConnecting) return
        isConnecting = true
        when {
            connectionLink.contains("@1?") -> wc1Connect(null, connectionLink)
            connectionLink.contains("@2?") -> wc2Connect(null, connectionLink)
        }
    }

    private fun connectSession(session: String, isV1: Boolean) {
        if (isConnecting) return
        isConnecting = true
        if (isV1) {
            wc1Connect(session, null)
        } else {
            wc2Connect(session, null)
        }
    }

    private fun wc1Connect(remotePeerId: String?, connectionLink: String?) {
        Log.e("connectWallet", "v1 connect")
        wc1Service = WC1Service(
            remotePeerId,
            connectionLink,
            App.wc1Manager,
            App.wc1SessionManager,
            App.wc1RequestManager,
            App.connectivityManager
        )
        val baseViewModel by navGraphViewModels<SafeWalletConnectViewModel>(R.id.mainFragment) {
            WalletConnectModule.Factory2(
                wc1Service!!
            )
        }
        baseViewModel.service = wc1Service!!

        wc1Service!!.connectionStateObservable
            .subscribe {
                Log.e("connectWallet", "connect state: $it, ${wc1Service?.state}")
                if (it is WalletConnectInteractor.State.Disconnected) {
                    wc1Service?.reconnect()
                }
            }
            .let {
                disposables.add(it)
            }
        wc1Service!!.stateObservable
            .subscribe {
                Log.e("connectWallet", "service state: $it")
                isConnecting = false

                when(it) {
                    WC1Service.State.WaitingForApproveSession -> {
                        wc1Service?.approveSession()
                        // 保存连接钱包链接， 下次进入时自动连接
                        connectionLink?.let {
                            val decodeUrl = URLDecoder.decode(connectionLink)
                            Log.e("connectWallet", "decode: $decodeUrl")
                            App.preferences.edit().putString(getKey(urlString), decodeUrl).commit()
                        }
                    }
                    is WC1Service.State.Invalid,
                    WC1Service.State.Killed -> {
                        wc1Service?.stop()
                        disposables.clear()
                    }
                }
            }
            .let {
                disposables.add(it)
            }
        wc1Service!!.requestObservable
            .subscribe {
                Log.e("connectWallet", "requestObservable: $it, ${Thread.currentThread().name}")

                when (it) {
                    is WC1SendEthereumTransactionRequest -> {
                        baseViewModel.sharedSendEthereumTransactionRequest = it
                    }
                    is WC1SignMessageRequest -> {
                        Log.e("connectWallet", "navigation sign message")
                        baseViewModel.sharedSignMessageRequest = it
                    }
                }
                openRequestLiveEvent.postValue(it)
            }
            .let {
                disposables.add(it)
            }
        wc1Service?.start()
    }

    private fun wc2Connect(topic: String?, connectionLink: String?) {
        wc2Service = WC2SessionService(
            App.wc2Service,
            App.wc2Manager,
            App.wc2SessionManager,
            App.accountManager,
            WC2PingService(),
            App.connectivityManager,
            topic,
            connectionLink,
        )
        wc2Service!!.connectionStateObservable
            .subscribe {
                Log.e("connectWallet", "connect state: $it")
                if (it is WC2PingService.State.Disconnected) {
                    wc2Service?.reconnect()
                }
            }
            .let {
                disposables.add(it)
            }
        wc2Service!!.stateObservable
            .subscribe {
                Log.e("connectWallet", "service state: $it")
                isConnecting = false
                when(it) {
                    WC2SessionService.State.WaitingForApproveSession -> {
                        wc2Service?.approve()
                        // 保存连接钱包链接， 下次进入时自动连接
                        connectionLink?.let {
                            val decodeUrl = URLDecoder.decode(connectionLink)
                            Log.e("connectWallet", "decode: $decodeUrl")
                            App.preferences.edit().putString(getKey(urlString), decodeUrl).commit()
                        }
                    }
                    is WC2SessionService.State.Invalid,
                    WC2SessionService.State.Killed -> {
                        wc2Service?.stop()
                        wc2Service = null
                    }
                }
            }
            .let {
                disposables.add(it)
            }
        App.wc2SessionManager.pendingRequestObservable
            .subscribe{
                openWalletConnectRequestLiveEvent.postValue(it)
            }.let {
                disposables.add(it)
            }
        wc2Service?.start()
    }

    override fun onDestroy() {
        webView?.let {
            (webView.parent as ViewGroup).removeView(webView)
            it.destroy()
        }
        disposables.dispose()
        wc1Service?.stop()
        wc2Service?.disconnect()
        wc1Service = null
        wc2Service = null
        super.onDestroy()
    }

}
