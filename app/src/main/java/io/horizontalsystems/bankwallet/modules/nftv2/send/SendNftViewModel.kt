package io.horizontalsystems.bankwallet.modules.nftv2.send

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.nft.INftAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.getEthereumKitAddress
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nftkit.models.NftType
import java.math.BigInteger

data class SendNftUiState(
    val address: Address? = null,
    val proceedEnabled: Boolean = false,
)

class SendNftViewModel(
    private val blockchainType: BlockchainType,
    private val contractAddress: String,
    private val tokenId: String,
    private val nftType: NftType,
) : ViewModel() {

    val tokenQuery = TokenQuery(blockchainType, TokenType.Native)
    val coinCode: String = try {
        App.marketKit.token(tokenQuery)?.coin?.code ?: ""
    } catch (e: Throwable) {
        ""
    }

    var uiState by mutableStateOf(SendNftUiState())
        private set

    private val nftAdapter: INftAdapter?
        get() {
            val account = App.accountManager.activeAccount ?: return null
            return App.nftAdapterManager.adapter(NftKey(account, blockchainType))
        }

    fun onAddressChange(address: Address?) {
        uiState = uiState.copy(
            address = address,
            proceedEnabled = address != null
        )
    }

    fun buildTransactionData(): TransactionData? {
        val adapter = nftAdapter ?: return null
        val to = uiState.address.getEthereumKitAddress() ?: return null

        return when (nftType) {
            NftType.Eip721 -> adapter.transferEip721TransactionData(contractAddress, to, tokenId)
            NftType.Eip1155 -> adapter.transferEip1155TransactionData(
                contractAddress, to, tokenId, BigInteger.ONE
            )
        }
    }

    class Factory(
        private val blockchainType: BlockchainType,
        private val contractAddress: String,
        private val tokenId: String,
        private val nftType: NftType,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendNftViewModel(blockchainType, contractAddress, tokenId, nftType) as T
        }
    }
}
