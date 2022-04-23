package io.horizontalsystems.bankwallet.modules.tg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import org.telegram.messenger.UserConfig
import org.telegram.ui.LaunchActivity

class StartTelegramsService(
    val activity: Activity
) {

    private var checkTelegramLoginState = false

    fun join(group: String) {
        val intent = Intent(activity, LaunchActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse(group)
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