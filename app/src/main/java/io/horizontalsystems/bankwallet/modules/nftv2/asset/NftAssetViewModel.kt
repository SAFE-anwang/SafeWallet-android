package io.horizontalsystems.bankwallet.modules.nftv2.asset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.NftAdapterManager
import io.horizontalsystems.bankwallet.core.providers.nft.NftEventsProvider
import io.horizontalsystems.bankwallet.core.providers.nft.NftMetadataResolver
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class NftEventViewItem(
    val type: String,
    val date: String?,
    val amount: String?,
)

data class NftAssetUiState(
    val loading: Boolean = true,
    val name: String = "",
    val imageUrl: String? = null,
    val tokenId: String = "",
    val contractAddress: String = "",
    val collectionName: String = "",
    val balance: Int = 0,
    val nftType: NftType = NftType.Eip721,
    val recordExists: Boolean = false,
    val description: String? = null,
    val events: List<NftEventViewItem> = emptyList(),
)

class NftAssetViewModel(
    private val blockchainType: BlockchainType,
    private val contractAddress: String,
    private val tokenId: String,
    collectionName: String,
    private val nftAdapterManager: NftAdapterManager,
    private val metadataResolver: NftMetadataResolver,
    private val nftEventsProvider: NftEventsProvider,
) : ViewModel() {

    private val nftUid = NftUid.Evm(blockchainType, contractAddress, tokenId)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    var uiState by mutableStateOf(
        NftAssetUiState(
            tokenId = tokenId,
            contractAddress = contractAddress,
            collectionName = collectionName,
            name = "#$tokenId"
        )
    )
        private set

    var record: EvmNftRecord? = null
        private set

    init {
        viewModelScope.launch {
            nftAdapterManager.adaptersUpdatedFlow.collect { adaptersMap ->
                val adapter = adaptersMap.entries
                    .firstOrNull { it.key.blockchainType == blockchainType }?.value
                val rec = adapter?.nftRecord(nftUid) as? EvmNftRecord
                if (rec != null) {
                    record = rec
                    uiState = uiState.copy(
                        loading = false,
                        recordExists = true,
                        balance = rec.balance,
                        nftType = rec.nftType,
                        name = rec.tokenName?.let { "$it #${rec.tokenId}" } ?: "#${rec.tokenId}"
                    )
                    resolveMetadata(rec)
                } else {
                    uiState = uiState.copy(loading = false, recordExists = false)
                }
            }
        }
        loadEvents()
    }

    private fun resolveMetadata(record: EvmNftRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val meta = metadataResolver.resolve(record.nftUid, record.nftType) ?: return@launch
            uiState = uiState.copy(
                name = meta.name ?: uiState.name,
                imageUrl = meta.imageUrl ?: uiState.imageUrl,
                description = meta.description ?: uiState.description
            )
        }
    }

    private fun loadEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (events, _) = nftEventsProvider.assetEventsMetadata(nftUid, null, null)
                val items = events.map { event ->
                    NftEventViewItem(
                        type = event.eventType?.name ?: "",
                        date = event.date?.let { dateFormat.format(it) },
                        amount = event.amount?.let { price ->
                            "${price.value.stripTrailingZeros().toPlainString()} ${price.token.coin.code}"
                        }
                    )
                }
                uiState = uiState.copy(events = items)
            } catch (e: Throwable) {
                // 无事件数据源时保持空列表
            }
        }
    }

    class Factory(
        private val blockchainType: BlockchainType,
        private val contractAddress: String,
        private val tokenId: String,
        private val collectionName: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NftAssetViewModel(
                blockchainType,
                contractAddress,
                tokenId,
                collectionName,
                App.nftAdapterManager,
                App.nftMetadataResolver,
                NftEventsProvider(App.marketKit)
            ) as T
        }
    }
}
