package io.horizontalsystems.bankwallet.modules.safe4.node.vote

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService.Companion
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class SafeFourVoteRecordService(
		val isSuperNode: Boolean,
		val safe4RpcBlockChain: RpcBlockchainSafe4,
		val nodeAddress: String,
		val nodeId: Int
): Clearable {

	private var loadedPageNumber = 0
	private val loading = AtomicBoolean(false)
	private var allLoaded = AtomicBoolean(false)

	private val voteRecordItems = CopyOnWriteArrayList<VoteRecordInfo>()

	private val itemsSubject = PublishSubject.create<List<VoteRecordInfo>>()
	val itemsObservable: Observable<List<VoteRecordInfo>> get() = itemsSubject

	private var maxVoteCount = -1
	private val disposables = CompositeDisposable()

	private var reloadCount = 0

	fun loadItems(start: Int, count: Int) {
		try {
			Log.d("longwen", "start=$start, count=$count")
			val single = safe4RpcBlockChain.getVoters(nodeAddress, start, count)
			single.subscribeOn(Schedulers.io())
					.map {
						val recordList = it.addrs.mapIndexed { index, address ->
							VoteRecordInfo(
									address.value,
									it.voteNums[index]
							)
						}
						recordList
					}
					.doFinally {
						loading.set(false)
					}
					.subscribe({
						voteRecordItems.addAll(it)
						itemsSubject.onNext(voteRecordItems)

					}, {
					}).let {
						disposables.add(it)
					}
		} catch (e: Exception) {
			Log.e("longwen", "error=$e")
			if (reloadCount < 3) {
				reloadCount ++
				loading.set(false)
				loadItems(start, count)
			}
		}
	}

	fun load() {
		var page = 0
		try {
			if (!isSuperNode) return
			if (loading.get()) return
			loading.set(true)
			if (maxVoteCount == -1) {
				maxVoteCount = safe4RpcBlockChain.getVoterNum(nodeAddress).blockingGet().toInt()
			}
			if (maxVoteCount <= 0) {
				itemsSubject.onNext(voteRecordItems)
				loading.set(false)
				return
			}
			var itemsCount = 0
			var pageCount = itemsPerPage
			if (pageCount >= maxVoteCount) {
				pageCount = maxVoteCount
			}

			while(pageCount < maxVoteCount) {
				loadItems(itemsCount, pageCount)
				page ++
				itemsCount = itemsPerPage * page
				if (itemsCount > maxVoteCount) {
					itemsCount = maxVoteCount
				}
				pageCount = itemsCount + itemsPerPage
				if (pageCount > maxVoteCount) {
					pageCount = maxVoteCount
				}
			}

		} catch (e: Exception) {

		}

	}

	fun loadNext() {
		if (!allLoaded.get()) {
//			loadItems(loadedPageNumber + 1)
		}
	}

	override fun clear() {
		disposables.clear()
	}

	companion object {
		const val itemsPerPage = 20
	}
}