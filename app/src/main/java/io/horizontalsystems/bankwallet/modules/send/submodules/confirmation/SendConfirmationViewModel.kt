package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.hodler.LockTimeInterval

class SendConfirmationViewModel(
    var confirmationViewItems: List<SendModule.SendConfirmationViewItem>?
) : ViewModel() {

    val viewDataLiveData = MutableLiveData<SendConfirmationModule.ViewData>()
    val sendButtonLiveData = MutableLiveData<SendConfirmationModule.SendButton>()

    init {
        var coinName = ""
        var coinAmount = ""
        var currencyAmount = ""
        var receiver = ""
        var feeAmount = ""
        var domain: String? = null
        var memo: String? = null
        var lockTimeInterval: LockTimeInterval? = null
        var wsafeHex: String? = null

        confirmationViewItems?.forEach { item ->
            when (item) {
                is SendModule.SendConfirmationAmountViewItem -> {
                    coinName = item.coinValue.coin.name
                    coinAmount = item.coinValue.getFormattedFull()
                    currencyAmount = item.currencyValue?.getFormattedFull() ?: ""
                    domain = item.receiver.domain
                    receiver = item.receiver.hex
                    wsafeHex = item.wsafeHex
                }
                is SendModule.SendConfirmationFeeViewItem -> {
                    feeAmount = item.coinValue.getFormattedFull()
                    item.currencyValue?.let {
                        feeAmount += " | ${it.getFormattedFull()}"
                    }
                }
                is SendModule.SendConfirmationMemoViewItem -> {
                    memo = item.memo
                }
                is SendModule.SendConfirmationLockTimeViewItem -> {
                    lockTimeInterval = item.lockTimeInterval
                }
            }
        }

        val viewData = SendConfirmationModule.ViewData(
            coinName = coinName,
            coinAmount = coinAmount,
            currencyAmount = currencyAmount,
            toAddress = receiver,
            domain = domain,
            memo = memo,
            lockTimeInterval = lockTimeInterval,
            feeAmount = feeAmount,
            wsafeHex = wsafeHex
        )

        viewDataLiveData.postValue(viewData)
        sendButtonLiveData.postValue(
            SendConfirmationModule.SendButton(R.string.Send_Confirmation_Send_Button, true)
        )
    }

    fun onSendError() {
        sendButtonLiveData.postValue(
            SendConfirmationModule.SendButton(R.string.Send_Confirmation_Send_Button, true)
        )
    }

    fun onSendClick() {
        sendButtonLiveData.postValue(
            SendConfirmationModule.SendButton(R.string.Send_Sending, false)
        )
    }
}
