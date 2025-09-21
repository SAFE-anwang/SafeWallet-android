package io.horizontalsystems.bankwallet.modules.safe4.node.vote.confirmation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.VoteData
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SafeFourVoteConfirmationViewModel(
        private val title: String,
        val isSuper: Boolean,
        private val wallet: Wallet,
        val voteData: VoteData,
        val safe4RpcBlockChain: RpcBlockchainSafe4,
        private val evmKitWrapper: EvmKitWrapper
) : ViewModelUiState<SafeFourVoteConfirmationModule.SafeFourVoteConfirmationUiState>()  {

    private val disposables = CompositeDisposable()

    var sendResult by mutableStateOf<SendResult?>(null)


    fun send() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sendResult = SendResult.Sending
                val result = if (voteData.isAppendRegister) {
                    if (isSuper) {
                        safe4RpcBlockChain.superAppendRegister(evmKitWrapper.signer!!.privateKey.toHexString(), voteData.vaule, voteData.dstAddr, voteData.lockDay.toBigInteger())
                    } else {
                        safe4RpcBlockChain.masterAppendRegister(evmKitWrapper.signer!!.privateKey.toHexString(), voteData.vaule, voteData.dstAddr, voteData.lockDay.toBigInteger())
                    }
                } else if (voteData.isSafeVote) {
                    safe4RpcBlockChain.voteOrApprovalWithAmount(evmKitWrapper.signer!!.privateKey.toHexString(), voteData.vaule, true, voteData.dstAddr)
                } else {
                    safe4RpcBlockChain.voteOrApproval(evmKitWrapper.signer!!.privateKey.toHexString(),true, voteData.dstAddr, voteData.recordsIds.map { it.lockId.toBigInteger() })
                }.blockingGet()

                sendResult = SendResult.Sent
            } catch (e: Exception) {
                if (e is SocketTimeoutException) {
                    send()
                } else {
                    sendResult = SendResult.Failed(createCaution(e))
                }
            }
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    override fun createState() = SafeFourVoteConfirmationModule.SafeFourVoteConfirmationUiState(
        title = title,
        voteNum = NodeCovertFactory.formatSafe(voteData.vaule),
        lockIdInfo = NodeCovertFactory.convertLockIdItemView(voteData.recordsIds.map {
                                                                                     it.copy(enable = false)
        }),
    )

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
