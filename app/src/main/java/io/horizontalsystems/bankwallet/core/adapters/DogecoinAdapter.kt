package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterErrorWrongParameters
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.purpose
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.Checkpoint
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.dogecoinkit.DogecoinKit
import io.horizontalsystems.dogecoinkit.DogecoinKit.NetworkType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.util.Calendar

class DogecoinAdapter(
    override val kit: DogecoinKit,
    syncMode: BitcoinCore.SyncMode,
    backgroundManager: BackgroundManager,
    wallet: Wallet,
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, confirmationsThreshold), DogecoinKit.Listener, ISendBitcoinAdapter {

    constructor(
            wallet: Wallet, syncMode:
            BitcoinCore.SyncMode,
            backgroundManager: BackgroundManager
    ) : this(createKit(wallet, syncMode), syncMode, backgroundManager, wallet)

    init {
        kit.listener = this
    }

    //
    // BitcoinBaseAdapter
    //

    override val satoshisInBitcoin: BigDecimal = BigDecimal.valueOf(Math.pow(10.0, decimal.toDouble()))

    //
    // LitecoinKit Listener
    //

    override val explorerTitle: String
        get() = "blockchair.com"


    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/dogecoin/transaction/$transactionHash"

    override fun onBalanceUpdate(balance: BalanceInfo) {
        balanceUpdatedSubject.onNext(Unit)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlockUpdatedSubject.onNext(Unit)
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        setState(state)
    }

    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
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

    override val unspentOutputs: List<UnspentOutputInfo>
        get() = kit.unspentOutputs

    override val blockchainType = BlockchainType.Dogecoin


    fun fallbackBlock(year: Int, month: Int) {
        lastBlockInfo?.let {
            kit.bitcoinCore.stop2()
            kit.bitcoinCore.stopDownload()
            val checkpoint = Checkpoint("${kit.networkName}.checkpoint")
            val lastBlockInfo = kit.bitcoinCore.storage.getLastBlockHash()
            val lastBlockHeight = if (lastBlockInfo != null) {
                it.height - 2000
            } else {
                checkpoint.block.height
            }
            val blocksList = kit.bitcoinCore.storage.getBlocksChunk(lastBlockHeight)
            if (blocksList.isNotEmpty()) {
                kit.bitcoinCore.storage.deleteBlocks(blocksList)
            }
            kit.mainNetDogecoin?.let {
                kit.bitcoinCore.updateLastBlockInfo(syncMode, it, checkpoint)
            }
            kit.bitcoinCore.start()
        }
    }

    companion object {
        private const val confirmationsThreshold = 3

        private fun createKit(
                wallet: Wallet,
                syncMode: BitcoinCore.SyncMode
        ): DogecoinKit {
            val account = wallet.account

            when (val accountType = account.type) {
                is AccountType.HdExtendedKey -> {
                    return DogecoinKit(
                        context = App.instance,
                        extendedKey = accountType.hdExtendedKey,
                        purpose = HDWallet.Purpose.BIP44,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                    )
                }
                is AccountType.Mnemonic -> {
                    return DogecoinKit(
                        context = App.instance,
                        words = accountType.words,
                        passphrase = accountType.passphrase,
                        walletId = account.id,
                        syncMode = syncMode,
                        networkType = NetworkType.MainNet,
                        confirmationsThreshold = confirmationsThreshold,
                        purpose = HDWallet.Purpose.BIP44
                    )
                }
                else -> throw UnsupportedAccountException()
            }
        }

        fun clear(walletId: String) {
            DogecoinKit.clear(App.instance, NetworkType.MainNet, walletId)
        }
    }
}
