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
import io.horizontalsystems.bankwallet.modules.safe4.node.CreateViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Node_Lock_Day
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

class SafeFourVoteViewModel(
    val wallet: Wallet,
    val sendToken: Token,
    val nodeId: Int,
    val isSuper: Boolean,
    val isJoin: Boolean,
    val adapter: ISendEthereumAdapter,
    private val nodeService: SafeFourNodeService,
    private val lockVoteService: SafeFourLockedVoteService,
    private val xRateService: XRateService,
    private val amountService: SendAmountService,
    val coinMaxAllowedDecimals: Int,
    private val connectivityManager: ConnectivityManager
) : ViewModelUiState<VoteUiState>() {
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set

    val tabs = if (isSuper && !isJoin) SafeFourVoteModule.Tab.values() else arrayOf(SafeFourVoteModule.Tab.SafeVote)
    val tabs2 = if (isSuper && !isJoin) SafeFourVoteModule.Tab2.values() else arrayOf(SafeFourVoteModule.Tab2.Creator)

    private var nodeInfo: NodeInfo? = null

    private var amountState = amountService.stateFlow.value

    private var lockIdsInfo: List<LockIdsInfo>? = null
    private var lockIdsInfoLocked: List<LockIdsInfo>? = null
    private var joinAmount =
        if (isSuper)
            NodeCovertFactory.Super_Node_Partner_Join_Amount
        else
            NodeCovertFactory.Master_Node_Partner_Join_Amount

    private var isLockVote = false

    private val disposables = CompositeDisposable()

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }

        lockVoteService.itemsObservable
                .subscribeIO {
                    lockIdsInfo = it.map {
                        LockIdsInfo(it.lockId, it.lockValue, it.enable, getChecked(it.lockId))
                    }
                    emitState()
                }
                .let {
                    disposables.add(it)
                }

        lockVoteService.itemsObservableLocked
                .subscribeIO {
                    lockIdsInfoLocked = it.map {
                        LockIdsInfo(it.lockId, it.lockValue, it.enable, false)
                    }
                    emitState()
                }
                .let {
                    disposables.add(it)
                }

        nodeService.nodeInfoObservable.subscribeIO {
            nodeInfo = it
            emitState()
        }.let {
            disposables.add(it)
        }

        viewModelScope.launch(Dispatchers.IO) {
            lockVoteService.loadItems(0)
            lockVoteService.loadItemsLocked(0)
            nodeService.getNodeInfo(nodeId)
        }
        if(isJoin) {
            onEnterAmount(BigDecimal(joinAmount))
        }
    }

    override fun createState() = if (nodeInfo == null) {
        VoteUiState(
                nodeInfo = null,
                availableBalance = amountState.availableBalance,
                amountCaution = amountState.amountCaution,
                canBeSend = amountState.canBeSend && (amountState.amount?.toInt() ?: 0) >= 1,
                recordVoteCanSend = getRecordVoteCanSend(),
                lockIdInfo = NodeCovertFactory.convertLockIdItemView(lockIdsInfo, lockIdsInfoLocked),
                creatorList = NodeCovertFactory.convertCreatorList(nodeInfo, adapter.evmKitWrapper.evmKit.receiveAddress.hex),
                joinSlider = getJoinAmountSlider()
        )
    } else {
        VoteUiState(
                nodeInfo = NodeCovertFactory.createNoteItemView(0, nodeInfo!!, isSuper),
                availableBalance = amountState.availableBalance,
                amountCaution = amountState.amountCaution,
                creator = nodeInfo!!.incentivePlan.creator.toFloat() / 100,
                creatorText = "${nodeInfo!!.incentivePlan.creator}%",
                partner = nodeInfo!!.incentivePlan.partner.toFloat() / 100,
                partnerText = "${nodeInfo!!.incentivePlan.partner}%",
                voter = nodeInfo!!.incentivePlan.voter.toFloat() / 100,
                voterText = "${nodeInfo!!.incentivePlan.voter}%",
                canBeSend = amountState.canBeSend && (amountState.amount?.toInt() ?: 0) >= 1,
                recordVoteCanSend = getRecordVoteCanSend(),
                lockIdInfo = NodeCovertFactory.convertLockIdItemView(lockIdsInfo, lockIdsInfoLocked),
                creatorList = NodeCovertFactory.convertCreatorList(nodeInfo, adapter.evmKitWrapper.evmKit.receiveAddress.hex),
                remainingShares = App.numberFormatter.formatCoinFull(NodeCovertFactory.valueConvert(nodeInfo!!.availableLimit), "SAFE", 2),
                joinSlider = getJoinAmountSlider()
        )
    }

    private fun getRecordVoteCanSend(): Boolean {
        return lockIdsInfo?.filter { it.checked }?.isNotEmpty() == true
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    fun setIsLockVote(isLockVote: Boolean) {
        this.isLockVote
    }

    fun getSafeVoteData(): VoteData? {
        return this.amountState.amount?.let {
            return VoteData(
                    it.movePointRight((adapter as BaseEvmAdapter).decimal).toBigInteger(),
                    isJoin,
                    true,
                    nodeInfo!!.addr.hex,
                    nodeInfo!!.id,
                    nodeInfo!!.name,
                    nodeInfo!!.description,
                    emptyList()
            )
        }
    }

    fun getLockVoteData(): VoteData? {
        val selectLock = lockIdsInfo?.filter { it.checked }
        if (selectLock.isNullOrEmpty())   return null
        return VoteData(
                BigInteger.ZERO,
                isJoin,
                false,
                nodeInfo!!.addr.hex,
                nodeInfo!!.id,
                nodeInfo!!.name,
                nodeInfo!!.description,
                selectLock
        )
    }

    fun getAppendRegisterData(): VoteData? {
            return VoteData(
                    NodeCovertFactory.scaleConvert(joinAmount),
                    true,
                    false,
                    nodeInfo!!.addr.hex,
                    nodeInfo!!.id,
                    nodeInfo!!.name,
                    nodeInfo!!.description,
                    emptyList()
            )
    }

    private fun getChecked(lockId: Int): Boolean {
        val info = lockIdsInfo?.find { it.lockId == lockId }
        return info?.checked ?: false
    }

    fun checkLockVote(lockId: Int, checked: Boolean) {
        lockIdsInfo?.forEach { lockIdsInfo ->
            if (lockId == lockIdsInfo.lockId) {
                lockIdsInfo.checked = checked
            }
        }
        emitState()
    }

    fun selectAllLock(checked: Boolean) {
        lockIdsInfo?.forEach { lockIdsInfo ->
            if (lockIdsInfo.enable) {
                lockIdsInfo.checked = checked
            }
        }
        emitState()
    }

    fun onBottomReached() {
        viewModelScope.launch(Dispatchers.IO) {
            lockVoteService.loadNext()
        }
    }

    fun selectJoinAmount(amount: Int) {
        joinAmount = amount

        onEnterAmount(BigDecimal(amount))
    }

    private fun getJoinAmountList(): List<JoinAmount> {
        if (nodeInfo == null) return emptyList()

        val amount = NodeCovertFactory.valueConvert(nodeInfo!!.founders.sumOf { it.amount }).toInt()
        val availableAmount = if (isSuper) {
            NodeCovertFactory.Super_Node_Create_Amount - amount
        } else {
            NodeCovertFactory.Master_Node_Create_Amount - amount
        }
        val count = if (isSuper) {
            availableAmount / NodeCovertFactory.Super_Node_Partner_Join_Amount
        } else {
            availableAmount / NodeCovertFactory.Master_Node_Partner_Join_Amount
        }
        val list = mutableListOf<JoinAmount>()
        for (i in 0 until count) {
            val amount = if (isSuper) {
                (i + 1 ) * NodeCovertFactory.Super_Node_Partner_Join_Amount
            } else {
                (i + 1 ) *  NodeCovertFactory.Master_Node_Partner_Join_Amount
            }
            val bean = JoinAmount(
                    amount,
                    joinAmount == amount
            )
            list.add(bean)
        }
        return list
    }

    private fun getJoinAmountSlider(): JoinAmountSlider {
        val step = if (isSuper) {
            NodeCovertFactory.Super_Node_Partner_Join_Amount
        } else {
            NodeCovertFactory.Master_Node_Partner_Join_Amount
        }
        if (nodeInfo == null) return JoinAmountSlider(step, step, 0)

        val amount = NodeCovertFactory.valueConvert(nodeInfo!!.founders.sumOf { it.amount }).toInt()
        val availableAmount = if (isSuper) {
            NodeCovertFactory.Super_Node_Create_Amount - amount
        } else {
            NodeCovertFactory.Master_Node_Create_Amount - amount
        }
        /*val list = mutableListOf<JoinAmount>()
        for (i in 0 until count) {
            val amount = if (isSuper) {
                (i + 1 ) * NodeCovertFactory.Super_Node_Partner_Join_Amount
            } else {
                (i + 1 ) *  NodeCovertFactory.Master_Node_Partner_Join_Amount
            }
            val bean = JoinAmount(
                    amount,
                    joinAmount == amount
            )
            list.add(bean)
        }*/
        return JoinAmountSlider(step, step, availableAmount)
    }
}

@Parcelize
data class VoteData(
        val vaule: BigInteger,
        val isAppendRegister: Boolean,
        val isSafeVote: Boolean,
        val dstAddr: String,
        val nodeId: Int,
        val nodeName: String,
        val nodeDesc: String,
        val recordsIds: List<LockIdsInfo> = listOf(),
        val lockDay: Int = Node_Lock_Day
): Parcelable {

}

@Parcelize
data class LockIdsInfo(
        val lockId: Int,
        val lockValue: BigInteger,
        val enable: Boolean,
        var checked: Boolean = false
): Parcelable {

}

data class LockIdsView(
        val index: Int,
        val lockIds: String,
        val lockValue: String,
        val enable: Boolean,
        val checked: Boolean = false
)

data class JoinAmount(
        val value: Int,
        var selected: Boolean
)

data class JoinAmountSlider(
        val step: Int = 0,
        val min: Int = 0,
        val max: Int = 0
)

data class VoteUiState (
        val nodeInfo: NodeViewItem?,
        val availableBalance: BigDecimal,
        val amountCaution: HSCaution?,
        val canBeSend: Boolean,
        val recordVoteCanSend: Boolean,
        val creator: Float = 0f,
        val creatorText: String = "0%",
        val partner: Float = 0f,
        val partnerText: String = "0%",
        val voter: Float = 0f,
        val voterText: String = "0%",
        val remainingShares: String = "",
        val lockIdInfo: List<LockIdsView>? = null,
        val creatorList: List<CreateViewItem> = listOf(),
        val joinSlider: JoinAmountSlider
)
