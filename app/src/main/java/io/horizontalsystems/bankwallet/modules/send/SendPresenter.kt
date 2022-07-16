package io.horizontalsystems.bankwallet.modules.send

import android.widget.Toast
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.CustomPriorityUnit
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import java.math.BigDecimal

class SendPresenter(
        private val interactor: SendModule.ISendInteractor,
        val router: SendModule.IRouter)
    : ViewModel(), SendModule.IViewDelegate, SendModule.ISendInteractorDelegate, SendModule.ISendHandlerDelegate {

    var amountModuleDelegate: SendAmountModule.IAmountModuleDelegate? = null
    var addressModuleDelegate: SendAddressModule.IAddressModuleDelegate? = null
    var feeModuleDelegate: SendFeeModule.IFeeModuleDelegate? = null
    var hodlerModuleDelegate: SendHodlerModule.IHodlerModuleDelegate? = null
    var customPriorityUnit: CustomPriorityUnit? = null

    override lateinit var view: SendModule.IView
    override lateinit var handler: SendModule.ISendHandler


    // SendModule.IViewDelegate

    override fun onViewDidLoad() {
        view.loadInputItems(handler.inputItems)

    }

    override fun onModulesDidLoad() {
        handler.onModulesDidLoad()
    }

    override fun onProceedClicked() {
        view.showConfirmation(handler.confirmationViewItems())
    }

    override fun onSendConfirmed(logger: AppLogger) {
        interactor.send(handler.sendSingle(logger), logger)
    }

    override fun onClear() {
        interactor.clear()

    }

    // ViewModel

    override fun onCleared() {
        interactor.clear()
        handler.onClear()
        SafeInfoManager.clear()
    }

    // SendModule.ISendInteractorDelegate

    override fun sync() {
        handler.sync()
    }

    override fun didSend() {
        router.closeWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        view.showErrorInToast(error)
    }

    // SendModule.ISendHandlerDelegate

    override fun onChange(isValid: Boolean, amountError: Throwable?, addressError: Throwable?) {
        val actionState: ActionState

        if (isValid) {
            actionState = ActionState.Enabled()
        } else if (amountError != null && !isEmptyAmountError(amountError)) {
            actionState = ActionState.Disabled("Invalid Amount")
        } else if (addressError != null && !isEmptyAddressError(addressError)) {
            actionState = ActionState.Disabled("Invalid Address")
        } else {
            actionState = ActionState.Disabled(null)
        }

        view.setSendButtonEnabled(actionState)
    }

    private fun isEmptyAmountError(error: Throwable): Boolean {
        return error is SendAmountModule.ValidationError.EmptyValue
    }

    private fun isEmptyAddressError(error: Throwable): Boolean {
        return error is AddressValidationException.Blank
    }

    sealed class ActionState {
        class Enabled : ActionState()
        class Disabled(val title: String?) : ActionState()
    }

    fun validMinAmount() {
        val safeInfoPO = SafeInfoManager.getSafeInfo()
        if (!safeInfoPO.eth.safe2eth) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Disabled), Toast.LENGTH_SHORT).show()
            return
        }
        val minSafe = BigDecimal(safeInfoPO.minamount)
//        if (handler.amountModule.coinAmount.value < minSafe) {
//            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Min_Fee, safeInfoPO.minamount), Toast.LENGTH_SHORT).show()
//            return
//        } else {
           onProceedClicked()
//        }
    }

}
