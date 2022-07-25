package io.horizontalsystems.bankwallet.modules.safe4.linelock

import android.widget.Toast
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.horizontalsystems.bitcoincore.utils.JsonUtils
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.Single
import org.apache.commons.lang3.StringUtils
import java.math.BigDecimal
import java.math.RoundingMode

class LineLockSendHandler(
    private val interactor: SendModule.ISendSafeInteractor
) : SendModule.ISendHandler, SendModule.ISendSafeInteractorDelegate,
    SendAmountModule.IAmountModuleDelegate,
    SendAddressModule.IAddressModuleDelegate, SendFeeModule.IFeeModuleDelegate,
    SendHodlerModule.IHodlerModuleDelegate {

    private fun syncValidation() {
        var amountError: Throwable? = null
        var addressError: Throwable? = null

        try {
            val value = amountModule.validAmount()
            hodlerModule?.setAmount(value)
        } catch (e: Exception) {
            amountError = e
        }

        try {
            addressModule.validateAddress()
        } catch (e: Exception) {
            addressError = e
        }

        delegate.onChange(
            amountError == null && addressError == null && feeModule.isValid,
            amountError,
            addressError
        )
    }

    private fun syncAvailableBalance() {
        interactor.fetchAvailableBalance(addressModule.currentAddress?.hex)
    }

    private fun syncFee() {
        interactor.fetchFee(amountModule.coinAmount.value, addressModule.currentAddress?.hex)
    }

    private fun syncMinimumAmount() {
        amountModule.setMinimumAmount(interactor.fetchMinimumAmount(addressModule.currentAddress?.hex))
        syncValidation()
    }

    // SendModule.ISendHandler

    override lateinit var amountModule: SendAmountModule.IAmountModule
    override lateinit var addressModule: SendAddressModule.IAddressModule
    override lateinit var feeModule: SendFeeModule.IFeeModule
    override lateinit var memoModule: SendMemoModule.IMemoModule
    override var hodlerModule: SendHodlerModule.IHodlerModule? = null
    private var lockedValue: String? = null
    private var startMonth: String? = null
    private var intervalMonth: String? = null

    override lateinit var delegate: SendModule.ISendHandlerDelegate
    override fun sync() {}

    override val inputItems: List<SendModule.Input> =
        mutableListOf<SendModule.Input>().apply {
            add(SendModule.Input.Amount)
            add(SendModule.Input.Address())
            add(SendModule.Input.Hodler)
            add(SendModule.Input.Fee)
            add(SendModule.Input.ProceedButton)
        }

    override fun onModulesDidLoad() {
        syncAvailableBalance()
        syncMinimumAmount()
    }

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        val hodlerData = hodlerModule?.pluginData()?.get(HodlerPlugin.id) as? HodlerData
        val lockTimeInterval = hodlerData?.lockTimeInterval
        val outputSize = amountModule.validAmount().divide(BigDecimal(lockedValue),0, RoundingMode.FLOOR)
        val totalAmount = BigDecimal(lockedValue) * outputSize
        return mutableListOf<SendModule.SendConfirmationViewItem>().apply {
            add(
                SendModule.SendConfirmationAmountViewItem(
                    amountModule.coinValue(),
                    amountModule.getLockedAmount(totalAmount),
                    addressModule.validAddress(),
                    lockTimeInterval != null
                )
            )

            add(
                SendModule.SendConfirmationFeeViewItem(
                    feeModule.coinValue,
                    feeModule.currencyValue
                )
            )

            lockTimeInterval?.let {
                add(SendModule.SendConfirmationLockTimeViewItem(it))
            }
        }
    }

    override fun sendSingle(logger: AppLogger): Single<Unit> {
        val outputSize = amountModule.validAmount().divide(BigDecimal(lockedValue),0, RoundingMode.FLOOR)
        val totalAmount = BigDecimal(lockedValue) * outputSize
        val reverseHex = JsonUtils.objToString(
            JsonUtils.LineLock(
                0,
                lockedValue.toString(),
                startMonth!!.toInt(),
                intervalMonth!!.toInt(),
                outputSize.toInt()
            )
        )
        return interactor.send(
            totalAmount,
            addressModule.validAddress().hex,
            logger,
            null,
            reverseHex
        )
    }

    // SendModule.ISendBitcoinInteractorDelegate

    override fun didFetchAvailableBalance(availableBalance: BigDecimal) {
        amountModule.setAvailableBalance(availableBalance)
        syncValidation()
    }

    override fun didFetchFee(fee: BigDecimal) {
        feeModule.setFee(fee)
    }

    // SendAmountModule.ModuleDelegate

    override fun onChangeAmount() {
        syncFee()
        syncValidation()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    // SendAddressModule.ModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        syncAvailableBalance()
        syncFee()
        syncMinimumAmount()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    // SendFeeModule.IFeeModuleDelegate

    override fun onUpdateFeeRate() {
    }

    override fun onUpdateLockTimeInterval(timeInterval: LockTimeInterval?) {
        syncAvailableBalance()
        syncFee()
        syncMinimumAmount()
    }

    fun checkLineLock(): Boolean {
        if (StringUtils.isBlank(lockedValue)) {
            Toast.makeText(App.instance, R.string.Safe4_Locked_Value_Error, Toast.LENGTH_SHORT)
                .show()
            return false
        }
        val amount = amountModule.validAmount()
        val outputSize = amount.divide(BigDecimal(lockedValue),0, RoundingMode.FLOOR)
        val totalAmount = BigDecimal(lockedValue) * outputSize
        if (totalAmount > amount) {
            Toast.makeText(App.instance, R.string.Safe4_Locked_Value_Error, Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (StringUtils.isBlank(startMonth) || BigDecimal(startMonth) <= BigDecimal.ZERO) {
            Toast.makeText(App.instance, R.string.Safe4_Starting_Month_Error, Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (StringUtils.isBlank(intervalMonth) || BigDecimal(intervalMonth) <= BigDecimal.ZERO) {
            Toast.makeText(App.instance, R.string.Safe4_Interval_Month_Error, Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (outputSize > BigDecimal(120)) {
            Toast.makeText(App.instance, R.string.Safe4_Locked_Month_Error, Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    override fun onUpdateLineLock(
        lockedValue: String?,
        startMonth: String?,
        intervalMonth: String?
    ) {
        this.lockedValue = lockedValue
        this.startMonth = startMonth
        this.intervalMonth = intervalMonth

        if (StringUtils.isNotBlank(lockedValue)
            && StringUtils.isNotBlank(startMonth)
            && StringUtils.isNotBlank(intervalMonth)
            && BigDecimal(lockedValue) > BigDecimal.ZERO
            && BigDecimal(startMonth) > BigDecimal.ZERO
            && BigDecimal(intervalMonth) > BigDecimal.ZERO
            && amountModule.currentAmount > BigDecimal.ZERO
        ) {
            val outputSize = amountModule.validAmount().divide(BigDecimal(lockedValue),0, RoundingMode.FLOOR)
            val totalAmount = BigDecimal(lockedValue) * outputSize
            val lineLockStr = Translator.getString(
                R.string.Safe4_Line_Lock_Tips,
                startMonth!!,
                intervalMonth!!,
                lockedValue!!,
                totalAmount
            )
            this.feeModule.setLineLockTips(lineLockStr)
        }
    }

}
