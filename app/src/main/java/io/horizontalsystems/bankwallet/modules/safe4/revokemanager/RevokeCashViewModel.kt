package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

import android.util.Log
import androidx.lifecycle.ViewModel
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.ethereumkit.models.Chain

class RevokeCashViewModel(
    val chain: Chain,
    val address: String,
    val account: Account
): ViewModel() {

    private val KEY = "revokeConnectInfo"

    init {
        if (getConnectInfo() == null) {
            val revokeConnectInfo = RevokeConnectInfo(address, chain.id, account.id, false)
            save(revokeConnectInfo)
        }
    }


    fun getUrl(): String {
        val connectInfo = getConnectInfo()
        val url =  if (connectInfo != null) {
            "https://revoke.cash/zh/address/${connectInfo.walletAddress}?chainId=${connectInfo.chainId}"
        }else {
            "https://revoke.cash"
        }
        Log.d("RevokeCash", "url=$url")
        return url
    }

    fun switchChain(chainId: Int?) {
        Log.d("RevokeCash", "switch chain=$chainId")
        chainId?.let {
            save(RevokeConnectInfo(address, it, account.id, false))
        }
    }

    private fun save(revokeConnectInfo: RevokeConnectInfo) {
        MMKV.defaultMMKV()?.encode(KEY, revokeConnectInfo)
    }

    private fun getConnectInfo(): RevokeConnectInfo? {
        return MMKV.defaultMMKV()?.decodeParcelable(KEY, RevokeConnectInfo::class.java)
    }

}