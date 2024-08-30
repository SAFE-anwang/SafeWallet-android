package io.horizontalsystems.bankwallet.modules.safe4.node.vote

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

class SafeFourVoteRecordViewModel(
    private val service: SafeFourVoteRecordService,
    private val walletAddress: String
) : ViewModelUiState<VoteRecordUiState>() {

    private var voteRecords: List<VoteRecordInfo>? = null

    private val disposables = CompositeDisposable()

    init {

        service.itemsObservable.subscribeIO{
            voteRecords = it
            emitState()
        }
        .let {
            disposables.add(it)
        }
        viewModelScope.launch(Dispatchers.IO) {
            service.loadItems(0)
        }
    }

    override fun createState() = VoteRecordUiState(
            NodeCovertFactory.covertVoteRecord(voteRecords, walletAddress)
        )


    fun onBottomReached() {
        service.loadNext()
    }

}


data class VoteRecordInfo(
        val address: String,
        val lockValue: BigInteger
)

data class VoteRecordView(
        val index: Int,
        val address: String,
        val lockValue: String,
        val isMine: Boolean
)

data class VoteRecordUiState (
        val voteRecords: List<VoteRecordView>? = null
)
