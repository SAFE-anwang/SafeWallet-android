package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class SafeFourProposalService(
		val safe4RpcBlockChain: RpcBlockchainSafe4,
		val evmKitWrapper: EvmKitWrapper
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

	private var allProposalNum = -1
	private var mineProposalNum = -1
	private val disposables = CompositeDisposable()

	init {
		getAllNum()
		getMinNum()
	}

	private fun getAllNum() {
		if (allProposalNum == -1) {
			safe4RpcBlockChain.getProposalNum()
					.subscribeOn(Schedulers.io())
					.subscribe( {
						allProposalNum = it.toInt()
						loadAllItems(loadedPageNumber)
					},{
					}).let {
						disposables.add(it)
					}
		} else {
			loadAllItems(loadedPageNumber)
		}
	}

	private fun getMinNum() {
		if (mineProposalNum == -1) {
			safe4RpcBlockChain.getMineNum(evmKitWrapper.signer!!.privateKey.toHexString())
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
	}

	fun loadAllItems(page: Int) {
		if (loading.get()) return
		loading.set(true)
		/*val itemsCount = *//*allProposalNum - *//*page * itemsPerPage
		val enableSingle = safe4RpcBlockChain.getAllProposal(itemsCount, itemsPerPage)*/
		var itemsCount = allProposalNum - (page + 1) * itemsPerPage
		var pageCount = if (itemsCount > 0) itemsPerPage else itemsPerPage + itemsCount
		if (itemsCount < 0) itemsCount = 0
		val enableSingle = safe4RpcBlockChain.getAllProposal(itemsCount, pageCount)
		enableSingle
				.subscribeOn(Schedulers.io())
				.map {
					it.map { id ->
						val info = safe4RpcBlockChain.getProposalInfo(id.toInt())
						covert(info)
					}
				}
				.doFinally {
					loading.set(false)
				}
				.subscribe( { enableRecord ->
					allLoaded.set(enableRecord.isEmpty() || enableRecord.size < itemsPerPage)
					allItems.addAll(enableRecord.reversed())
					allSubject.onNext(allItems)

					loadedPageNumber = page
				},{
				}).let {
					disposables.add(it)
				}
	}


	fun loadMineItems(page: Int) {
		if (loadingMine.get()) return
		loadingMine.set(true)

//		val itemsCount = /*mineProposalNum -*/ page * itemsPerPage
		var itemsCount = mineProposalNum - (page + 1) * itemsPerPage
		var pageCount = if (itemsCount > 0) itemsPerPage else itemsPerPage + itemsCount
		if (itemsCount < 0) itemsCount = 0
		// already vote
		val disableSingle = safe4RpcBlockChain.getMineProposal(evmKitWrapper.signer!!.privateKey.toHexString(), itemsCount, pageCount)

		disableSingle
				.subscribeOn(Schedulers.io())
				.map {
					it.map { id ->
						val info = safe4RpcBlockChain.getProposalInfo(id.toInt())

						covert(info)
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
				}
	}

	private fun covert(info: com.anwang.types.proposal.ProposalInfo): ProposalInfo {
		val state = if (info.state.toInt() == 0 && info.endPayTime.toLong() < System.currentTimeMillis() / 1000) {
			2
		} else {
			info.state.toInt()
		}
		return ProposalInfo(
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
				info.updateHeight.toLong()
		)
	}

	fun loadNext() {
		if (!allLoaded.get()) {
			/*if (allProposalNum == -1) {
				getAllNum()
			} else {*/
				loadAllItems(loadedPageNumber + 1)
//			}
		}
		if (!allLoadedMine.get()) {
			/*if (mineProposalNum == -1) {
				getMinNum()
			} else {*/
				loadMineItems(loadedPageNumberMine + 1)
//			}
		}
	}

	fun getProposalInfo(id: Int, type: Int):ProposalInfo?{
		return if (type == 0) {
			allItems.find { it.id == id }
		} else {
			mineItems.find { it.id == id }
		}
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