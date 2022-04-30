package io.horizontalsystems.bankwallet.modules.send

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.Toast

import androidx.lifecycle.ViewModel
import com.anwang.safewallet.safekit.model.SafeInfo
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
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
        view.showConfirmation(handler.confirmationViewItems())
    }

    fun onSafeConvertClicked(context: Context){
        // 处理safe跨链逻辑
        handlerSafeConvert(context)
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

    private fun handlerSafeConvert(context: Context) {
        App.safeProvider.getSafeInfo()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it == null || !it.eth.eth2safe) {
                    Toast.makeText(App.instance, "跨链转账业务暂停使用，请稍后再试", Toast.LENGTH_SHORT).show()
                } else {
                    validMinAmount(it, context)
                }
            }, {
                Log.e("safe4", "getSafeInfo error", it)
            })
            .let {
                disposables.add(it)
            }
    }

    private fun validMinAmount(safeInfo: SafeInfo, context: Context) {
        val minSafe = BigDecimal(safeInfo.minamount)
        if (handler.amountModule.coinAmount.value < minSafe) {
            Toast.makeText(App.instance, "跨链转账最小金额是${safeInfo.minamount} SAFE", Toast.LENGTH_SHORT).show()
        } else {
            showAlertDialog(safeInfo, context)
        }
    }

    private fun showAlertDialog(safeInfo: SafeInfo, context: Context){
        val fee = handler.amountModule.coinAmount.value * BigDecimal(safeInfo.eth.safe_fee)
        val builder = AlertDialog.Builder(context)
        builder.setTitle("温馨提示")
        builder.setMessage("跨链费用为: ${fee} SAFE，该费用仅为参考，以实际扣除为准，确认兑换？")
        builder.setPositiveButton("确认"){ _, _ ->
            view.showConfirmation(handler.confirmationViewItems())
        }
        builder.setNegativeButton("取消"){ _, _ ->

        }
        val dialog:AlertDialog = builder.create()
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

}
