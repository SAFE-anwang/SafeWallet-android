package io.horizontalsystems.bankwallet.core.managers

import android.net.Uri
import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.EvmSyncSourceStorage
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.horizontalsystems.bankwallet.entities.EvmSyncSourceRecord
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URI

class EvmSyncSourceManager(
    private val appConfigProvider: AppConfigProvider,
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val evmSyncSourceStorage: EvmSyncSourceStorage,
) {

    companion object {
        const val TAG = "EvmSyncSourceManager"

        /** 全局交易数据源开关的 MMKV 键，与 EvmTransactionViewModel 保持一致 */
        const val KEY_TRANSACTION_SOURCE = "evm_transaction_source"

        private const val ID_ETHERSCAN = 0
        private const val ID_CHAINSTACK = 1
    }

    private val syncSourceSubject = PublishSubject.create<BlockchainType>()

    private val _syncSourcesUpdatedFlow =
        MutableSharedFlow<BlockchainType>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val syncSourcesUpdatedFlow = _syncSourcesUpdatedFlow.asSharedFlow()

    private fun defaultTransactionSource(blockchainType: BlockchainType): TransactionSource {
        return when (blockchainType) {
            BlockchainType.Ethereum -> TransactionSource.ethereum(listOf(appConfigProvider.etherscanApiKey))
            BlockchainType.BinanceSmartChain -> TransactionSource.binance(listOf(appConfigProvider.bscscanApiKey))
            BlockchainType.Avalanche -> TransactionSource.avalanche(listOf(appConfigProvider.bscscanApiKey))
            BlockchainType.Optimism -> TransactionSource.optimism(listOf(appConfigProvider.bscscanApiKey))
            BlockchainType.Base -> TransactionSource.base(listOf(appConfigProvider.bscscanApiKey))
            BlockchainType.Polygon-> TransactionSource.polygon(listOf(appConfigProvider.polygonscanApiKey))
            BlockchainType.ArbitrumOne -> TransactionSource.arbitrumOne(listOf(appConfigProvider.arbiscanApiKey))
            BlockchainType.Gnosis -> TransactionSource.gnosis(listOf(appConfigProvider.gnosisscanApiKey))
            BlockchainType.Fantom -> TransactionSource.fantom(listOf(appConfigProvider.ftmscanApiKey))
            BlockchainType.ZkSync -> TransactionSource.zkSync(appConfigProvider.otherScanApiKey)
            BlockchainType.Tron -> TransactionSource.ethereum(emptyList()) // unused for Tron; TronKitManager handles its own TransactionSource
            BlockchainType.SafeFour -> TransactionSource.safeFourscan(appConfigProvider.bscscanApiKey)
            else -> throw Exception("Non-supported EVM blockchain")
        }
    }

    /**
     * Chainstack 数据源：使用 Chainstack RPC 节点做区块扫描同步交易，
     * 不依赖 Etherscan 等浏览器 API。txBaseUrl 仅用于拼接交易详情跳转链接。
     */
    private fun chainstackTransactionSource(blockchainType: BlockchainType): TransactionSource? {
        val rpcSource = chainstackRpcSource(blockchainType) ?: return null
        val txBaseUrl = when (blockchainType) {
            BlockchainType.Ethereum -> "https://etherscan.io"
            BlockchainType.BinanceSmartChain -> "https://bscscan.com"
            BlockchainType.Polygon -> "https://polygonscan.com"
            BlockchainType.Avalanche -> "https://snowtrace.io"
            BlockchainType.Optimism -> "https://optimistic.etherscan.io"
            BlockchainType.Base -> "https://basescan.org"
            BlockchainType.ArbitrumOne -> "https://arbiscan.io"
            BlockchainType.Gnosis -> "https://gnosisscan.io"
            BlockchainType.Fantom -> "https://ftmscan.com"
            BlockchainType.ZkSync -> "https://era.zksync.network"
            else -> return null
        }
        return TransactionSource.chainstack(rpcSource.uris.map { it.toString() }, txBaseUrl)
    }

    private fun chainstackRpcSource(blockchainType: BlockchainType): RpcSource.Http? {
        return when (blockchainType) {
            BlockchainType.Ethereum -> RpcSource.Http(
                listOf(URI(appConfigProvider.blocksDecodedEthereumRpc)),
                null
            )
            BlockchainType.BinanceSmartChain -> RpcSource.bscRpcHttp()
            BlockchainType.Polygon -> RpcSource.polygonRpcHttp()
            BlockchainType.Avalanche -> RpcSource.avaxNetworkHttp()
            BlockchainType.Optimism -> RpcSource.optimismRpcHttp()
            BlockchainType.ArbitrumOne -> RpcSource.arbitrumOneRpcHttp()
            BlockchainType.Gnosis -> RpcSource.gnosisRpcHttp()
            BlockchainType.Fantom -> RpcSource.fantomRpcHttp()
            BlockchainType.Base -> RpcSource.baseRpcHttp()
            BlockchainType.ZkSync -> RpcSource.zkSyncRpcHttp()
            else -> null
        }
    }

    val syncSourceObservable: Observable<BlockchainType>
        get() = syncSourceSubject

    fun defaultSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> {
        return when (blockchainType) {
            BlockchainType.Ethereum -> listOf(
                evmSyncSource(
                    blockchainType,
                    "BlocksDecoded",
                    RpcSource.Http(listOf(URI(appConfigProvider.blocksDecodedEthereumRpc)), null),
                    defaultTransactionSource(blockchainType)
                ),
                /*evmSyncSource(
                    blockchainType,
                    "LlamaNodes",
                    RpcSource.Http(listOf(URI("https://eth.llamarpc.com")), null),
                    defaultTransactionSource(blockchainType)
                )*/
            )

            BlockchainType.BinanceSmartChain -> listOf(
                /*evmSyncSource(
                    blockchainType,
                    "Binance",
                    RpcSource.binanceSmartChainHttp(),
                    defaultTransactionSource(blockchainType)
                ),*/
                /*evmSyncSource(
                    blockchainType,
                    "BlockRazor",
                    RpcSource.Http(listOf(URI("https://unstoppable.bsc.blockrazor.xyz")), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "48club",
                    RpcSource.Http(listOf(URI("https://unstoppable.rpc.48.club")), null),
                    defaultTransactionSource(blockchainType)
                ),*/
                evmSyncSource(
                    blockchainType,
                    "BSC RPC",
                    RpcSource.bscRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                        BlockchainType.BinanceSmartChain,
                        "p2pify",
                        RpcSource.p2pifyRpcHttp(),
                        defaultTransactionSource(BlockchainType.BinanceSmartChain)
                )/*,
                evmSyncSource(
                    blockchainType,
                    "Omnia",
                    RpcSource.Http(listOf(URI("https://endpoints.omniatech.io/v1/bsc/mainnet/public")), null),
                    defaultTransactionSource(blockchainType)
                )*/
            )

            BlockchainType.Polygon -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Polygon RPC",
                    RpcSource.polygonRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "LlamaNodes",
                    RpcSource.Http(listOf(URI("https://polygon.llamarpc.com")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Avalanche -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Avax Network",
                    RpcSource.avaxNetworkHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "PublicNode",
                    RpcSource.Http(listOf(URI("https://avalanche-evm.publicnode.com")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Optimism -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Optimism",
                    RpcSource.optimismRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Omnia",
                    RpcSource.Http(
                        listOf(URI("https://endpoints.omniatech.io/v1/op/mainnet/public")),
                        null
                    ),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Base -> listOf(
                evmSyncSource(
                    blockchainType,
                    "PublicNode",
                    RpcSource.Http(listOf(URI("https://base-rpc.publicnode.com")), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "dRPC",
                    RpcSource.Http(listOf(URI("https://base.drpc.org")), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Base",
                    RpcSource.baseRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
            )

            BlockchainType.ZkSync -> listOf(
                evmSyncSource(
                    blockchainType,
                    "ZKsync",
                    RpcSource.zkSyncRpcHttp(),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.ArbitrumOne -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Arbitrum",
                    RpcSource.arbitrumOneRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Omnia",
                    RpcSource.Http(listOf(URI("https://endpoints.omniatech.io/v1/arbitrum/one/public")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Gnosis -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Gnosis Chain",
                    RpcSource.gnosisRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Ankr",
                    RpcSource.Http(listOf(URI("https://rpc.ankr.com/gnosis")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Fantom -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Fantom Chain",
                    RpcSource.fantomRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Fantom Chain (Mirror)",
                    RpcSource.Http(listOf(URI("https://rpcapi.fantom.network/")), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Ankr",
                    RpcSource.Http(listOf(URI("https://rpc.ankr.com/fantom")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Tron -> listOf(
                evmSyncSource(
                    blockchainType,
                    "TronGrid",
                    RpcSource.Http(listOf(URI("https://api.trongrid.io/")), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Pocket Network",
                    RpcSource.Http(listOf(URI("https://tron.api.pocket.network")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.SafeFour -> listOf(
                    evmSyncSource(
                            blockchainType,
                            "SAFE4",
                            RpcSource.safeFourHttp(App.localStorage.isSafe4TestNet),
                            defaultTransactionSource(blockchainType)
                    )
            )

            else -> listOf()
        }
    }

    fun customSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> {
        val records = evmSyncSourceStorage.evmSyncSources(blockchainType)
        return try {
            records.mapNotNull { record ->
                val uri = Uri.parse(record.url)
                val rpcSource = when (uri.scheme) {
                    "http",
                    "https" -> RpcSource.Http(listOf(URI(record.url)), record.auth)

                    "ws",
                    "wss" -> RpcSource.WebSocket(URI(record.url), record.auth)

                    else -> return@mapNotNull null
                }
                EvmSyncSource(
                    id = blockchainType.uid + "|" + record.url,
                    name = uri.host ?: "",
                    rpcSource = rpcSource,
                    transactionSource = defaultTransactionSource(blockchainType)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun evmSyncSource(
        blockchainType: BlockchainType,
        name: String,
        rpcSource: RpcSource,
        transactionSource: TransactionSource
    ) =
        EvmSyncSource(
            id = "${blockchainType.uid}|${name}|${transactionSource.name}|${
                rpcSource.uris.joinToString(separator = ",") { it.toString() }
            }",
            name = name,
            rpcSource = rpcSource,
            transactionSource = transactionSource
        )

    /**
     * 便捷方法：同一 RPC 节点同时生成 Etherscan 与 Chainstack 两个交易数据源选项，
     * 供用户在设置页二选一。
     */
    private fun EvmSyncSource.withTransactionSource(
        blockchainType: BlockchainType,
        useChainstack: Boolean
    ): EvmSyncSource {
        val source = if (useChainstack) {
            chainstackTransactionSource(blockchainType) ?: transactionSource
        } else {
            defaultTransactionSource(blockchainType)
        }
        return EvmSyncSource(
            id = "${blockchainType.uid}|${name}|${source.name}|${
                rpcSource.uris.joinToString(separator = ",") { it.toString() }
            }",
            name = name,
            rpcSource = rpcSource,
            transactionSource = source
        )
    }

    /**
     * 供设置页展示的全部可选交易数据源（Etherscan / Chainstack）。
     * 返回的数据源与当前 RPC 节点一致，仅交易同步方式不同。
     */
    fun transactionSourceOptions(blockchainType: BlockchainType): List<EvmSyncSource> {
        val rpcSource = getSyncSource(blockchainType).rpcSource
        val base = EvmSyncSource(
            id = "${blockchainType.uid}|option|${rpcSource.uris.joinToString(",") { it.toString() }}",
            name = "option",
            rpcSource = rpcSource,
            transactionSource = defaultTransactionSource(blockchainType)
        )
        val options = mutableListOf(base.withTransactionSource(blockchainType, false))
        if (chainstackTransactionSource(blockchainType) != null) {
            options.add(base.withTransactionSource(blockchainType, true))
        }
        return options
    }

    /** 支持交易数据源切换的全部 EVM 链 */
    private val switchableChains = listOf(
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Base,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ZkSync,
    )

    /**
     * 全局切换交易数据源：写入全局开关后，通知所有已支持的 EVM 链重新读取同步源，
     * 触发适配器重建与数据重新同步。
     *
     * 注意：Etherscan 与 Chainstack 共用同一个 RPC 节点，仅交易数据源不同，
     * 无法靠 syncSource.uri 区分，因此这里只维护全局开关，由 getSyncSource() 决定实际数据源。
     */
    @Synchronized
    fun saveTransactionSourceForAllChains(useChainstack: Boolean) {
        try {
            com.tencent.mmkv.MMKV.defaultMMKV()?.encode(
                KEY_TRANSACTION_SOURCE,
                if (useChainstack) ID_CHAINSTACK else ID_ETHERSCAN
            )
        } catch (e: Throwable) {
            Log.e(TAG, "saveTransactionSource: failed to persist switch: $e")
        }

        Log.d(TAG, "saveTransactionSourceForAllChains: useChainstack=$useChainstack")

        switchableChains.forEach { blockchainType ->
            try {
                if (useChainstack && chainstackTransactionSource(blockchainType) == null) {
                    Log.w(TAG, "skip $blockchainType: chainstack source unavailable")
                    return@forEach
                }
                // 通知该链重新读取同步源并重建适配器
                syncSourceSubject.onNext(blockchainType)
            } catch (e: Throwable) {
                Log.e(TAG, "switch transaction source failed for $blockchainType: $e")
            }
        }
    }

    fun allSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> =
        defaultSyncSources(blockchainType) + customSyncSources(blockchainType)

    /**
     * 当前使用的同步源。
     *
     * Chainstack 与 Etherscan 共用同一个 RPC 节点，仅在交易数据源上不同，
     * 因此不能只按 RPC uri 区分。这里优先读取 [EvmTransactionViewModel] 保存的
     * 全局数据源开关：开启 Chainstack 时返回对应链的 Chainstack 数据源。
     */
    fun getSyncSource(blockchainType: BlockchainType): EvmSyncSource {
        // Chainstack 模式：直接返回该链的 Chainstack 数据源（RPC 节点沿用当前配置）
        if (isChainstackMode() ) {
            chainstackTransactionSource(blockchainType)?.let { chainstackSource ->
                val rpcSource = chainstackRpcSource(blockchainType)
                if (rpcSource != null) {
                    Log.d(
                        TAG,
                        "getSyncSource: Chainstack mode for $blockchainType, " +
                                "rpc=${rpcSource.uris.firstOrNull()}"
                    )
                    return evmSyncSource(blockchainType, "Chainstack", rpcSource, chainstackSource)
                }
            }
        }

        val syncSources = allSyncSources(blockchainType)

        val syncSourceUrl = blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)
        val syncSource = syncSources.firstOrNull { it.uri.toString() == syncSourceUrl }

        return syncSource ?: syncSources[0]
    }

    /**
     * 全局数据源开关：是否使用 Chainstack 作为交易数据源。
     * 与 [EvmTransactionViewModel] 共用同一个 MMKV 键。
     */
    private fun isChainstackMode(): Boolean {
        return try {
            com.tencent.mmkv.MMKV.defaultMMKV()
                ?.decodeInt(KEY_TRANSACTION_SOURCE, ID_ETHERSCAN) == ID_CHAINSTACK
        } catch (e: Throwable) {
            false
        }
    }

    fun getHttpSyncSource(blockchainType: BlockchainType): EvmSyncSource? {
        // 与 getSyncSource 保持一致：Chainstack 模式下 RPC 节点不变，
        // 这里同样返回携带 Chainstack 交易源的数据源
        if (isChainstackMode()) {
            chainstackTransactionSource(blockchainType)?.let { chainstackSource ->
                val rpcSource = chainstackRpcSource(blockchainType)
                if (rpcSource != null) {
                    return evmSyncSource(blockchainType, "Chainstack", rpcSource, chainstackSource)
                }
            }
        }

        val syncSources = allSyncSources(blockchainType)
        blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)?.let { url ->
            syncSources.firstOrNull { it.uri.toString() == url && it.isHttp }?.let { syncSource ->
                return syncSource
            }
        }

        return syncSources.firstOrNull { it.isHttp }
    }

    fun save(syncSource: EvmSyncSource, blockchainType: BlockchainType) {
        blockchainSettingsStorage.save(syncSource.uri.toString(), blockchainType)
        syncSourceSubject.onNext(blockchainType)
    }

    fun saveSyncSource(blockchainType: BlockchainType, url: String, auth: String?) {
        val record = EvmSyncSourceRecord(
            blockchainTypeUid = blockchainType.uid,
            url = url,
            auth = auth
        )

        evmSyncSourceStorage.save(record)

        customSyncSources(blockchainType).firstOrNull { it.uri.toString() == url }?.let {
            save(it, blockchainType)
        }

        _syncSourcesUpdatedFlow.tryEmit(blockchainType)
    }

    fun delete(syncSource: EvmSyncSource, blockchainType: BlockchainType) {
        val isCurrent = getSyncSource(blockchainType) == syncSource

        evmSyncSourceStorage.delete(blockchainType.uid, syncSource.uri.toString())

        if (isCurrent) {
            syncSourceSubject.onNext(blockchainType)
        }

        _syncSourcesUpdatedFlow.tryEmit(blockchainType)
    }

    fun resync(blockchainType: BlockchainType) {
        syncSourceSubject.onNext(blockchainType)
    }

}
