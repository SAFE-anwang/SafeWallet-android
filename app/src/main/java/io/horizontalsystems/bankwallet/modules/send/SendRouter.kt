package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData

class SendRouter : SendModule.IRouter {

    val closeWithSuccess = MutableLiveData<Boolean?>(false)

    override fun closeWithSuccess() {
        closeWithSuccess.value = true
    }
}
