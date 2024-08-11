package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.confirmation.SafeFourConfirmationModule
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.BigInteger

class SafeFourCreateProposalViewModel(
        val wallet: Wallet,
        private val rpcBlockchainSafe4: RpcBlockchainSafe4
) : ViewModelUiState<SafeFourProposalModule.SafeFourProposalCreateUiState>()  {

    var tmpItemToShow: NodeInfo? = null
    private var title = ""
    private var description = ""
    private var startTime = 0L
    private var endTime = 0L
    private var payTimes = 1
    private var amount: BigDecimal? = null

    private val disposables = CompositeDisposable()

    private var balance: BigInteger = BigInteger.ZERO

    init {
    	getBalance()
    }

    override fun createState() = SafeFourProposalModule.SafeFourProposalCreateUiState(
            balance = App.numberFormatter.formatCoinFull(NodeCovertFactory.valueConvert(balance), "SAFE", 2),
            canSend = validCanSend()
    )

    private fun validCanSend(): Boolean {
        if (title.isNullOrBlank()) return false
        if (description.isNullOrBlank()) return false
        if (amount == null || BigDecimal.ZERO.compareTo(amount) >= 0) return false
        if (startTime == 0L || endTime == 0L || startTime > endTime) return false
        return true
    }

    private fun getBalance() {
        rpcBlockchainSafe4.getProposalBalance()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    balance = it
                    emitState()
                }, {

                })?.let {
                    disposables.add(it)
                }
    }

    fun onEnterAmount(text: String) {
        this.amount = if (text.isNotBlank()) text.toBigDecimalOrNull()?.stripTrailingZeros() else null
        emitState()
    }

    fun onEnterTitle(title: String?) {
        this.title = title ?: ""
        emitState()
    }

    fun onEnterDescription(description: String?) {
        this.description = description ?: ""
        emitState()
    }

    fun onSetPayTimes(payTimes: Int) {
        this.payTimes = payTimes
        emitState()
    }

    fun onEnterStartTime(time: Long) {
        this.startTime = time
        emitState()
    }

    fun onEnterEndTime(time: Long) {
        this.endTime = time
        emitState()
    }

    fun clearTime() {
        startTime = 0L
        endTime = 0L
    }

    fun isValid(text: String): Boolean {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull() else null
        if (amount == null) return false

        return amount.scale() <= 1
    }

    fun isValidTimes(text: String): Boolean {
        val amount = if (text.isNotBlank()) text.toInt() else null
        if (amount == null) return false

        return amount > 1
    }

    fun getCreateProposalData(): SafeFourProposalModule.CreateProposalData {
        return SafeFourProposalModule.CreateProposalData(
                amount!!.movePointRight(18).toBigInteger(),
                payTimes.toBigInteger(),
                startTime,
                endTime,
                title,
                description,
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
