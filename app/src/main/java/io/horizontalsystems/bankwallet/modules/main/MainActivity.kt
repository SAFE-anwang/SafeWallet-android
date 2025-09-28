package io.horizontalsystems.bankwallet.modules.main

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.Utils
import com.walletconnect.web3.wallet.client.Wallet
import com.xuexiang.xupdate.XUpdate
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_NEW_VERSION
import com.xuexiang.xupdate.listener.IUpdateParseCallback
import com.xuexiang.xupdate.proxy.IUpdateParser
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.UpgradeVersion
import io.horizontalsystems.bankwallet.modules.intro.IntroActivity
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.bankwallet.modules.safe4.Safe4Module
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordManager
import io.horizontalsystems.bankwallet.modules.safe4.src20.SyncSafe4Tokens
import io.horizontalsystems.bankwallet.modules.safe4.src20.SyncSafe4TokensService
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.net.SafeNetWork
import io.horizontalsystems.bankwallet.net.VpnConnectService

import io.horizontalsystems.bankwallet.modules.walletconnect.AuthEvent
import io.horizontalsystems.bankwallet.modules.walletconnect.SignEvent
import io.horizontalsystems.bankwallet.modules.walletconnect.WCViewModel
import io.horizontalsystems.core.hideKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory()
    }

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                VpnConnectService.startVpn(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (App.localStorage.currentTheme == ThemeType.Blue) {
            setTheme(R.style.Theme_AppTheme_DayNightBlue)
            window.statusBarColor = getColor(R.color.safe_blue)
        }
        setContentView(R.layout.activity_main)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHost.navController

        navController.setGraph(R.navigation.main_graph, intent.extras)
        navController.addOnDestinationChangedListener { _, _, _ ->
            currentFocus?.hideKeyboard(this)
        }

        viewModel.navigateToMainLiveData.observe(this) {
            if (it) {
                navController.popBackStack(navController.graph.startDestinationId, false)
                viewModel.onNavigatedToMain()
            }
        }

        viewModel.wcEvent.observe(this) { wcEvent ->
            if (wcEvent != null) {
                when (wcEvent) {
                    is Wallet.Model.SessionRequest -> {
                        navController.slideFromBottom(R.id.wcRequestFragment)
                    }
                    is Wallet.Model.SessionProposal -> {
                        if (!MainModule.isOpenDapp) {
                            navController.slideFromBottom(R.id.wcSessionFragment)
                        }
                    }
                    else -> {}
                }

                viewModel.onWcEventHandled()
            }
        }

        val filter = IntentFilter()
        filter.addAction(AppConfig.BROADCAST_ACTION_ACTIVITY)
        filter.addAction("com.anwang.safe.connect")
        filter.addAction("com.anwang.safe.reconnect")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mMsgReceiver, filter, RECEIVER_NOT_EXPORTED)
        }else {
            registerReceiver(mMsgReceiver, filter)
        }

        startVpn()
        startUpgradeVersion()
        App.accountManager.activeAccount
        Thread(
            Runnable {
                Thread.sleep(2000)
                SyncSafe4Tokens.getTokens()
            }
        ).start()
        /*Handler(mainLooper).postDelayed(
            Runnable {
                SyncSafe4Tokens.getTokens()
            },
            40000
        )*/
        viewModel.viewModelScope.launch {
            LockRecordManager.newProposalRecordState.collectLatest {
                if (it) {
                    withContext(Dispatchers.Main) {
                        val dialog = AlertDialog.Builder(this@MainActivity)
                            .setTitle("提案")
                            .setMessage("有新的提案，是否查看?")
                            .setPositiveButton("查看"
                            ) { p0, p1 ->
                                LockRecordManager.updateProposalStatus()
                                p0.dismiss()
                                Safe4Module.handlerNode(Safe4Module.SafeFourType.Proposal, navController)
                            }
                            .setNegativeButton("不在提醒") { p0, p1 ->
                                LockRecordManager.updateProposalStatus()
                                p0.dismiss()
                            }.create()
                        if (App.localStorage.currentTheme == ThemeType.Blue) {
                            dialog.setOnShowListener {
                                // 获取按钮并修改颜色
                                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                                positiveButton.setTextColor(Color.BLACK)
                                negativeButton.setTextColor(Color.GRAY)
                            }
                        }
                        dialog.show()
                    }
                }

            }
        }

    }


    override fun onResume() {
        super.onResume()
        validate()
    }

    private fun validate() = try {
        viewModel.validate()
    } catch (e: MainScreenValidationError.NoSystemLock) {
        KeyStoreActivity.startForNoSystemLock(this)
        finish()
    } catch (e: MainScreenValidationError.KeyInvalidated) {
        KeyStoreActivity.startForInvalidKey(this)
        finish()
    } catch (e: MainScreenValidationError.UserAuthentication) {
        KeyStoreActivity.startForUserAuthentication(this)
        finish()
    } catch (e: MainScreenValidationError.Welcome) {
        IntroActivity.start(this)
        finish()
    } catch (e: MainScreenValidationError.Unlock) {
        LockScreenActivity.start(this)
    }

    private fun handleWeb3WalletEvents(
        navController: NavController,
        wcViewModel: WCViewModel,
    ) {
        wcViewModel.walletEvents
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { event ->
                when (event) {
                    is SignEvent.SessionProposal -> {
                        if (!MainModule.isOpenDapp) {
                            navController.slideFromBottom(R.id.wcSessionFragment)
                        }
                    }
                    is SignEvent.SessionRequest -> {
                        navController.slideFromBottom(R.id.wcRequestFragment,)
                    }

                    is SignEvent.Disconnect -> {
                    }

                    is AuthEvent.OnRequest -> {
                    }

                    else -> Unit
                }
            }
            .launchIn(lifecycleScope)
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mMsgReceiver)
        Utils.stopVService(this)
        VpnConnectService.startLoopCheckConnection = false
        LockRecordManager.exit()
    }

    private fun startUpgradeVersion() {
        XUpdate.get()
                .isWifiOnly(false) // By default, only version updates are checked under WiFi
                .isGet(true) // The default setting uses Get request to check versions
                .isAutoMode(false) // The default setting is non automatic mode
                .setOnUpdateFailureListener { error ->
                    // Set listening for version update errors
                    if (error.code != CHECK_NO_NEW_VERSION) {          // Handling different errors

                    }
                }
                .supportSilentInstall(true) // Set whether silent installation is supported. The default is true
                .setIUpdateHttpService(OKHttpUpdateHttpService()) // This must be set! Realize the network request function.
                .init(this.application)
       val build =  XUpdate.newBuild(this)
                .updateUrl("https://safewallet.anwang.com/v1/getLatestApp")
                .updateParser(object : IUpdateParser {
                    override fun parseJson(json: String?): UpdateEntity? {
                        Log.e("VersionUpdate", "json=$json")
                        val gson = Gson()
                        val result = gson.fromJson<UpgradeVersion>(json, UpgradeVersion::class.java)
                        if (result != null) {
                            return UpdateEntity()
                                    .setHasUpdate(getVersionCode() < result.versionCode)
                                    .setIsIgnorable(true)
                                    .setVersionCode(result.versionCode)
                                    .setVersionName(result.version)
                                    .setUpdateContent(result.upgradeMsg)
                                    .setDownloadUrl(result.url)
                        }
                        return null
                    }

                    override fun parseJson(json: String?, callback: IUpdateParseCallback?) {

                    }

                    override fun isAsyncParser(): Boolean {
                        return false
                    }
                })
        if (App.localStorage.currentTheme == ThemeType.Blue) {
            build.promptButtonTextColor(Color.BLACK)
        }
        build.update()
    }

    private fun getVersionCode(): Int {
        try {
            val packageManager = packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            return packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {

        }
        return 0
    }
}

