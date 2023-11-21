package io.horizontalsystems.bankwallet.modules.dapp

import android.os.Build
import android.os.Bundle
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
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget1.LinearLayoutManager
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.databinding.FragmentDappBrowseBinding
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WC2RequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2.WC2SignMessageRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2MainViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionService
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.*
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.net.URLDecoder

class DAppBrowseFragment: BaseFragment(){

//    private lateinit  var baseViewModel : WalletConnectViewModel
    private val wc2MainViewModel by viewModels<WC2MainViewModel> {
        WC2MainViewModel.Factory()
    }
    private val viewModel by viewModels<WC2SessionViewModel> {
        WC2SessionModule.Factory(arguments?.getString(WC2SessionModule.SESSION_TOPIC_KEY))
    }

    private val HISTORY_KEY = "dapp_history"
    private var _binding: FragmentDappBrowseBinding? = null
    private val binding get() = _binding!!

    private lateinit var webView: WebView
    /*private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var webRootView: LinearLayout*/
    private lateinit var urlString: String

    private var wc2Service: WC2SessionService? = null

    private val disposables = CompositeDisposable()
    private val openWalletConnectRequestLiveEvent = SingleLiveEvent<Long>()
    private val errorLiveEvent = SingleLiveEvent<String?>()

    private var autoConnect = true
    private var isConnecting = false
    private var isShowWarning = false

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
    }

    private fun initUI() {
        /*toolbar = findViewById(R.id.dappToolbar)
        progressBar = findViewById(R.id.progressBar)
        webRootView = findViewById(R.id.webRootView)*/
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
        val url = arguments?.getString("url") ?: ""
        val name = arguments?.getString("name")
        val isInput = arguments?.getBoolean("isInput")?.let {
            binding.dappToolbar.visibility = if (it) View.GONE else View.VISIBLE
            binding.layoutInput.visibility = if (it) View.VISIBLE else View.GONE
            binding.layoutHistory.visibility = if (it) View.VISIBLE else View.GONE
        }
        binding.inputWebUrl.setText(url)
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
        openWalletConnectRequestLiveEvent.observe(viewLifecycleOwner, Observer {requestId ->
            findNavController().slideFromBottom(
                    R.id.wc2RequestFragment,
                    WC2RequestFragment.prepareParams(requestId)
            )
            /*when (it) {
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
            }*/
        })
        errorLiveEvent.observe(viewLifecycleOwner, Observer { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        })

        wc2MainViewModel.sessionProposalLiveEvent.observe(viewLifecycleOwner) {
            viewModel.connect()
        }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webViewSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    private fun getSession() {
        val accountId = App.accountManager.activeAccount?.id ?: return
        val cacheConnectLink = App.preferences.getString(getKey(urlString), null) ?: return
        Log.e("connectWallet", "auto connect $cacheConnectLink")

        App.wc2SessionManager.sessions.forEach {
            Log.e("connectWallet", "auto connect v2 ${it.topic}, ${it.metaData?.url}, $cacheConnectLink")
            if (cacheConnectLink == it.metaData?.url) {
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
            connectionLink.contains("@1?") -> {

            }
//            connectionLink.contains("@2/wc?") -> {}
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
        wc2Service = WC2SessionService(
            App.wc2Service,
            App.wc2Manager,
            App.wc2SessionManager,
            App.accountManager,
            WC2PingService(),
            App.connectivityManager,
            App.evmBlockchainManager,
            topic,
            connectionLink,
        )
        wc2Service!!.connectionStateObservable
            .subscribe {
                Log.e("connectWallet", "connect state v2: $it")
                if (it is WC2PingService.State.Disconnected) {
//                    wc2Service?.reconnect()
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
                    is WC2SessionService.State.Invalid -> {
                        errorLiveEvent.postValue(it.error.message)
                        disposables.clear()
                        wc2Service?.stop()
                        wc2Service = null
                    }
                    WC2SessionService.State.Killed -> {
                        wc2Service?.stop()
                        wc2Service = null
                    }
                    else -> {}
                }
            }
            .let {
                disposables.add(it)
            }
        App.wc2SessionManager.pendingRequestObservable
            .subscribe{
                Log.e("connectWallet", "pendingRequestObservable: $it")
                openWalletConnectRequestLiveEvent.postValue(it)
            }.let {
                disposables.add(it)
            }
        wc2Service?.start()
    }

    override fun onDestroy() {
        viewModel.cancel()
        webView?.let {
            (webView.parent as ViewGroup).removeView(webView)
            it.destroy()
        }
        disposables.dispose()
        wc2Service?.disconnect()
        wc2Service = null
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

}
