package io.horizontalsystems.bankwallet.modules.dapp

import android.os.Bundle
import android.os.Message
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectModule
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionService
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2PingService
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.net.URLDecoder
import java.net.URLEncoder

class DAppBrowseActivity: BaseActivity(){

    private lateinit var webView: WebView
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var webRootView: LinearLayout
    private lateinit var urlString: String

    private var wc1Service: WC1Service? = null
    private var wc2Service: WC2SessionService? = null

    private val disposables = CompositeDisposable()

    private var autoConnect = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dapp_browse)

        initUI()
    }

    private fun initUI() {
        toolbar = findViewById(R.id.dappToolbar)
        progressBar = findViewById(R.id.progressBar)
        webRootView = findViewById(R.id.webRootView)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        val url = intent?.extras?.getString("url")
        val name = intent?.extras?.getString("name")
        toolbar.title = name
        progressBar.progress = 0
        addWebView()
        url?.let {
            urlString = it
            webView.loadUrl(url)
        }
    }

    private fun addWebView() {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        webView = WebView(applicationContext)
        webRootView.addView(webView, layoutParams)
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
                progressBar.progress = newProgress
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

        App.wc1SessionManager.sessions.forEach {
            if (it.accountId == accountId && cacheConnectLink == it.session.toUri()) {
                Log.e("connectWallet", "auto connect")
                wc1Connect(it.remotePeerId, null)
            }
        }
        App.wc2SessionManager.sessions.forEach {
            if (cacheConnectLink == it.metaData?.url) {
                Log.e("connectWallet", "auto connect")
                wc2Connect(it.topic, null)
            }
        }
    }

    private fun getKey(linkString: String): String {
        return Base64.encodeToString(linkString.toByteArray(), Base64.DEFAULT)
    }

    private fun connectWallet(connectionLink: String) {
        when {
            connectionLink.contains("@1?") -> wc1Connect(null, connectionLink)
            connectionLink.contains("@2?") -> wc2Connect(null, connectionLink)
        }
    }

    private fun wc1Connect(remotePeerId: String?, connectionLink: String?) {
        wc1Service = WC1Service(
            remotePeerId,
            connectionLink,
            App.wc1Manager,
            App.wc1SessionManager,
            App.wc1RequestManager,
            App.connectivityManager,
            App.evmBlockchainManager
        )
        wc1Service!!.connectionStateObservable
            .subscribe {
                Log.e("connectWallet", "connect state: $it")
                if (it == WalletConnectInteractor.State.Connected) {
                }
            }
            .let {
                disposables.add(it)
            }
        wc1Service!!.stateObservable
            .subscribe {
                Log.e("connectWallet", "service state: $it")
                if (it == WC1Service.State.WaitingForApproveSession) {
                    wc1Service?.approveSession()
                    // 保存连接钱包链接， 下次进入时自动连接
                    connectionLink?.let {
                        val decodeUrl = URLDecoder.decode(connectionLink)
                        Log.e("connectWallet", "decode: $decodeUrl")
                        App.preferences.edit().putString(getKey(urlString), decodeUrl).commit()
                    }
                }
            }
            .let {
                disposables.add(it)
            }
        wc1Service!!.requestObservable
            .subscribe {

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
            App.evmBlockchainManager,
            topic,
            connectionLink,
        )
        wc2Service!!.connectionStateObservable
            .subscribe {
                Log.e("connectWallet", "connect state: $it")
                if (it == WC2PingService.State.Connected) {
                }
            }
            .let {
                disposables.add(it)
            }
        wc2Service!!.stateObservable
            .subscribe {
                Log.e("connectWallet", "service state: $it")
                if (it == WC2SessionService.State.WaitingForApproveSession) {
                    wc2Service?.approve()
                }
            }
            .let {
                disposables.add(it)
            }
        wc2Service?.start()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        webView?.let {
            (webView.parent as ViewGroup).removeView(webView)
            it.destroy()
        }
        disposables.dispose()
        super.onDestroy()
    }

}
