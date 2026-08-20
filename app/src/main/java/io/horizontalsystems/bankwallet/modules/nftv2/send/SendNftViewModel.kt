package io.horizontalsystems.bankwallet.modules.nftv2.send

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.nft.INftAdapter
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.models.NftType
import java.math.BigInteger

data class SendNftUiState(
    val address: String = "",
    val addressError: String? = null,
    val proceedEnabled: Boolean = false,
)

class SendNftViewModel(
    private val blockchainType: BlockchainType,
    private val contractAddress: String,
    private val tokenId: String,
    private val nftType: NftType,
) : ViewModel() {

    var uiState by mutableStateOf(SendNftUiState())
        private set

    private val nftAdapter: INftAdapter?
        get() {
            val account = App.accountManager.activeAccount ?: return null
            return App.nftAdapterManager.adapter(NftKey(account, blockchainType))
        }

    fun onAddressChange(value: String) {
        val trimmed = value.trim()
        val valid = isValidAddress(trimmed)
        uiState = uiState.copy(
            address = trimmed,
            addressError = if (trimmed.isEmpty() || valid) null else "Invalid address",
            proceedEnabled = valid
        )
    }

    private fun isValidAddress(address: String): Boolean {
        if (address.isEmpty()) return false
        return try {
            io.horizontalsystems.ethereumkit.models.Address(address)
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun buildTransactionData(): TransactionData? {
        val adapter = nftAdapter ?: return null
        val to = try {
            io.horizontalsystems.ethereumkit.models.Address(uiState.address)
        } catch (e: Throwable) {
            return null
        }

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
