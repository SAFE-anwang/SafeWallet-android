package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

import android.webkit.JavascriptInterface
import com.google.android.exoplayer2.util.Log

class RevokeCashMessageHandler {


    @JavascriptInterface
    fun handleResponse(response: String) {
        Log.d("RevokeCash", "response=$response")
    }

}