package io.horizontalsystems.bankwallet.modules.safe4.node

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import java.math.BigInteger
import java.util.concurrent.TimeUnit

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
            Pair(0,R.string.Safe_Four_Master_Node_All),
            Pair(2, R.string.Safe_Four_Master_Node_Crowdfunding),
            Pair(1, R.string.Safe_Four_Master_Node_Mine)
    )

    private val disposables = CompositeDisposable()

    companion object {
        /** 搜索接口查询防抖时间：连续输入停顿超过该时长才真正发起 RPC 请求 */
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    private var nodes: List<NodeInfo>? = null
    private var mineNodes: List<NodeInfo>? = null
    private var crowdfundingNodes: List<NodeInfo>? = null

    /** 主节点众筹判定：founders 质押总额未满创建额（1000 SAFE） */
    private fun isCrowdfunding(node: NodeInfo): Boolean {
        val pledged = NodeCovertFactory.valueConvert(node.founders.sumOf { it.amount })
        return pledged.toInt() < NodeCovertFactory.Master_Node_Create_Amount
    }

    private var creatorList: List<String> = emptyList()
    private var isSuperOrMasterNode: Boolean = true
    private var isFounder: Boolean = true

    private var isRegisterNode = Pair(true, true)
    private var query: String? = null
    private var isFilterId: Boolean = false

    /** 接口查询返回的节点（覆盖未加载/未缓存的节点），null 表示尚未有接口结果 */
    private var searchResultNodes: List<NodeInfo>? = null

    /** 搜索防抖定时器，连续输入时只保留最后一次 */
    private var searchDisposable: Disposable? = null


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
                    val distinctNodes = it.distinctBy { it.id }
                    nodes = distinctNodes
                    crowdfundingNodes = distinctNodes.filter { node -> isCrowdfunding(node) }
                            .sortedByDescending { node -> node.founders.sumOf { it.amount } }
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        nodeService.searchResultObservable
                .subscribeIO { result ->
                    searchResultNodes = result
                    emitState()
                }
                .let {
                    disposables.add(it)
                }
        nodeService.mineNodeItemsObservable
                .subscribeIO {
                    mineNodes = it
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
        nodeService.isFounderNodeObservable
                .subscribeIO {
                    isFounder = it
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
            }
        }
        getCacheData()
    }

    private fun getCacheData() {
        viewModelScope.launch(Dispatchers.IO) {
            // type: 0=超级节点，1=主节点
            val chainType = if (App.localStorage.isSafe4TestNet) 1 else 0
            val cacheDatas = App.appDatabase.nodeInfoDao().getNodeInfoList(if (isSuperNode) 0 else 1, chainType)
            nodes = cacheDatas
            mineNodes = cacheDatas.filter { it.creator.lowercase() == ethereumKit.receiveAddress.hex.lowercase() }
            if (!isSuperNode) {
                crowdfundingNodes = cacheDatas.filter { isCrowdfunding(it) }
                        .sortedByDescending { node -> node.founders.sumOf { it.amount } }
            }
            emitState()
        }
    }

    private fun isRegisterNode(): Boolean {
        return if (isSuperNode) {
            isRegisterNode.first
        } else {
            isRegisterNode.second
        }
    }

    fun receiveAddress(): String {
        return ethereumKit.receiveAddress.hex
    }

    override fun createState() = SafeFourModule.SafeFourNodeUiState(
        title = title,
        crowdfundingList = toViewItems(
            if (this.query.isNullOrBlank()) crowdfundingNodes else mergeSearchResult(crowdfundingNodes, crowdfunding = true)
        ),
        nodeList = toViewItems(
            if (this.query.isNullOrBlank()) nodes else mergeSearchResult(nodes)
        ),
        // 「我的节点」不参与接口搜索，仅本地过滤，但同样经 toViewItems 转换，
        // 保证三个分支的统一类型为 List<NodeInfo>?（避免 if 分支推断成 List<Any>?）
        mineList = toViewItems(
            if (this.query.isNullOrBlank()) mineNodes else mineNodes?.filter { node -> matchQuery(node) }
        ),
        isRegisterNode = isRegisterNode
    )

    private fun toViewItems(nodes: List<NodeInfo>?): List<NodeViewItem>? {
        return nodes?.mapIndexed { index, nodeItem ->
            NodeCovertFactory.createNoteItemView(index, nodeItem, isSuperNode, isSuperOrMasterNode, isCreator(), receiveAddress = receiveAddress())
        }
    }

    /**
     * 合并搜索结果：接口查询结果优先，本地过滤结果补充（按 id 去重）。
     *
     * 接口结果能覆盖「尚未加载/未在缓存中」的节点；本地结果保证输入过程中即时响应。
     */
    private fun mergeSearchResult(localNodes: List<NodeInfo>?, crowdfunding: Boolean = false): List<NodeInfo>? {
        // 接口结果仍需通过当前查询条件校验，避免用户继续输入后展示上一次查询的过期结果
        val fromApi: List<NodeInfo> = searchResultNodes.orEmpty().filter { node ->
            matchQuery(node) && (!crowdfunding || isCrowdfunding(node))
        }
        val fromLocal: List<NodeInfo> = localNodes.orEmpty().filter { matchQuery(it) }
        val merged: List<NodeInfo> = (fromApi + fromLocal).distinctBy { it.id }
        return merged.ifEmpty { null }
    }

    /** 本地匹配：ID 精确匹配，或地址包含查询串 */
    private fun matchQuery(node: NodeInfo): Boolean {
        val q = query ?: return true
        return if (isFilterId) node.id.toString() == q else node.addr.contains(q, true)
    }

    private fun isCreator(): Boolean {
        return isRegisterNode.first || creatorList.contains(receiveAddress())
    }

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
        searchDisposable?.dispose()
        searchDisposable = null
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
        // 本地过滤即时生效；列表重建放到 IO 线程，避免快速输入时在主线程重建大列表造成卡顿
        emitStateOnIO()

        // 接口查询做防抖：连续输入只在停顿后发一次请求，避免每按一个键都打 RPC
        searchDisposable?.dispose()
        searchDisposable = null

        val trimmed = query.trim()
        // 地址需至少 4 个字符才有意义（数字 ID 长度>1 即可）
        val queryValid = trimmed.isNotEmpty() && (isFilterId || trimmed.length >= 4)
        if (!queryValid) {
            searchResultNodes = null
            return
        }

        searchDisposable = Observable.timer(SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    // 停顿后查询期间用户可能已继续输入，校验查询串是否仍是当前值
                    if (this.query?.trim() != trimmed) return@subscribe
                    nodeService.searchNodeByQuery(trimmed)
                }
    }

    fun clearQuery() {
        this.query = null
        searchResultNodes = null
        searchDisposable?.dispose()
        searchDisposable = null
        emitState()
    }

}

/**
 * 节点缓存实体（超级节点与主节点共用一张表，用 [type] 区分：0=超级节点，1=主节点）。
 *
 * 主键必须是 [id]+[type]+[chainType] 的复合主键：
 * 超级节点与主节点的 id 都由链上从 1 开始各自编号，若仅用 id 作主键，
 * 主节点会与同 id 的超级节点互相覆盖（REPLACE），导致缓存数据缺失。
 */
@Entity(
    primaryKeys = ["id", "type", "chainType"]
)
data class NodeInfo(
        val id: Int,
        val addr: String,
        val creator: String,
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
        var availableLimit: BigInteger = BigInteger.ZERO,
    var type: Int = 0,
    var sortOrder: Int = 0,
    val chainType: Int = 0
) {
    override fun toString(): String {
        return "NodeItem(id=$id, addr=$addr, creator=$creator, enode='$enode', description='$description', isOfficial=$isOfficial, state=$state, founders=$founders, incentivePlan=$incentivePlan, lastRewardHeight=$lastRewardHeight, createHeight=$createHeight, updateHeight=$updateHeight, name='$name')"
    }
}

@Parcelize
data class NodeMemberInfo(
        val lockID: Long,
        val addr: Address,
        val amount: BigInteger,
        val height: Long
):Parcelable {
    override fun toString(): String {
        return "NodeMemberInfo(lockID=$lockID, addr=$addr, amount=$amount, height=$height)"
    }
}

@Parcelize
data class NodeIncentivePlan(
        val creator: Int,
        val partner: Int,
        val voter: Int
): Parcelable {
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
        val address: String,
        val creator: String,
        val status: NodeStatus,
        val founders: List<NodeMemberInfo> = emptyList(),
        val enode: String = "",
        val createPledge: String = "5,000 SAFE",
        val canJoin: Boolean = false,
        val isEdit: Boolean = false,
        val isMine: Boolean = false,
        val isVoteEnable: Boolean = false,
        val isPartner: Boolean = false,
        val isCreator: Boolean = false,
        val isAddLockDay: Boolean = false,
        val incentivePlan: NodeIncentivePlan,
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

        fun convert(status: NodeStatus): Int {
            return if (status == Online)
                1
            else
                0
        }
    }
}
