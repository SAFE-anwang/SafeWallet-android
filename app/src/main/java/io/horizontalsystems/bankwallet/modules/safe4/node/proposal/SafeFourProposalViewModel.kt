package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.apache.commons.lang3.StringUtils
import java.math.BigInteger

class SafeFourProposalViewModel(
        val wallet: Wallet,
        private val nodeService: SafeFourProposalService
) : ViewModelUiState<SafeFourProposalModule.SafeFourProposalUiState>()  {

    val tabs = SafeFourProposalModule.Tab.values()

    private val disposables = CompositeDisposable()

    private var allProposal: List<ProposalViewItem>? = null
    private var mineProposal: List<ProposalViewItem>? = null

    private var query: String? = null
    var currentScreen = 0

    init {
        nodeService.allItemsObservable
                .subscribeIO {
                    allProposal = it.mapIndexed { index, nodeItem -> NodeCovertFactory.createProposalItemView(index, nodeItem) }
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        nodeService.mineItemsObservable
                .subscribeIO {
                    mineProposal = it.mapIndexed { index, nodeItem -> NodeCovertFactory.createProposalItemView(index, nodeItem) }
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        viewModelScope.launch(Dispatchers.IO) {
            nodeService.getAllNum()
            nodeService.getMinNum()
            nodeService.loadAllItems(0)
        }
    }

    override fun createState() = SafeFourProposalModule.SafeFourProposalUiState(
        allProposalList = if (this.query.isNullOrBlank()) {
            allProposal
        } else allProposal?.filter {
                it.id.toString() == query
        },
            mineProposalList = if (this.query.isNullOrBlank()) {
                mineProposal
            } else mineProposal?.filter {
            it.id.toString() == query
        },
    )

    fun getProposalInfo(id: Int, type: Int): ProposalInfo? {
        return nodeService.getProposalInfo(id, type)
    }

    fun onBottomReached() {
        nodeService.loadNext()
    }


    fun searchByQuery(query: String) {
        this.query = query
        emitState()
    }

    fun clearQuery() {
        this.query = null
        emitState()
    }


    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

}


@Parcelize
data class ProposalInfo(
        val id: Int,
        val creator: String,
        val title: String,
        val payAmount: BigInteger,
        val payTimes: Long,
        val startPayTime: Long,
        val endPayTime: Long,
        val description: String,
        val state: Int,
        val createHeight: Long,
        val updateHeight: Long
) : Parcelable {

}


@Immutable
data class ProposalViewItem(
        val index: Int,
        val id: Int,
        val title: String,
        val desc: String,
        val creator: String,
        val status: ProposalStatus,
        val amount: String,
        val endDate: String
)

sealed class ProposalStatus {
    object Lose : ProposalStatus()
    object Voting : ProposalStatus()
    object Adopt : ProposalStatus()

    fun title(): TranslatableString {
        return when (this) {
            is Lose -> TranslatableString.ResString(R.string.Safe_Four_Proposal_State_Lose)
            is Voting -> TranslatableString.ResString(R.string.Safe_Four_Proposal_State_Voting)
            is Adopt -> TranslatableString.ResString(R.string.Safe_Four_Proposal_State_Adopt)
            else -> TranslatableString.PlainString("")
        }
    }

    companion object {
        fun getStatus(type: Int):ProposalStatus {
            return if (type == 0) {
                Voting
            } else if(type == 1) {
                Adopt
            } else {
                Lose
            }
        }
    }
}