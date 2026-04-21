package io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.Executors

enum class SwapPendingAllowanceState {
    NA, Revoking, Revoked, Approving, Approved;

    fun loading() = this == Revoking || this == Approving
}

class LiquidityPendingAllowanceService(
    private val adapterManager: IAdapterManager,
    private val allowanceService: LiquidityAllowanceService
) {
    private var token: Token? = null
    private var pendingAllowance: BigDecimal? = null

    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(singleDispatcher)

    private val stateSubject = PublishSubject.create<SwapPendingAllowanceState>()
    var state: SwapPendingAllowanceState = SwapPendingAllowanceState.NA
        private set(value) {
            if (field != value) {
                field = value
                stateSubject.onNext(value)
            }
        }
    val stateObservable: Observable<SwapPendingAllowanceState> = stateSubject

    init {
        coroutineScope.launch {
            allowanceService.stateFlow
                .collect { sync() }
        }
    }

    fun set(token: Token?) {
        this.token = token
        pendingAllowance = null

        syncAllowance()
    }

    fun syncAllowance() {
        val coin = token ?: return
        val adapter = adapterManager.getAdapterForToken(coin) as? Eip20Adapter ?: return

        adapter.pendingTransactions.forEach { transaction ->
            if (transaction is ApproveTransactionRecord) {
                pendingAllowance = transaction.value.decimalValue
            }
        }

        sync()
    }

    fun onCleared() {
        coroutineScope.cancel()
    }

    private fun sync() {
        val pendingAllowance = pendingAllowance
        val allowanceState = allowanceService.state

        if (pendingAllowance == null || allowanceState == null || allowanceState !is LiquidityAllowanceService.State.Ready) {
            state = SwapPendingAllowanceState.NA
            return
        }

        val pendingAllowanceConfirmed = allowanceState.allowance.value.compareTo(pendingAllowance) == 0

        state = if (pendingAllowance.compareTo(BigDecimal.ZERO) == 0) {
            when {
                pendingAllowanceConfirmed -> SwapPendingAllowanceState.Revoked
                else -> SwapPendingAllowanceState.Revoking
            }
        } else {
            when {
                pendingAllowanceConfirmed -> SwapPendingAllowanceState.Approved
                else -> SwapPendingAllowanceState.Approving
            }
        }
    }

}
