package io.horizontalsystems.bankwallet.modules.tg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import org.telegram.messenger.UserConfig
import org.telegram.ui.AnWangUtils
import org.telegram.ui.LaunchActivity

class StartTelegramsService(
    val activity: Activity
) {

    private var checkTelegramLoginState = false

    fun join(group: String) {
        val intent = Intent(activity, LaunchActivity::class.java)
        // 未登录过，是首次打开tg，需要直接进群
        if (UserConfig.getActivatedAccountsCount() <= 0 || AnWangUtils.isLeaveAnwangGroup) {
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(group)
        } else {
            if (AnWangUtils.lastOpenGroupId != "") {
                intent.action = Intent.ACTION_VIEW
                intent.data = Uri.parse(AnWangUtils.lastOpenGroupId)
            }
            AnWangUtils.isCheckInAnwangGroup = false
        }
        activity.startActivity(intent)
        startCheckLoginState(group)
    }

    private fun startCheckLoginState(group: String) {
        if (UserConfig.getActivatedAccountsCount() > 0) return
        checkTelegramLoginState = true
        GlobalScope.launch(Dispatchers.IO) {
            while (checkTelegramLoginState) {
                if (UserConfig.getActivatedAccountsCount() > 0) {
                    checkTelegramLoginState = false
                    withContext(Dispatchers.Main) {
                        join(group)
                    }
                    break
                }
                delay(2000)
            }
        }
    }

    fun stopCheckLoginStatus() {
        checkTelegramLoginState = false
    }
}