package io.horizontalsystems.bankwallet.modules.safe4.node.vote

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

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

	fun loadItems(page: Int) {
		try {
			if (!isSuperNode) return
			if (loading.get()) return
			loading.set(true)
			if (maxVoteCount == -1) {
				maxVoteCount = safe4RpcBlockChain.getVoterNum(nodeAddress).blockingGet().toInt()
			}
			val itemsCount = page * itemsPerPage
			if (maxVoteCount == -1) {
				loading.set(false)
				return
			}
			val single = safe4RpcBlockChain.getVoters(nodeAddress, 0, maxVoteCount)
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
						allLoaded.set(it.isEmpty() || it.size < itemsPerPage)
						voteRecordItems.addAll(it)
						itemsSubject.onNext(voteRecordItems)

						loadedPageNumber = page
					}, {
					}).let {
						disposables.add(it)
					}
		} catch (e: Exception) {
			if (reloadCount < 3) {
				reloadCount ++
				loading.set(false)
				loadItems(page)
			}
		}
	}


	fun loadNext() {
		if (!allLoaded.get()) {
			loadItems(loadedPageNumber + 1)
		}
	}

	override fun clear() {
		disposables.clear()
	}

	companion object {
		const val itemsPerPage = 20
	}
}