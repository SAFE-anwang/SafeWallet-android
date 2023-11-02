package io.horizontalsystems.bankwallet.modules.main

import android.graphics.Color
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.Utils
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Wallet
//import io.horizontalsystems.bankwallet.modules.send.SendActivity
import io.horizontalsystems.bankwallet.net.SafeNetWork
import io.horizontalsystems.bankwallet.net.VpnConnectService
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WC2RequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2MainViewModel
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.bankwallet.modules.theme.ThemeType

class MainActivity : BaseActivity() {

    private val wc2MainViewModel by viewModels<WC2MainViewModel> {
        WC2MainViewModel.Factory()
    }

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory()
    }

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                VpnConnectService.startVpn(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (App.localStorage.currentTheme == ThemeType.Blue) {
            setTheme(R.style.Theme_AppTheme_DayNightBlue)
            window.statusBarColor = getColor(R.color.safe_blue)
        }
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHost.navController

        navController.setGraph(R.navigation.main_graph, intent.extras)
        navController.addOnDestinationChangedListener(this)

        wc2MainViewModel.sessionProposalLiveEvent.observe(this) {
            if (!MainModule.isOpenDapp) {
                navController.slideFromBottom(R.id.wc2SessionFragment)
            }
        }
        wc2MainViewModel.openWalletConnectRequestLiveEvent.observe(this) { requestId ->
            navController.slideFromBottom(
                R.id.wc2RequestFragment,
                WC2RequestFragment.prepareParams(requestId)
            )
        }

        viewModel.navigateToMainLiveData.observe(this) {
            if (it) {
                navController.popBackStack(navController.graph.startDestinationId, false)
                viewModel.onNavigatedToMain()
            }
        }

        val filter = IntentFilter()
        filter.addAction(AppConfig.BROADCAST_ACTION_ACTIVITY)
        filter.addAction("com.anwang.safe.connect")
        filter.addAction("com.anwang.safe.reconnect")
        registerReceiver(mMsgReceiver, filter)

        startVpn()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mMsgReceiver)
        Utils.stopVService(this)
        VpnConnectService.startLoopCheckConnection = false
    }

    fun openSend(wallet: Wallet) {
        /*startActivity(Intent(this, SendActivity::class.java).apply {
        putExtra(SendActivity.WALLET, wallet)
    })*/
    }

    private fun startVpn() {
        if (!getSharedPreferences("vpnSetting", Context.MODE_PRIVATE).getBoolean(
                "vpnOpen",
                true
            )
        ) {
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

