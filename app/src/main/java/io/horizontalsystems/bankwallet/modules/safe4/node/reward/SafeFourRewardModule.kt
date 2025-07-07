package io.horizontalsystems.bankwallet.modules.safe4.node.reward

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.ProposalInfoViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.ProposalVoteViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.SafeFourProposalInfoService
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.SafeFourProposalInfoViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class SafeFourRewardModule {

    class Factory(val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val evmKitWrapper =App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).getEvmKitWrapper(
                    wallet.account,
                    BlockchainType.SafeFour
            )
            val safeFourProvider = SafeFourProvider(App.appConfigProvider.safe4Api)
            val rpcBlockchainSafe4 = evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
            val service = WithdrawService(rpcBlockchainSafe4, evmKitWrapper)
            return SafeFourRewardViewModel(
                evmKitWrapper.evmKit.receiveAddress.hex,
                safeFourProvider,
                service,
                App.connectivityManager
            ) as T
        }
    }



    data class SafeFourRewardUiState(
            val rewardList: List<RewardViewItem>?,
            val canWithdraw: Boolean = false,
        val showConfirmDialog: Boolean = false
    )


    @Parcelize
    data class Input(
            val wallet: Wallet
    ): Parcelable {

    }
}