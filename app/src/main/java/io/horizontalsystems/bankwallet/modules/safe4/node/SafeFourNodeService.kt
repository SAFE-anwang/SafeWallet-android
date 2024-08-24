package io.horizontalsystems.bankwallet.modules.safe4.node

import com.anwang.types.masternode.MasterNodeInfo
import com.anwang.types.supernode.SuperNodeInfo
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Master_Node_Create_Amount
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Super_Node_Create_Amount
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalService
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourLockedVoteService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class SafeFourNodeService(
		val nodeType: NodeType,
		val safe4RpcBlockChain: RpcBlockchainSafe4,
		val provider: SafeFourProvider,
		val walletAddress: Address
): Clearable {

	private val TAG = "SafeFourNodeService"

	private var loadedPageNumber = 0
	private val loading = AtomicBoolean(false)
	private var allLoaded = AtomicBoolean(false)

	private var loadedPageNumberMine = 0
	private val loadingMine = AtomicBoolean(false)
	private var allLoadedMine = AtomicBoolean(false)

	private val nodeItems = CopyOnWriteArrayList<NodeInfo>()

	private val itemsSubject = PublishSubject.create<List<NodeInfo>>()
	val itemsObservable: Observable<List<NodeInfo>> get() = itemsSubject

	private val nodeInfoSubject = PublishSubject.create<NodeInfo>()
	val nodeInfoObservable: Observable<NodeInfo> get() = nodeInfoSubject

	private val registerNodeSubject = PublishSubject.create<Pair<Boolean, Boolean>>()
	val registerNodeObservable: Observable<Pair<Boolean, Boolean>> get() = registerNodeSubject


	private val mineNodeItems = CopyOnWriteArrayList<NodeInfo>()

	private val mineItemsSubject = PublishSubject.create<List<NodeInfo>>()
	val mineNodeItemsObservable: Observable<List<NodeInfo>> get() = mineItemsSubject

	private val disposables = CompositeDisposable()

	private val isSuperNode = nodeType == NodeType.SuperNode
	private var mineNodeMaxCount = -1
	private var nodeMaxCount = -1
	init {
		getMineCreatorNode()
	}

	fun loadItems(page: Int) {
		if (loading.get()) return
		loading.set(true)
		val isSuperNode = nodeType == NodeType.SuperNode
		if (nodeMaxCount == -1) {
			nodeMaxCount = safe4RpcBlockChain.getNodeNum(isSuperNode).toInt()
		}
//		val itemsCount = page * SafeFourLockedVoteService.itemsPerPage

		var itemsCount = if (isSuperNode)
			page * itemsPerPage
			else
			nodeMaxCount - (page + 1) * itemsPerPage
		var pageCount = if (isSuperNode) itemsPerPage else {
			if (itemsCount > 0) itemsPerPage else itemsPerPage + itemsCount
		}
		if (itemsCount < 0) itemsCount = 0
		if (itemsCount >= nodeMaxCount) {
			loading.set(false)
			return
		}
		val single = when(nodeType) {
			NodeType.SuperNode -> {
				safe4RpcBlockChain.superNodeGetAll(itemsCount, pageCount)
			}
			NodeType.MainNode -> {
				safe4RpcBlockChain.masterNodeGetAll(itemsCount, pageCount)
			}
		}
		var allVoteNum = BigInteger.ZERO
		single.subscribeOn(Schedulers.io())
				.map {
					val nodeList = mutableListOf<NodeInfo>()
					it.forEach { address ->
						val info = when(nodeType) {
							NodeType.SuperNode -> {
								getSuperNodeInfo(address)
							}
							NodeType.MainNode -> {
								getMasterNodeInfo(address)
							}
						}
						when(nodeType) {
							NodeType.SuperNode -> {
								info?.totalVoteNum = getTotalVoteNum(address)
								info?.totalAmount = getTotalAmount(address)
							}
							NodeType.MainNode -> {
								info?.let {
									info.totalVoteNum = info.founders.sumOf { it.amount }
								}
							}
						}
						if (allVoteNum == BigInteger.ZERO) {
							allVoteNum = getAllVoteNum()
						}

						info?.allVoteNum = allVoteNum
						info?.let {
							nodeList.add(it)
						}
					}
					nodeList
				}
				.doFinally {
					loading.set(false)
				}
				.subscribe( {
					allLoaded.set(it.isEmpty() || it.size < itemsPerPage)
					if (isSuperNode) {
						nodeItems.addAll(it)
					} else {
						nodeItems.addAll(it.reversed())
					}
					itemsSubject.onNext(nodeItems)

					loadedPageNumber = page
				},{
					Log.e(TAG, "error=$it")
				}).let {
					disposables.add(it)
				}
	}


	fun loadItemsMine(page: Int) {
		if (loadingMine.get()) return
		loadingMine.set(true)

		if (mineNodeMaxCount == -1) {
			mineNodeMaxCount = safe4RpcBlockChain.getAddrNum4Creator(isSuperNode, walletAddress.hex).blockingGet().toInt()
		}
//		val itemsCount = page * itemsPerPage

		val isSuperNode = nodeType == NodeType.SuperNode
		if (nodeMaxCount == -1) {
			nodeMaxCount = safe4RpcBlockChain.getNodeNum(isSuperNode).toInt()
		}
//		val itemsCount = page * SafeFourLockedVoteService.itemsPerPage

		var itemsCount = if (isSuperNode)
			page * itemsPerPage
		else
			mineNodeMaxCount - (page + 1) * itemsPerPage
		var pageCount = if (isSuperNode) itemsPerPage else {
			if (itemsCount > 0) itemsPerPage else itemsPerPage + itemsCount
		}
		if (itemsCount < 0) itemsCount = 0

		if (itemsCount >= mineNodeMaxCount) {
			mineItemsSubject.onNext(mineNodeItems)
			loadingMine.set(false)
			return
		}
		val single = safe4RpcBlockChain.getAddrs4Creator(isSuperNode, walletAddress.hex, itemsCount, pageCount)

		var allVoteNum = BigInteger.ZERO
		single.subscribeOn(Schedulers.io())
				.map {
					val nodeList = mutableListOf<NodeInfo>()
					it.forEach { address ->
						val info = when(nodeType) {
							NodeType.SuperNode -> {
								getSuperNodeInfo(address.value)
							}
							NodeType.MainNode -> {
								getMasterNodeInfo(address.value)
							}
						}
						when(nodeType) {
							NodeType.SuperNode -> {
								info?.totalVoteNum = getTotalVoteNum(address.value)
								info?.totalAmount = getTotalAmount(address.value)
							}
							NodeType.MainNode -> {
								info?.let {
									info.totalVoteNum = info.founders.sumOf { it.amount }
								}
							}
						}
						if (allVoteNum == BigInteger.ZERO) {
							allVoteNum = getAllVoteNum()
						}

						info?.allVoteNum = allVoteNum
						info?.let {
							nodeList.add(it)
						}
					}
					nodeList
				}
				.doFinally {
					loadingMine.set(false)
				}
				.subscribe( {
					allLoadedMine.set(it.isEmpty() || it.size < itemsPerPage)
					if (isSuperNode) {
						mineNodeItems.addAll(it)
					} else {
						mineNodeItems.addAll(it.reversed())
					}
					mineItemsSubject.onNext(mineNodeItems)

					loadedPageNumberMine = page
				},{
					Log.e(TAG, "error=$it")
				}).let {
					disposables.add(it)
				}
	}

	private fun getTotalVoteNum(address: String): BigInteger {
		try {
			return safe4RpcBlockChain.getTotalVoteNum(address)
		} catch (e: Exception) {
			return BigInteger.ZERO
		}
	}

	private fun getTotalAmount(address: String): BigInteger {
		try {
			return safe4RpcBlockChain.getTotalAmount(address)
		} catch (e: Exception) {
			return BigInteger.ZERO
		}
	}

	private fun getAllVoteNum(): BigInteger {
		try {
			return safe4RpcBlockChain.getAllVoteNum()
		} catch (e: Exception) {
			return BigInteger.ZERO
		}
	}

	private fun getSuperNodeInfo(address: String): NodeInfo? {
		try {
			val info = safe4RpcBlockChain.superNodeInfo(address)
			return covertSuperNode(info)
		} catch (e: Exception) {
			Log.e(TAG, "super node info error=$e")
			return null
		}
	}

	private fun getMasterNodeInfo(address: String): NodeInfo? {
		try {
			val info = safe4RpcBlockChain.masterNodeInfo(address)
			return covertMasterNode(info)
		} catch (e: Exception) {
			Log.e(TAG, "master node info error=$e")
			return null
		}
	}

	private fun covertSuperNode(info: SuperNodeInfo): NodeInfo {
		return NodeInfo(
				info.id.toInt(),
				io.horizontalsystems.bankwallet.entities.Address(info.addr.value),
				io.horizontalsystems.bankwallet.entities.Address(info.creator.value),
				info.enode,
				info.description,
				info.isOfficial,
				if (info.isOfficial) NodeStatus.Online else NodeStatus.Exception,
				info.founders.map {
					NodeMemberInfo(it.lockID.toInt(), io.horizontalsystems.bankwallet.entities.Address(it.addr.value), it.amount, it.height.toLong())
				},
				NodeIncentivePlan(info.incentivePlan.creator.toInt(), info.incentivePlan.partner.toInt(), info.incentivePlan.voter.toInt()),
				info.lastRewardHeight.toLong(),
				info.createHeight.toLong(),
				info.updateHeight.toLong(),
				info.name,
				availableLimit = NodeCovertFactory.scaleConvert(Super_Node_Create_Amount) - info.founders.sumOf { it.amount },
				isEdit = walletAddress.hex.equals(info.creator.value, true)

		)
	}

	private fun covertMasterNode(info: MasterNodeInfo): NodeInfo {
		return NodeInfo(
				info.id.toInt(),
				io.horizontalsystems.bankwallet.entities.Address(info.addr.value),
				io.horizontalsystems.bankwallet.entities.Address(info.creator.value),
				info.enode,
				info.description,
				info.isOfficial,
				if (info.state.toInt() == 2) NodeStatus.Exception else NodeStatus.Online ,
				info.founders.map {
					NodeMemberInfo(it.lockID.toInt(), io.horizontalsystems.bankwallet.entities.Address(it.addr.value), it.amount, it.height.toLong())
				},
				NodeIncentivePlan(info.incentivePlan.creator.toInt(), info.incentivePlan.partner.toInt(), info.incentivePlan.voter.toInt()),
				info.lastRewardHeight.toLong(),
				info.createHeight.toLong(),
				info.updateHeight.toLong(),
				availableLimit = NodeCovertFactory.scaleConvert(Master_Node_Create_Amount) - info.founders.sumOf { it.amount },
				isEdit = walletAddress.hex.equals(info.creator.value, true)
		)
	}

	private fun getMineCreatorNode() {
		val superSingle = safe4RpcBlockChain.getAddrNum4Creator(isSuperNode = true, walletAddress.hex)
		val masterSingle = safe4RpcBlockChain.getAddrNum4Creator(isSuperNode = false, walletAddress.hex)

		Single.zip(superSingle, masterSingle) {
			t1, t2 ->
			mineNodeMaxCount = if (isSuperNode) t1.toInt() else t2.toInt()
			val superList = if (t1 > BigInteger.ZERO) safe4RpcBlockChain.getAddrs4Creator(isSuperNode = true, walletAddress.hex, 0, t1.toInt()).blockingGet() else emptyList()
			val masterList = if (t2 > BigInteger.ZERO) safe4RpcBlockChain.getAddrs4Creator(isSuperNode = false, walletAddress.hex, 0, t2.toInt()).blockingGet() else emptyList()

			Pair(superList.map { it.value }, masterList.map { it.value })
		}
				.subscribeOn(Schedulers.io())
				.subscribe( { (superList, masterList) ->
					val isRegisterSuper = superList.find { it.equals(walletAddress.hex, true) } != null
					val isRegisterMaster = masterList.find { it.equals(walletAddress.hex, true) } != null
					registerNodeSubject.onNext(Pair(isRegisterSuper, isRegisterMaster))

				}, {

				})?.let {
					disposables.add(it)
				}
	}

	fun loadNext() {
		if (!allLoaded.get()) {
			loadItems(loadedPageNumber + 1)
		}
	}

	fun getNodeItem(ranking: Int): NodeInfo? {
		return nodeItems.find { it.id == ranking }
	}

	fun getNodeInfo(id: Int) {
		val single = when(nodeType) {
			NodeType.SuperNode -> {
				safe4RpcBlockChain.superNodeInfoById(id)
			}
			NodeType.MainNode -> {
				safe4RpcBlockChain.masterNodeInfoById(id)
			}
		}

		single.subscribeOn(Schedulers.io())
				.map {
					val nodeInfo = when(it) {
						is SuperNodeInfo -> {
							covertSuperNode(it)
						}

						is MasterNodeInfo -> {
							covertMasterNode(it)
						}
						else -> {
							null
						}
					}
					nodeInfo?.apply {
						this.totalAmount = getTotalAmount(this.creator.hex)
					}
					nodeInfo
				}
				.subscribe( {
					it?.let {
						nodeInfoSubject.onNext(it)
					}
				},{
					Log.e(TAG, "error=$it")
				}).let {
					disposables.add(it)
				}
	}

	override fun clear() {
		disposables.clear()
	}

	companion object {
		const val itemsPerPage = 20
	}
}

enum class NodeType {
	SuperNode, MainNode;

	companion object {
		fun getType(type: Int): NodeType {
			return when(type) {
				0 -> SuperNode
				1 -> MainNode
				else -> SuperNode
			}
		}
	}
}