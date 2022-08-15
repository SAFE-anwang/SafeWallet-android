package io.horizontalsystems.bankwallet.net

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.VpnServerInfo
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import kotlinx.coroutines.*
import okhttp3.Request

object SafeNetWork {

    // chain.anwang.com is enable
    var isSafeNetConnectEnable = true
    var vpnNodes: List<VpnServerInfo>? = null

    fun testAnWangConnect() {
        GlobalScope.launch(Dispatchers.IO) {
            delay(1000)
            try {
                val httpClient = NetworkUtils.getUnsafeOkHttpClient()
                val request = Request.Builder().get().url("https://chain.anwang.com").build()
                val code = httpClient.newCall(request).execute().code
                val isReachable = code >= 200 && code < 400
                withContext(Dispatchers.Main) {
                    if (!isReachable) {
                        // 测试失败，改用IP访问chain.anwang.com
                        isSafeNetConnectEnable = false
                    }
                }
            } catch (e: Exception) {
                isSafeNetConnectEnable = false
            }
        }
    }

    fun getSafeDomainName(): String {
        return if (isSafeNetConnectEnable) {
            "chain.anwang.com"
        } else {
            getIp()
        }
    }

    private fun getIp(): String {
        // default
        return "47.88.254.135"
    }

    fun getVpnServerInfo(): List<VpnServerInfo> {
        return vpnNodes ?: getDefaultInfo()
    }

    fun getDefaultInfo(): List<VpnServerInfo> {
        val localNoteList = App.vpnServerStorage.allVpnServer()
        if (localNoteList.isNotEmpty()) {
            return localNoteList
        }
        val noteList = mutableListOf<VpnServerInfo>()
        noteList.add(
            VpnServerInfo(
            "192.46.228.81",
            52369,
            "b8e23012-d6ea-42a2-8864-d8065527f06c",
            "0",
            "ws",
            "none"
        ))
        noteList.add(
            VpnServerInfo(
            "139.162.75.142",
            41167,
            "6407ce3d-9284-44d7-865d-344187454171",
            "0",
            "ws",
            "none"
        ))
        noteList.add(
            VpnServerInfo(
            "172.105.114.148",
            52369,
            "b8e23012-d6ea-42a2-8864-d8065527f06c",
            "0",
            "ws",
            "none"
        ))
        noteList.add(
            VpnServerInfo(
            "172.104.115.92",
                    411674,
            "b90ab1f5-f57d-47a5-b448-6c8f310114b1",
            "0",
            "ws",
            "none"
        ))
        return noteList
    }
}