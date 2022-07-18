package io.horizontalsystems.bankwallet.modules.dapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*

class DAppBrowseActivity: BaseActivity(){

    private lateinit var webView: WebView
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var webRootView: LinearLayout

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
                url?.let {
                    view?.loadUrl(it)
                }
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Log.e("longwen", "progress: $newProgress")
                progressBar.progress = newProgress
                super.onProgressChanged(view, newProgress)
            }
        }
        val webViewSettings = webView.settings
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.loadWithOverviewMode = true
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        Log.e("longwen", "progress:-----")
        webView?.let {
            (webView.parent as ViewGroup).removeView(webView)
            it.destroy()
        }
        super.onDestroy()
    }

}
