package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.storage.ProposalStateStorage
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create.SafeFourCreateProposalConfirmationViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create.SafeFourCreateProposalViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.ProposalInfoViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.ProposalVoteViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.SafeFourProposalInfoService
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info.SafeFourProposalInfoViewModel
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

class SafeFourProposalModule {

    class Factory(val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when(modelClass) {
                SafeFourProposalViewModel::class.java -> {
                    val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
                    val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
                    val evmKitWrapper =App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).getEvmKitWrapper(
                            wallet.account,
                            BlockchainType.SafeFour
                    )
                    val service = SafeFourProposalService(rpcBlockchainSafe4, evmKitWrapper)
                    SafeFourProposalViewModel(wallet, service) as T
                }
                SafeFourCreateProposalViewModel::class.java -> {
                    val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
                    val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4

                    SafeFourCreateProposalViewModel(wallet, rpcBlockchainSafe4) as T
                }
                else -> throw IllegalArgumentException()
            }


        }
    }

    class FactoryCreateProposal(val wallet: Wallet, val createProposalData: CreateProposalData) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
            val evmKitWrapper =App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).getEvmKitWrapper(
                    wallet.account,
                    BlockchainType.SafeFour
            )
            return SafeFourCreateProposalConfirmationViewModel(wallet, createProposalData, rpcBlockchainSafe4, evmKitWrapper) as T
        }
    }

    class FactoryInfo(val wallet: Wallet, val proposalInfo: ProposalInfo) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
            val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
            val evmKitWrapper =App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).getEvmKitWrapper(
                    wallet.account,
                    BlockchainType.SafeFour
            )
            val service = if (proposalInfo.state == 2) null else SafeFourProposalInfoService(proposalInfo.id, rpcBlockchainSafe4, evmKitWrapper)
            val storage = ProposalStateStorage(App.appDatabase)
            return SafeFourProposalInfoViewModel(wallet, evmKitWrapper.evmKit.receiveAddress.hex, proposalInfo, storage, service) as T
        }
    }

    data class SafeFourProposalUiState(
            val allProposalList: List<ProposalViewItem>?,
            val mineProposalList: List<ProposalViewItem>?
    )

    data class SafeFourProposalInfoUiState(
            val proposalInfo: ProposalInfoViewItem,
            val voteList: List<ProposalVoteViewItem>?,
            val showConfirmationDialog: Boolean,
            val isVoted: Boolean = false,
            val voteEnable: Boolean = false
    )

    data class SafeFourProposalConfirmationUiState(
            val title: String,
            val desc: String,
            val amount: String,
            val startDate: String,
            val endDate: String,
            val payTimes: String,
            val isOncePay: Boolean
    )

    data class SafeFourProposalCreateUiState(
            val balance: String,
            val canSend: Boolean
    )

    @Parcelize
    data class CreateProposalData(
            val payAmount: BigInteger,
            val payTimes: BigInteger,
            val startPayTime: Long,
            val endPayTime: Long,
            val title: String,
            val description: String,
    ): Parcelable {
    }

    @Parcelize
    data class CreateProposalInput(
            val wallet: Wallet,
            val data: CreateProposalData
    ): Parcelable {

    }

    @Parcelize
    data class Input(
            val wallet: Wallet
    ): Parcelable {

    }


    @Parcelize
    data class InfoInput(
            val wallet: Wallet,
            val proposalInfo: ProposalInfo?,
    ): Parcelable {

    }

    enum class Tab(@StringRes val titleResId: Int) {
        AllProposal(R.string.Safe_Four_Proposal_All),
        MineProposal(R.string.Safe_Four_Proposal_Mine);
    }
}