package io.horizontalsystems.bankwallet.modules.safe4.node.vote

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalService
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalService.Companion
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

	val zeroAddress = "0x0000000000000000000000000000000000000000"

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


	private val mineItems = CopyOnWriteArrayList<LockIdsInfo>()

	private val mineItemsSubject = PublishSubject.create<List<LockIdsInfo>>()
	val mineItemsObservable: Observable<List<LockIdsInfo>> get() = mineItemsSubject

	private val loadingMine = AtomicBoolean(false)

	private var enableLockedMaxCount = -1
	private var disableLockedMaxCount = -1
	private var mineProposalNum = -1
	private var getMineNumCount = 0
	private var getMineListCount = 0

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
						getLockInfo(id.toLong())
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

	private fun getLockInfo(id: Long):LockIdsInfo {
		val info = safe4RpcBlockChain.getRecordByID(id)

		// 查询记录锁定信息
		val lockInfo = safe4RpcBlockChain.getRecordUseInfo(id.toInt())
		val isSuperNode =
			safe4RpcBlockChain.superAddressExist(lockInfo.frozenAddr.value)
		// 当前高度小于releaseheight时也不能投票
		val currentHeight = ethereumKit.lastBlockHeight ?: 0L
		val enabled = lockInfo.votedAddr.value == zeroAddress
				&& !isSuperNode && currentHeight > lockInfo.releaseHeight.toLong()
		return LockIdsInfo(
			id.toLong(), info.amount,
			NodeCovertFactory.valueConvert(info.amount).toInt() >= 1 && enabled,
			unlockHeight = info.unlockHeight,
			releaseHeight = lockInfo.releaseHeight,
			address = lockInfo.votedAddr.value,
			address2 = lockInfo.frozenAddr.value
		)
	}

	fun lockEnableVote(lockRecordInfo: LockRecordInfo): Boolean {
		val isSuperNode =
			lockRecordInfo.frozenAddr?.let { safe4RpcBlockChain.superAddressExist(it) } ?: false
		// 当前高度小于releaseheight时也不能投票
		val currentHeight = ethereumKit.lastBlockHeight ?: 0L
//		Log.d("LockedInfoViewModel", "id=${lockRecordInfo.id}, currentHeight=$currentHeight, releaseHeight=${lockRecordInfo.releaseHeight}, isSuperNode=$isSuperNode, ${lockRecordInfo.address2}")
		val enable1 = lockRecordInfo.address2 == zeroAddress
		val enable2 = lockRecordInfo.address2 != zeroAddress && currentHeight > (lockRecordInfo.releaseHeight ?: 0)
		return NodeCovertFactory.valueConvert(lockRecordInfo.value).toInt() >= 1 && !isSuperNode && (enable1 || enable2)
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
			Log.d("WithdrawService", "disableLockedMaxCount=$disableLockedMaxCount, ${itemsCount}, $itemsPerPage")
		// already vote
		val disableSingle = safe4RpcBlockChain.getVotedIDs4Voter(address.hex, itemsCount, itemsPerPage)

		disableSingle
				.subscribeOn(Schedulers.io())
				.map {
					it.map { id ->
						val info = safe4RpcBlockChain.getRecordByID(id.toLong())
						// 查询记录锁定信息
						val lockInfo = safe4RpcBlockChain.getRecordUseInfo(id.toInt())
						Log.d("WithdrawService", "lockInfo=$lockInfo")
						LockIdsInfo(id.toLong(), info.amount,
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



	fun getMinProposalNum() {
		try {
			if (mineProposalNum == -1) {
				safe4RpcBlockChain.getMineNum(ethereumKit.receiveAddress.hex)
					.subscribeOn(Schedulers.io())
					.subscribe( {
						mineProposalNum = it.toInt()
						loadMineProposal()
					},{
					}).let {
						disposables.add(it)
					}
			} else {
				loadMineProposal()
			}
		} catch (e: Exception) {
			if (!disposables.isDisposed && getMineNumCount < 3) {
				getMineNumCount ++
				getMinProposalNum()
			}
		}
	}

	fun loadMineProposal() {
		try {
			if (mineProposalNum == 0) {
				mineItemsSubject.onNext(mineItems)
				return
			}
			if (loadingMine.get()) return
			loadingMine.set(true)

			// already vote
			val disableSingle = safe4RpcBlockChain.getMineProposal(ethereumKit.receiveAddress.hex, 0, mineProposalNum)

			disableSingle
				.subscribeOn(Schedulers.io())
				.map {
					it.map { id ->
						val rewards = safe4RpcBlockChain.getRewardIDs(id.toInt())
						rewards.map {
							getLockInfo(it.toLong())
						}
					}.flatMap { it }
				}
				.doFinally {
					loadingMine.set(false)
				}
				.subscribe( { disableRecord ->
					mineItems.addAll(disableRecord)
					mineItemsSubject.onNext(mineItems)
				},{
				}).let {
					disposables.add(it)
				}
		} catch (e: Exception) {
			if (getMineListCount < 3) {
				getMineListCount++
				loadingMine.set(false)
				loadMineProposal()
			}
		}
	}

	override fun clear() {
		disposables.clear()
	}

	companion object {
		const val itemsPerPage = 10
	}
}