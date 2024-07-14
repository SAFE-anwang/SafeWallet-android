package io.horizontalsystems.bankwallet.modules.safe4.node.confirmation

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

class SafeFourConfirmationModule {

    class Factory(val title: String, val isSuper: Boolean, val wallet: Wallet, val createNodeData: CreateNodeData) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val evmKitWrapper =App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).getEvmKitWrapper(
                    wallet.account,
                    BlockchainType.SafeFour
            )
            return SafeFourCreateNodeConfirmationViewModel(title, isSuper, wallet, createNodeData, evmKitWrapper) as T
        }
    }


    data class SafeFourCreateNodeConfirmationUiState(
            val title: String
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
            val data: CreateNodeData,
            val wallet: Wallet,
            val sendNavId: Int,
            val sendEntryPointDestId: Int
    ) : Parcelable {
    }
}