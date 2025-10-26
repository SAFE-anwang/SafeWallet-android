package io.horizontalsystems.bankwallet.modules.send.evm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.google.android.exoplayer2.util.Log
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.safe4.src20.approve.ApproveState
import io.horizontalsystems.bankwallet.modules.safe4.src20.approve.SRC20ApproveManager
import io.horizontalsystems.bankwallet.modules.send.SendUiState
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinPluginService
import io.horizontalsystems.bankwallet.modules.swap.scaleUp
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.SafeExtend.isSafeFourCustomCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

class SendEvmViewModel(
    val wallet: Wallet,
    val sendToken: Token,
    val adapter: ISendEthereumAdapter,
    private val xRateService: XRateService,
    private val amountService: SendAmountService,
    private val addressService: SendEvmAddressService,
    val coinMaxAllowedDecimals: Int,
    private val showAddressInput: Boolean,
    private val connectivityManager: ConnectivityManager,
    private val pluginService: SendBitcoinPluginService
) : ViewModelUiState<SendUiState>() {
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set

    val isLockTimeEnabled by pluginService::isLockTimeEnabled
    val lockTimeIntervals by pluginService::lockTimeIntervals
    private var pluginState = pluginService.stateFlow.value
    private var approveState: ApproveState? = null

    val src20TradeManager by lazy {
        SRC20ApproveManager(
            adapter.evmKitWrapper.evmKit,
            adapter.evmKitWrapper.signer?.privateKey?.toString(16),
            adapter.evmKitWrapper.evmKit.receiveAddress.hex,
            (sendToken.tokenQuery.tokenType as TokenType.Eip20).address,
        )
    }

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        xRateService.getRateFlow(sendToken.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }

        pluginService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedPluginState(it)
        }
        if (isSetLogoCoin()) {
            src20TradeManager.stateFlow.collectWith(viewModelScope) {
                approveState = it
                Log.d("scr20approve", "$approveState")
                emitState()
            }
        }
    }

    override fun createState() = SendUiState(
        availableBalance = amountState.availableBalance,
        amountCaution = amountState.amountCaution,
        addressError = addressState.addressError,
        canBeSend = amountState.canBeSend && addressState.canBeSend && src20IsApproved(),
        showAddressInput = showAddressInput,
        lockTimeInterval = pluginState.lockTimeInterval,
        lockAmountError = getLockAmountError(),
        approveState = approveState
    )

    private fun src20IsApproved(): Boolean{
        if (!isSetLogoCoin())   return true
        return if (pluginState.lockTimeInterval != null) {
            !(approveState?.needApprove ?: true)
        } else {
            true
        }
    }

    private fun checkApproveState() {
        // SRC20 校验是否需要批准
        if (pluginState.lockTimeInterval == null)   return
        if (isSetLogoCoin()) {
            val amount = amountState.amount?.scaleUp(sendToken.decimals) ?: BigInteger.ZERO
            if (amount == BigInteger.ZERO)  return
            viewModelScope.launch(Dispatchers.IO) {
                src20TradeManager.checkNeedApprove(amount)
            }
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState
        checkApproveState()
        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    fun getSendData(): SendEvmData? {
        val tmpAmount = amountState.amount ?: return null
        val evmAddress = addressState.evmAddress ?: return null

        var transactionData = adapter.getTransactionData(tmpAmount, evmAddress)
        pluginState.lockTimeInterval?.let {
            transactionData.lockTime = it.value() * 30
        }
        if (isSetLogoCoin() && transactionData.lockTime != null) {
            transactionData.isSRC20Lock = true
        }
        return SendEvmData(transactionData)
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    fun onEnterLockTimeInterval(lockTimeInterval: LockTimeInterval?) {
        pluginService.setLockTimeInterval(lockTimeInterval)
        checkApproveState()
    }


    private fun handleUpdatedPluginState(pluginState: SendBitcoinPluginService.State) {
        this.pluginState = pluginState

        emitState()
    }

    private fun getLockAmountError(): Boolean {
        return if (pluginState.lockTimeInterval != null) {
            val amount = amountState.amount?.toInt() ?: 0
            return amount < 1
        } else {
            false
        }
    }

    fun isSetLogoCoin(): Boolean {
        return sendToken.coin.uid.isSafeFourCustomCoin() && MMKV.defaultMMKV()?.getString(sendToken.tokenQuery.customCoinUid, null) != null
    }
}
