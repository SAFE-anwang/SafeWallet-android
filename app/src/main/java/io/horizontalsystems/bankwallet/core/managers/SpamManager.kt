package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.ethereumkit.models.Transaction
import java.math.BigDecimal

class SpamManager(
    private val localStorage: ILocalStorage
) {
    private val stableCoinCodes = listOf("USDT", "USDC", "DAI", "BUSD", "EURS")
    private val negligibleValue = BigDecimal("0.01")

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    var hideWithdrawTx = localStorage.hideWithdrawTransactions
        private set

    var hideUploadTx = localStorage.hideUploadTransactions
        private set

    fun isSpam(
        incomingEvents: List<TransferEvent>,
        outgoingEvents: List<TransferEvent>
    ): Boolean {
        val allEvents = incomingEvents + outgoingEvents
        return allEvents.all { spamEvent(it) }
    }

    private fun spamEvent(event: TransferEvent): Boolean {
        return when (val eventValue = event.value) {
            is TransactionValue.CoinValue -> {
                spamValue(eventValue.coinCode, eventValue.value)
            }

            is TransactionValue.NftValue -> {
                eventValue.value <= BigDecimal.ZERO
            }

            else -> true
        }
    }

    private fun spamValue(coinCode: String, value: BigDecimal): Boolean {
        return if (stableCoinCodes.contains(coinCode)) {
            value < negligibleValue
        } else {
            value <= BigDecimal.ZERO
        }
    }

    fun isIncomingSpam(transactionValue: TransactionValue): Boolean {
        return when(transactionValue) {
            is TransactionValue.CoinValue -> transactionValue.value <= BigDecimal.ZERO
            else -> false
        }
    }

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    fun updateFilterHideWithdrawTx(hide: Boolean) {
        localStorage.hideWithdrawTransactions = hide
        hideWithdrawTx = hide
    }

    fun updateFilterHideUploadTx(hide: Boolean) {
        localStorage.hideUploadTransactions = hide
        hideUploadTx = hide
    }

}
