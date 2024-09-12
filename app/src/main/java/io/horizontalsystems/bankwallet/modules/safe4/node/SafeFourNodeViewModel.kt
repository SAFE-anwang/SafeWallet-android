package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import java.math.BigInteger

class SafeFourNodeViewModel(
        val wallet: Wallet,
        private val title: String,
        private val nodeService: SafeFourNodeService,
        private val isSuperNode: Boolean,
        private val ethereumKit: EthereumKit
) : ViewModelUiState<SafeFourModule.SafeFourNodeUiState>()  {

    val tabs = if (isSuperNode) listOf(
            Pair(0, R.string.Safe_Four_Super_Node_All), Pair(1, R.string.Safe_Four_Super_Node_Mine)
    ) else listOf(
            Pair(0,R.string.Safe_Four_Master_Node_All), Pair(1, R.string.Safe_Four_Master_Node_Mine)
    )

    private val disposables = CompositeDisposable()

    private var nodes: List<NodeViewItem>? = null
    private var mineNodes: List<NodeViewItem>? = null

    private var creatorList: List<String> = emptyList()
    private var isSuperOrMasterNode: Boolean = false

    private var isRegisterNode = Pair(true, true)
    private var query: String? = null
    private var isFilterId: Boolean = false


    init {
        nodeService.registerNodeObservable
                .subscribeIO{
                    isRegisterNode = it
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        nodeService.itemsObservable
                .subscribeIO {
                    nodes = it.mapIndexed { index, nodeItem -> NodeCovertFactory.createNoteItemView(index, nodeItem, isSuperNode, isSuperOrMasterNode, isRegisterNode.first || creatorList.contains(receiveAddress()), receiveAddress =  receiveAddress()) }
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        nodeService.mineNodeItemsObservable
                .subscribeIO {
                    mineNodes = it.mapIndexed { index, nodeItem -> NodeCovertFactory.createNoteItemView(index, nodeItem, isSuperNode, isSuperOrMasterNode,  isRegisterNode.first || creatorList.contains(receiveAddress()), receiveAddress =  receiveAddress()) }
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        nodeService.creatorObservable
                .subscribeIO {
                    creatorList = it
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        nodeService.isSuperOrMasterNodeObservable
                .subscribeIO {
                    isSuperOrMasterNode = it
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                nodeService.getMineCreatorNode()
                nodeService.checkNodeExist(receiveAddress())
                nodeService.getTops4Creator()
                nodeService.loadItems(0)
                nodeService.loadItemsMine(0)
            } catch (e: Exception) {
                Log.e("longwen", "node e=$e")
            }
        }
    }

    private fun isRegisterNode(): Boolean {
        return if (isSuperNode) {
            isRegisterNode.first
        } else {
            isRegisterNode.second
        }
    }

    private fun receiveAddress(): String {
        return ethereumKit.receiveAddress.hex
    }

    override fun createState() = SafeFourModule.SafeFourNodeUiState(
        title = title,
        nodeList = if (this.query.isNullOrBlank()) {
            nodes
        } else nodes?.filter {
            if (isFilterId) {
                it.id.toString() == query
            } else {
                it.id.toString() == query || it.address.hex.contains(query!!, true)
            }
        },
        mineList = if (this.query.isNullOrBlank()) {
            mineNodes
        } else mineNodes?.filter {
            if (isFilterId) {
                it.id.toString() == query
            } else {
                it.id.toString() == query || it.address.hex.contains(query!!, true)
            }
        },
        isRegisterNode = isRegisterNode
    )


    fun onBottomReached() {
        viewModelScope.launch(Dispatchers.IO) {
            nodeService.loadNext()
        }
    }

    fun getNodeItem(viewItem: NodeViewItem) = nodeService.getNodeItem(viewItem.id)

    fun isSuperNode(): Boolean {
        return isSuperNode
    }

    fun getNodeType(): Int {
        return if (isSuperNode) {
            NodeType.SuperNode
        } else {
            NodeType.MainNode
        }.ordinal
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun getMenuName(): Int {
        return if (isSuperNode) {
            R.string.Safe_Four_Register_Super_Node
        } else {
            R.string.Safe_Four_Register_Master_Node
        }
    }

    fun menuEnable(): Boolean {
        return !isRegisterNode.first && !isRegisterNode.second
    }

    fun getAlreadyRegisterText(): Int {
        return if (isRegisterNode.first) {
            R.string.Safe_Four_Register_Super_Node_Register
        } else {
            R.string.Safe_Four_Register_Master_Node_Register
        }
    }

    fun getRegisterHintText(): Int {
        return if (isSuperNode) {
            R.string.Safe_Four_Register_Super_Node_Register_Hint
        } else {
            R.string.Safe_Four_Register_Master_Node_Register_Hint
        }
    }

    fun getVoteButtonName(): Int {
        return if (isSuperNode) {
            R.string.Safe_Four_Node_Super_Node_Vote
        } else {
            R.string.Safe_Four_Node_Master_Node_Vote
        }
    }

    fun getJoinButtonName(): Int {
        return if (isSuperNode) {
            R.string.Safe_Four_Node_Super_Node_Join
        } else {
            R.string.Safe_Four_Node_Master_Node_Vote
        }
    }

    fun searchByQuery(query: String) {
        this.query = query
        isFilterId = query.length > 1 && StringUtils.isNumeric(query)
        emitState()
    }

    fun clearQuery() {
        this.query = null
        emitState()
    }

}

data class NodeInfo(
        val id: Int,
        val addr: Address,
        val creator: Address,
        val enode: String,
        val description: String,
        val isOfficial: Boolean,
        val state: NodeStatus,
        val founders: List<NodeMemberInfo>,
        val incentivePlan: NodeIncentivePlan,
        val lastRewardHeight: Long,
        val createHeight: Long,
        val updateHeight: Long,
        val name: String = "",
        val isEdit: Boolean = false,
        var totalVoteNum: BigInteger = BigInteger.ZERO,
        var totalAmount: BigInteger = BigInteger.ZERO,
        var allVoteNum: BigInteger = BigInteger.ZERO,
        var availableLimit: BigInteger = BigInteger.ZERO
) {
    override fun toString(): String {
        return "NodeItem(id=$id, addr=$addr, creator=$creator, enode='$enode', description='$description', isOfficial=$isOfficial, state=$state, founders=$founders, incentivePlan=$incentivePlan, lastRewardHeight=$lastRewardHeight, createHeight=$createHeight, updateHeight=$updateHeight, name='$name')"
    }
}

data class NodeMemberInfo(
        val lockID: Int,
        val addr: Address,
        val amount: BigInteger,
        val height: Long
) {
    override fun toString(): String {
        return "NodeMemberInfo(lockID=$lockID, addr=$addr, amount=$amount, height=$height)"
    }
}

data class NodeIncentivePlan(
        val creator: Int,
        val partner: Int,
        val voter: Int
) {
    override fun toString(): String {
        return "NodeIncentivePlan(creator=$creator, partner=$partner, voter=$voter)"
    }
}

@Immutable
data class NodeViewItem(
        val ranking: Int,
        val id: Int,
        val name: String,
        val desc: String,
        val voteCount: String,
        val voteCompleteCount: String,
        val progress: Float,
        val progressText: String,
        val address: Address,
        val creator: Address,
        val status: NodeStatus,
        val enode: String = "",
        val createPledge: String = "5,000 SAFE",
        val canJoin: Boolean = false,
        val isEdit: Boolean = false,
        val isMine: Boolean = false,
        val isVoteEnable: Boolean = false
)

data class CreateViewItem(
        val id: String,
        val address: String,
        val amount: String,
        val isMine: Boolean
)

sealed class NodeStatus {
    object Online : NodeStatus()
    object Exception : NodeStatus()

    fun title(): TranslatableString {
        return when (this) {
            is Online -> TranslatableString.ResString(R.string.Safe_Four_Node_Online)
            is Exception -> TranslatableString.ResString(R.string.Safe_Four_Node_Exception)
            else -> TranslatableString.PlainString("")
        }
    }

    companion object {
        fun get(state: Int): NodeStatus {
            return if (state == 1)
                Online
            else
                Exception
        }
    }
}
