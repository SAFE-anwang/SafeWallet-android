package io.horizontalsystems.bankwallet.modules.nftv2.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.NftAdapterManager
import io.horizontalsystems.bankwallet.core.providers.nft.NftMetadataResolver
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
)

class NftCollectionViewModel(
    private val blockchainType: BlockchainType,
    private val contractAddress: String,
    collectionName: String,
    private val nftAdapterManager: NftAdapterManager,
    private val metadataResolver: NftMetadataResolver,
) : ViewModel() {

    var uiState by mutableStateOf(
        NftCollectionUiState(collectionName = collectionName, contractAddress = contractAddress)
    )
        private set

    private var collectJob: Job? = null

    init {
        viewModelScope.launch {
            nftAdapterManager.adaptersUpdatedFlow.collect { adaptersMap ->
                subscribe(adaptersMap.keys.firstOrNull { it.blockchainType == blockchainType })
            }
        }
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
        val assets = records
            .filter { it.contractAddress.equals(contractAddress, true) }
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

        uiState = uiState.copy(viewState = ViewState.Success, assets = assets)

        // 异步解析图片
        viewModelScope.launch(Dispatchers.IO) {
            records.filter { it.contractAddress.equals(contractAddress, true) }
                .forEach { record ->
                    if (metadataResolver.cached(record.nftUid) == null) {
                        val meta = metadataResolver.resolve(record.nftUid, record.nftType)
                        if (meta != null) {
                            updateAssetMeta(record.tokenId, meta.name, meta.imageUrl)
                        }
                    }
                }
        }
    }

    private fun updateAssetMeta(tokenId: String, name: String?, imageUrl: String?) {
        val updated = uiState.assets.map {
            if (it.tokenId == tokenId) {
                it.copy(
                    name = name ?: it.name,
                    imageUrl = imageUrl ?: it.imageUrl
                )
            } else it
        }
        uiState = uiState.copy(assets = updated)
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
                App.nftMetadataResolver
            ) as T
        }
    }
}
