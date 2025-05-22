package io.horizontalsystems.bankwallet.modules.safe4.node.vote

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class SafeFourLockedVoteService(
		val safe4RpcBlockChain: RpcBlockchainSafe4,
		val ethereumKit: EthereumKit,
		val address: Address
): Clearable {

	private var loadedPageNumber = 0
	private val loading = AtomicBoolean(false)
	private var allLoaded = AtomicBoolean(false)

	private var loadedPageNumberLocked = 0
	private val loadingLocked = AtomicBoolean(false)
	private var allLoadedLocked = AtomicBoolean(false)

	private val lockedIdsItems = CopyOnWriteArrayList<LockIdsInfo>()

	private val itemsSubject = PublishSubject.create<List<LockIdsInfo>>()
	val itemsObservable: Observable<List<LockIdsInfo>> get() = itemsSubject

	private val lockedIdsItemsLocked = CopyOnWriteArrayList<LockIdsInfo>()

	private val itemsSubjectLocked = PublishSubject.create<List<LockIdsInfo>>()
	val itemsObservableLocked: Observable<List<LockIdsInfo>> get() = itemsSubjectLocked

	private var enableLockedMaxCount = -1
	private var disableLockedMaxCount = -1

	private val disposables = CompositeDisposable()


	fun loadItems(page: Int) {
		if (loading.get()) return
		loading.set(true)
		try {
			val itemsCount = page * itemsPerPage
			val enableSingle = safe4RpcBlockChain.getLockIds(address.hex, itemsCount, itemsPerPage)
			enableSingle
				.subscribeOn(Schedulers.io())
				.map {
					it.map { id ->
						val info = safe4RpcBlockChain.getRecordByID(id.toInt())

						// 查询记录锁定信息
						val lockInfo = safe4RpcBlockChain.getRecordUseInfo(id.toInt())
						val isSuperNode =
							safe4RpcBlockChain.superAddressExist(lockInfo.frozenAddr.value)
						// 当前高度小于releaseheight时也不能投票
						val currentHeight = ethereumKit.lastBlockHeight ?: 0L
						val enabled =
							!isSuperNode && currentHeight > lockInfo.releaseHeight.toLong()
						LockIdsInfo(
							id.toInt(), info.amount,
							NodeCovertFactory.valueConvert(info.amount).toInt() >= 1 && enabled,
							unlockHeight = info.unlockHeight,
							releaseHeight = lockInfo.releaseHeight,
							address = lockInfo.votedAddr.value,
							address2 = lockInfo.frozenAddr.value
						)
					}
				}
				.doFinally {
					loading.set(false)
				}
				.subscribe({ enableRecord ->
					allLoaded.set(enableRecord.isEmpty() || enableRecord.size < itemsPerPage)
					lockedIdsItems.addAll(enableRecord)
					itemsSubject.onNext(lockedIdsItems)

					loadedPageNumber = page
				}, {
					loadItems(page)
				}).let {
					disposables.add(it)
				}
		} catch (e: Exception) {
			loading.set(false)
			loadItems(page)
		} finally {
		}
	}


	fun loadItemsLocked(page: Int) {
		if (loadingLocked.get()) return
		loadingLocked.set(true)

		try {
		if (disableLockedMaxCount == -1) {
			disableLockedMaxCount = safe4RpcBlockChain.getVotedIDNum4Voter(address.hex).blockingGet().toInt()
		}
		val itemsCount = page * itemsPerPage
		if (itemsCount >= disableLockedMaxCount) {
			loadingLocked.set(false)
			itemsSubjectLocked.onNext(lockedIdsItemsLocked)
			return
		}
		// already vote
		val disableSingle = safe4RpcBlockChain.getVotedIDs4Voter(address.hex, itemsCount, itemsPerPage)

		disableSingle
				.subscribeOn(Schedulers.io())
				.map {
					it.map { id ->
						val info = safe4RpcBlockChain.getRecordByID(id.toInt())
						// 查询记录锁定信息
						val lockInfo = safe4RpcBlockChain.getRecordUseInfo(id.toInt())
						LockIdsInfo(id.toInt(), info.amount,
							lockInfo.releaseHeight.toLong() < (ethereumKit.lastBlockHeight ?: 0L),
							unlockHeight = info.unlockHeight,
							releaseHeight = lockInfo.releaseHeight,
							address = lockInfo.votedAddr.value)
					}
				}
				.doFinally {
					loadingLocked.set(false)
				}
				.subscribe( { disableRecord ->
					allLoadedLocked.set(disableRecord.isEmpty() || disableRecord.size < itemsPerPage)
					lockedIdsItemsLocked.addAll(disableRecord)
					itemsSubjectLocked.onNext(lockedIdsItemsLocked)

					loadedPageNumberLocked = page
				},{
					loadItemsLocked(page)
				}).let {
					disposables.add(it)
				}
		} catch (e: Exception) {
			loadingLocked.set(false)
			loadItemsLocked(page)
		} finally {
		}
	}

	fun loadNext() {
		if (!allLoaded.get()) {
			loadItems(loadedPageNumber + 1)
		}
		if (!allLoadedLocked.get()) {
			loadItemsLocked(loadedPageNumberLocked + 1)
		}
	}

	override fun clear() {
		disposables.clear()
	}

	companion object {
		const val itemsPerPage = 10
	}
}