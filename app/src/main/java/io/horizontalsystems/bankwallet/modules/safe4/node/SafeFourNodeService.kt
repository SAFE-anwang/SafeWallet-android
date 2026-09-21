package io.horizontalsystems.bankwallet.modules.safe4.node

import com.anwang.types.masternode.MasterNodeInfo
import com.anwang.types.supernode.SuperNodeInfo
import android.util.Log
import io.horizontalsystems.bankwallet.core.App
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
import org.apache.commons.lang3.StringUtils
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class SafeFourNodeService(
		val nodeType: NodeType,
		val safe4RpcBlockChain: RpcBlockchainSafe4,
		val walletAddress: Address
): Clearable {

	private val TAG = "SafeFourNodeService"

	private var loadedPageNumber = 0
	private val loading = AtomicBoolean(false)
	private var allLoaded = AtomicBoolean(false)

	private var loadedPageNumberMine = 0
	private val loadingMine = AtomicBoolean(false)
	private var allLoadedMine = AtomicBoolean(false)

	private var loadedPageNumberPartner = 0
	private val loadingPartner = AtomicBoolean(false)
	private var allLoadedPartner = AtomicBoolean(false)

	private val nodeItems = CopyOnWriteArrayList<NodeInfo>()

	private val itemsSubject = PublishSubject.create<List<NodeInfo>>()
	val itemsObservable: Observable<List<NodeInfo>> get() = itemsSubject

	private val nodeInfoSubject = PublishSubject.create<NodeInfo>()
	val nodeInfoObservable: Observable<NodeInfo> get() = nodeInfoSubject

	private val searchResultSubject = PublishSubject.create<List<NodeInfo>>()
	val searchResultObservable: Observable<List<NodeInfo>> get() = searchResultSubject

	private val registerNodeSubject = PublishSubject.create<Pair<Boolean, Boolean>>()
	val registerNodeObservable: Observable<Pair<Boolean, Boolean>> get() = registerNodeSubject


	private val mineNodeItems = CopyOnWriteArrayList<NodeInfo>()

	private val mineItemsSubject = PublishSubject.create<List<NodeInfo>>()
	val mineNodeItemsObservable: Observable<List<NodeInfo>> get() = mineItemsSubject

	var creatorList = CopyOnWriteArrayList<String>()
	private val creatorSubject = PublishSubject.create<List<String>>()
	val creatorObservable: Observable<List<String>> get() = creatorSubject


	var isSuperOrMasterNode = false
	private val isSuperOrMasterNodeSubject = PublishSubject.create<Boolean>()
	val isSuperOrMasterNodeObservable: Observable<Boolean> get() = isSuperOrMasterNodeSubject



	var isFounder = false
	private val isFounderNodeSubject = PublishSubject.create<Boolean>()
	val isFounderNodeObservable: Observable<Boolean> get() = isFounderNodeSubject


	private val disposables = CompositeDisposable()

	private val isSuperNode = nodeType == NodeType.SuperNode
	private var mineNodeMaxCount = -1
	private var nodeMaxCount = -1
	private var partnerNodeMaxCount = -1

	private var reloadCountAll = 0
	private var reloadCountMine = 0
	private var reloadCountPartner = 0

	fun loadItems(page: Int) {
		try {
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
				itemsSubject.onNext(nodeItems)
				loading.set(false)
				return
			}
			val single = when (nodeType) {
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
							val info = when (nodeType) {
								NodeType.SuperNode -> {
									getSuperNodeInfo(address)
								}

								NodeType.MainNode -> {
									getMasterNodeInfo(address)
								}
							}
							when (nodeType) {
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
							// 主节点与超级节点都需要排序字段，否则读缓存时 ORDER BY sortOrder 失效
							info?.sortOrder = nodeList.size
							info?.let {
								nodeList.add(it)
							}
						}
						if (nodeList.isNotEmpty()) {
							val chainType = if (App.localStorage.isSafe4TestNet) 1 else 0
							// 注意：这里只做 upsert，不能先 deleteNodeInfoList。
							// 分页加载每次只拿到当前页（itemsPerPage 条），若先清空再写入，
							// 会把启动时缓存的全量数据删成只剩当前页，导致缓存越用越少。
							App.appDatabase.nodeInfoDao().insert(nodeList.map { it.copy(chainType = chainType) })
						}
						nodeList
					}
					.doFinally {
						loading.set(false)
					}
					.subscribe({
						allLoaded.set(it.isEmpty() || it.size < itemsPerPage)
						if (isSuperNode) {
							nodeItems.addAll(it)
						} else {
							nodeItems.addAll(it.reversed())
						}
						itemsSubject.onNext(nodeItems)

						loadedPageNumber = page
					}, {
						Log.e(TAG, "error=$it")
					}).let {
						disposables.add(it)
					}
		} catch (e: Exception) {
			if (reloadCountAll < 3) {
				reloadCountAll ++
				loading.set(false)
				loadItems(page)
			}
		}
	}


	fun loadItemsMine(page: Int) {
		loadItemsPartner(page)
		try {
			if (loadingMine.get()) return
			loadingMine.set(true)

			if (mineNodeMaxCount == -1) {
				mineNodeMaxCount = safe4RpcBlockChain.getAddrNum4Creator(isSuperNode, walletAddress.hex).blockingGet().toInt()
			}
			val isSuperNode = nodeType == NodeType.SuperNode

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
		} catch (e: Exception) {
			if (reloadCountMine < 3) {
				reloadCountMine++
				loadingMine.set(false)
				loadItemsMine(page)
			}
		}
	}


	private fun loadItemsPartner(page: Int) {
		try {
			if (loadingPartner.get()) return
			loadingPartner.set(true)

			if (partnerNodeMaxCount == -1) {
				partnerNodeMaxCount = safe4RpcBlockChain.getAddrNum4Partner(isSuperNode, walletAddress.hex).blockingGet().toInt()
			}
			val isSuperNode = nodeType == NodeType.SuperNode

			var itemsCount = if (isSuperNode)
				page * itemsPerPage
			else
				partnerNodeMaxCount - (page + 1) * itemsPerPage
			var pageCount = if (isSuperNode) itemsPerPage else {
				if (itemsCount > 0) itemsPerPage else itemsPerPage + itemsCount
			}
			if (itemsCount < 0) itemsCount = 0

			if (itemsCount >= partnerNodeMaxCount) {
				mineItemsSubject.onNext(mineNodeItems)
				loadingPartner.set(false)
				return
			}
			val single = safe4RpcBlockChain.getAddrs4Partner(isSuperNode, walletAddress.hex, itemsCount, pageCount)

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
						loadingPartner.set(false)
					}
					.subscribe( {
						allLoadedPartner.set(it.isEmpty() || it.size < itemsPerPage)
						if (isSuperNode) {
							mineNodeItems.addAll(it)
						} else {
							mineNodeItems.addAll(it.reversed())
						}
						mineItemsSubject.onNext(mineNodeItems)

						loadedPageNumberPartner = page
					},{
						Log.e(TAG, "error=$it")
					}).let {
						disposables.add(it)
					}
		} catch (e: Exception) {
			if (reloadCountPartner < 3) {
				reloadCountPartner++
				loadingPartner.set(false)
				loadItemsPartner(page)
			}
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
			return NodeCovertFactory.covertSuperNode(info, walletAddress)
		} catch (e: Exception) {
			Log.e(TAG, "super node info error=$e")
			return null
		}
	}

	private fun getMasterNodeInfo(address: String): NodeInfo? {
		try {
			val info = safe4RpcBlockChain.masterNodeInfo(address)
			return NodeCovertFactory.covertMasterNode(info, walletAddress)
		} catch (e: Exception) {
			Log.e(TAG, "master node info error=$e")
			return null
		}
	}

	/**
	 * 从接口查询节点：
	 * - 纯数字视为节点 ID，走 xxxNodeInfoById；
	 * - 否则视为地址，走 xxxNodeInfo(address)。
	 *
	 * 查询结果经 [searchResultObservable] 发出（未查到为空列表）。
	 */
	fun searchNodeByQuery(query: String) {
		val trimmed = query.trim()
		if (trimmed.isEmpty()) return

		val single: Single<NodeInfo?> = (if (StringUtils.isNumeric(trimmed)) {
            // 分别为两种节点类型显式声明泛型，避免 when 分支把 Single 的泛型
            // 推断为共同的父类型（DynamicStruct）导致 convert 方法签名不匹配
            val nodeId = trimmed.toInt()
            val infoSingle: Single<NodeInfo> = when (nodeType) {
                NodeType.SuperNode -> safe4RpcBlockChain.superNodeInfoById(nodeId)
                    .map { info -> NodeCovertFactory.covertSuperNode(info, walletAddress) }

                NodeType.MainNode -> safe4RpcBlockChain.masterNodeInfoById(nodeId)
                    .map { info -> NodeCovertFactory.covertMasterNode(info, walletAddress) }
            }
            infoSingle
        } else {
            // 显式声明可空泛型，与 then 分支的 Single<NodeInfo> 统一为 Single<NodeInfo?>
            val addressSingle: Single<NodeInfo?> = Single.fromCallable<NodeInfo?> {
                when (nodeType) {
                    NodeType.SuperNode -> getSuperNodeInfo(trimmed)
                    NodeType.MainNode -> getMasterNodeInfo(trimmed)
                }
            }
            addressSingle
        }) as Single<NodeInfo?>

		single
				.subscribeOn(Schedulers.io())
				.onErrorReturn { e ->
					Log.e(TAG, "search node error=$e")
					null
				}
				.map { node -> listOfNotNull(node) }
				.subscribe { result ->
					searchResultSubject.onNext(result)
				}?.let {
					disposables.add(it)
				}
	}


	fun getMineCreatorNode() {
		try {
			val isSuperSingle = safe4RpcBlockChain.nodeExist(isSuper = true, walletAddress.hex)
			val isMasterSingle = safe4RpcBlockChain.nodeExist(isSuper = false, walletAddress.hex)

			Single.zip(isSuperSingle, isMasterSingle) { t1, t2 ->
				Pair(t1 , t2)
			}
					.subscribeOn(Schedulers.io())
					.subscribe({ (isSuperNode, isMasterNode) ->
						registerNodeSubject.onNext(Pair(isSuperNode, isMasterNode))
					}, {

					})?.let {
						disposables.add(it)
					}
		} catch (e: Exception) {

		}
	}

	fun getTops4Creator() {
		if (isSuperNode) {
			safe4RpcBlockChain.getTops4Creator(walletAddress.hex)
					.subscribeOn(Schedulers.io())
					.subscribe({
						creatorList.addAll(it)
						creatorSubject.onNext(creatorList)
					}, {

					})?.let {
						disposables.add(it)
					}
		}
	}

	fun checkNodeExist(address: String) {
		safe4RpcBlockChain.existNodeAddress(address)
				.subscribeOn(Schedulers.io())
				.subscribe({
					isSuperOrMasterNode = it
					isSuperOrMasterNodeSubject.onNext(isSuperOrMasterNode)
				}, {

				}).let {
					disposables.add(it)
				}
		existNodeFounder(address)
	}

	private fun existNodeFounder(address: String) {
		safe4RpcBlockChain.existNodeFounder(address)
				.subscribeOn(Schedulers.io())
				.subscribe({
					isFounder = it
					isFounderNodeSubject.onNext(isFounder)
				}, {

				}).let {
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
		try {
			val single = when (nodeType) {
				NodeType.SuperNode -> {
					safe4RpcBlockChain.superNodeInfoById(id)
				}

				NodeType.MainNode -> {
					safe4RpcBlockChain.masterNodeInfoById(id)
				}
			}

			var allVoteNum = BigInteger.ZERO
			single.subscribeOn(Schedulers.io())
					.map {
						val nodeInfo = when (it) {
							is SuperNodeInfo -> {
								NodeCovertFactory.covertSuperNode(it, walletAddress)
							}

							is MasterNodeInfo -> {
								NodeCovertFactory.covertMasterNode(it, walletAddress)
							}

							else -> {
								null
							}
						}
						nodeInfo?.apply {
							when (nodeType) {
								NodeType.SuperNode -> {
									this.totalVoteNum = getTotalVoteNum(this.addr)
									this.totalAmount = getTotalAmount(this.addr)
								}

								NodeType.MainNode -> {
									this.totalVoteNum = this.founders.sumOf { it.amount }
								}
							}
						}

						if (allVoteNum == BigInteger.ZERO) {
							allVoteNum = getAllVoteNum()
						}

						nodeInfo?.allVoteNum = allVoteNum
						nodeInfo
					}
					.subscribe({
						it?.let {
							nodeInfoSubject.onNext(it)
						}
					}, {
						Log.e(TAG, "error=$it")
					}).let {
						disposables.add(it)
					}
		} catch (e: Exception) {
			getNodeInfo(id)
		}
	}

	override fun clear() {
		disposables.clear()
	}

	companion object {
		const val itemsPerPage = 10
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