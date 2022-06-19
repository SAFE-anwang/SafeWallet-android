package io.horizontalsystems.bankwallet.modules.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.NavHostFragment
import com.v2ray.ang.AppConfig
import androidx.activity.viewModels
import com.v2ray.ang.util.Utils
import com.walletconnect.walletconnectv2.client.WalletConnectClient
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.net.SafeNetWork
import io.horizontalsystems.bankwallet.net.VpnConnectService
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2.WC2SignMessageRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SignMessageRequest
import io.horizontalsystems.bitcoincore.ReConnectVpn

class MainActivity : BaseActivity() {

    val viewModel by viewModels<MainActivityViewModel>(){
        MainModule.FactoryForActivityViewModel()
    }

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            VpnConnectService.startVpn(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navHost.navController.setGraph(R.navigation.main_graph, intent.extras)
        navHost.navController.addOnDestinationChangedListener(this)

        val filter = IntentFilter()
        filter.addAction(AppConfig.BROADCAST_ACTION_ACTIVITY)
        filter.addAction("com.anwang.safe.connect")
        filter.addAction("com.anwang.safe.reconnect")
        registerReceiver(mMsgReceiver, filter)

        startVpn()

        viewModel.openWalletConnectRequestLiveEvent.observe(this) { wcRequest ->
            when (wcRequest) {
                is WC2SignMessageRequest -> {
                    navHost.navController.slideFromBottom(
                        R.id.wc2SignMessageRequestFragment,
                        WC2SignMessageRequestFragment.prepareParams(wcRequest.id)
                    )
                }
                is WC2SendEthereumTransactionRequest -> {
                    navHost.navController.slideFromBottom(
                        R.id.wc2SendEthereumTransactionRequestFragment,
                        WC2SendEthereumTransactionRequestFragment.prepareParams(wcRequest.id)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mMsgReceiver)
        WalletConnectClient.shutdown()
        Utils.stopVService(this)
        VpnConnectService.startLoopCheckConnection = false
    }

    override fun onTrimMemory(level: Int) {
        if (level >= TRIM_MEMORY_COMPLETE) {
            val logger = AppLogger("low memory")
            logger.info("onTrimMemory level: $level")
            Utils.stopVService(this)
        }
        super.onTrimMemory(level)
    }

    private fun startVpn() {
        if (!getSharedPreferences("vpnSetting", Context.MODE_PRIVATE).getBoolean("vpnOpen", true)) {
            return
        }
        val intent = VpnService.prepare(this)
        if (intent == null) {
            VpnConnectService.startVpn(this)
        } else {
            requestVpnPermission.launch(intent)
        }
    }

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == "com.anwang.safe.connect") {
                Log.e("VpnConnectService", "connect node broadcast")
                VpnConnectService.connectVpn(this@MainActivity)
                return
            }
            if (intent?.action == "com.anwang.safe.reconnect") {
                Log.e("VpnConnectService", "re connect node broadcast")
                VpnConnectService.reConnectVpn(this@MainActivity)
                return
            }
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING -> {

                    Log.e("VpnConnectService", "connect running")
                }
                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    Log.e("VpnConnectService", "connect not running")
                    VpnConnectService.connecting = false
                }
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    Log.e("VpnConnectService", "connect success-")
                    VpnConnectService.connecting = false
                    // 测试是否能范围外网
                    VpnConnectService.lookCheckVpnConnection(this@MainActivity)
                    // 检查chain.anwang.com 是否可连接
                    SafeNetWork.testAnWangConnect()
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    Log.e("VpnConnectService", "connect failure")
                }
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    Log.e("VpnConnectService", "stop success")
                    VpnConnectService.startLoopCheckConnection = false
                    VpnConnectService.connecting = false
                }
            }
        }
    }
}
