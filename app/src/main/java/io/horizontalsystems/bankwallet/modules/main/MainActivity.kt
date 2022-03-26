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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendActivity
import io.horizontalsystems.bankwallet.net.SafeNetWork
import io.horizontalsystems.bankwallet.net.VpnConnectService

class MainActivity : BaseActivity() {

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            VpnConnectService.startVpn(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null) // null prevents fragments restoration on theme switch

        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navHost.navController.setGraph(R.navigation.main_graph, intent.extras)
        navHost.navController.addOnDestinationChangedListener(this)

        registerReceiver(mMsgReceiver, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY))

        startVpn()
    }

    override fun onTrimMemory(level: Int) {
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
                if (App.backgroundManager.inBackground) {
                    val logger = AppLogger("low memory")
                    logger.info("Kill activity due to low memory, level: $level")
                    finishAffinity()
                }
            }
            else -> {  /*do nothing*/
            }
        }

        super.onTrimMemory(level)
    }

    fun openSend(wallet: Wallet) {
        startActivity(Intent(this, SendActivity::class.java).apply {
            putExtra(SendActivity.WALLET, wallet)
        })
    }

    override fun onDestroy() {
        unregisterReceiver(mMsgReceiver)
        super.onDestroy()
    }

    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            VpnConnectService.startVpn(this)
        } else {
            requestVpnPermission.launch(intent)
        }
    }

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING -> {
                    Log.e("VpnConnectService", "connect running")
                }
                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    Log.e("VpnConnectService", "connect not running")
                }
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    Log.e("VpnConnectService", "connect success")
                    // 测试是否能范围外网
                    VpnConnectService.testVpnConnect(this@MainActivity)
                    // 检查chain.anwang.com 是否可连接
                    SafeNetWork.testAnWangConnect()
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    Log.e("VpnConnectService", "connect failure")
                }
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    Log.e("VpnConnectService", "stop success")
                }
            }
        }
    }
}
