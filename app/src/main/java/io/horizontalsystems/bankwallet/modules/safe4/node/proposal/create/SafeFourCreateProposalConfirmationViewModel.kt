package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.net.UnknownHostException

class SafeFourCreateProposalConfirmationViewModel(
        val wallet: Wallet,
        private val createProposalData: SafeFourProposalModule.CreateProposalData,
        private val rpcBlockchainSafe4: RpcBlockchainSafe4,
        private val evmKitWrapper: EvmKitWrapper
) : ViewModelUiState<SafeFourProposalModule.SafeFourProposalConfirmationUiState>()  {


    var sendResult by mutableStateOf<SendResult?>(null)

    private val disposables = CompositeDisposable()


    fun send() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sendResult = SendResult.Sending
            	val single = rpcBlockchainSafe4.proposalCreate(
                        evmKitWrapper.signer!!.privateKey.toHexString(),
                        createProposalData.title,
                        createProposalData.payAmount,
                        createProposalData.payTimes,
                        (createProposalData.startPayTime / 1000).toBigInteger(),
                        (createProposalData.endPayTime / 1000).toBigInteger(),
                        createProposalData.description
                )
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

    override fun createState() = SafeFourProposalModule.SafeFourProposalConfirmationUiState(
            title = createProposalData.title,
            desc = createProposalData.description,
            amount = NodeCovertFactory.formatSafe(createProposalData.payAmount),
            startDate = NodeCovertFactory.formatDate(createProposalData.startPayTime.toLong()),
            endDate = NodeCovertFactory.formatDate(createProposalData.endPayTime.toLong()),
            isOncePay = createProposalData.payTimes == BigInteger.ONE,
            payTimes = createProposalData.payTimes.toInt().toString()
    )


    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
