package io.horizontalsystems.bankwallet.modules.safe4.node

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 在APP启动时获取超级节点信息并缓存到数据库
 *
 * 复用 SafeFourNodeService 中的 RPC 调用逻辑，但是批量拉取所有页面（而非分页加载），
 * 将所有超级节点信息一次性写入 NodeInfoDao 缓存。
 */
object SuperNodeCacheManager {

	private const val TAG = "SuperNodeCacheManager"
	private const val ITEMS_PER_PAGE = 10

	/**
	 * 获取所有超级节点信息并缓存到数据库。
	 * 需要 SafeFour 钱包已初始化且 adapter 已就绪。
	 */
	fun cacheAllSuperNodes() {
		CoroutineScope(Dispatchers.IO).launch {
			try {
				val safe4Wallet = getSafeFourWallet() ?: run {
					Log.w(TAG, "No SafeFour wallet found, skip super node caching")
					return@launch
				}

				val rpc = getRpcBlockchainSafe4(safe4Wallet) ?: run {
					Log.w(TAG, "Cannot get RpcBlockchainSafe4, skip super node caching")
					return@launch
				}

				val walletAddress = rpc.receiveAddress ?: Address("0x0000000000000000000000000000000000000000")

				// 获取超级节点总数
				val totalSuperCount = try {
					rpc.blockchain.getNodeNum(true).toInt()
				} catch (e: Exception) {
					Log.e(TAG, "Failed to get super node count", e)
					return@launch
				}

				Log.i(TAG, "Total super nodes: $totalSuperCount, start caching...")

				if (totalSuperCount <= 0) {
					Log.i(TAG, "No super nodes found")
					return@launch
				}

				val allNodes = mutableListOf<NodeInfo>()
				var offset = 0

				while (offset < totalSuperCount) {
					val pageCount = minOf(ITEMS_PER_PAGE, totalSuperCount - offset)

					try {
						val addresses = rpc.blockchain.superNodeGetAll(offset, pageCount).blockingGet()

						for (address in addresses) {
							try {
								val nodeInfo = fetchSuperNodeDetail(rpc.blockchain, address, walletAddress,allNodes.size)
								if (nodeInfo != null) {
									allNodes.add(nodeInfo)
								}
							} catch (e: Exception) {
								Log.e(TAG, "Failed to fetch super node detail for address: $address", e)
							}
						}

						offset += pageCount
						Log.d(TAG, "Fetched page ($offset/$totalSuperCount)")

					} catch (e: Exception) {
						Log.e(TAG, "Failed to fetch super node page at offset=$offset", e)
						break
					}
				}

				// 批量写入数据库
				if (allNodes.isNotEmpty()) {
					val chainType = if (App.localStorage.isSafe4TestNet) 1 else 0
					App.appDatabase.nodeInfoDao().deleteNodeInfoList(0, chainType)
					App.appDatabase.nodeInfoDao().insert(allNodes.map { it.copy(chainType = chainType) })
					Log.i(TAG, "Cached ${allNodes.size} super nodes to database")
				}
			} catch (e: Exception) {
				Log.e(TAG, "cacheAllSuperNodes failed", e)
			}
		}
	}

	/**
	 * 获取所有主节点信息并缓存到数据库
	 */
	fun cacheAllMasterNodes() {
		CoroutineScope(Dispatchers.IO).launch {
			try {
				val safe4Wallet = getSafeFourWallet() ?: run {
					Log.w(TAG, "No SafeFour wallet found, skip master node caching")
					return@launch
				}

				val rpc = getRpcBlockchainSafe4(safe4Wallet) ?: run {
					Log.w(TAG, "Cannot get RpcBlockchainSafe4, skip master node caching")
					return@launch
				}

				val walletAddress = rpc.receiveAddress ?: Address("0x0000000000000000000000000000000000000000")

				// 获取主节点总数
				val totalMasterCount = try {
					rpc.blockchain.getNodeNum(false).toInt()
				} catch (e: Exception) {
					Log.e(TAG, "Failed to get master node count", e)
					return@launch
				}

				Log.i(TAG, "Total master nodes: $totalMasterCount, start caching...")

				if (totalMasterCount <= 0) {
					Log.i(TAG, "No master nodes found")
					return@launch
				}

				val allNodes = mutableListOf<NodeInfo>()
				var offset = 0

				while (offset < totalMasterCount) {
					val pageCount = minOf(ITEMS_PER_PAGE, totalMasterCount - offset)

					try {
						val addresses = rpc.blockchain.masterNodeGetAll(offset, pageCount).blockingGet()

						for (address in addresses) {
							try {
								val nodeInfo = fetchMasterNodeDetail(rpc.blockchain, address, walletAddress)
								if (nodeInfo != null) {
									allNodes.add(nodeInfo)
								}
							} catch (e: Exception) {
								Log.e(TAG, "Failed to fetch master node detail for address: $address", e)
							}
						}

						offset += pageCount
						Log.d(TAG, "Fetched master page ($offset/$totalMasterCount)")

					} catch (e: Exception) {
						Log.e(TAG, "Failed to fetch master node page at offset=$offset", e)
						break
					}
				}

				// 批量写入数据库
				if (allNodes.isNotEmpty()) {
					val chainType = if (App.localStorage.isSafe4TestNet) 1 else 0
					App.appDatabase.nodeInfoDao().deleteNodeInfoList(1, chainType)
					App.appDatabase.nodeInfoDao().insert(allNodes.map { it.copy(chainType = chainType) })
					Log.i(TAG, "Cached ${allNodes.size} master nodes to database")
				}
			} catch (e: Exception) {
				Log.e(TAG, "cacheAllMasterNodes failed", e)
			}
		}
	}

	/**
	 * 同时缓存超级节点和主节点
	 */
	fun cacheAllNodes() {
		cacheAllSuperNodes()
		cacheAllMasterNodes()
	}

	// ========== Private helpers ==========

	private data class RpcContext(
		val blockchain: RpcBlockchainSafe4,
		val receiveAddress: Address?
	)

	private fun getSafeFourWallet(): Wallet? {
		return try {
			App.walletManager.activeWallets
				.find { it.token.blockchainType == BlockchainType.SafeFour }
		} catch (e: Exception) {
			Log.e(TAG, "Failed to get SafeFour wallet", e)
			null
		}
	}

	private fun getRpcBlockchainSafe4(wallet: Wallet): RpcContext? {
		return try {
			val adapter = App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter
				?: return null
			val blockchain = adapter.evmKitWrapper.evmKit.blockchain as? RpcBlockchainSafe4
				?: return null
			RpcContext(blockchain, adapter.evmKitWrapper.evmKit.receiveAddress)
		} catch (e: Exception) {
			Log.e(TAG, "Failed to get RpcBlockchainSafe4", e)
			null
		}
	}

	/**
	 * 获取单个超级节点的详细信息（复用 NodeCovertFactory.covertSuperNode 逻辑）
	 */
	private fun fetchSuperNodeDetail(
		rpc: RpcBlockchainSafe4,
		address: String,
		walletAddress: Address,
		index: Int
	): NodeInfo? {
		return try {
			val info = rpc.superNodeInfo(address)

			// NodeCovertFactory.covertSuperNode 是 internal 方法，这里直接使用公开的创建逻辑
			val nodeInfo = NodeInfo(
				id = info.id.toInt(),
				addr = info.addr.value,
				creator = info.creator.value,
				enode = info.enode,
				description = info.description,
				isOfficial = info.isOfficial,
				state = if (info.state.toInt() == 2) NodeStatus.Exception else NodeStatus.Online,
				founders = info.founders.map {
					NodeMemberInfo(it.lockID.toLong(), io.horizontalsystems.bankwallet.entities.Address(it.addr.value), it.amount, it.unlockHeight.toLong())
				},
				incentivePlan = NodeIncentivePlan(info.incentivePlan.creator.toInt(), info.incentivePlan.partner.toInt(), info.incentivePlan.voter.toInt()),
				lastRewardHeight = info.lastRewardHeight.toLong(),
				createHeight = info.createHeight.toLong(),
				updateHeight = info.updateHeight.toLong(),
				name = info.name,
				isEdit = walletAddress.hex.equals(info.creator.value, true),
				availableLimit = NodeCovertFactory.scaleConvert(NodeCovertFactory.Super_Node_Create_Amount) - info.founders.sumOf { it.amount },
				type = 0,
				sortOrder = index
			)

			// 补充投票和金额信息
			try {
				nodeInfo.totalVoteNum = rpc.getTotalVoteNum(address)
			} catch (_: Exception) {}
			try {
				nodeInfo.totalAmount = rpc.getTotalAmount(address)
			} catch (_: Exception) {}
			try {
				nodeInfo.allVoteNum = rpc.getAllVoteNum()
			} catch (_: Exception) {}

			nodeInfo
		} catch (e: Exception) {
			Log.e(TAG, "Failed to get super node info for $address", e)
			null
		}
	}

	/**
	 * 获取单个主节点的详细信息
	 */
	private fun fetchMasterNodeDetail(
		rpc: RpcBlockchainSafe4,
		address: String,
		walletAddress: Address
	): NodeInfo? {
		return try {
			val info = rpc.masterNodeInfo(address)

			val nodeInfo = NodeInfo(
				id = info.id.toInt(),
				addr = info.addr.value,
				creator = info.creator.value,
				enode = info.enode,
				description = info.description,
				isOfficial = info.isOfficial,
				state = if (info.state.toInt() == 2) NodeStatus.Exception else NodeStatus.Online,
				founders = info.founders.map {
					NodeMemberInfo(it.lockID.toLong(), io.horizontalsystems.bankwallet.entities.Address(it.addr.value), it.amount, it.unlockHeight.toLong())
				},
				incentivePlan = NodeIncentivePlan(info.incentivePlan.creator.toInt(), info.incentivePlan.partner.toInt(), info.incentivePlan.voter.toInt()),
				lastRewardHeight = info.lastRewardHeight.toLong(),
				createHeight = info.createHeight.toLong(),
				updateHeight = info.updateHeight.toLong(),
				isEdit = walletAddress.hex.equals(info.creator.value, true),
				availableLimit = NodeCovertFactory.scaleConvert(NodeCovertFactory.Master_Node_Create_Amount) - info.founders.sumOf { it.amount },
				type = 1
			)

			// 补充投票信息
			nodeInfo.totalVoteNum = info.founders.sumOf { it.amount }
			try {
				nodeInfo.allVoteNum = rpc.getAllVoteNum()
			} catch (_: Exception) {}

			nodeInfo
		} catch (e: Exception) {
			Log.e(TAG, "Failed to get master node info for $address", e)
			null
		}
	}
}
