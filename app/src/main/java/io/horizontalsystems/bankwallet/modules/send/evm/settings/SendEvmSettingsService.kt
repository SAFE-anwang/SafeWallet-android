package io.horizontalsystems.bankwallet.modules.send.evm.settings

import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.GasData
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmFeeService
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.horizontalsystems.bankwallet.modules.evmfee.Transaction as TransactionFeeData

class SendEvmSettingsService(
    private val feeService: IEvmFeeService,
    private val nonceService: SendEvmNonceService
) {
    private var feeState: DataState<TransactionFeeData>? = null

    var state: DataState<Transaction> = DataState.Loading
        private set(value) {
            field = value
            _stateFlow.tryEmit(value)
        }
    private val _stateFlow: MutableSharedFlow<DataState<Transaction>> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val stateFlow: Flow<DataState<Transaction>> = _stateFlow.asSharedFlow()

    var nonce: Long? = null
    var gasData: GasData? = null

    suspend fun start() = withContext(Dispatchers.IO) {
        launch {
            feeService.transactionStatusFlow.collect {
                feeState = it
                sync()
            }
        }
        launch {
            nonceService.stateFlow.collect {
                sync()
            }
        }

        nonceService.start()
    }

    fun clear() {
        feeService.clear()
    }

    private fun sync() {
        val feeState = feeState
        val nonceState = nonceService.state
        nonce = nonceState.dataOrNull?.nonce
        gasData = feeState?.dataOrNull?.gasData

        state = when {
            feeState == DataState.Loading -> DataState.Loading
            nonceState == DataState.Loading -> DataState.Loading
            feeState is DataState.Error -> feeState
            nonceState is DataState.Error -> nonceState
            feeState is DataState.Success && nonceState is DataState.Success -> {
                val feeData = feeState.data
                val nonceData = nonceState.data

                val errors = feeData.errors.ifEmpty { nonceData.errors }
                val warnings = if (errors.isEmpty())
                    feeData.warnings.ifEmpty { nonceData.warnings }
                else
                    listOf()

                DataState.Success(
                    Transaction(
                        transactionData = feeData.transactionData,
                        gasData = feeData.gasData,
                        nonce = nonceData.nonce,
                        default = feeData.default && nonceData.default,
                        warnings = warnings,
                        errors = errors
                    )
                )
            }
            else -> DataState.Loading
        }
    }

    suspend fun reset() {
        feeService.reset()
        nonceService.reset()
    }

    data class Transaction(
        val transactionData: TransactionData,
        val gasData: GasData,
        val nonce: Long?,
        val default: Boolean,
        val warnings: List<Warning> = listOf(),
        val errors: List<Throwable> = listOf()
    )

}
