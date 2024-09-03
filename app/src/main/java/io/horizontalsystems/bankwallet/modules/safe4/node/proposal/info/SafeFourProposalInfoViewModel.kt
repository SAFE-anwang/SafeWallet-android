package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.storage.ProposalStateStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalState
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalStatus
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class SafeFourProposalInfoViewModel(
        val wallet: Wallet,
        private val reciverAddress: String,
        private val proposalInfo: ProposalInfo,
        private val storage: ProposalStateStorage,
        private val nodeService: SafeFourProposalInfoService?
) : ViewModelUiState<SafeFourProposalModule.SafeFourProposalInfoUiState>()  {

    private val disposables = CompositeDisposable()

    private var proposalVote: List<ProposalVote>? = null
    private var isTopsAddress = false
    private var voteType = 0
    private var showConfirmationDialg = false
    private var isAlreadyVote = false
    private var voteStatus: Int? = null

    var sendResult by mutableStateOf<SendResult?>(null)

    init {
        nodeService?.allItemsObservable
                ?.subscribeIO {
                    proposalVote = it

                    val voteInfo = it.filter { reciverAddress.equals(it.address, true)  }
                    if (voteInfo.isNotEmpty()) {
                        voteStatus = voteInfo[0].state
                    }
                    isAlreadyVote = voteInfo.isNotEmpty()
                    if (isAlreadyVote) {
                        storage.update(
                                ProposalState(
                                        proposalInfo.id,
                                        reciverAddress,
                                        voteInfo[0].state
                                )
                        )
                    }
                    emitState()
                }
                ?.let {
                    disposables.add(it)
                }
        nodeService?.topsObservable
                ?.subscribeIO {
                    isTopsAddress = it.filter { reciverAddress.equals(it, true)  }.isNotEmpty()
                    emitState()
                }
                ?.let {
                    disposables.add(it)
                }
        viewModelScope.launch(Dispatchers.IO) {
            nodeService?.loadAllItems(0)
            nodeService?.getTops()

            storage.get(reciverAddress, proposalInfo.id)?.let {
                isAlreadyVote = true
                if (proposalVote == null) {
                    proposalVote = listOf(ProposalVote(it.address, it.state))
                }
            }
        }
    }

    override fun createState() = SafeFourProposalModule.SafeFourProposalInfoUiState(
        NodeCovertFactory.createProposalInfoItemView(proposalInfo, proposalVote),
            proposalVote?.map {
                ProposalVoteViewItem(it.address, ProposalVoteStatus.getStatus(it.state))
            },
            showConfirmationDialg,
            isAlreadyVote,
            voteStatus,
            isTopsAddress
    )

    fun getVoteType(): Int {
        return voteType
    }

    fun vote(vote: Int) {
        voteType = vote
        showConfirmationDialg = true
        emitState()
    }

    fun closeDialog() {
        showConfirmationDialg = false
        emitState()
    }

    fun send() {
        sendResult = SendResult.Sending
        closeDialog()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                nodeService?.vote(voteType)?.blockingGet()
                sendResult = SendResult.Sent
                storage.save(ProposalState(proposalInfo.id, reciverAddress, voteType))
                voteStatus = voteType
                isAlreadyVote = true
                emitState()
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

    fun onBottomReached() {
        viewModelScope.launch(Dispatchers.IO) {
            nodeService?.loadNext()
        }
    }


    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

}


@Immutable
data class ProposalInfoViewItem(
        val id: Int,
        val title: String,
        val desc: String,
        val creator: String,
        val status: ProposalStatus,
        val payTimes: Int,
        val amount: String,
        val startDate: String,
        val endDate: String,
        val agreeNum: Int = 0,
        val rejectNmu: Int = 0,
        val abstentionNum: Int = 0,
)

data class ProposalVote(
        val address: String,
        val state: Int
)

data class ProposalVoteViewItem(
        val address: String,
        val state: ProposalVoteStatus
)

sealed class ProposalVoteStatus {
    object Agree : ProposalVoteStatus()
    object Refuse : ProposalVoteStatus()
    object Abstain : ProposalVoteStatus()
    fun title(): TranslatableString {
        return when (this) {
            is Agree -> TranslatableString.ResString(R.string.Safe_Four_Proposal_Vote_Agree)
            is Refuse -> TranslatableString.ResString(R.string.Safe_Four_Proposal_Vote_Refuse)
            is Abstain -> TranslatableString.ResString(R.string.Safe_Four_Proposal_Vote_Give_up)
            else -> TranslatableString.PlainString("")
        }
    }

    companion object {
        fun getStatus(type: Int):ProposalVoteStatus {
            return if (type == 1) {
                Agree
            } else if (type == 2)  {
                Refuse
            } else {
                Abstain
            }
        }
    }
}