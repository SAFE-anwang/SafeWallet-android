package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType

class SafeFourModule {

    class Factory(val address: Address, val nodeType: Int, val title: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val safeFourProvider = SafeFourProvider("")
            val service = SafeFourNodeService(NodeType.getType(nodeType), safeFourProvider, address)
            return SafeFourNodeViewModel(title, service) as T
        }
    }

    data class SafeFourNodeUiState(
            val title: String,
            val nodeList: List<NodeViewItem>?
    )

    data class SafeFourCreateNodeUiState(
            val title: String,
            val addressError: Throwable?,
            val canSend: Boolean
    )

    companion object {
        const val Title_Key = "title"
        const val Node_Type = "nodeType"
    }
}