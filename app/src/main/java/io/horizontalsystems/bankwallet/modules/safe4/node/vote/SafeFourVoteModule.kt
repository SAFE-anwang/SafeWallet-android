package io.horizontalsystems.bankwallet.modules.safe4.node.vote

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeType
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeInfoViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.models.Address
import java.math.RoundingMode

object SafeFourVoteModule {

	class Factory(
			private val wallet: Wallet,
			private val nodeId: Int,
			private val address: Address,
			private val nodeAddress: String,
			private val nodeType: Int,
			private val isJoin: Boolean
	) : ViewModelProvider.Factory {
		val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			val amountValidator = AmountValidator()
			val coinMaxAllowedDecimals = wallet.token.decimals

			val amountService = SendAmountService(
					amountValidator,
					wallet.token.coin.code,
					adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
					wallet.token.type.isNative
			)
			val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

			val safeFourProvider = SafeFourProvider("https://safe4.anwang.com/api/")
			val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
			val service = SafeFourNodeService(NodeType.getType(nodeType), rpcBlockchainSafe4, safeFourProvider, address)
			val lockVoteService = SafeFourLockedVoteService(rpcBlockchainSafe4,  adapter.evmKitWrapper.evmKit, nodeAddress, address)
			val isSuperNode = nodeType == NodeType.SuperNode.ordinal
			return SafeFourVoteViewModel(
					wallet,
					wallet.token,
					nodeId,
					isSuperNode,
					isJoin,
					adapter,
					service,
					lockVoteService,
					xRateService,
					amountService,
					coinMaxAllowedDecimals,
					App.connectivityManager
			) as T
		}
	}

	class FactoryInfo(
			private val wallet: Wallet,
			private val nodeId: Int,
			private val address: Address,
			private val nodeType: Int,
	) : ViewModelProvider.Factory {
		val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {

			val safeFourProvider = SafeFourProvider("https://safe4.anwang.com/api/")
			val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
			val service = SafeFourNodeService(NodeType.getType(nodeType), rpcBlockchainSafe4, safeFourProvider, address)
			val isSuperNode = nodeType == NodeType.SuperNode.ordinal
			return SafeFourNodeInfoViewModel(
					wallet,
					nodeId,
					isSuperNode,
					service,
			) as T
		}
	}

	class FactoryRecord(
			private val wallet: Wallet,
			private val nodeAddress: String,
			private val isSuperNode: Boolean,
			private val nodeId: Int
	) : ViewModelProvider.Factory {
		val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {

			val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
			val voteRecordService = SafeFourVoteRecordService(isSuperNode, rpcBlockchainSafe4, nodeAddress, nodeId)
			return SafeFourVoteRecordViewModel(
					voteRecordService
			) as T
		}
	}

	enum class Tab(@StringRes val titleResId: Int) {
		SafeVote(R.string.Safe_Four_Node_Vote_Tab_Safe_Vote),
		LockVote(R.string.Safe_Four_Node_Vote_Tab_Lock_Vote),
		Creator(R.string.Safe_Four_Node_Vote_Tab_Creator),
		Voters(R.string.Safe_Four_Node_Vote_Tab_Voter);
	}

	enum class TabInfo(@StringRes val titleResId: Int) {
		Info(R.string.Safe_Four_Node_Info),
		Creator(R.string.Safe_Four_Node_Vote_Tab_Creator),
		Voters(R.string.Safe_Four_Node_Vote_Tab_Voter);
	}
}