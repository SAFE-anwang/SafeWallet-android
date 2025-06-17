package io.horizontalsystems.bankwallet.modules.safe4.linelocksafe

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionData
import org.apache.commons.lang3.StringUtils
import java.math.BigDecimal
import java.math.RoundingMode

class LineLockSendSafeViewModel(
    val wallet: Wallet,
    private val amountService: SendAmountService,
    private val addressService: SendEvmAddressService,
    private val ethereumKit: EthereumKit,
    val coinMaxAllowedDecimals: Int,
    private val xRateService: XRateService,
    private val rpcBlockchainSafe4: RpcBlockchainSafe4,
    private val connectivityManager: ConnectivityManager,
): ViewModelUiState<LineLockSafeModule.LineLockSafeUiState>() {

    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    var coinRate by mutableStateOf(xRateService.getRate(wallet.token.coin.uid))
        private set

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    private var totalAmount: BigDecimal? = null
    private var outputSize: Int = 0
    private var lockedValue: String? = null
    private var startMonth: String? = null
    private var intervalMonth: String? = null
    private var lockTips: String? = null

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            amountState = it
            emitState()
        }

        addressService.stateFlow.collectWith(viewModelScope) {
            addressState = it
            emitState()
        }
    }

    override fun createState(): LineLockSafeModule.LineLockSafeUiState {
        return LineLockSafeModule.LineLockSafeUiState(
            amountState.canBeSend && addressState.canBeSend && checkLineLock(),
                availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            fiatMaxAllowedDecimals,
            lockedValue,
            startMonth,
            intervalMonth,
            lockTips,
            addressState.addressError
        )
    }

    fun receiveAddress(): String {
        return ethereumKit.receiveAddress.hex
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
        amountState.amount?.let {
            setAmount(it)
        }
    }

    fun onEnterLockAmount(amount: String) {
        this.lockedValue = amount
    }

    fun onEnterLockMonth(startMonth: String) {
        this.startMonth = startMonth
    }

    fun onEnterLockInterval(intervalMonth: String) {
        this.intervalMonth = intervalMonth
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }


    fun setAmount(amount: BigDecimal) {
        if (amount == BigDecimal.ZERO)  return
        var lockedValue: String
        val startMonth: String
        val intervalMonth: String
//        val outputSize: Int
        if (amount < BigDecimal(1)) { // 小于1 SAFE
            outputSize = 1
            lockedValue = amount.toPlainString()
            startMonth = "1"
            intervalMonth = "1"
        } else if (amount >= BigDecimal(1) &&amount < BigDecimal(120)) { // 1-120 SAFE
            outputSize = (amount / BigDecimal(1)).toInt()
            lockedValue = "1"
            startMonth = "1"
            intervalMonth = "1"
        } else if (amount >= BigDecimal(120) && amount < BigDecimal(1000)) { // 120-1000 SAFE
            outputSize = (amount / BigDecimal(120)).toInt()
            lockedValue = (amount / BigDecimal(outputSize)).toLong().toString()
            startMonth = "1"
            intervalMonth = "1"
        } else {
            outputSize = (amount / BigDecimal(1000)).toInt()    // 大于等于1000 SAFE
            lockedValue = (amount / BigDecimal(outputSize)).toLong().toString()
            startMonth = "12"
            intervalMonth = "1"
        }
        try {
            lockedValue = checkAmount(amount, BigDecimal(lockedValue)).toString()
            totalAmount = BigDecimal(lockedValue) * BigDecimal(outputSize)
        } catch (e: Exception) {

        }
        Log.i("safe4", "totalAmount: $totalAmount")
        this.lockedValue = lockedValue
        this.startMonth = startMonth
        this.intervalMonth = intervalMonth
        emitState()
    }

    fun checkAmount(amount: BigDecimal, lockedValue: BigDecimal) : BigDecimal{
        val outputSize = (amount / lockedValue).toInt()
        val totalAmount = lockedValue * BigDecimal(outputSize)
        if (totalAmount > amount) {
            return (lockedValue.minus(BigDecimal((totalAmount % amount).toLong())))
        } else {
            return lockedValue
        }
    }

    fun checkLineLock(): Boolean {
        if (StringUtils.isBlank(lockedValue)) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Locked_Value_Error), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        val amount = amountState.amount
        if (BigDecimal(lockedValue) > amount) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Locked_Value_Error), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (StringUtils.isBlank(startMonth) || BigDecimal(startMonth) <= BigDecimal.ZERO) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Starting_Month_Error), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (StringUtils.isBlank(intervalMonth) || BigDecimal(intervalMonth) <= BigDecimal.ZERO) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Interval_Month_Error), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (BigDecimal(startMonth) > BigDecimal(120)) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Starting_Month_Max), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (BigDecimal(intervalMonth) > BigDecimal(120)) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Interval_Month_Max), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    fun onUpdateLineLock(
        lockedValue: String?,
        startMonth: String?,
        intervalMonth: String?
    ) {
        this.lockedValue = lockedValue
        this.startMonth = startMonth
        this.intervalMonth = intervalMonth

        try {
            if (StringUtils.isNotBlank(lockedValue)
                && StringUtils.isNotBlank(startMonth)
                && StringUtils.isNotBlank(intervalMonth)
                && BigDecimal(lockedValue) > BigDecimal.ZERO
                && BigDecimal(startMonth) > BigDecimal.ZERO
                && BigDecimal(intervalMonth) > BigDecimal.ZERO
                && amountState.amount != null
                && amountState.amount!! > BigDecimal.ZERO
            ) {
                var outputSize = amountState.amount!!.divide(BigDecimal(lockedValue),0, RoundingMode.FLOOR)
                outputSize = checkMaxInterval(outputSize, BigDecimal(startMonth!!), BigDecimal(intervalMonth!!))
                val totalAmount = BigDecimal(lockedValue) * outputSize
                val lineLockStr = Translator.getString(
                    R.string.Safe4_Line_Lock_Tips,
                    startMonth,
                    intervalMonth,
                    lockedValue!!,
                    totalAmount
                )
                lockTips = lineLockStr
            }
        } catch (e: Exception) {
        }
    }

    fun checkMaxInterval(outputSize: BigDecimal, startMonth: BigDecimal, intervalMonth: BigDecimal) : BigDecimal {
        var maxOutputSize = outputSize.toInt()
        val maxMonth = BigDecimal(120)
        for (index in 0 .. outputSize.toLong()) {
            maxOutputSize = index.toInt()
            val nextMonth = startMonth + intervalMonth * BigDecimal(index)
            if (nextMonth > maxMonth) {
                break
            }
        }
        return BigDecimal(maxOutputSize)
    }

    fun getSendData(): SendEvmData {
        return SendEvmData(
            TransactionData(
                io.horizontalsystems.ethereumkit.models.Address(addressState.address!!.hex),
                NodeCovertFactory.scaleConvert(totalAmount!!),
                byteArrayOf(),
                times = outputSize,
                spaceDay = intervalMonth!!.toInt() * 30,
                startDay = startMonth!!.toInt() * 30
            )
        )
    }
}