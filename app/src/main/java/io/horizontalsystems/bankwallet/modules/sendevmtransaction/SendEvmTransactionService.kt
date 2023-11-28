package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmSettingsService
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Connect
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Constants
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.TransactionContractSend
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import java.math.BigInteger

interface ISendEvmTransactionService {
    val state: SendEvmTransactionService.State
    val stateObservable: Flowable<SendEvmTransactionService.State>

    val txDataState: SendEvmTransactionService.TxDataState

    val sendState: SendEvmTransactionService.SendState
    val sendStateObservable: Flowable<SendEvmTransactionService.SendState>

    val ownAddress: Address

    val settingsService: SendEvmSettingsService

    suspend fun start()
    fun send(logger: AppLogger)
    fun addLiqudity(logger: AppLogger)
    fun methodName(input: ByteArray): String?
    fun clear()
}

class SendEvmTransactionService(
    private val sendEvmData: SendEvmData,
    private val evmKitWrapper: EvmKitWrapper,
    override val settingsService: SendEvmSettingsService,
    private val evmLabelManager: EvmLabelManager
) : Clearable, ISendEvmTransactionService {
    private val disposable = CompositeDisposable()

    private val evmKit = evmKitWrapper.evmKit
    private val stateSubject = PublishSubject.create<State>()

    override var state: State = State.NotReady()
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    override val stateObservable: Flowable<State> = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val sendStateSubject = PublishSubject.create<SendState>()
    override var sendState: SendState = SendState.Idle
        private set(value) {
            field = value
            sendStateSubject.onNext(value)
        }
    override val sendStateObservable: Flowable<SendState> = sendStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override var txDataState: TxDataState = TxDataState(
        sendEvmData.transactionData,
        sendEvmData.additionalInfo,
        evmKit.decorate(sendEvmData.transactionData)
    )
        private set

    override val ownAddress: Address = evmKit.receiveAddress

    override suspend fun start() = withContext(Dispatchers.IO) {
        launch {
            settingsService.stateFlow
                .collect {
                    sync(it)
                }
        }

        settingsService.start()
    }

    private fun sync(settingsState: DataState<SendEvmSettingsService.Transaction>) {
        when (settingsState) {
            is DataState.Error -> {
                state = State.NotReady(errors = listOf(settingsState.error))
                syncTxDataState()
            }
            DataState.Loading -> {
                state = State.NotReady()
            }
            is DataState.Success -> {
                syncTxDataState(settingsState.data)

                val warnings = settingsState.data.warnings + sendEvmData.warnings
                state = if (settingsState.data.errors.isNotEmpty()) {
                    State.NotReady(warnings, settingsState.data.errors)
                } else {
                    State.Ready(warnings)
                }
            }
        }
    }

    override fun send(logger: AppLogger) {
        if (state !is State.Ready) {
            logger.info("state is not Ready: ${state.javaClass.simpleName}")
            return
        }
        val txConfig = settingsService.state.dataOrNull ?: return

        sendState = SendState.Sending
        logger.info("sending tx")

        evmKitWrapper.sendSingle(
            txConfig.transactionData,
            txConfig.gasData.gasPrice,
            txConfig.gasData.gasLimit,
            txConfig.nonce
        )
            .subscribeIO({ fullTransaction ->
                sendState = SendState.Sent(fullTransaction.transaction.hash)
                logger.info("success")
            }, { error ->
                sendState = SendState.Failed(error)
                logger.warning("failed", error)
            })
            .let { disposable.add(it) }
    }

    override fun addLiqudity(logger: AppLogger) {
        /*if (state !is State.Ready) {
            logger.info("state is not Ready: ${state.javaClass.simpleName}")
            return
        }*/
//        val txConfig = settingsService.state.dataOrNull ?: return

        sendState = SendState.Sending

        logger.info("sending tx")
        GlobalScope.launch {
            try {
                val web3j: Web3j = Connect.connect()
                val nonce = web3j.ethGetTransactionCount(
                    evmKitWrapper.evmKit.receiveAddress.hex,
                    DefaultBlockParameterName.LATEST
                )
                    .send().transactionCount
                val hash = TransactionContractSend.send(
                    web3j, Credentials.create(evmKitWrapper.signer!!.privateKey.toString(16)),
                    Constants.DEX.PANCAKE_V2_ROUTER_ADDRESS,
                    sendEvmData.transactionData.input.toHexString(),
                    BigInteger.ZERO, nonce,
                    Convert.toWei("10", Convert.Unit.GWEI).toBigInteger(),  // GAS PRICE : 5GWei
                    BigInteger("500000") // GAS LIMIT
                )
                withContext(Dispatchers.Main) {
                    sendState = SendState.Sent(hash.toByteArray())
                    logger.info("success")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sendState = SendState.Failed(e)
                    logger.warning("failed", e)
                }
            }
        }
    }

    override fun methodName(input: ByteArray): String? =
        evmLabelManager.methodLabel(input)

    override fun clear() {
        disposable.clear()
        settingsService.clear()
    }

    private fun syncTxDataState(transaction: SendEvmSettingsService.Transaction? = null) {
        val transactionData = transaction?.transactionData ?: sendEvmData.transactionData
        txDataState = TxDataState(transactionData, sendEvmData.additionalInfo, evmKit.decorate(transactionData))
    }

    sealed class State {
        class Ready(val warnings: List<Warning> = listOf()) : State()
        class NotReady(val warnings: List<Warning> = listOf(), val errors: List<Throwable> = listOf()) : State()
    }

    data class TxDataState(
        val transactionData: TransactionData?,
        val additionalInfo: SendEvmData.AdditionalInfo?,
        val decoration: TransactionDecoration?
    )

    sealed class SendState {
        object Idle : SendState()
        object Sending : SendState()
        class Sent(val transactionHash: ByteArray) : SendState()
        class Failed(val error: Throwable) : SendState()
    }

    sealed class TransactionError : Throwable() {
        class InsufficientBalance(val requiredBalance: BigInteger) : TransactionError()
    }

}
