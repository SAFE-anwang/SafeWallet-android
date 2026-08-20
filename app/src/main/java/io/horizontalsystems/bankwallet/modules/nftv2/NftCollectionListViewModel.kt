package io.horizontalsystems.bankwallet.modules.nftv2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.nft.INftAdapter
import io.horizontalsystems.bankwallet.core.managers.NftAdapterManager
import io.horizontalsystems.bankwallet.core.providers.nft.NftMetadataResolver
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
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

data class NftCollectionListUiState(
    val viewState: ViewState = ViewState.Loading,
    val collections: List<NftCollectionViewItem> = emptyList(),
    val syncing: Boolean = false,
)

class NftCollectionListViewModel(
    private val nftAdapterManager: NftAdapterManager,
    private val metadataResolver: NftMetadataResolver,
) : ViewModel() {

    var uiState by mutableStateOf(NftCollectionListUiState())
        private set

    private var collectJob: Job? = null

    init {
        viewModelScope.launch {
            nftAdapterManager.adaptersUpdatedFlow.collect { adaptersMap ->
                subscribeToAdapters(adaptersMap)
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

    private fun emitItems(records: List<NftRecord>) {
        val collections = records
            .filterIsInstance<EvmNftRecord>()
            .groupBy { it.blockchainType to it.contractAddress.lowercase() }
            .map { (key, group) ->
                val first = group.first()
                NftCollectionViewItem(
                    blockchainType = key.first,
                    contractAddress = first.contractAddress,
                    name = first.tokenName ?: first.contractAddress.take(10),
                    count = group.sumOf { it.balance },
                    sampleTokenId = first.tokenId,
                    imageUrl = null
                )
            }
            .sortedByDescending { it.count }

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
        // 若 3 秒内没有数据更新，结束刷新状态
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (uiState.syncing) {
                uiState = uiState.copy(syncing = false, viewState = ViewState.Success)
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NftCollectionListViewModel(
                App.nftAdapterManager,
                App.nftMetadataResolver
            ) as T
        }
    }
}
