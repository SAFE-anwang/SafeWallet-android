package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfoRepository
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeType
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalRecordRepository
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalService
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourLockedVoteService
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.proposal.WithdrawAvailableViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.vote.WithdrawVoteViewModel
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

object WithdrawModule {

	class Factory(val isSuperNode: Boolean, val wallet: Wallet) : ViewModelProvider.Factory {

		val adapterEvm = (App.adapterManager.getAdapterForWallet(wallet) as? BaseEvmAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return when (modelClass) {
				WithdrawNodeViewModel::class.java -> {
					val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
					val service = WithdrawService(rpcBlockchainSafe4, adapterEvm.evmKitWrapper)
					val nodeService = SafeFourNodeService(
						NodeType.getType(if (isSuperNode) 0 else 1), rpcBlockchainSafe4, adapterEvm.evmKitWrapper.evmKit.receiveAddress)
					WithdrawNodeViewModel(
						adapterEvm.evmKitWrapper.evmKit,
						isSuperNode,
						service,
						nodeService,
						App.connectivityManager) as T
				}

				WithdrawVoteViewModel::class.java -> {
					val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
					val service = WithdrawService(rpcBlockchainSafe4, adapterEvm.evmKitWrapper)
					val lockVoteService = SafeFourLockedVoteService(rpcBlockchainSafe4,
						adapterEvm.evmKitWrapper.evmKit,
						adapterEvm.evmKitWrapper.evmKit.receiveAddress)
					WithdrawVoteViewModel(
						adapterEvm.evmKitWrapper.evmKit,
						service,
						LockRecordInfoRepository(App.appDatabase.lockRecordDao()),
						lockVoteService,
						App.connectivityManager
					) as T
				}
				LockedInfoViewModel::class.java -> {
					val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
					val service = WithdrawService(rpcBlockchainSafe4, adapterEvm.evmKitWrapper)
					LockedInfoViewModel(
						wallet,
						adapterEvm.evmKitWrapper.evmKit,
						service,
						LockRecordInfoRepository(App.appDatabase.lockRecordDao()),
						App.connectivityManager
					) as T
				}
				WithdrawAvailableViewModel::class.java -> {
					val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
					val service = WithdrawService(rpcBlockchainSafe4, adapterEvm.evmKitWrapper)
					val serviceProposal = SafeFourProposalService(rpcBlockchainSafe4,
						adapterEvm.evmKitWrapper, ProposalRecordRepository(App.appDatabase.proposalRecordDao()) ,true)
					WithdrawAvailableViewModel(
						adapterEvm.evmKitWrapper.evmKit.receiveAddress,
						service,
						serviceProposal,
						App.connectivityManager
					) as T
				}
				else -> throw IllegalArgumentException()
			}

		}
	}


	data class WithDrawNodeUiState(
		val list: List<WithDrawInfo>?,
		val enableWithdraw: Boolean = false,
		val showConfirmDialog: Boolean = false,
		val enableReleaseAll: Boolean = false
	)

	data class WithDrawLockInfoUiState(
		val list: List<WithDrawLockedInfo>?,
		val showConfirmDialog: Boolean = false,
		val canWithdrawAll: Boolean = false
	)

	data class WithDrawInfo(
		val id: Long,
		val height: Long?,
		val releaseHeight: Long?,
		val amount: String,
		val address: String?,
		val enable: Boolean,
		var checked: Boolean = false
	)

	data class WithDrawLockedInfo(
		val id: Long,
		val unlockHeight: Long?,
		val releaseHeight: Long?,
		val amount: String,
		val value: BigInteger,
		val address: String?,
		val address2: String?,
		val frozenAddr: String?,
		var withdrawEnable: Boolean = false,
		var addLockDayEnable: Boolean? = false,
		var contract: String = "",
		val type: Int = 0
	)

	@Parcelize
	data class Input(
		val isSuperNode: Boolean,
		val wallet: Wallet
	) : Parcelable {
	}

}