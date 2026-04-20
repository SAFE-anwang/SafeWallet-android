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
        var uri = ""
        // 未登录过，是首次打开tg，需要直接进群
        if (UserConfig.getActivatedAccountsCount() <= 0 || AnWangUtils.isLeaveAnwangGroup) {
            uri = group
        } else {
            if (AnWangUtils.lastOpenGroupId != "") {
                uri = AnWangUtils.lastOpenGroupId
            }
            AnWangUtils.isCheckInAnwangGroup = false
        }
        startTelegram(uri)
        startCheckLoginState(group)
    }

    private fun startTelegram(uri: String) {
        val intent = Intent(activity, LaunchActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse(uri)
        activity.startActivity(intent)
    }

    private fun startCheckLoginState(group: String) {
        if (UserConfig.getActivatedAccountsCount() > 0) return
        checkTelegramLoginState = true
        GlobalScope.launch(Dispatchers.IO) {
            while (checkTelegramLoginState) {
                if (UserConfig.getActivatedAccountsCount() > 0) {
                    checkTelegramLoginState = false
                    withContext(Dispatchers.Main) {
                        startTelegram(group)
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