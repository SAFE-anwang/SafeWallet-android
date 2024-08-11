package io.horizontalsystems.bankwallet.modules.safe4.node.confirmation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.UnknownHostException

class SafeFourCreateNodeConfirmationViewModel(
        private val isSuper: Boolean,
        private val wallet: Wallet,
        val createNodeData: SafeFourConfirmationModule.CreateNodeData,
        private val evmKitWrapper: EvmKitWrapper
) : ViewModelUiState<SafeFourConfirmationModule.SafeFourCreateNodeConfirmationUiState>()  {

    var sendResult by mutableStateOf<SendResult?>(null)

    private val disposables = CompositeDisposable()

    fun send() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sendResult = SendResult.Sending
                val single = if (isSuper) {
                    evmKitWrapper.createSuperNode(
                            createNodeData.value,
                            createNodeData.isUnion,
                            createNodeData.address,
                            createNodeData.lockDay,
                            createNodeData.name,
                            createNodeData.enode,
                            createNodeData.description,
                            createNodeData.creatorIncentive,
                            createNodeData.partnerIncentive,
                            createNodeData.voterIncentive
                    )
                } else {
                    evmKitWrapper.createMasterNode(
                            createNodeData.value,
                            createNodeData.isUnion,
                            createNodeData.address,
                            createNodeData.lockDay,
                            createNodeData.enode,
                            createNodeData.description,
                            createNodeData.creatorIncentive,
                            createNodeData.partnerIncentive
                    )
                }
                val result = single.blockingGet()
                sendResult = SendResult.Sent
            } catch (e: Exception) {
                sendResult = SendResult.Failed(createCaution(e))
            }
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    override fun createState() = SafeFourConfirmationModule.SafeFourCreateNodeConfirmationUiState(
            NodeCovertFactory.formatSafe(createNodeData.value),
        true
    )

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
