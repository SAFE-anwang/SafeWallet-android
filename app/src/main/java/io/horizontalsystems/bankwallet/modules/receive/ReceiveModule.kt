package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UsedAddress
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ui.MoreAddressInfo
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveAddressViewModel
import java.math.BigDecimal

object ReceiveModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val evmKitManager = try {
                App.evmBlockchainManager.getEvmKitManager(wallet.token.blockchainType)
            } catch (e: Exception) {
                null
            }
            return ReceiveAddressViewModel(wallet, App.adapterManager, evmKitManager) as T
        }
    }

    sealed class AdditionalData {
        class Amount(val value: String) : AdditionalData()
        class Memo(val value: String) : AdditionalData()
        object AccountNotActive : AdditionalData()
    }

    data class UiState(
        val viewState: ViewState,
        val address: String,
        val usedAddresses: List<UsedAddress>,
        val usedChangeAddresses: List<UsedAddress>,
        val uri: String,
        val networkName: String,
        val watchAccount: Boolean,
        val additionalItems: List<AdditionalData>,
        val amount: BigDecimal?,
        val alertText: AlertText?,
        val showMoreButton: Boolean = false,
        val moreAddress: List<MoreAddressInfo> = listOf()
    )

    sealed class AlertText {
        class Normal(val content: String) : AlertText()
        class Critical(val content: String) : AlertText()
    }
    class NoReceiverAdapter : Error("No Receiver Adapter")
}
