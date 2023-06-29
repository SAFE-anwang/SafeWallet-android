package io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.ErrorShareService
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import kotlinx.coroutines.launch

class LiquidityAllowanceViewModel(
    private val errorShareService: ErrorShareService,
    private val allowanceServiceA: LiquidityAllowanceService,
    private val allowanceServiceB: LiquidityAllowanceService,
    private val pendingAllowanceServiceA: LiquidityPendingAllowanceService,
    private val pendingAllowanceServiceB: LiquidityPendingAllowanceService,
    private val formatter: SwapViewItemHelper
) : ViewModel() {

    private var isVisible = false
    private var allowance: String? = null
    private var isError = false
    private var revokeRequired = false

    var uiState by mutableStateOf(
        UiState(
            isVisible = isVisible,
            allowance = allowance,
            isError = isError,
            revokeRequired = revokeRequired,
        )
    )
        private set

    init {
        viewModelScope.launch {
            allowanceServiceA.stateFlow
                .collect { allowanceState ->
                    handle(/*allowanceState*/)
                }
        }
        viewModelScope.launch {
            allowanceServiceB.stateFlow
                .collect { allowanceState ->
                    handle(/*allowanceState*/)
                }
        }
        viewModelScope.launch {
            errorShareService.errorsStateFlow
                .collect { errors ->
                    handle(errors)
                }
        }

        handle(/*allowanceServiceA.state*/)
    }

    private fun emitState() {
        uiState = UiState(
            isVisible = isVisible,
            allowance = allowance,
            isError = isError,
            revokeRequired = revokeRequired,
        )
    }

    private fun syncVisible(/*state: LiquidityAllowanceService.State? = null*/) {
        val allowanceState = /*state ?: */allowanceServiceA.state
        val allowanceStateB = /*state ?: */allowanceServiceB.state

        isVisible = when {
            allowanceState == null || allowanceStateB == null -> false
            pendingAllowanceServiceA.state.loading() || pendingAllowanceServiceB.state.loading()  -> true
            allowanceState is LiquidityAllowanceService.State.NotReady
                    || allowanceStateB is LiquidityAllowanceService.State.NotReady-> true
            else -> isError || revokeRequired
        }
    }

    private fun handle(errors: List<Throwable>) {
        isError = errors.any { it is SwapError.InsufficientAllowance }
        revokeRequired = errors.any { it is SwapError.RevokeAllowanceRequired }

        syncVisible()
        emitState()
    }

    private fun handle(/*allowanceState: LiquidityAllowanceService.State?*/) {
        syncVisible(/*allowanceState*/)

        allowanceServiceA.state?.let {
            allowance = allowance(it)
        }

        allowanceServiceB.state?.let {
            allowance = allowance(it)
        }

        emitState()
    }

    private fun allowance(allowanceState: LiquidityAllowanceService.State): String {
        return when (allowanceState) {
            LiquidityAllowanceService.State.Loading -> Translator.getString(R.string.Alert_Loading)
            is LiquidityAllowanceService.State.Ready -> allowanceState.allowance.let { formatter.coinAmount(it.value, it.coin.code) }
            is LiquidityAllowanceService.State.NotReady -> Translator.getString(R.string.NotAvailable)
        }
    }

    data class UiState(
        val isVisible: Boolean,
        val allowance: String?,
        val isError: Boolean,
        val revokeRequired: Boolean
    )

}
