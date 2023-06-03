package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AccountType
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
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.net.SafeNetWork
import io.horizontalsystems.bitcoincore.models.Checkpoint
import io.horizontalsystems.bitcoincore.utils.JsonUtils
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class SafeAdapter(
    override val kit: SafeKit,
    syncMode: BitcoinCore.SyncMode,
    backgroundManager: BackgroundManager,
    wallet: Wallet,
    testMode: Boolean
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, testMode), SafeKit.Listener, ISendBitcoinAdapter, ISendSafeAdapter {

    constructor(wallet: Wallet, syncMode: BitcoinCore.SyncMode, testMode: Boolean, backgroundManager: BackgroundManager) :
            this(createKit(wallet, syncMode, testMode), syncMode, backgroundManager, wallet, testMode)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    // ITransactionsAdapter

    override val explorerTitle: String
        get() = "anwang.com"

    override fun getTransactionUrl(transactionHash: String): String? =
        if (testMode) null else "https://${SafeNetWork.getSafeDomainName()}/tx/$transactionHash"

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

    override fun availableBalanceSafe(address: String?): BigDecimal {
        return availableBalance(feeRate, address, mapOf())
    }

    override fun minimumSendAmountSafe(address: String?): BigDecimal? {
        return try {
            satoshiToBTC(kit.minimumSpendableValue(address).toLong(), RoundingMode.CEILING)
        } catch (e: Exception) {
            null
        }
    }

    private fun satoshiToBTC(value: Long, roundingMode: RoundingMode = RoundingMode.HALF_EVEN): BigDecimal {
        return BigDecimal(value).divide(satoshisInBitcoin, decimal, roundingMode)
    }

    override fun feeSafe(amount: BigDecimal, address: String?): BigDecimal? {
        return fee(amount, feeRate, address, mapOf())
    }

    override fun convertFeeSafe(amount: BigDecimal, address: String?): BigDecimal? {
        // 增加兑换WSAFE流量手续费
        var convertFeeRate = feeRate
        convertFeeRate += 50
        return fee(amount, convertFeeRate, address, mapOf())
    }

    override fun validateSafe(address: String) {
        validate(address, mapOf())
    }

    override fun sendSafe(amount: BigDecimal, address: String, logger: AppLogger, lockedTimeInterval: LockTimeInterval? , reverseHex: String ?): Single<Unit> {
        var unlockedHeight = 0L
        if ( lockedTimeInterval != null ){
            val lockedMonth = lockedTimeInterval.value();
            val step = 86400 * lockedMonth
            unlockedHeight = (kit.lastBlockInfo !! .height.toLong()).plus( step )
        }
        return Single.create { emitter ->
            try {
                // 增加兑换WSAFE流量手续费
                var convertFeeRate = feeRate
                var newReverseHex = reverseHex
                if (reverseHex != null && reverseHex.startsWith("73616665")) {
                    convertFeeRate += 50
                } else if(reverseHex != null && !reverseHex.startsWith("73616665")) {
                    convertFeeRate += 50
                    val lineLock = JsonUtils.stringToObj(reverseHex)
                    // 设置最新区块高度
                    lineLock.lastHeight = kit.lastBlockInfo !! .height.toLong()
                    lineLock.lockedValue = (BigDecimal(lineLock.lockedValue) * satoshisInBitcoin).toLong().toString()
                    newReverseHex = JsonUtils.objToString(lineLock)
                    Log.i("safe4", "---线性锁仓信息: $lineLock")
                }
                kit.sendSafe(address, (amount * satoshisInBitcoin).toLong(), true, convertFeeRate.toInt(), TransactionDataSortType.Shuffle, mapOf(), unlockedHeight, newReverseHex)
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                ex.printStackTrace()
                emitter.onError(ex)
            }
        }
    }

    /*override fun minimumSendAmount(address: String?): BigDecimal? {
        return try {
            satoshiToBTC(kit.minimumSpendableValue(address).toLong(), RoundingMode.CEILING)
        } catch (e: Exception) {
            null
        }
    }*/

    /*private fun satoshiToBTC(value: Long, roundingMode: RoundingMode = RoundingMode.HALF_EVEN): BigDecimal {
        return BigDecimal(value).divide(satoshisInBitcoin, decimal, roundingMode)
    }*/

    override val blockchainType = BlockchainType.Safe

    fun fallbackBlock(year: Int, month: Int) {
        lastBlockInfo?.let {
            val date = "$year${if(month <10) "0" else ""}$month"
            kit.fallbackBlockDate = date
            val calendar = Calendar.getInstance()
            calendar.clear()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            kit.bitcoinCore.stop2()
            kit.bitcoinCore.stopDownload()
            val checkpoint = Checkpoint("${kit.networkName}_${date}.checkpoint")
            val blocksList = kit.bitcoinCore.storage.getBlocksChunk(checkpoint.block.height)
            if (blocksList.isNotEmpty()) {
                kit.bitcoinCore.storage.deleteBlocks(blocksList)
            }
            kit.mainNetSafe?.let {
                kit.bitcoinCore.updateLastBlockInfo(syncMode, it)
            }
            kit.bitcoinCore.start()
        }
    }

    companion object {

        private const val feeRate = 10L

        private fun getNetworkType(testMode: Boolean) =
                if (testMode) SafeKit.NetworkType.TestNet else SafeKit.NetworkType.MainNet

        private fun createKit(wallet: Wallet, syncMode: BitcoinCore.SyncMode, testMode: Boolean): SafeKit {
            val account = wallet.account
            when(val accountType = account.type) {
                is AccountType.HdExtendedKey -> {
                    return SafeKit(
                        context = App.instance,
                        connectionManager = App.bitCoinConnectionManager,
                        extendedKey = accountType.hdExtendedKey,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = getNetworkType(testMode),
                        confirmationsThreshold = confirmationsThreshold
                    )
                }
                is AccountType.Mnemonic -> {
                    return SafeKit(context = App.instance,
                        connectionManager = App.bitCoinConnectionManager,
                        words = accountType.words,
                        passphrase = accountType.passphrase,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = getNetworkType(testMode),
                        confirmationsThreshold = confirmationsThreshold)
                }
                else -> throw UnsupportedAccountException()
            }
        }

        fun clear(walletId: String, testMode: Boolean) {
            SafeKit.clear(App.instance, getNetworkType(testMode), walletId)
        }
    }
}
