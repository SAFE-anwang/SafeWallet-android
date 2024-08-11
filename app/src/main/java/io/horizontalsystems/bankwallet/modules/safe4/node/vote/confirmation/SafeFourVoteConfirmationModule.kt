package io.horizontalsystems.bankwallet.modules.safe4.node.vote.confirmation

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.LockIdsView
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.VoteData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

class SafeFourVoteConfirmationModule {

    class Factory(val title: String, val isSuper: Boolean, val wallet: Wallet, val createNodeData: VoteData) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            val evmKitWrapper = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).getEvmKitWrapper(
                    wallet.account,
                    BlockchainType.SafeFour
            )
            val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4

            return SafeFourVoteConfirmationViewModel(title, isSuper, wallet, createNodeData, rpcBlockchainSafe4, evmKitWrapper) as T
        }
    }


    data class SafeFourVoteConfirmationUiState(
            val title: String,
            val voteNum: String,
            val lockIdInfo: List<LockIdsView>? = null,
    )

    companion object {
        const val Title_Key = "title"
        const val Node_Type = "nodeType"
    }

    @Parcelize
    data class CreateNodeData(
            val value: BigInteger,
            val isUnion: Boolean,
            val address: String,
            val lockDay: BigInteger,
            val name: String,
            val enode: String,
            val description: String,
            val creatorIncentive: BigInteger,
            val partnerIncentive: BigInteger,
            val voterIncentive: BigInteger
    ): Parcelable {
    }

    @Parcelize
    data class Input(
            val title: String,
            val isSuper: Boolean,
            val data: VoteData,
            val wallet: Wallet,
            val sendEntryPointDestId: Int
    ) : Parcelable {
    }
}