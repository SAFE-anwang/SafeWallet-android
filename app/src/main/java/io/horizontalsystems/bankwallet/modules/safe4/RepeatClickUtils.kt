package io.horizontalsystems.bankwallet.modules.safe4

object RepeatClickUtils {

    var lastClickTime: Long = 0

    val isRepeat: Boolean
        get() {
            val time = System.currentTimeMillis()
            val timeD = time - lastClickTime
            if (timeD < 800) {
                return true
            }
            lastClickTime = time
            return false
        }

}