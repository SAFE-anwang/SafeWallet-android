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

    class Factory(
        private val wallet: Wallet,
        private val isTransparentAddress: Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val evmKitManager = try {
                App.evmBlockchainManager.getEvmKitManager(wallet.token.blockchainType)
            } catch (e: Exception) {
                null
            }
            return ReceiveAddressViewModel(wallet, App.adapterManager, isTransparentAddress, evmKitManager) as T
        }
    }

    sealed class AdditionalData {
        class Amount(val value: String) : AdditionalData()
        class Memo(val value: String) : AdditionalData()
    }

    abstract class AbstractUiState {
        abstract val viewState: ViewState
        abstract val alertText: AlertText?
        abstract val uri: String
        abstract val address: String
        abstract val mainNet: Boolean
        abstract val blockchainName: String?
        abstract val addressType: String?
        abstract val addressFormat: String?
        abstract val watchAccount: Boolean
        abstract val amount: BigDecimal?
        abstract val amountString: String?
        abstract val showMoreButton: Boolean
    }

    data class UiState(
        override val viewState: ViewState,
        override val address: String,
        override val mainNet: Boolean,
        val usedAddresses: List<UsedAddress>,
        val usedChangeAddresses: List<UsedAddress>,
        override val uri: String,
        override val blockchainName: String?,
        override val addressType: String?,
        override val addressFormat: String?,
        override val watchAccount: Boolean,
        override val amount: BigDecimal?,
        override val amountString: String?,
        override val alertText: AlertText?,
        override val showMoreButton: Boolean,
        val moreAddress: List<MoreAddressInfo> = listOf()
    ) : AbstractUiState()

    sealed class AlertText {
        class Critical(val content: String) : AlertText()
    }
    class NoReceiverAdapter : Error("No Receiver Adapter")
}
