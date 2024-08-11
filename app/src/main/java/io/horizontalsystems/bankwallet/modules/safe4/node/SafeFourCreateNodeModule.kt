package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.safe4.node.supernode.SafeFourCreateNodeViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import java.math.BigDecimal
import java.math.RoundingMode

class SafeFourCreateNodeModule {

	class Factory(val wallet: Wallet, val isSuperNode: Boolean) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

			val amountValidator = AmountValidator()
			val coinMaxAllowedDecimals = wallet.token.decimals
			val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

			val amountService = SendAmountService(
					amountValidator,
					wallet.token.coin.code,
					adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
					wallet.token.type.isNative
			)
			val addressService = SendEvmAddressService(adapter.evmKitWrapper.evmKit.receiveAddress.hex)

			return SafeFourCreateNodeViewModel(wallet, isSuperNode, amountService, addressService, adapter.evmKitWrapper.evmKit, coinMaxAllowedDecimals, xRateService) as T
		}
	}

	data class SafeFourCreateNodeUiState(
			val title: Int,
			val addressError: Throwable?,
			val availableBalance: BigDecimal,
			val amountCaution: HSCaution?,
			val canBeSend: Boolean,
			val lockAmount: String
	)

}