package io.horizontalsystems.bankwallet.modules.nftv2.src721

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedException
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import kotlinx.parcelize.Parcelize

object SRC721Module {

    class Factory(val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter =
                (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter)
                    ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            val web3j = (adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4).web3j
            return when (modelClass) {
                SRC721DeployViewModel::class.java -> {
                    SRC721DeployViewModel(SRC721Service(web3j), adapter.evmKitWrapper) as T
                }
                SRC721ManagerViewModel::class.java -> {
                    SRC721ManagerViewModel(web3j, adapter.evmKitWrapper) as T
                }
                else -> {
                    throw UnsupportedException(modelClass.name)
                }
            }
        }
    }

    @Parcelize
    data class Input(
        val wallet: Wallet
    ) : Parcelable

    @Parcelize
    data class MintInput(
        val wallet: Wallet,
        val contract: SRC721ContractInfo
    ) : Parcelable

    @Parcelize
    data class EditInput(
        val wallet: Wallet,
        val contract: SRC721ContractInfo
    ) : Parcelable

    class EditFactory(val input: EditInput) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter =
                (App.adapterManager.getAdapterForWallet(input.wallet) as? ISendEthereumAdapter)
                    ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            val web3j = (adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4).web3j
            return SRC721EditViewModel(input.contract, web3j, adapter.evmKitWrapper) as T
        }
    }

    class MintFactory(val input: MintInput) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter =
                (App.adapterManager.getAdapterForWallet(input.wallet) as? ISendEthereumAdapter)
                    ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            val web3j = (adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4).web3j
            return SRC721MintViewModel(
                input.contract,
                web3j,
                adapter.evmKitWrapper
            ) as T
        }
    }

    fun isValidDecimalInput(newInput: String, previousValue: String): Boolean {
        if (newInput.isEmpty()) return true
        if (!newInput.matches(Regex("^[0-9]*\\.?[0-9]*$"))) return false
        val dotCount = newInput.count { it == '.' }
        if (dotCount > 1) return false
        val parts = newInput.split('.')
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else ""
        if (integerPart.isNotEmpty() && integerPart.length > 20) return false
        if (decimalPart.isNotEmpty() && decimalPart.length > 8) return false
        if (integerPart.length > 1 && integerPart.startsWith("0") && !integerPart.startsWith("0.")) return false
        return true
    }
}

data class SRC721DeployUiState(
    val type: SRC721DeployType,
    val deployDesc: Int,
    val proceedEnabled: Boolean,
    val showConfirmationDialog: Boolean,
)

data class SRC721ManagerUiState(
    val list: List<SRC721ManagerItem> = emptyList(),
    val refreshing: Boolean = false,
)

data class SRC721ManagerItem(
    val info: SRC721ContractInfo,
    val totalSupply: String? = null,
    val remainSupply: String? = null,
    val loadFailed: Boolean = false,
)

enum class SRC721DeployType(val type: Int) {
    SRC721(0),
    SRC721Burnable(1);

    companion object {
        fun valueOf(value: Int): SRC721DeployType {
            return when (value) {
                0 -> SRC721
                else -> SRC721Burnable
            }
        }
    }
}
