package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionRecord
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.Safe4DepositEvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.Safe4DepositEvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.SwapTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.UnknownSwapTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.nftUids
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronApproveTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.NftMetadataService
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class TransactionInfoService(
    val transactionRecord: TransactionRecord,
    private val adapter: ITransactionsAdapter,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val nftMetadataService: NftMetadataService,
    balanceHidden: Boolean,
) {

    val transactionHash: String get() = transactionRecord.transactionHash
    val source: TransactionSource get() = transactionRecord.source

    private val _transactionInfoItemFlow = MutableStateFlow<TransactionInfoItem?>(null)
    val transactionInfoItemFlow = _transactionInfoItemFlow.filterNotNull()

    var transactionInfoItem = TransactionInfoItem(
        transactionRecord,
        adapter.lastBlockInfo,
        TransactionInfoModule.ExplorerData(adapter.explorerTitle, adapter.getTransactionUrl(transactionRecord.transactionHash)),
        mapOf(),
        mapOf(),
        balanceHidden
    )
        private set(value) {
            field = value
            _transactionInfoItemFlow.update { value }
        }

    private val coinUidsForRates: List<String>
        get() {
            val coinUids = mutableListOf<String?>()

            val txCoinTypes = when (val tx = transactionRecord) {
                is TonTransactionRecord -> listOf(tx.mainValue.coinUid, tx.fee?.coinUid)
                is Safe4DepositEvmIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is Safe4DepositEvmOutgoingTransactionRecord -> listOf(tx.fee?.coinUid, tx.value.coinUid)
                is EvmIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is EvmOutgoingTransactionRecord -> listOf(tx.fee?.coinUid, tx.value.coinUid)
                is SwapTransactionRecord -> listOf(tx.fee, tx.valueIn, tx.valueOut).map { it?.coinUid }
                is UnknownSwapTransactionRecord -> listOf(tx.fee, tx.valueIn, tx.valueOut).map { it?.coinUid }
                is ApproveTransactionRecord -> listOf(tx.fee?.coinUid, tx.value.coinUid)
                is ContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingEvents.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEvents.map { it.value.coinUid })
                    tempCoinUidList
                }
                is ExternalContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingEvents.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEvents.map { it.value.coinUid })
                    tempCoinUidList
                }
                is BitcoinIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is BitcoinOutgoingTransactionRecord -> listOf(tx.fee, tx.value).map { it?.coinUid }
                is BinanceChainIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is BinanceChainOutgoingTransactionRecord -> listOf(tx.fee, tx.value).map { it.coinUid }
                is SolanaIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is SolanaOutgoingTransactionRecord -> listOf(tx.fee?.coinUid, tx.value.coinUid)
                is SolanaUnknownTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingTransfers.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingTransfers.map { it.value.coinUid })
                    tempCoinUidList
                }
                is TronOutgoingTransactionRecord -> {
                    listOf(tx.value.coinUid, tx.fee?.coinUid)
                }
                is TronIncomingTransactionRecord -> {
                    listOf(tx.value.coinUid)
                }
                is TronApproveTransactionRecord -> {
                    listOf(tx.value.coinUid, tx.fee?.coinUid)
                }
                is TronContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingEvents.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEvents.map { it.value.coinUid })
                    tempCoinUidList
                }
                is TronExternalContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingEvents.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEvents.map { it.value.coinUid })
                    tempCoinUidList
                }
                else -> emptyList()
            }

            (transactionRecord as? EvmTransactionRecord)?.let { transactionRecord ->
                if (!transactionRecord.foreignTransaction) {
                    coinUids.add(transactionRecord.fee?.coinUid)
                }
            }

            (transactionRecord as? TronTransactionRecord)?.let { transactionRecord ->
                if (!transactionRecord.foreignTransaction) {
                    coinUids.add(transactionRecord.fee?.coinUid)
                }
            }

            coinUids.addAll(txCoinTypes)

            return coinUids.filterNotNull().filter { it.isNotBlank() }.distinct()
        }

    suspend fun start() = withContext(Dispatchers.IO) {
        _transactionInfoItemFlow.update { transactionInfoItem }

        launch {
            adapter.getTransactionRecordsFlowable(null, FilterTransactionType.All, null).asFlow()
                .collect { transactionRecords ->
                    val record = transactionRecords.find { it == transactionRecord }

                    if (record != null) {
                        handleRecordUpdate(record)
                    }
                }
        }

        launch {
            adapter.lastBlockUpdatedFlowable.asFlow()
                .collect {
                    handleLastBlockUpdate()
                }
        }

        launch {
            nftMetadataService.assetsBriefMetadataFlow.collect {
                handleNftMetadata(it)
            }
        }

        fetchRates()
        fetchNftMetadata()
    }

    private suspend fun fetchNftMetadata() {
        val nftUids = transactionRecord.nftUids
        val assetsBriefMetadata = nftMetadataService.assetsBriefMetadata(nftUids)

        handleNftMetadata(assetsBriefMetadata)

        if (nftUids.subtract(assetsBriefMetadata.keys).isNotEmpty()) {
            nftMetadataService.fetch(nftUids)
        }
    }

    private suspend fun fetchRates() = withContext(Dispatchers.IO) {
        val coinUids = coinUidsForRates
        val timestamp = transactionRecord.timestamp

        val rates = coinUids.mapNotNull { coinUid ->
            var uid = coinUid
            try {
                val rate = marketKit
                    .coinHistoricalPriceSingle(uid, currencyManager.baseCurrency.code, timestamp)
                    .await()
                if (rate != BigDecimal.ZERO) {
                    Pair(coinUid, CurrencyValue(currencyManager.baseCurrency, rate))
                } else {
                    null
                }
            } catch (error: Exception) {
                null
            }
        }.toMap()

        handleRates(rates)
    }

    @Synchronized
    private fun handleLastBlockUpdate() {
        transactionInfoItem = transactionInfoItem.copy(lastBlockInfo = adapter.lastBlockInfo)
    }

    @Synchronized
    private fun handleRecordUpdate(transactionRecord: TransactionRecord) {
        transactionInfoItem = transactionInfoItem.copy(record = transactionRecord)
    }

    @Synchronized
    private fun handleRates(rates: Map<String, CurrencyValue>) {
        transactionInfoItem = transactionInfoItem.copy(rates = rates)
    }

    @Synchronized
    private fun handleNftMetadata(nftMetadata: Map<NftUid, NftAssetBriefMetadata>) {
        transactionInfoItem = transactionInfoItem.copy(nftMetadata = nftMetadata)
    }

    fun getRawTransaction(): String? {
        return adapter.getRawTransaction(transactionRecord.transactionHash)
    }

    /*private fun getExplorerData(record: TransactionRecord): TransactionInfoModule.ExplorerData {
        val hash = record.transactionHash
        val blockchain = record.source.blockchain
        val account = record.source.account

        return when (blockchain) {
            is TransactionSource.Blockchain.Bitcoin -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/bitcoin/transaction/$hash"
            )
            is TransactionSource.Blockchain.BitcoinCash -> TransactionInfoModule.ExplorerData(
                "btc.com",
                if (testMode) null else "https://bch.btc.com/$hash"
            )
            is TransactionSource.Blockchain.Litecoin -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/litecoin/transaction/$hash"
            )
            is TransactionSource.Blockchain.Dash -> TransactionInfoModule.ExplorerData(
                "dash.org",
                if (testMode) null else "https://insight.dash.org/insight/tx/$hash"
            )
            is TransactionSource.Blockchain.Safe -> TransactionInfoModule.ExplorerData(
                "anwang.com",
                if (testMode) null else "https://${SafeNetWork.getSafeDomainName()}/tx/$hash"
            )
            is TransactionSource.Blockchain.Ethereum -> {
                val domain = when (ethereumChain(account)) {
                    Chain.Ethereum -> "etherscan.io"
                    Chain.EthereumRopsten -> "ropsten.etherscan.io"
                    Chain.EthereumKovan -> "kovan.etherscan.io"
                    Chain.EthereumRinkeby -> "rinkeby.etherscan.io"
                    Chain.EthereumGoerli -> "goerli.etherscan.io"
                    else -> throw IllegalArgumentException("")
                }
                TransactionInfoModule.ExplorerData("etherscan.io", "https://$domain/tx/0x$hash")
            }
            is TransactionSource.Blockchain.Bep2 -> TransactionInfoModule.ExplorerData(
                "binance.org",
                if (testMode) "https://testnet-explorer.binance.org/tx/$hash" else "https://explorer.binance.org/tx/$hash"
            )
            is TransactionSource.Blockchain.BinanceSmartChain -> TransactionInfoModule.ExplorerData(
                "bscscan.com",
                "https://bscscan.com/tx/0x$hash"
            )
            is TransactionSource.Blockchain.Zcash -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/zcash/transaction/$hash"
            )
        }
    }*/

}
