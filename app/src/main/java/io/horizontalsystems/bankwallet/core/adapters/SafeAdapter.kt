package io.horizontalsystems.bankwallet.core.adapters

import android.util.Log
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.Single
import com.anwang.safewallet.safekit.SafeKit
import io.horizontalsystems.bankwallet.net.SafeNetWork
import java.math.BigDecimal

class SafeAdapter(
        override val kit: SafeKit,
        syncMode: SyncMode?,
        backgroundManager: BackgroundManager,
        wallet: Wallet,
        testMode: Boolean
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, testMode), SafeKit.Listener, ISendSafeAdapter {

    constructor(wallet: Wallet, syncMode: SyncMode?, testMode: Boolean, backgroundManager: BackgroundManager) :
            this(createKit(wallet, syncMode, testMode), syncMode, backgroundManager, wallet, testMode)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    // ITransactionsAdapter

    override val explorerTitle: String = "anwang.com"

    override fun explorerUrl(transactionHash: String): String? {
        Log.e("anwangTransaction", "chain.anwang.com ---1 https://chain.anwang.com/tx/$transactionHash")
        return if (testMode) null else "https://${SafeNetWork.getSafeDomainName()}/tx/$transactionHash"
    }

    //
    // DashKit Listener
    //

    override fun onBalanceUpdate(balance: BalanceInfo) {
        balanceUpdatedSubject.onNext(Unit)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlockUpdatedSubject.onNext(Unit)
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        setState(state)
    }

    override fun onTransactionsUpdate(inserted: List<DashTransactionInfo>, updated: List<DashTransactionInfo>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }

        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        // ignored for now
    }

    // ISendDashAdapter

    override fun availableBalance(address: String?): BigDecimal {
        return availableBalance(feeRate, address, mapOf())
    }

    override fun fee(amount: BigDecimal, address: String?): BigDecimal {
        return fee(amount, feeRate, address, mapOf())
    }

    override fun validate(address: String) {
        validate(address, mapOf())
    }

    override fun send(amount: BigDecimal, address: String, logger: AppLogger, lockedTimeInterval: LockTimeInterval? ): Single<Unit> {
        var unlockedHeight = 0L;
        if ( lockedTimeInterval != null ){
            val lockedMonth = lockedTimeInterval.value();
            val step = 86400 * lockedMonth;
            unlockedHeight = (kit.lastBlockInfo !! .height.toLong()).plus( step );
        }
        return Single.create { emitter ->
            try {
                kit.sendSafe(address, (amount * satoshisInBitcoin).toLong(), true, feeRate.toInt(), TransactionDataSortType.Shuffle, mapOf(),unlockedHeight )
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    companion object {

        private const val feeRate = 10L

        private fun getNetworkType(testMode: Boolean) =
                if (testMode) SafeKit.NetworkType.TestNet else SafeKit.NetworkType.MainNet

        private fun createKit(wallet: Wallet, syncMode: SyncMode?, testMode: Boolean): SafeKit {
            val account = wallet.account
            val accountType = account.type
            if (accountType is AccountType.Mnemonic) {
                return SafeKit(context = App.instance,
                        words = accountType.words,
                        passphrase = accountType.passphrase,
                        walletId = account.id,
                        syncMode = getSyncMode(syncMode),
                        networkType = getNetworkType(testMode),
                        confirmationsThreshold = confirmationsThreshold)
            }

            throw UnsupportedAccountException()
        }

        fun clear(walletId: String, testMode: Boolean) {
            SafeKit.clear(App.instance, getNetworkType(testMode), walletId)
        }
    }
}
