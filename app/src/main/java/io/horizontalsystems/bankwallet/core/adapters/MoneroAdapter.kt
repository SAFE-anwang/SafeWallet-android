package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.BackgroundManagerState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendMoneroAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager.MoneroNode
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import android.util.Log
import io.horizontalsystems.monerokit.Balance
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.Seed
import io.horizontalsystems.monerokit.SyncState
import io.horizontalsystems.monerokit.data.Subaddress
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.roundToInt

class MoneroAdapter(
    private val kit: MoneroKit,
    private val transactionsProvider: MoneroTransactionsProvider,
    private val transactionsAdapter: MoneroTransactionsAdapter,
    private val backgroundManager: BackgroundManager,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendMoneroAdapter, ITransactionsAdapter by transactionsAdapter {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var balance = Balance(0, 0)

    private var retryJob: Job? = null
    private var retryCount = 0

    override var balanceState: AdapterState = kit.syncStateFlow.value.toAdapterState()

    override val balanceData: BalanceData
        get() = balance.toBalanceData()

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress: String
        get() = kit.receiveAddress

    override val isMainNet: Boolean
        get() = true

    override fun start() {
        kit.balanceFlow.collectWith(coroutineScope) {
            balance = it

            balanceUpdatedSubject.onNext(Unit)
        }

        kit.syncStateFlow.collectWith(coroutineScope) {
            balanceState = it.toAdapterState()

            balanceStateUpdatedSubject.onNext(Unit)

            handleSyncRetry(it)
        }

        kit.allTransactionsFlow.collectWith(coroutineScope, transactionsProvider::onTransactions)

        kit.start()

        coroutineScope.launch {
            backgroundManager.stateFlow.collect {
                if (it == BackgroundManagerState.EnterBackground) {
                    kit.saveState()
                }
            }
        }
    }

    override fun stop() {
        cancelRetry()
        kit.saveState()
        kit.stop()
        coroutineScope.cancel()
    }

    override fun refresh() {
        cancelRetry()
        retryCount = 0
        if (kit.syncStateFlow.value is SyncState.NotSynced) {
            kit.stop()
            kit.start()
        }
    }

    private fun handleSyncRetry(state: SyncState) {
        when (state) {
            is SyncState.Synced -> {
                cancelRetry()
                retryCount = 0
            }
            is SyncState.NotSynced -> {
                if (state.error is MoneroKit.SyncError.NotStarted) {
                    return
                }
                if (retryCount >= MAX_RETRY_COUNT) {
                    Log.e(TAG, "Max retry count ($MAX_RETRY_COUNT) reached, giving up", state.error)
                    return
                }
                val delayMs = (RETRY_DELAY_MS * 2.0.pow(retryCount)).toLong()
                Log.w(TAG, "Sync failed (attempt ${retryCount + 1}/$MAX_RETRY_COUNT), retrying in ${delayMs}ms", state.error)
                retryJob?.cancel()
                retryJob = coroutineScope.launch {
                    delay(delayMs)
                    retryCount++
                    if (kit.syncStateFlow.value is SyncState.NotSynced) {
                        Log.d(TAG, "Retrying Monero sync (attempt ${retryCount})")
                        kit.stop()
                        kit.start()
                    }
                }
            }
            else -> {
                cancelRetry()
            }
        }
    }

    private fun cancelRetry() {
        retryJob?.cancel()
        retryJob = null
    }

    override val debugInfo: String
        get() = ""

    override suspend fun send(amount: BigDecimal, address: String, memo: String?): String {
        val amountInPiconero = amount.movePointRight(DECIMALS).toLong()
        return kit.send(amountInPiconero, address, memo)
    }

    override suspend fun estimateFee(
        amount: BigDecimal,
        address: String,
        memo: String?
    ): BigDecimal {
        val amountInPiconero = amount.movePointRight(DECIMALS).toLong()
        return kit.estimateFee(amountInPiconero, address, memo).scaledDown(DECIMALS)
    }

    fun getSubaddresses(): List<Subaddress> {
        return kit.getSubaddresses()
    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    companion object {
        private const val TAG = "MoneroAdapter"
        const val DECIMALS = 12
        private const val MAX_RETRY_COUNT = 5
        private const val RETRY_DELAY_MS = 10_000L

        fun create(
            context: Context,
            wallet: Wallet,
            restoreSettings: RestoreSettings,
            node: MoneroNode
        ): MoneroAdapter {
            val birthdayHeightStr: String?
            val seed: Seed
            when (val accountType = wallet.account.type) {
                is AccountType.Mnemonic -> {
                    birthdayHeightStr = restoreSettings.birthdayHeight?.toString()
                    seed = Seed.Bip39(accountType.words, accountType.passphrase)
                }

                is AccountType.MoneroWatchAccount -> {
                    birthdayHeightStr = accountType.restoreHeight.toString()
                    seed = Seed.WatchOnly(accountType.address, accountType.privateViewKey)
                }

                else -> throw IllegalStateException("Unsupported account type: ${wallet.account.type.javaClass.simpleName}")
            }

            val birthdayHeightOrDate: String = when (wallet.account.origin) {
                AccountOrigin.Created -> {
                    birthdayHeightStr ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }

                AccountOrigin.Restored -> {
                    birthdayHeightStr ?: "0"
                }
            }

            val kit = MoneroKit.getInstance(
                context,
                seed,
                birthdayHeightOrDate,
                wallet.account.id,
                node.serialized,
                node.trusted
            )

            val transactionsProvider = MoneroTransactionsProvider()
            val transactionsAdapter = MoneroTransactionsAdapter(kit, transactionsProvider, wallet)

            return MoneroAdapter(
                kit,
                transactionsProvider,
                transactionsAdapter,
                App.backgroundManager
            )
        }

        fun clear(walletId: String) {
            MoneroKit.deleteWallet(App.instance, walletId)
        }
    }
}

fun Long.scaledDown(decimals: Int): BigDecimal {
    return this.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
}

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.NotSynced -> {
        if (error is MoneroKit.SyncError.NotStarted) {
            AdapterState.Connecting
        } else {
            AdapterState.NotSynced(error)
        }
    }
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Connecting -> AdapterState.Connecting
    is SyncState.Syncing -> AdapterState.Syncing(
        progress = progress?.let {
            (it * 100).roundToInt().coerceAtMost(100)
        },
        blocksRemained = remainingBlocks
    )
}

fun AccountType.toMoneroSeed() = when (this) {
    is AccountType.Mnemonic -> Seed.Bip39(words, passphrase)
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to Monero Seed")
}

fun Balance.toBalanceData(): BalanceData {
    val available = unlocked.scaledDown(MoneroAdapter.DECIMALS)
    val pending = (all - unlocked).coerceAtLeast(0).scaledDown(MoneroAdapter.DECIMALS)
    return BalanceData(available, pending = pending)
}