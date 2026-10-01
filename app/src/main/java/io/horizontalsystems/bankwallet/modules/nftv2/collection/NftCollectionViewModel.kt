package io.horizontalsystems.bankwallet.modules.nftv2.collection

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.NftAdapterManager
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.core.providers.nft.BuiltinNftCollections
import io.horizontalsystems.bankwallet.core.providers.nft.NftContractAssetsProvider
import io.horizontalsystems.bankwallet.core.providers.nft.NftMetadataResolver
import io.horizontalsystems.bankwallet.core.providers.nft.Safe4NftAssetsService
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.nftv2.NftFavoritesStorage
import io.horizontalsystems.bankwallet.modules.nftv2.src721.SRC721LogoProvider
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "NftCollectionVM"

data class NftAssetViewItem(
    val tokenId: String,
    val name: String,
    val imageUrl: String?,
    val balance: Int,
    val nftType: NftType,
)

data class NftCollectionUiState(
    val viewState: ViewState = ViewState.Loading,
    val assets: List<NftAssetViewItem> = emptyList(),
    val collectionName: String = "",
    val contractAddress: String = "",
    val iconUrl: String? = null,
    /** 本地缓存的合约 logo 路径（仅 SAFE4），优先级高于 [iconUrl] */
    val localLogoPath: String? = null,
    /** 合约在 insight 登记的 logo URL（仅 SAFE4，链上 logo 读不到时使用） */
    val collectionLogoUrl: String? = null,
    val description: String? = null,
    val standard: String = "ERC721",
    val floorPrice24h: String = "0",
    val averagePrice24h: String = "0",
    val volume24h: String = "0",
    val baseToken: Token? = null,
    val isFavorite: Boolean = false,
)

class NftCollectionViewModel(
    private val accountId: String,
    private val blockchainType: BlockchainType,
    private val contractAddress: String,
    collectionName: String,
    private val nftAdapterManager: NftAdapterManager,
    private val metadataResolver: NftMetadataResolver,
    private val nftMetadataManager: NftMetadataManager,
    private val contractAssetsProvider: NftContractAssetsProvider,
    marketKit: MarketKitWrapper,
) : ViewModel() {

    private val builtin = BuiltinNftCollections.find(blockchainType, contractAddress)

    // 收藏按账户隔离，避免不同钱包之间互相影响
    private var isFavorite = NftFavoritesStorage.isFavorite(accountId, blockchainType.uid, contractAddress)

    var uiState by mutableStateOf(
        NftCollectionUiState(
            collectionName = builtin?.name ?: collectionName,
            contractAddress = contractAddress,
            iconUrl = builtin?.imageUrl,
            description = builtin?.description,
            baseToken = try {
                marketKit.token(TokenQuery(blockchainType, TokenType.Native))
            } catch (e: Throwable) {
                null
            },
            isFavorite = isFavorite
        )
    )
        private set

    private var collectJob: Job? = null
    private var nftKey: NftKey? = null
    private var ownAssets: List<NftAssetViewItem> = emptyList()
    private var availableAssets: List<NftAssetViewItem> = emptyList()

    /** 是否已经在本页面触发过针对当前合约的请求，避免重复拉取 */
    private var safe4FetchStarted = false

    init {
        Log.d(TAG, "VM 创建：$contractAddress  实例=${System.identityHashCode(this)}")
        viewModelScope.launch {
            nftAdapterManager.adaptersUpdatedFlow.collect { adaptersMap ->
                nftKey = adaptersMap.keys.firstOrNull { it.blockchainType == blockchainType }
                subscribe(nftKey)
                // 适配器只在全局 sync() 时刷新数据（且要遍历账户持有的所有合约），
                // 这里进页面时针对当前合约单独补一次请求：既保证数据新鲜，
                // 也避免「适配器那次请求失败 → 页面永远是空的」
                if (!safe4FetchStarted && nftKey != null) {
                    safe4FetchStarted = true
                    loadSafe4Assets()
                }
            }
        }
        viewModelScope.launch {
            nftMetadataManager.addressMetadataFlow.collect { pair ->
                if (pair != null && pair.first.blockchainType == blockchainType && ownAssets.isEmpty()) {
                    emitOpenSeaAssets(pair.second)
                }
            }
        }
        loadAvailableAssets()
        loadContractLogo()
    }

    /**
     * SAFE4 专属：读取合约自身 logo（owner 付费上传的图标），作为合集图标展示。
     * 其他链无此机制，直接跳过。
     *
     * 与 NFT 列表页保持一致的来源顺序：链上 `logo()` 优先，
     * 读不到时回退 insight `nft/tokens` 下发的 `logoURI`。
     * 合集图标既用于页面头部，也用于资产卡片无图时的兜底。
     */
    private fun loadContractLogo() {
        if (blockchainType != BlockchainType.SafeFour) return
        viewModelScope.launch(Dispatchers.IO) {
            val path = SRC721LogoProvider.fetchPath(contractAddress)
            // 合约未设置 logo 时，回退到 insight `nft/tokens` 下发的 logoURI
            val logoUrl = if (path != null) {
                null
            } else {
                Safe4NftAssetsService.collectionLogo(contractAddress)?.takeIf { it.isNotBlank() }
            }

            // Compose 状态必须在主线程写入：后台写入与首帧组合存在竞争，
            // 偶发不触发重组（数据已算出、页面却一直空白）
            withContext(Dispatchers.Main.immediate) {
                uiState = uiState.copy(localLogoPath = path, collectionLogoUrl = logoUrl)
            }
        }
    }

    /**
     * SAFE4 专属：进入页面时按合约单独拉一次资产明细。
     *
     * 适配器的记录只在全局 `sync()` 时刷新（要遍历账户持有的所有合约），
     * 如果那次请求失败或还没跑完，进入页面就只能是空白的。
     * 这里针对当前合约直接请求 [Safe4NftAssetsService.fetchAssetsOfToken]，
     * 拉取成功后交给 [emitAssets] 重建列表并刷新页面；
     * 失败时保留适配器已有的数据，不做清空。
     */
    private fun loadSafe4Assets() {
        if (blockchainType != BlockchainType.SafeFour) return
        viewModelScope.launch(Dispatchers.IO) {
            val key = nftKey ?: run {
                Log.w(TAG, "loadSafe4Assets: nftKey 未就绪，跳过")
                return@launch
            }
            val userAddress = nftAdapterManager.adapter(key)?.userAddress ?: run {
                Log.w(TAG, "loadSafe4Assets: 未找到适配器 $key，跳过")
                return@launch
            }

            val assets = Safe4NftAssetsService.fetchAssetsOfToken(userAddress, contractAddress)
                ?: run {
                    Log.w(TAG, "loadSafe4Assets: 接口失败 $contractAddress")
                    return@launch
                }

            val collectionName = Safe4NftAssetsService.fetchCollectionName(contractAddress)
            val records = assets
                .filter { it.isErc721 }
                .mapNotNull { asset ->
                    val tokenId = asset.tokenId ?: return@mapNotNull null
                    EvmNftRecord(
                        blockchainType = BlockchainType.SafeFour,
                        nftType = NftType.Eip721,
                        contractAddress = contractAddress,
                        tokenId = tokenId,
                        tokenName = collectionName,
                        balance = asset.balance
                    )
                }
                // 接口若返回重复条目，会与列表 key = tokenId 冲突
                .distinctBy { it.tokenId }

            Log.d(
                TAG,
                "loadSafe4Assets: $contractAddress 接口返回 ${assets.size} 条 → 转换 ${records.size} 条" +
                        "（样例 tokenType=${assets.take(3).map { it.tokenType }}," +
                        " tokenId=${assets.take(3).map { it.tokenId }}）"
            )

            if (records.isNotEmpty()) {
                emitAssets(records)
            }
        }
    }

    private fun subscribe(nftKey: NftKey?) {
        collectJob?.cancel()
        val adapter = nftKey?.let { nftAdapterManager.adapter(it) }
        collectJob = viewModelScope.launch {
            adapter?.nftRecordsFlow?.collect { records ->
                // 缓存命中需要读 MMKV（合集可能有上千个 NFT），放 IO 线程避免卡主线程
                withContext(Dispatchers.IO) {
                    emitAssets(records.filterIsInstance<EvmNftRecord>())
                }
            } ?: emitAssets(emptyList())
        }
    }

    private suspend fun emitAssets(records: List<EvmNftRecord>) {
        val filtered = records.filter { it.contractAddress.equals(contractAddress, true) }
        Log.d(TAG, "emitAssets: 收到 ${records.size} 条，命中 $contractAddress 的 ${filtered.size} 条")
        if (filtered.isEmpty()) {
            // 链上无记录时回退到 OpenSea 数据（SAFE4 没有该数据源，这里得到 null）
            viewModelScope.launch(Dispatchers.IO) {
                val key = nftKey ?: return@launch
                nftMetadataManager.addressMetadata(key)?.let { emitOpenSeaAssets(it) }
            }
            return
        }

        ownAssets = filtered
            .map { record ->
                val cached = metadataResolver.cached(record.nftUid)
                NftAssetViewItem(
                    tokenId = record.tokenId,
                    // 优先链上元数据 name；其次「合集名 #tokenId」；最后仅显示 #tokenId
                    name = cached?.name
                        ?: record.tokenName?.let { collection -> "$collection #${record.tokenId}" }
                        ?: "#${record.tokenId}",
                    imageUrl = cached?.imageUrl ?: cachedSafe4Image(record),
                    balance = record.balance,
                    nftType = record.nftType
                )
            }
            .sortedBy { it.tokenId.toBigIntegerOrNull() }

        rebuildAssets(standard = standardName(filtered.first().nftType))

        // 异步解析图片
        viewModelScope.launch(Dispatchers.IO) {
            records.filter { it.contractAddress.equals(contractAddress, true) }
                .forEach { record ->
                    if (metadataResolver.cached(record.nftUid) == null) {
                        val meta = metadataResolver.resolve(record.nftUid, record.nftType)
                        if (meta != null) {
                            updateAssetMeta(record.tokenId, meta.name, meta.imageUrl, meta.description)
                        }
                    }
                }
        }
    }

    private suspend fun emitOpenSeaAssets(metadata: NftAddressMetadata) {
        val assets = metadata.assets
            .filter { it.nftUid.contractAddress.equals(contractAddress, true) }

        // metadata 是「账户地址级」的（包含该地址下所有合约），且
        // NftStorage.addressInfo 在查不到数据时返回的是空 metadata 而不是 null，
        // 所以 SAFE4（没有 OpenSea 数据源）这里必然过滤成空列表。
        // 此时若直接赋值 ownAssets，会把页面上已经显示的资产整片清空。
        if (assets.isEmpty()) {
            Log.d(TAG, "emitOpenSeaAssets: $contractAddress 无 OpenSea 数据，保留现有资产")
            return
        }

        ownAssets = assets
            .map {
                NftAssetViewItem(
                    tokenId = it.nftUid.tokenId,
                    name = it.displayName,
                    imageUrl = it.previewImageUrl,
                    balance = 1,
                    nftType = NftType.Eip721
                )
            }
            .sortedBy { it.tokenId.toBigIntegerOrNull() }

        rebuildAssets()
    }

    private fun loadAvailableAssets() {
        viewModelScope.launch(Dispatchers.IO) {
            val assets = contractAssetsProvider.availableAssets(blockchainType, contractAddress)
            if (assets.isNotEmpty()) {
                availableAssets = assets.map {
                    NftAssetViewItem(
                        tokenId = it.tokenId,
                        name = it.name ?: "#${it.tokenId}",
                        imageUrl = it.imageUrl,
                        balance = 0,
                        nftType = NftType.Eip721
                    )
                }
                rebuildAssets()

                // 对缺少图片或名称的条目，解析链上 tokenURI 补充。
                // 只有「解析成功但名称与图片都为空」才视为无效数据移除；
                // 解析失败（网络异常、页面退出导致的取消）要保留条目并等待下次重试，
                // 否则一次瞬时失败会把整个合集的资产全部删除，页面直接变空白
                assets.filter { it.imageUrl == null || it.name == null }.map { asset ->
                    async(Dispatchers.IO) {
                        val nftUid = NftUid.Evm(blockchainType, contractAddress, asset.tokenId)
                        val meta = metadataResolver.resolve(nftUid, NftType.Eip721)
                        when {
                            meta != null && (meta.name != null || meta.imageUrl != null) ->
                                updateAssetMeta(asset.tokenId, meta.name, meta.imageUrl, meta.description)

                            meta != null ->
                                removeAvailableAsset(asset.tokenId)
                        }
                    }
                }.awaitAll()
            }
        }
    }

    /**
     * 重建资产列表并写入 UI 状态。
     *
     * 计算在本线程完成，`uiState` 必须回主线程写入：
     * Compose 状态的后台写入与页面首帧的组合存在竞争，偶发不会触发重组，
     * 表现为「数据已算出（rebuildAssets: merged=N）、页面却一直空白」。
     */
    private suspend fun rebuildAssets(standard: String? = null) {
        val ownedIds = ownAssets.map { it.tokenId }.toSet()
        val merged = ownAssets + availableAssets
            .filter { it.tokenId !in ownedIds }
            .sortedBy { it.tokenId.toBigIntegerOrNull() }

        Log.d(
            TAG,
            "rebuildAssets: own=${ownAssets.size} available=${availableAssets.size} merged=${merged.size}"
        )

        withContext(Dispatchers.Main.immediate) {
            uiState = uiState.copy(
                viewState = ViewState.Success,
                assets = merged,
                standard = standard ?: uiState.standard,
                iconUrl = uiState.iconUrl ?: merged.firstNotNullOfOrNull { it.imageUrl }
            )
        }
    }

    private suspend fun removeAvailableAsset(tokenId: String) {
        availableAssets = availableAssets.filterNot { it.tokenId == tokenId }
        rebuildAssets()
    }

    private suspend fun updateAssetMeta(
        tokenId: String,
        name: String?,
        imageUrl: String?,
        description: String?
    ) {
        ownAssets = ownAssets.map {
            if (it.tokenId == tokenId) {
                it.copy(name = name ?: it.name, imageUrl = imageUrl ?: it.imageUrl)
            } else it
        }
        availableAssets = availableAssets.map {
            if (it.tokenId == tokenId) {
                it.copy(name = name ?: it.name, imageUrl = imageUrl ?: it.imageUrl)
            } else it
        }

        withContext(Dispatchers.Main.immediate) {
            val updated = uiState.assets.map {
                if (it.tokenId == tokenId) {
                    it.copy(
                        name = name ?: it.name,
                        imageUrl = imageUrl ?: it.imageUrl
                    )
                } else it
            }
            uiState = uiState.copy(
                assets = updated,
                iconUrl = uiState.iconUrl ?: imageUrl,
                description = uiState.description ?: description
            )
        }
    }

    private fun standardName(nftType: NftType): String = when (nftType) {
        NftType.Eip721 -> "ERC721"
        NftType.Eip1155 -> "ERC1155"
    }

    /**
     * SAFE4 的图片优先取 insight 接口下发的 tokenImage。
     * 接口未提供时返回 null，交由 NftMetadataResolver 走链上 tokenURI 解析。
     */
    private fun cachedSafe4Image(record: EvmNftRecord): String? {
        if (record.blockchainType != BlockchainType.SafeFour) return null
        return Safe4NftAssetsService
            .cachedAsset(record.contractAddress, record.tokenId)
            ?.tokenImage
            ?.takeIf { it.isNotBlank() }
    }

    fun toggleFavorite() {
        isFavorite = NftFavoritesStorage.toggle(accountId, blockchainType.uid, contractAddress)
        uiState = uiState.copy(isFavorite = isFavorite)
    }

    class Factory(
        private val accountId: String,
        private val blockchainType: BlockchainType,
        private val contractAddress: String,
        private val collectionName: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NftCollectionViewModel(
                accountId,
                blockchainType,
                contractAddress,
                collectionName,
                App.nftAdapterManager,
                App.nftMetadataResolver,
                App.nftMetadataManager,
                App.nftContractAssetsProvider,
                App.marketKit
            ) as T
        }
    }
}
