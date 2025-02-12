package io.horizontalsystems.bankwallet.modules.safe4.node

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.safe4.node.addlockday.AddLockDayViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

class SafeFourModule {

    class Factory(val address: Address, val nodeType: Int, val title: String, val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val safeFourProvider = SafeFourProvider(App.appConfigProvider.safe4Api)
            val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
            val service = SafeFourNodeService(NodeType.getType(nodeType), rpcBlockchainSafe4, safeFourProvider, address)
            val isSuperNode = nodeType == NodeType.SuperNode.ordinal
            return SafeFourNodeViewModel(wallet, title, service, isSuperNode, adapter.evmKitWrapper.evmKit) as T
        }
    }


    class FactoryEdit(
            private val wallet: Wallet,
            private val nodeType: Int,
            private val name: String,
            private val enode: String,
            private val address: String,
            private val desc: String,
    ) : ViewModelProvider.Factory {
        val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val evmKitWrapper =App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).getEvmKitWrapper(
                    wallet.account,
                    BlockchainType.SafeFour
            )
            val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
            val isSuperNode = nodeType == NodeType.SuperNode.ordinal

            val addressService = SendEvmAddressService(adapter.evmKitWrapper.evmKit.receiveAddress.hex)

            return SafeFourNodeEditViewModel(
                    wallet,
                    isSuperNode,
                    name,
                    address,
                    enode,
                    desc,
                    rpcBlockchainSafe4,
                    addressService,
                    evmKitWrapper.signer!!.privateKey.toHexString(),
                    evmKitWrapper.evmKit.receiveAddress.hex
            ) as T
        }
    }



    class FactoryAddLockDay(
            private val wallet: Wallet,
            private val lockId: List<Int>
    ) : ViewModelProvider.Factory {
        val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val evmKitWrapper =App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).getEvmKitWrapper(
                    wallet.account,
                    BlockchainType.SafeFour
            )
            val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4

            return AddLockDayViewModel(
                    lockId,
                    rpcBlockchainSafe4,
                    evmKitWrapper.signer!!.privateKey.toHexString()
            ) as T
        }
    }

    data class AddLockDayUiState(
          val maxLockDay: Int,
          val addEnable: Boolean = false,
          val recordList: List<AddLockRecordInfo> = listOf(),
          val error: Int? = null,
          val errorMaxDay: Int? = null,
          val showConfirmationDialog: Boolean = false
    )

    data class AddLockRecordInfo(
            val recordId: Long,
            val lockedDay: Int,
            val maxLockDay: Int,
            val enable: Boolean = false,
    )

    data class SafeFourNodeUiState(
            val title: String,
            val nodeList: List<NodeViewItem>?,
            val mineList: List<NodeViewItem>?,
            val isRegisterNode: Pair<Boolean, Boolean> = Pair(false, false)
    )

    companion object {
        const val Title_Key = "title"
        const val Node_Type = "nodeType"
    }

    @Parcelize
    data class Input(
            val titleRes: Int,
            val nodeType: Int,
            val wallet: Wallet
    ): Parcelable {

    }

    @Parcelize
    data class AddLockDayInput(
            val lockId: List<Int>,
            val wallet: Wallet
    ): Parcelable {

    }

    @Parcelize
    data class CreateInput(
            val wallet: Wallet,
            val isSuper: Boolean
    ): Parcelable {

    }

}