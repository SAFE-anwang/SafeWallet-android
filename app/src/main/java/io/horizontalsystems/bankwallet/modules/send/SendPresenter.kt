package io.horizontalsystems.bankwallet.modules.send

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.anwang.safewallet.safekit.model.SafeNet
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.SendSafeConvertHandler
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.CustomPriorityUnit
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
        if (handler is SendSafeConvertHandler) {
            // 处理safe跨链逻辑
            handlerSafeConvert()
            return
        }
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
        disposables.clear()
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

    private val disposables = CompositeDisposable()

    private fun handlerSafeConvert() {
        App.safeProvider.getSafeNet()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.i("safe4", "getSafeNet data: $it")
                if (it == null || !it.eth.eth2safe) {
                    Toast.makeText(App.instance, "跨链转账业务暂停使用，请稍后再试", Toast.LENGTH_SHORT).show()
                } else {
                    validMinAmount(it)
                }
            }, {
                Log.e("safe4", "getSafeNet error", it)
            })
            .let {
                disposables.add(it)
            }
    }

    private fun validMinAmount(safeNet: SafeNet) {
        val minSafe = BigDecimal(safeNet.minamount)
        if (handler.amountModule.coinAmount.value < minSafe) {
            Toast.makeText(App.instance, "跨链转账最小金额是${safeNet.minamount}SAFE", Toast.LENGTH_SHORT).show()
        } else {
            view.showConfirmation(handler.confirmationViewItems())
        }
    }

}
