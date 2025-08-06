package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedException
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.TotalSupply
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import kotlinx.parcelize.Parcelize

object SRC20Module {

    class Factory(val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter =
                (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter)
                    ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            return when(modelClass) {
                SRC20DeployViewModel::class.java -> {

                    val rpcBlockchainSafe4 =
                        adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val service = SRC20Service(DeployType.SRC20, rpcBlockchainSafe4.web3j)
                    SRC20DeployViewModel(service, adapter.evmKitWrapper) as T
                }
                SRC20ManagerViewModel::class.java -> {
                    val rpcBlockchainSafe4 =
                        adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val service = SRC20Service(DeployType.SRC20, rpcBlockchainSafe4.web3j)
                    val tokenService = SyncSafe4TokensService(service, adapter.evmKitWrapper.evmKit)
                    SRC20ManagerViewModel(service, tokenService) as T
                }
                else -> {
                    throw  UnsupportedException(modelClass.name)
                }
            }
        }
    }


    class FactoryEdit(val wallet: Wallet, val token: CustomToken) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter =
                (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter)
                    ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            return when(modelClass) {
                SRC20EditViewModel::class.java -> {
                    val rpcBlockchainSafe4 =
                        adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val service = SRC20Service(token.getDeployType(), rpcBlockchainSafe4.web3j, token.address)

                    SRC20EditViewModel(DeployType.valueOf(token.type), service, adapter.evmKitWrapper) as T
                }
                SRC20PromotionViewModel::class.java -> {
                    val rpcBlockchainSafe4 =
                        adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val service = SRC20Service(token.getDeployType(), rpcBlockchainSafe4.web3j, token.address)

                    SRC20PromotionViewModel(DeployType.valueOf(token.type), service, adapter.evmKitWrapper) as T
                }
                SRC20AdditionalViewModel::class.java -> {
                    val rpcBlockchainSafe4 =
                        adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val service = SRC20Service(token.getDeployType(), rpcBlockchainSafe4.web3j, token.address)

                    SRC20AdditionalViewModel(token.getTypeForVersion(),token.symbol, token.creator,  service, adapter.evmKitWrapper) as T
                }
                SRC20DestroyViewModel::class.java -> {
                    val rpcBlockchainSafe4 =
                        adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val service = SRC20Service(token.getDeployType(), rpcBlockchainSafe4.web3j, token.address)
                    SRC20DestroyViewModel(token.getTypeForVersion(), token.symbol, token.address, service, adapter.evmKitWrapper, adapter.balanceData.total) as T
                }
                else -> {
                    throw  UnsupportedException(modelClass.name)
                }
            }
        }
    }


    @Parcelize
    data class Input(
        val wallet: Wallet
    ) : Parcelable {
    }

    @Parcelize
    data class InputEdit(
        val wallet: Wallet,
        val customToken: CustomToken
    ) : Parcelable {
    }

}


data class DeployUiState(
    val type: DeployType,
    val deplopDesc: Int,
    val proceedEnabled: Boolean,
    val showConfirmationDialog: Boolean,
)


data class DeployManagerUiState(
    val list: List<MangerItem>? = null,
)

data class DeployEditUIState(
    val orgName: String? = null,
    val whitePaperUrl: String? = null,
    val officialUrl: String? = null,
    val description: String? = null,
    val canUpdate: Boolean,
    val showConfirmationDialog: Boolean
)

data class DeployPromotionUIState(
    val fee: String? = null,
    val canUpdate: Boolean,
    val showConfirmationDialog: Boolean
)

data class DeployAdditionalUIState(
    val totalSupply: String,
    val balance: String,
    val canUpdate: Boolean,
    val showConfirmationDialog: Boolean
)

data class DeployDestroyUIState(
    val totalSupply: String,
    val balance: String,
    val canUpdate: Boolean,
    val showConfirmationDialog: Boolean
)

data class MangerItem(
    val token: CustomToken,
    val canAdditionalIssuance: Boolean = false,
    val canDestroy: Boolean = false,
)


enum class DeployType(val type: Int) {
    SRC20(0),
    SRC20Mintable(1),
    SRC20Burnable(2);

    companion object {
        fun valueOf(value: Int): DeployType {
            return when (value) {
                0 -> SRC20
                1 -> SRC20Mintable
                else -> SRC20Burnable
            }
        }
    }

}