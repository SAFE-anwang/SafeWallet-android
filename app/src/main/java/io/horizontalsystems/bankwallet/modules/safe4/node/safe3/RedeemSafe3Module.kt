package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.confirmation.SafeFourConfirmationModule
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import kotlinx.parcelize.Parcelize

object RedeemSafe3Module {

	class Factory(val wallet: Wallet, val safe3Wallet: Wallet) : ViewModelProvider.Factory {

		val adapter =
				(App.adapterManager.getAdapterForWallet(safe3Wallet) as? ISendBitcoinAdapter) ?: throw IllegalStateException("SendBitcoinAdapter is null")

		val baseAdapter =
				(App.adapterManager.getAdapterForWallet(safe3Wallet) as? BitcoinBaseAdapter) ?: throw IllegalStateException("SendBitcoinAdapter is null")

		val adapterEvm = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			val addressService = SendBitcoinAddressService(adapter, null)
			val rpcBlockchainSafe4 = adapterEvm.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
			return RedeemSafe3ViewModel(wallet, safe3Wallet, addressService, rpcBlockchainSafe4, adapterEvm.evmKitWrapper, baseAdapter.kit.bitcoinCore) as T
		}
	}

	data class RedeemSafe3UiState(
			val step: Int,
			val addressError: Throwable?,
			val lockItemView: List<Safe3LockItemView>? = null,
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
			val safe3Wallet: Wallet,
	) : Parcelable {
	}

}