package io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.horizontalsystems.bitcoincore.utils.HashUtils
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.wsafekit.WSafeManager
import io.reactivex.Single
import java.math.BigDecimal

class SendSafeConvertHandler(
    private val interactor: SendModule.ISendSafeInteractor,
    private val ethAdapter: ISendEthereumAdapter
) : SendModule.ISendHandler, SendModule.ISendSafeInteractorDelegate,
    SendAmountModule.IAmountModuleDelegate,
    SendAddressModule.IAddressModuleDelegate, SendFeeModule.IFeeModuleDelegate,
    SendHodlerModule.IHodlerModuleDelegate {

    private val safeConvertAddress =
        WSafeManager(ethAdapter.evmKitWrapper.evmKit.chain).getSafeConvertAddress()

    private fun syncValidation() {
        var amountError: Throwable? = null
        var addressError: Throwable? = null

        try {
            amountModule?.validAmount()
        } catch (e: Exception) {
            amountError = e
        }

        try {
            addressModule?.validateAddress()
        } catch (e: Exception) {
            addressError = e
        }

        delegate.onChange(
            amountError == null && addressError == null && feeModule?.isValid == true,
            amountError,
            addressError
        )
    }

    private fun syncAvailableBalance() {
        interactor.fetchAvailableBalance(safeConvertAddress)
    }

    private fun syncFee() {
        interactor.fetchFee(amountModule!!.coinAmount.value, safeConvertAddress)
    }

    private fun syncMinimumAmount() {
        interactor.fetchMinimumAmount(safeConvertAddress)?.let { amountModule?.setMinimumAmount(it) }
        syncValidation()
    }

    // SendModule.ISendHandler

    override var amountModule: SendAmountModule.IAmountModule? = null
    override var addressModule: SendAddressModule.IAddressModule? = null
    override var feeModule: SendFeeModule.IFeeModule? = null
    override var memoModule: SendMemoModule.IMemoModule? = null
    override var hodlerModule: SendHodlerModule.IHodlerModule? = null

    override lateinit var delegate: SendModule.ISendHandlerDelegate
    override fun sync() {}

    override val inputItems: List<SendModule.Input> =
        mutableListOf<SendModule.Input>().apply {
            add(SendModule.Input.Amount)
            add(SendModule.Input.Address())
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
        return mutableListOf<SendModule.SendConfirmationViewItem>().apply {
            add(
                SendModule.SendConfirmationAmountViewItem(
                    amountModule!!.coinValue(),
                    amountModule!!.currencyValue(),
                    Address(safeConvertAddress),
                    lockTimeInterval != null,
                    addressModule!!.validAddress().hex
                )
            )

            add(
                SendModule.SendConfirmationFeeViewItem(
                    feeModule!!.coinValue,
                    feeModule!!.currencyValue
                )
            )

            lockTimeInterval?.let {
                add(SendModule.SendConfirmationLockTimeViewItem(it))
            }
        }
    }

    override fun sendSingle(logger: AppLogger): Single<Unit> {
        return interactor.send(
            amountModule!!.validAmount(),
            safeConvertAddress,
            logger,
            null,
            getReverseHex()
        )
    }

    fun getReverseHex(): String {
        val wsafeAddress: String
        val safeRemarkPrex = "736166650100c9dcee22bb18bd289bca86e2c8bbb6487089adc9a13d875e538dd35c70a6bea42c0100000a020100122e"
        if (ethAdapter.evmKitWrapper.evmKit.chain == Chain.BinanceSmartChain) {
            wsafeAddress = "bsc:" + addressModule?.validAddress()?.hex
        } else if(ethAdapter.evmKitWrapper.evmKit.chain == Chain.Polygon) {
            wsafeAddress = "matic:" + addressModule?.validAddress()?.hex
        }else {
            wsafeAddress = "eth:" + addressModule?.validAddress()?.hex
        }
        val wsafeHex = HashUtils.toHexString(wsafeAddress.toByteArray())
        return safeRemarkPrex + wsafeHex
    }


    // SendModule.ISendBitcoinInteractorDelegate

    override fun didFetchAvailableBalance(availableBalance: BigDecimal) {
        amountModule?.setAvailableBalance(availableBalance)
        syncValidation()
    }

    override fun didFetchFee(fee: BigDecimal) {
        val safeInfoPO = SafeInfoManager.getSafeInfo()
        var newFee = fee + BigDecimal(safeInfoPO.eth.safe_fee)
        if (ethAdapter.evmKitWrapper.evmKit.chain == Chain.BinanceSmartChain) {
             newFee = fee + BigDecimal(safeInfoPO.bsc.safe_fee)
        } else if (ethAdapter.evmKitWrapper.evmKit.chain == Chain.Polygon) {
            newFee = fee + BigDecimal(safeInfoPO.matic.safe_fee)
        }
        feeModule?.setFee(newFee)
    }

    // SendAmountModule.ModuleDelegate

    override fun onChangeAmount() {
        syncFee()
        syncValidation()
    }

    override fun onChangeInputType(inputType: AmountInputType) {
        feeModule?.setInputType(inputType)
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
        amountModule?.setAmount(amount)
    }

    // SendFeeModule.IFeeModuleDelegate

    override fun onUpdateFeeRate() {
    }

    override fun onUpdateLockTimeInterval(timeInterval: LockTimeInterval?) {
        syncAvailableBalance()
        syncFee()
        syncMinimumAmount()
    }

    override fun onUpdateLineLock(
        lockedValue: String?,
        startMonth: String?,
        intervalMonth: String?
    ) {

    }

}
