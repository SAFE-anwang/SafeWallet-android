package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService.Companion
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class SafeFourProposalService(
		val safe4RpcBlockChain: RpcBlockchainSafe4,
		val evmKitWrapper: EvmKitWrapper,
		val repository: ProposalRecordRepository,
	val isWithdraw: Boolean = false
): Clearable {

	var type: ProposalType = ProposalType.All

	private var loadedPageNumber = 0
	private val loading = AtomicBoolean(false)
	private var allLoaded = AtomicBoolean(false)

	private var loadedPageNumberMine = 0
	private val loadingMine = AtomicBoolean(false)
	private var allLoadedMine = AtomicBoolean(false)

	private val allItems = CopyOnWriteArrayList<ProposalInfo>()

	private val allSubject = PublishSubject.create<List<ProposalInfo>>()
	val allItemsObservable: Observable<List<ProposalInfo>> get() = allSubject

	private val mineItems = CopyOnWriteArrayList<ProposalInfo>()

	private val mineItemsSubject = PublishSubject.create<List<ProposalInfo>>()
	val mineItemsObservable: Observable<List<ProposalInfo>> get() = mineItemsSubject

	private val lastBlockHeightSubject = PublishSubject.create<Long>()
	val lastBlockHeightObservable: Observable<Long> get() = lastBlockHeightSubject

	private var allProposalNum = -1
	private var mineProposalNum = -1
	private val disposables = CompositeDisposable()

	private var getAllNumCount = 0
	private var getMineNumCount = 0
	private var getAllListCount = 0
	private var getMineListCount = 0


	var lockRecordTotal = 0
	var offset = 0
	var page = 0
	val limit = 20

	init {
		lastBlockHeight()

//		getMinNum()
	}

	private fun lastBlockHeight() {
		evmKitWrapper.evmKit.lastBlockHeightFlowable
				.subscribeOn(Schedulers.io())
				.subscribe {
					lastBlockHeightSubject.onNext(it)
				}
				.let { disposables.add(it) }
	}

	fun getLastBlockHeight(): Long {
		return safe4RpcBlockChain.lastBlockHeight ?: 0
	}

	fun getCacheData() {
		lockRecordTotal = repository.getTotal()
		Log.d("Proposal", "proposal total num = $lockRecordTotal")
		if (lockRecordTotal == 0)   return
		try {
			offset = page * limit
			val records = repository.getRecordsPaged(limit, offset)
			val info = records.map {
				covert(it)
			}
			allItems.addAll(info)
			allSubject.onNext(allItems)
			page ++
		} catch (e: Exception) {
			Log.d("Proposal", "get proposal error = $e")
		}
	}

	fun getAllNum() {
		try {
			if (allProposalNum == -1) {
				safe4RpcBlockChain.getProposalNum()
						.subscribeOn(Schedulers.io())
						.subscribe({
							allProposalNum = it.toInt()
							loadAllItems(loadedPageNumber)
						}, {
						}).let {
							disposables.add(it)
						}
			} else {
				loadAllItems(loadedPageNumber)
			}
		} catch (e: Exception) {
			if (!disposables.isDisposed && getAllNumCount < 3) {
				getAllNumCount ++
				getAllNum()
			}
		}
	}

	/*fun getMinNum() {
		try {
			if (mineProposalNum == -1) {
				safe4RpcBlockChain.getMineNum(evmKitWrapper.evmKit.receiveAddress.hex)
						.subscribeOn(Schedulers.io())
						.subscribe( {
							mineProposalNum = it.toInt()
							loadMineItems(loadedPageNumberMine)
						},{
						}).let {
							disposables.add(it)
						}
			} else {
				loadMineItems(loadedPageNumberMine)
			}
		} catch (e: Exception) {
			if (!disposables.isDisposed && getMineNumCount < 3) {
				getMineNumCount ++
				getMinNum()
			}
		}
	}*/

	fun loadAllItems(page: Int, lastCreateTime: Long = 1725926400) {
		try {
			val allProposalNum = safe4RpcBlockChain.getProposalNum().blockingGet().toInt()
			Log.d("Proposal", "allProposalNum=$allProposalNum")
			if (allProposalNum == 0 || allProposalNum == repository.getTotal()) {
				return
			}
			if (loading.get()) return
			loading.set(true)

			var page = page
			if (page == 0) {
				page = repository.getTotal() / WithdrawService.itemsPerPage
			}

			var itemsCount = allProposalNum - (page + 1) * itemsPerPage
			var pageCount = if (itemsCount > 0) itemsPerPage else itemsPerPage + itemsCount
			if (itemsCount < 0) itemsCount = 0
			val enableSingle = safe4RpcBlockChain.getAllProposal(itemsCount, pageCount)
			val record = enableSingle.blockingGet()
			Log.d("Proposal", "allProposal =$record")
			val recordInfo = record.map { id ->
				val info = safe4RpcBlockChain.getProposalInfo(id.toInt())
				covertData(info, lastCreateTime)
			}
			repository.save(recordInfo)
			loadedPageNumber = page
			loadAllItems(page + 1)

		} catch (e: Exception) {
			loading.set(false)
//			loadAllItems(page)
		}

	}


	fun loadMineItems() {
		try {
			if (mineProposalNum == 0) {
				mineItemsSubject.onNext(mineItems)
				return
			}
			if (loadingMine.get()) return
			loadingMine.set(true)
			val creater = evmKitWrapper.evmKit.receiveAddress.hex
			val mineRecord = repository.getMineRecordsPaged(creater)
			val infos = mineRecord.map {
				val rewards = safe4RpcBlockChain.getRewardIDs(it.id)
				covert(it, rewards.map { it.toLong() })
			}
			mineItems.addAll(infos)
			mineItemsSubject.onNext(mineItems)

			/*var itemsCount = mineProposalNum - (page + 1) * itemsPerPage
			var pageCount = if (itemsCount > 0) itemsPerPage else itemsPerPage + itemsCount
			if (itemsCount < 0) itemsCount = 0
			// already vote
			val disableSingle = safe4RpcBlockChain.getMineProposal(evmKitWrapper.evmKit.receiveAddress.hex, itemsCount, pageCount)

			disableSingle
					.subscribeOn(Schedulers.io())
					.map {
						it.map { id ->
							val info = safe4RpcBlockChain.getProposalInfo(id.toInt())
							val rewards = safe4RpcBlockChain.getRewardIDs(id.toInt())

							covert(info, rewards.map { it.toLong() })
						}
					}
					.doFinally {
						loadingMine.set(false)
					}
					.subscribe( { disableRecord ->
						allLoadedMine.set(disableRecord.isEmpty() || disableRecord.size < itemsPerPage)
						mineItems.addAll(disableRecord.reversed())
						mineItemsSubject.onNext(mineItems)

						loadedPageNumberMine = page
					},{
					}).let {
						disposables.add(it)
					}*/
		} catch (e: Exception) {
			loadingMine.set(false)
			loadMineItems()
		}
	}

	private fun covert(info: ProposalRecordInfo, list: List<Long> = listOf()): ProposalInfo {
		val state = if (info.state.toInt() == 0 && info.endPayTime.toLong() < System.currentTimeMillis() / 1000) {
			2
		} else {
			info.state.toInt()
		}
		return ProposalInfo(
				info.id,
				info.creator,
				info.title,
				info.payAmount,
				info.payTimes,
				info.startPayTime,
				info.endPayTime,
				info.description,
				state,
				info.createHeight ?: 0,
				info.updateHeight ?: 0,
			list
		)
	}

	private fun covertData(info: com.anwang.types.proposal.ProposalInfo, lastCreateTime: Long): ProposalRecordInfo {
		val state = if (info.state.toInt() == 0 && info.endPayTime.toLong() < System.currentTimeMillis() / 1000) {
			2
		} else {
			info.state.toInt()
		}
		return ProposalRecordInfo(
				info.id.toInt(),
				info.creator.value,
				info.title,
				info.payAmount,
				info.payTimes.toLong(),
				info.startPayTime.toLong(),
				info.endPayTime.toLong(),
				info.description,
				state,
				info.createHeight.toLong(),
				info.updateHeight.toLong(),
			if (info.startPayTime.toLong() * 1000 > lastCreateTime) 1 else 0
		)
	}

	fun loadNext() {
		if (!allLoaded.get() && !isWithdraw) {
			getCacheData()
		}

	}


	fun getProposalInfo(id: Int, type: Int):ProposalInfo?{
		return if (type == 0) {
			allItems.find { it.id == id }
		} else {
			mineItems.find { it.id == id }
		}
	}

	fun start() {
		loadAllItems(0, repository.getLocalLastCreateTime())
	}

	override fun clear() {
		disposables.clear()
	}

	companion object {
		const val itemsPerPage = 20
	}
}

enum class ProposalType {
	All, Mine;

	companion object {
		fun getType(type: Int): ProposalType {
			return when(type) {
				0 -> All
				1 -> Mine
				else -> All
			}
		}
	}
}