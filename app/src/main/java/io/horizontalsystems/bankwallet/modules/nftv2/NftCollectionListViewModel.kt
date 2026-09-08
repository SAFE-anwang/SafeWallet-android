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
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.bankwallet.entities.nft.NftRecord
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
    }

    private fun subscribeToAdapters(adaptersMap: Map<NftKey, INftAdapter>) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
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

        var collections = items.values.sortedByDescending { it.count }

        // 收藏 Tab 只显示已收藏的合集
        if (uiState.tab == NftListTab.Favorites) {
            val favorites = NftFavoritesStorage.all()
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
                if (item.imageUrl == null) {
                    resolveCollectionImage(item)
                }
            }
        }
    }

    private suspend fun resolveCollectionImage(item: NftCollectionViewItem) {
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
