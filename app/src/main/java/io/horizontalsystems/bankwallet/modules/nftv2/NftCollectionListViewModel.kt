package io.horizontalsystems.bankwallet.modules.nftv2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.nft.INftAdapter
import io.horizontalsystems.bankwallet.core.managers.NftAdapterManager
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.core.managers.NftMetadataSyncer
import io.horizontalsystems.bankwallet.core.providers.nft.BuiltinNftCollections
import io.horizontalsystems.bankwallet.core.providers.nft.NftMetadataResolver
import io.horizontalsystems.bankwallet.core.providers.nft.Safe4NftAssetsService
import io.horizontalsystems.bankwallet.core.providers.nft.Safe4NftToken
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import io.horizontalsystems.bankwallet.modules.nftv2.src721.SRC721LogoProvider
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class NftCollectionViewItem(
    val blockchainType: BlockchainType,
    val contractAddress: String,
    val name: String,
    val count: Int,
    val sampleTokenId: String,
    val imageUrl: String?,
    /**
     * 本地缓存的合约 logo 文件路径（仅 SAFE4）。
     * 由合约 owner 付费上传，优先级高于 [imageUrl]。
     */
    val localLogoPath: String? = null,
)

enum class NftListTab(val titleRes: Int) {
    All(R.string.Nft_Tab_All),
    Favorites(R.string.Nft_Tab_Favorites)
}

data class NftCollectionListUiState(
    val viewState: ViewState = ViewState.Loading,
    val collections: List<NftCollectionViewItem> = emptyList(),
    val syncing: Boolean = false,
    val tab: NftListTab = NftListTab.All,
)

class NftCollectionListViewModel(
    private val nftAdapterManager: NftAdapterManager,
    private val metadataResolver: NftMetadataResolver,
    private val nftMetadataManager: NftMetadataManager,
    private val nftMetadataSyncer: NftMetadataSyncer,
) : ViewModel() {

    var uiState by mutableStateOf(NftCollectionListUiState())
        private set

    private var collectJob: Job? = null
    private var onChainRecords: List<NftRecord> = emptyList()
    private var openSeaData: Map<NftKey, NftAddressMetadata> = emptyMap()

    /** 当前 NFT 数据所属账户地址（收藏按该地址隔离存储） */
    private var currentAccount: String? = null

    /** 已解析到的 SAFE4 合约 logo：contract(lowercase) -> 本地文件路径 */
    private val safe4LogoPaths = mutableMapOf<String, String>()

    /**
     * SAFE4 推广合集：insight `nft/tokens` 中登记了 logoURI 的合约。
     *
     * 展示规则：发行且推广上传过 logo 的合集，在所有钱包的 NFT 列表都展示；
     * 未推广的合集只出现在持有它的钱包里（由 `nft/assets` 持仓数据决定）。
     * 这里以 [Safe4NftToken.logoURI] 是否存在作为「已推广」的判断依据。
     */
    private var promotedSafe4: List<Safe4NftToken> = emptyList()

    init {
        viewModelScope.launch {
            nftAdapterManager.adaptersUpdatedFlow.collect { adaptersMap ->
                subscribeToAdapters(adaptersMap)
                loadStoredOpenSeaMetadata(adaptersMap.keys)
            }
        }
        viewModelScope.launch {
            nftMetadataManager.addressMetadataFlow.collect { pair ->
                if (pair != null) {
                    openSeaData = openSeaData + (pair.first to pair.second)
                    rebuildItems()
                }
            }
        }
        refresh()
        loadPromotedSafe4()
    }

    /**
     * 拉取全网 SAFE4 合集索引，取出其中「已推广」（登记了 logoURI）的合约。
     * 这些合集不依赖本钱包是否持有，所有钱包的 NFT 列表都要展示。
     */
    private fun loadPromotedSafe4() {
        viewModelScope.launch(Dispatchers.IO) {
            promotedSafe4 = Safe4NftAssetsService.fetchTokens()
                .orEmpty()
                .filter { !it.logoURI.isNullOrBlank() }
            rebuildItems()
        }
    }

    private fun subscribeToAdapters(adaptersMap: Map<NftKey, INftAdapter>) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            // 记录当前账户 id，供收藏过滤按账户读取（收藏数据按账户隔离存储）
            currentAccount = adaptersMap.keys.firstOrNull()?.account?.id
            if (adaptersMap.isEmpty()) {
                emitItems(emptyList())
                return@launch
            }
            kotlinx.coroutines.flow.combine(
                adaptersMap.values.map { it.nftRecordsFlow }
            ) { arrayOfRecords ->
                arrayOfRecords.toList().flatten()
            }.collect { records ->
                emitItems(records)
            }
        }
    }

    private fun loadStoredOpenSeaMetadata(nftKeys: Set<NftKey>) {
        viewModelScope.launch(Dispatchers.IO) {
            val stored = nftKeys.mapNotNull { nftKey ->
                nftMetadataManager.addressMetadata(nftKey)?.let { nftKey to it }
            }.toMap()
            openSeaData = stored
            rebuildItems()
        }
    }

    private fun emitItems(records: List<NftRecord>) {
        onChainRecords = records
        rebuildItems()
    }

    private fun rebuildItems() {
        val items = mutableMapOf<Pair<BlockchainType, String>, NftCollectionViewItem>()

        // OpenSea 数据：名称与图片更完整
        openSeaData.forEach { (_, metadata) ->
            metadata.assets
                .groupBy { it.nftUid.blockchainType to it.nftUid.contractAddress.lowercase() }
                .forEach { (key, assets) ->
                    val first = assets.first()
                    val collectionMeta = metadata.collections.firstOrNull {
                        it.providerUid == first.providerCollectionUid
                    }
                    items[key] = NftCollectionViewItem(
                        blockchainType = key.first,
                        contractAddress = first.nftUid.contractAddress,
                        name = collectionMeta?.name ?: first.nftUid.contractAddress.take(10),
                        count = assets.size,
                        sampleTokenId = first.nftUid.tokenId,
                        imageUrl = collectionMeta?.thumbnailImageUrl
                            ?: assets.firstNotNullOfOrNull { it.previewImageUrl }
                    )
                }
        }

        // 链上记录：余额数量最准确，覆盖数量并补充缺失的名称
        onChainRecords
            .filterIsInstance<EvmNftRecord>()
            .groupBy { it.blockchainType to it.contractAddress.lowercase() }
            .forEach { (key, group) ->
                val first = group.first()
                val existing = items[key]
                items[key] = NftCollectionViewItem(
                    blockchainType = key.first,
                    contractAddress = first.contractAddress,
                    name = first.tokenName ?: existing?.name ?: first.contractAddress.take(10),
                    count = group.sumOf { it.balance },
                    sampleTokenId = first.tokenId,
                    imageUrl = existing?.imageUrl
                )
            }

        // 应用内置合集的规范名称与图标
        items.keys.toList().forEach { key ->
            BuiltinNftCollections.find(key.first, key.second)?.let { builtin ->
                items[key] = items[key]!!.copy(
                    name = builtin.name,
                    imageUrl = builtin.imageUrl ?: items[key]!!.imageUrl
                )
            }
        }

        // 内置合集：未持有也展示（数量为 0）
        BuiltinNftCollections.all().forEach { builtin ->
            if (!builtin.showAlways) return@forEach
            val key = builtin.blockchainType to builtin.contractAddress.lowercase()
            if (!items.containsKey(key)) {
                items[key] = NftCollectionViewItem(
                    blockchainType = builtin.blockchainType,
                    contractAddress = builtin.contractAddress,
                    name = builtin.name,
                    count = 0,
                    sampleTokenId = "",
                    imageUrl = builtin.imageUrl
                )
            }
        }

        // SAFE4 推广合集：发行且推广上传过 logo 的，在所有钱包的 NFT 列表都展示。
        // 本钱包已持有（持仓数据里已有）的保持真实数量，不重复添加；
        // 未推广的合集不会出现在这里，只能随 `nft/assets` 持仓在本钱包展示。
        if (uiState.tab == NftListTab.All) {
            promotedSafe4.forEach { token ->
                val key = BlockchainType.SafeFour to token.address.lowercase()
                if (items.containsKey(key)) return@forEach
                items[key] = NftCollectionViewItem(
                    blockchainType = BlockchainType.SafeFour,
                    contractAddress = token.address,
                    name = token.displayName ?: token.address.take(10),
                    count = token.totalAssets ?: 0,
                    sampleTokenId = "",
                    imageUrl = token.logoURI
                )
            }
        }

        // 已解析的合约 logo（本地文件）作为 SAFE4 合集图标，优先级最高
        items.keys.toList().forEach { key ->
            if (key.first != BlockchainType.SafeFour) return@forEach
            safe4LogoPaths[key.second]?.let { path ->
                items[key] = items[key]!!.copy(localLogoPath = path)
            }
        }

        var collections = items.values.sortedByDescending { it.count }

        // 收藏 Tab 只显示已收藏的合集（收藏按账户隔离，需按当前账户读取）
        if (uiState.tab == NftListTab.Favorites) {
            val favorites = currentAccount?.let { NftFavoritesStorage.all(it) } ?: emptySet()
            collections = collections.filter { item ->
                NftFavoritesStorage.composeKey(item.blockchainType.uid, item.contractAddress) in favorites
            }
        }

        uiState = uiState.copy(
            viewState = ViewState.Success,
            collections = collections,
            syncing = false
        )

        // 异步解析每个集合的代表图片
        viewModelScope.launch(Dispatchers.IO) {
            collections.forEach { item ->
                // SAFE4 需始终尝试读取合约 logo（用户付费上传的图标应当覆盖其他来源）
                if (item.imageUrl == null || item.blockchainType == BlockchainType.SafeFour) {
                    resolveCollectionImage(item)
                }
            }
        }

        // 异步解析 SAFE4 合集名称（需读合约 name()，不能在主线程做）
        viewModelScope.launch(Dispatchers.IO) {
            collections
                .filter { it.blockchainType == BlockchainType.SafeFour }
                .forEach { resolveSafe4CollectionName(it) }
        }
    }

    /**
     * SAFE4 合集的规范名称来自 insight 的 `nft/tokens` 接口。
     * 链上记录里的 tokenName 通常已带名称，这里只处理仍是地址缩写的条目。
     */
    private suspend fun resolveSafe4CollectionName(item: NftCollectionViewItem) {
        if (!item.name.equals(item.contractAddress.take(10), true)) return

        val name = Safe4NftAssetsService.fetchCollectionName(item.contractAddress)
            ?: return

        uiState = uiState.copy(
            collections = uiState.collections.map {
                if (it.blockchainType == BlockchainType.SafeFour &&
                    it.contractAddress.equals(item.contractAddress, true)
                ) {
                    it.copy(name = name)
                } else it
            }
        )
    }

    private suspend fun resolveCollectionImage(item: NftCollectionViewItem) {
        // SAFE4 优先展示合约自身的 logo：它由用户在合约上付费上传，是最权威的来源
        if (item.blockchainType == BlockchainType.SafeFour) {
            val path = SRC721LogoProvider.fetchPath(item.contractAddress)
            if (path != null) {
                safe4LogoPaths[item.contractAddress.lowercase()] = path
                uiState = uiState.copy(
                    collections = uiState.collections.map {
                        if (it.blockchainType == item.blockchainType &&
                            it.contractAddress.equals(item.contractAddress, true)
                        ) {
                            it.copy(localLogoPath = path)
                        } else it
                    }
                )
                return
            }

            // 合约未设置 logo 时，回退到 insight `nft/tokens` 下发的 logoURI
            val logo = Safe4NftAssetsService.collectionLogo(item.contractAddress)
            if (!logo.isNullOrBlank()) {
                uiState = uiState.copy(
                    collections = uiState.collections.map {
                        if (it.blockchainType == item.blockchainType &&
                            it.contractAddress.equals(item.contractAddress, true)
                        ) {
                            it.copy(imageUrl = logo)
                        } else it
                    }
                )
                return
            }
        }

        try {
            val record = currentRecords().filterIsInstance<EvmNftRecord>().firstOrNull {
                it.blockchainType == item.blockchainType &&
                        it.contractAddress.equals(item.contractAddress, true) &&
                        it.tokenId == item.sampleTokenId
            } ?: return

            val meta = metadataResolver.resolve(record.nftUid, record.nftType) ?: return
            val imageUrl = meta.imageUrl ?: return

            val updated = uiState.collections.map {
                if (it.blockchainType == item.blockchainType && it.contractAddress.equals(item.contractAddress, true)) {
                    it.copy(imageUrl = imageUrl)
                } else it
            }
            uiState = uiState.copy(collections = updated)
        } catch (e: Throwable) {
            // ignore
        }
    }

    private fun currentRecords(): List<NftRecord> {
        return nftAdapterManager.adaptersUpdatedFlow.value.values.flatMap { it.nftRecords }
    }

    fun refresh() {
        uiState = uiState.copy(syncing = true)
        nftAdapterManager.refresh()
        nftMetadataSyncer.refresh()
        // 若 3 秒内没有数据更新，结束刷新状态
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (uiState.syncing) {
                uiState = uiState.copy(syncing = false, viewState = ViewState.Success)
            }
        }
    }

    fun onTabChange(tab: NftListTab) {
        uiState = uiState.copy(tab = tab)
        rebuildItems()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NftCollectionListViewModel(
                App.nftAdapterManager,
                App.nftMetadataResolver,
                App.nftMetadataManager,
                App.nftMetadataSyncer
            ) as T
        }
    }
}
