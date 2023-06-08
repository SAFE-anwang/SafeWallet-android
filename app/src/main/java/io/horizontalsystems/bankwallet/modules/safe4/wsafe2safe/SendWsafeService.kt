package io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevm.IAmountInputService
import io.horizontalsystems.bankwallet.modules.sendevm.IAvailableBalanceService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.wsafekit.WsafeKit
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import io.horizontalsystems.ethereumkit.models.Address as EvmAddress

class SendWsafeService(
    val sendCoin: Token,
    val adapter: ISendEthereumAdapter
) : IAvailableBalanceService, IAmountInputService, Clearable {

    private val stateSubject = PublishSubject.create<State>()
    var state: State = State.NotReady
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private var evmAmount: BigInteger? = null
    private var addressData: AddressData? = null
    private var toSafeAddr: Address? = null

    private val amountCautionSubject = PublishSubject.create<AmountCaution>()
    private var amountCaution: AmountCaution = AmountCaution()
        set(value) {
            field = value
            amountCautionSubject.onNext(value)
        }
    val amountCautionObservable: Flowable<AmountCaution>
        get() = amountCautionSubject.toFlowable(BackpressureStrategy.BUFFER)

    private fun syncState() {
        val amountError = this.amountCaution.error
        val evmAmount = this.evmAmount
        val addressData = this.addressData
        state = if (amountError == null && evmAmount != null && addressData != null) {
            if(toSafeAddr != null){
                val wsafeKit = WsafeKit.getInstance(adapter.evmKitWrapper.evmKit.chain)
                val safeAddr = toSafeAddr!!.hex
                val transactionData = wsafeKit.transactionData(evmAmount, safeAddr)
                val additionalInfo = SendEvmData.AdditionalInfo.Send(SendEvmData.SendInfo())
                State.Ready(SendEvmData(transactionData, additionalInfo))
            } else {
                State.NotReady
            }
        } else {
            State.NotReady
        }
    }

    @Throws
    private fun validEvmAmount(amount: BigDecimal): BigInteger {
        val evmAmount = try {
            amount.movePointRight(sendCoin.decimals).toBigInteger()
        } catch (error: Throwable) {
            throw AmountError.InvalidDecimal
        }
        if (amount > adapter.balanceData.available) {
            throw AmountError.InsufficientBalance
        }
        return evmAmount
    }

    //region IAvailableBalanceService
    override val availableBalance: BigDecimal
        get() = adapter.balanceData.available
    //endregion

    //region IAmountInputService
    override val amount: BigDecimal
        get() = BigDecimal.ZERO

    override val coin: Token
        get() = sendCoin

    override val balance: BigDecimal
        get() = adapter.balanceData.available

    override val amountObservable: Flowable<BigDecimal>
        get() = Flowable.empty()

    override val coinObservable: Flowable<Optional<Token>>
        get() = Flowable.empty()

    override fun onChangeAmount(amount: BigDecimal) {
        if (amount > BigDecimal.ZERO) {
            var amountWarning: AmountWarning? = null
            try {
                if (amount == balance && (sendCoin.blockchainType is BlockchainType.Ethereum || sendCoin.blockchainType is BlockchainType.BinanceSmartChain || sendCoin.blockchainType is BlockchainType.Polygon)) {
                    amountWarning = AmountWarning.CoinNeededForFee
                }
                evmAmount = validEvmAmount(amount)
                amountCaution = AmountCaution(null, amountWarning)
            } catch (error: Throwable) {
                evmAmount = null
                amountCaution = AmountCaution(error, null)
            }
        } else {
            evmAmount = null
            amountCaution = AmountCaution()
        }

        syncState()
    }
    //endregion

    //region IRecipientAddressService

    fun setRecipientAddress(address: Address?, to: Address?) {
        addressData = address?.let {
            AddressData(evmAddress = EvmAddress(it.hex), domain = it.domain)
        }
        toSafeAddr = to
        syncState()
    }

    override fun clear() = Unit

    //endregion

    sealed class State {
        class Ready(val sendData: SendEvmData) : State()
        object NotReady : State()
    }

    sealed class AmountError : Throwable() {
        object InvalidDecimal : AmountError()
        object InsufficientBalance : AmountError() {
            override fun getLocalizedMessage(): String {
                return Translator.getString(R.string.Swap_ErrorInsufficientBalance)
            }
        }
    }

    class AmountCaution(val error: Throwable? = null, val amountWarning: AmountWarning? = null)

    enum class AmountWarning {
        CoinNeededForFee
    }

    data class AddressData(val evmAddress: EvmAddress, val domain: String?)

    fun isSendMinAmount(safeInfoPO: SafeInfoManager.SafeInfoPO): Boolean {
        val minSafe = BigDecimal(safeInfoPO.minamount).movePointRight(18)
        val safeAmount = (evmAmount ?: 0) as BigInteger
        return BigDecimal(safeAmount) >= minSafe
    }

}
