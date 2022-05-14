package io.horizontalsystems.bankwallet.net

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.ServerConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.VpnServerInfo
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewFragment
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.*

object VpnConnectService {

    val connectNode = HashSet<String>()
    var startLoopCheckConnection = false
    private var firstCheck = 0
    private var httpClient: OkHttpClient? = OkHttpClient()

    fun startVpn(activity: Activity) {
        if (!activity.getSharedPreferences("vpnSetting", Context.MODE_PRIVATE).getBoolean("vpnOpen", true)) {
            return
        }
        SafeNetService().getVpnNodes().map { response ->
            response.map { vpnInfo ->
                val array = vpnInfo["endpoint"]!!.split(":")
                VpnServerInfo(
                    array[0],
                    array[1].toInt(),
                    vpnInfo["uid"]!!,
                    "0",
                    "ws",
                    "none"
                )
            }
        }.subscribeOn(Schedulers.io())
            .subscribe({
                // cache
                App.vpnServerStorage.clearAll()
                it.forEach {
                    App.vpnServerStorage.save(it)
                }
                SafeNetWork.vpnNodes =  it
                connectVpn(activity)
            },
            {
                connectVpn(activity)
            }).let {
            }
    }

    fun connectVpn(activity: Activity) {
        if (setServerConfig()) {
            V2RayServiceManager.startV2Ray(activity)
        }
    }

    fun stopConnect(activity: Activity) {
        V2RayServiceManager.stopV2Ray(activity)
    }

    private fun setServerConfig(): Boolean {
        // 设置走代理的APP
        setProxyApps()
        val serverInfo = getConnectServerNode() ?: return false
        Log.e("VpnConnectService", "connect ip: ${serverInfo.address}")
        // 移除上次配置的服务器
        MmkvManager.removeServer()
        connectNode.add(serverInfo.address)
        val config = ServerConfig.create(serverInfo.type)
        initOutbound(serverInfo, config)
        return true
    }

    private fun setProxyApps() {
        val apps = HashSet<String>()
        apps.add("com.anwang.safewallet")
        apps.add("com.anwang.safewallet.dev")
        apps.add("com.anwang.safewallet.dev.testnet")
        apps.add("com.anwang.safewallet.dev.appcenter")
        apps.add("com.anwang.safewallet.appcenter")
        apps.add("org.telegram.messenger.web")
        apps.add("org.telegram.messenger")
        MmkvManager.setProxyApps(apps)
    }

    private fun initOutbound(serverInfo: VpnServerInfo, config: ServerConfig) {
        config.outboundBean?.settings?.vnext?.get(0)?.let {
            it.address = serverInfo.address
            it.port = serverInfo.port
            it.users[0].id = serverInfo.clientId
            if (config.configType == EConfigType.VMESS) {
                it.users[0].security = serverInfo.securitys
                it.users[0].encryption = "auto"
            } else if (config.configType == EConfigType.VLESS) {
                /*it.users[0].encryption = et_security.text.toString().trim()
                if (streamSecuritys[sp_stream_security.selectedItemPosition] == V2rayConfig.XTLS) {
//                vnext.users[0].flow = flows[sp_flow.selectedItemPosition].ifBlank { V2rayConfig.DEFAULT_FLOW }
                } else {
                    it.users[0].flow = ""
                }*/
            }
        }

        config.outboundBean?.settings?.servers?.get(0)?.let { server ->
            server.address = serverInfo.address
            server.port = serverInfo.port
            server.method = "auto"
            if (config.configType == EConfigType.SHADOWSOCKS) {
                server.password = ""
                server.method = "auto"
            } else if (config.configType == EConfigType.SOCKS) {
                server.users = null
                /*val socksUsersBean = V2rayConfig.OutboundBean.OutSettingsBean.ServersBean.SocksUsersBean()
                socksUsersBean.user = ""
                socksUsersBean.pass = ""
                server.users = listOf(socksUsersBean)*/
            } else if (config.configType == EConfigType.TROJAN) {
                server.password = serverInfo.clientId
            }
        }
        config.outboundBean?.streamSettings?.let { streamSetting ->
            val network = serverInfo.protocol
            val type = serverInfo.camouflageType
            val requestHost = ""
            val path = ""
            var sni = streamSetting.populateTransportSettings(
                transport = network,
                headerType = type,
                host = requestHost,
                path = path,
                seed = path,
                quicSecurity = requestHost,
                key = path,
                mode = type,
                serviceName = path
            )
            val allowInsecure = false
            val defaultTls = if (config.configType == EConfigType.TROJAN) V2rayConfig.TLS else ""
            streamSetting.populateTlsSettings(
                defaultTls,
                allowInsecure,
                sni
            )
        }

        MmkvManager.encodeServerConfig("", config)
    }

    private fun getConnectServerNode(): VpnServerInfo? {
        val noteList = SafeNetWork.getVpnServerInfo()
        if (noteList.size == 1) {
            return noteList[0]
        }
        if (connectNode.size == noteList.size) {
            return null
        }
        val random = Random()
        var index = 0
        while(true) {
            index = random.nextInt(noteList.size)
            if (!connectNode.contains(noteList[index].address)) {
                break
            }
        }
        return noteList[index]
    }

    fun refreshData(activity: Activity) {
        // 连接成功后，刷新钱包，连接VPN过程中有可能导致同步失败
        App.adapterManager.refresh()
        val mainActivity = activity as MainActivity
        try {
            getMarketOverviewFragment(mainActivity.supportFragmentManager.fragments) { fragment ->
                if (fragment is MarketOverviewFragment) {
                    fragment.viewModel.refresh()
                }
            }

        }catch (e: Exception) {
        }
    }

    private fun getMarketOverviewFragment(fragments: List<Fragment>, callback: (Fragment) -> Unit) {
        fragments.forEach {
            if (it is MarketOverviewFragment) {
                callback.invoke(it)
                return
            }
            val childFragment = it.childFragmentManager.fragments
            getMarketOverviewFragment(childFragment, callback)
        }
        return
    }

    fun lookCheckVpnConnection(activity: Activity) {
        if (startLoopCheckConnection) return
        startLoopCheckConnection = true
        firstCheck = 0
        GlobalScope.launch(Dispatchers.IO) {
            Log.e("VpnConnectService", "start look check connection")
            delay(2000)
            while (startLoopCheckConnection) {
                firstCheck ++
                val result = connectionNetwork()
                Log.e("VpnConnectService", "look check connection result: $result")
                if (!result) {
                    withContext(Dispatchers.Main) {
                        stopConnect(activity)
                    }
                    firstCheck = 0
                    startLoopCheckConnection = false
                    delay(500)
                    withContext(Dispatchers.Main) {
                        activity.sendBroadcast(Intent("com.anwang.safe.connect"))
                    }
                    break
                } else {
                    delay(3000)
                    if (firstCheck % 5 == 0) {
                        firstCheck = 1
                        withContext(Dispatchers.Main) {
                            refreshData(activity)
                        }
                    }
                }
            }
        }
    }

    private fun connectionNetwork(): Boolean {
        var result = false
        try {
            val request: Request = Request.Builder()
                .url("https://www.google.com")
                .build()
            val response = httpClient?.newCall(request)?.execute()
            response?.let {
                Log.e("VpnConnectService", "google response: ${response.code}")
                result = response.code in 200..399
            }
        } catch (e: IOException) {
            Log.e("VpnConnectService", "google error: ${e.message}")
        } finally {

        }
        return result
    }
}