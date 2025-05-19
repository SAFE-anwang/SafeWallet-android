package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.anwang.types.safe3.AvailableSafe3Info
import com.anwang.types.safe3.LockedSafe3Info
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.confirmation.SafeFourConfirmationModule
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourLockedVoteService
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.proposal.WithdrawAvailableViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.vote.WithdrawVoteViewModel
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
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
					WithdrawNodeViewModel(
						adapterEvm.evmKitWrapper.evmKit,
						isSuperNode,
						service, App.connectivityManager) as T
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
						lockVoteService,
						App.connectivityManager
					) as T
				}
				LockedInfoViewModel::class.java -> {
					val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
					val service = WithdrawService(rpcBlockchainSafe4, adapterEvm.evmKitWrapper)
					val lockVoteService = SafeFourLockedVoteService(rpcBlockchainSafe4,
						adapterEvm.evmKitWrapper.evmKit,
						adapterEvm.evmKitWrapper.evmKit.receiveAddress)
					LockedInfoViewModel(
						wallet,
						adapterEvm.evmKitWrapper.evmKit,
						service,
						lockVoteService,
						App.connectivityManager
					) as T
				}
				WithdrawAvailableViewModel::class.java -> {
					val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
					val service = WithdrawService(rpcBlockchainSafe4, adapterEvm.evmKitWrapper)
					WithdrawAvailableViewModel(
						adapterEvm.evmKitWrapper.evmKit.receiveAddress,
						service,
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
		val showConfirmDialog: Boolean = false
	)

	data class WithDrawLockInfoUiState(
		val list: List<WithDrawLockedInfo>?,
		val showConfirmDialog: Boolean = false
	)

	data class WithDrawInfo(
		val id: Int,
		val height: Long?,
		val amount: String,
		val address: String?,
		val enable: Boolean,
		var checked: Boolean = false
	)

	data class WithDrawLockedInfo(
		val id: Int,
		val height: Long?,
		val amount: String,
		val address: String?,
		val address2: String?,
		var withdrawEnable: Boolean = false,
		var addLockDayEnable: Boolean? = false
	)

	@Parcelize
	data class Input(
		val isSuperNode: Boolean,
		val wallet: Wallet
	) : Parcelable {
	}

}