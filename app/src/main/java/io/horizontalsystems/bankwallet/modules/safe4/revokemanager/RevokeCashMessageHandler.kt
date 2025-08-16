package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

import android.webkit.JavascriptInterface
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.hexStringToIntOrNull
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger


class RevokeCashMessageHandler(
    val onTransactionData: (TransactionData) -> Unit,
    val onSwitchChain: (Int?) -> Unit
) {

    @JavascriptInterface
    fun handleResponse(response: String) {
        Log.d("RevokeCash", "param=$response")
        try {
            val json: JSONObject = JSONObject(response)
            val type = json.getString("type")
            val id = json.getInt("id")
            val method = json.getString("method")
            val params = json.getJSONArray("params")
            when(method) {
                Web3Method.ethSendTransaction.methodName -> {
                    processTransaction(params.getJSONObject(0))
                }
                Web3Method.walletSwitchChain.methodName -> {
                    switchChain(params.getJSONObject(0))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processTransaction(params: JSONObject) {
        val data = params.getString("data")
        val from = params.getString("from")
        val to = params.getString("to")
        val transactionData = TransactionData(
            Address(to), BigInteger.ZERO, data.hexStringToByteArray()
        )
        onTransactionData.invoke(transactionData)
    }

    private fun switchChain(params: JSONObject) {
        val chainId = params.getString("chainId")
        onSwitchChain.invoke(chainId.hexStringToIntOrNull())
    }

}