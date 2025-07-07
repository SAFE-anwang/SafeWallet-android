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
import java.text.SimpleDateFormat

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

    private var lockedValue: BigDecimal? = null
    private var startMonth: String? = null
    private var intervalMonth: String? = null
    private var lockTimes: Int? = null
    private var lockTips: String? = null

    private val format = SimpleDateFormat("yyyy-MM-dd")
    private var startLockTime: Long = System.currentTimeMillis()

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            amountState = it
            onUpdateLineLock()
            emitState()
        }

        addressService.stateFlow.collectWith(viewModelScope) {
            addressState = it
            emitState()
        }
    }

    override fun createState(): LineLockSafeModule.LineLockSafeUiState {
        return LineLockSafeModule.LineLockSafeUiState(
            amountState.canBeSend && addressState.canBeSend && amountState.amount!! >= BigDecimal.ONE
                    && lockTimes != null && lockTimes!! >= 1
                    && intervalMonth != null && intervalMonth!! >= "1"
                    && startLockTime >= 0 ,
                availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            fiatMaxAllowedDecimals,
            lockTimes?.toString(),
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
        lockedValue = amount
        checkBalance()
    }

    private fun checkBalance() {
        val temp = if (lockTimes != null && lockedValue != null) {
            lockedValue!!.multiply(lockTimes!!.toBigDecimal())
        } else {
            lockedValue
        }
        amountService.setAmount(temp)
    }

    fun onEnterLockTimes(startMonth: String) {
        try {
            this.lockTimes = startMonth.toInt()

            checkBalance()
        } catch (e: Exception) {
            this.lockTimes =  null
        }
        onUpdateLineLock()
        emitState()
    }

    fun onEnterLockInterval(intervalMonth: String) {
        this.intervalMonth = intervalMonth
        onUpdateLineLock()
        emitState()
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
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

    fun onUpdateLineLock() {
        try {
            if (StringUtils.isNotBlank(lockedValue?.toString())
                && StringUtils.isNotBlank(lockTimes?.toString())
                && StringUtils.isNotBlank(intervalMonth)
                && lockedValue!! > BigDecimal.ZERO
                && BigDecimal(lockTimes?.toString()) > BigDecimal.ZERO
                && BigDecimal(intervalMonth) > BigDecimal.ZERO
                && amountState.amount != null
                && amountState.amount!! > BigDecimal.ONE
            ) {

                val totalAmount = amountState.amount!!
                val lineLockStr = Translator.getString(
                    R.string.Safe4_Line_Lock_Tips,
                    format.format(startLockTime),
                    intervalMonth!!,
                    lockedValue!!,
                    lockTimes.toString(),
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

    fun onEnterStartTime(time: Long) {
        this.startLockTime = time
        emitState()
    }

    fun getSendData(): SendEvmData {
        return SendEvmData(
            TransactionData(
                io.horizontalsystems.ethereumkit.models.Address(addressState.address!!.hex),
                NodeCovertFactory.scaleConvert(amountState.amount!!),
                byteArrayOf(),
                times = lockTimes!!,
                spaceDay = intervalMonth!!.toInt() * 30,
                startDay = ((startLockTime - System.currentTimeMillis())).toInt() / (1000 * 24 * 60 * 60)
            )
        )
    }
}