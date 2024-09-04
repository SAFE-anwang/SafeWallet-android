package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.anwang.types.safe3.AvailableSafe3Info
import com.anwang.types.safe3.LockedSafe3Info
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.confirmation.SafeFourConfirmationModule
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

object RedeemSafe3Module {

	class Factory(val wallet: Wallet) : ViewModelProvider.Factory {

		val adapterEvm = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return when (modelClass) {
				RedeemSafe3ViewModel::class.java -> {
					val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
					RedeemSafe3ViewModel(wallet, rpcBlockchainSafe4, adapterEvm.evmKitWrapper) as T
				}

				RedeemSafe3SelectViewModel::class.java -> {
					RedeemSafe3SelectViewModel() as T
				}
				else -> throw IllegalArgumentException()
			}

		}
	}

	class Factory2(val wallet: Wallet, val safe3Wallet: Wallet) : ViewModelProvider.Factory {

		val adapter =
				(App.adapterManager.getAdapterForWallet(safe3Wallet) as? ISendBitcoinAdapter) ?: throw IllegalStateException("SendBitcoinAdapter is null")

		val baseAdapter =
				(App.adapterManager.getAdapterForWallet(safe3Wallet) as? BitcoinBaseAdapter) ?: throw IllegalStateException("SendBitcoinAdapter is null")

		val adapterEvm = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return when (modelClass) {
				RedeemSafe3LocalViewModel::class.java -> {
					val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
					RedeemSafe3LocalViewModel(wallet, safe3Wallet, rpcBlockchainSafe4, adapterEvm.evmKitWrapper, baseAdapter.kit.bitcoinCore, App.redeemStorage) as T
				}
				else -> throw IllegalArgumentException()
			}

		}
	}

	data class RedeemSafe3UiState(
			val step: Int,
			val safe3Balance: String,
			val safe3LockBalance: String,
			val privateKeyError: Boolean,
			val existAvailable: Boolean,
			val existLocked: Boolean,
			val canRedeem: Boolean,
			val safe4address: String? = null,
			val safe3LockNum: Int = 0,
			val masterNodeLock: String? = null
	)

	data class RedeemSafe3LocalUiState(
			val step: Int,
			val syncing: Boolean,
			val canRedeem: Boolean,
			val safe4address: String,
			val list: List<Safe3LocalItemView>,
			val isRedeemSuccess: Boolean,
			val showConfirmationDialog: Boolean = false
	)

	data class RedeemSafe3SelectUiState(
			val syncing: Boolean
	)


	data class Safe3LocalItemView(
			val address: String,
			val safe3Balance: String,
			val safe3LockBalance: String,
			val masterNodeLock: String?,
			val existAvailable: Boolean,
			val existLocked: Boolean,
			val existMasterNode: Boolean,
			val safe3LockNum: Int
	)


	data class Safe3LocalInfo(
			val address: String,
			val safe3Balance: BigInteger,
			val safe3LockBalance: BigInteger,
			val existAvailable: Boolean,
			val existLocked: Boolean,
			val existMasterNode: Boolean,
			val safe3LockNum: Int = 0,
			val masterNodeLock: BigInteger?,
			val privateKey: BigInteger
	)

	data class Safe3LockItemView(
			val safe3Addr: String,
			val amount: String,
			val txid: String,
			val lockHeight: Long,
			val unlockHeight: Long,
			val remainLockHeight: Long,
			val lockDay: Int,
			val isMN: Boolean,
			val safe4Addr: String,
			val redeemHeight: Long,
	)


	@Parcelize
	data class Input(
			val wallet: Wallet,
			val safe3Wallet: Wallet?
	) : Parcelable {
	}

}