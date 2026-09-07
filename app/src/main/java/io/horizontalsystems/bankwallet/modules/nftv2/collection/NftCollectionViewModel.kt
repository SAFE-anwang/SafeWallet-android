package io.horizontalsystems.bankwallet.modules.nftv2.collection

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
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.bankwallet.entities.nft.NftUid
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
    val description: String? = null,
    val standard: String = "ERC721",
    val floorPrice24h: String = "0",
    val averagePrice24h: String = "0",
    val volume24h: String = "0",
    val baseToken: Token? = null,
)

class NftCollectionViewModel(
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
            }
        )
    )
        private set

    private var collectJob: Job? = null
    private var nftKey: NftKey? = null
    private var ownAssets: List<NftAssetViewItem> = emptyList()
    private var availableAssets: List<NftAssetViewItem> = emptyList()

    init {
        viewModelScope.launch {
            nftAdapterManager.adaptersUpdatedFlow.collect { adaptersMap ->
                nftKey = adaptersMap.keys.firstOrNull { it.blockchainType == blockchainType }
                subscribe(nftKey)
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
    }

    private fun subscribe(nftKey: NftKey?) {
        collectJob?.cancel()
        val adapter = nftKey?.let { nftAdapterManager.adapter(it) }
        collectJob = viewModelScope.launch {
            adapter?.nftRecordsFlow?.collect { records ->
                emitAssets(records.filterIsInstance<EvmNftRecord>())
            } ?: emitAssets(emptyList())
        }
    }

    private fun emitAssets(records: List<EvmNftRecord>) {
        val filtered = records.filter { it.contractAddress.equals(contractAddress, true) }
        if (filtered.isEmpty()) {
            // 链上无记录时回退到 OpenSea 数据
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
                    name = cached?.name ?: record.tokenName?.let { "$it #${record.tokenId}" } ?: "#${record.tokenId}",
                    imageUrl = cached?.imageUrl,
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

    private fun emitOpenSeaAssets(metadata: NftAddressMetadata) {
        ownAssets = metadata.assets
            .filter { it.nftUid.contractAddress.equals(contractAddress, true) }
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

                // 对缺少图片或名称的条目，解析链上 tokenURI 补充；
                // 解析失败或解析结果全空的视为无效数据移除
                assets.filter { it.imageUrl == null || it.name == null }.map { asset ->
                    async(Dispatchers.IO) {
                        val nftUid = NftUid.Evm(blockchainType, contractAddress, asset.tokenId)
                        val meta = metadataResolver.resolve(nftUid, NftType.Eip721)
                        if (meta != null && (meta.name != null || meta.imageUrl != null)) {
                            updateAssetMeta(asset.tokenId, meta.name, meta.imageUrl, meta.description)
                        } else {
                            removeAvailableAsset(asset.tokenId)
                        }
                    }
                }.awaitAll()
            }
        }
    }

    private fun rebuildAssets(standard: String? = null) {
        val ownedIds = ownAssets.map { it.tokenId }.toSet()
        val merged = ownAssets + availableAssets
            .filter { it.tokenId !in ownedIds }
            .sortedBy { it.tokenId.toBigIntegerOrNull() }

        uiState = uiState.copy(
            viewState = ViewState.Success,
            assets = merged,
            standard = standard ?: uiState.standard,
            iconUrl = uiState.iconUrl ?: merged.firstNotNullOfOrNull { it.imageUrl }
        )
    }

    private fun removeAvailableAsset(tokenId: String) {
        availableAssets = availableAssets.filterNot { it.tokenId == tokenId }
        rebuildAssets()
    }

    private fun updateAssetMeta(tokenId: String, name: String?, imageUrl: String?, description: String?) {
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

    private fun standardName(nftType: NftType): String = when (nftType) {
        NftType.Eip721 -> "ERC721"
        NftType.Eip1155 -> "ERC1155"
    }

    class Factory(
        private val blockchainType: BlockchainType,
        private val contractAddress: String,
        private val collectionName: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NftCollectionViewModel(
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
