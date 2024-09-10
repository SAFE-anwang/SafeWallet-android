package io.horizontalsystems.bankwallet.modules.safe4.node

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
import io.horizontalsystems.bankwallet.modules.safe4.node.CreateViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Node_Lock_Day
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourLockedVoteService
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteModule
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

class SafeFourNodeInfoViewModel(
        val wallet: Wallet,
        val nodeId: Int,
        val isSuper: Boolean,
        private val nodeService: SafeFourNodeService,
        private val walletAddress: String,
) : ViewModelUiState<NodeInfoUiState>() {

    val tabs = if (isSuper) SafeFourVoteModule.TabInfo.values() else arrayOf(SafeFourVoteModule.TabInfo.Creator)

    private var nodeInfo: NodeInfo? = null

    private val disposables = CompositeDisposable()

    init {

        nodeService.nodeInfoObservable.subscribeIO {
            nodeInfo = it
            emitState()
        }.let {
            disposables.add(it)
        }

        viewModelScope.launch(Dispatchers.IO) {
            nodeService.getNodeInfo(nodeId)
        }
    }

    override fun createState() = if (nodeInfo == null) {
        NodeInfoUiState(
                nodeInfo = null,
                creatorList = NodeCovertFactory.convertCreatorList(nodeInfo, walletAddress)
        )
    } else {
        NodeInfoUiState(
                nodeInfo = NodeCovertFactory.createNoteItemView(0, nodeInfo!!, isSuper),
                creator = nodeInfo!!.incentivePlan.creator.toFloat() / 100,
                creatorText = "${nodeInfo!!.incentivePlan.creator}%",
                partner = nodeInfo!!.incentivePlan.partner.toFloat() / 100,
                partnerText = "${nodeInfo!!.incentivePlan.partner}%",
                voter = nodeInfo!!.incentivePlan.voter.toFloat() / 100,
                voterText = "${nodeInfo!!.incentivePlan.voter}%",
                creatorList = NodeCovertFactory.convertCreatorList(nodeInfo, walletAddress),
                remainingShares = App.numberFormatter.formatCoinFull(NodeCovertFactory.valueConvert(nodeInfo!!.availableLimit), "SAFE", 2),
        )
    }
}


data class NodeInfoUiState (
        val nodeInfo: NodeViewItem?,
        val creator: Float = 0f,
        val creatorText: String = "0%",
        val partner: Float = 0f,
        val partnerText: String = "0%",
        val voter: Float = 0f,
        val voterText: String = "0%",
        val remainingShares: String = "",
        val creatorList: List<CreateViewItem> = listOf()
)
