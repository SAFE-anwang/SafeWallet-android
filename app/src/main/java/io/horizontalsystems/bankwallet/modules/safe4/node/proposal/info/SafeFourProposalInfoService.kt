package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.storage.ProposalStateStorage
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteRecordService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class SafeFourProposalInfoService(
		val proposalId: Int,
		val safe4RpcBlockChain: RpcBlockchainSafe4,
		val evmKitWrapper: EvmKitWrapper
): Clearable {

	var type: ProposalType = ProposalType.All

	private var loadedPageNumber = 0
	private val loading = AtomicBoolean(false)
	private var allLoaded = AtomicBoolean(false)


	private val allItems = CopyOnWriteArrayList<ProposalVote>()

	private val allSubject = PublishSubject.create<List<ProposalVote>>()
	val allItemsObservable: Observable<List<ProposalVote>> get() = allSubject


	private val topsAddress = CopyOnWriteArrayList<String>()
	private val topsSubject = PublishSubject.create<List<String>>()
	val topsObservable: Observable<List<String>> get() = topsSubject

	private var maxVoteCount = -1

	private val disposables = CompositeDisposable()

	fun loadAllItems(page: Int) {
		if (loading.get()) return
		loading.set(true)
		if (maxVoteCount == -1) {
			maxVoteCount = safe4RpcBlockChain.getProposalVoterNum(proposalId).blockingGet().toInt()
		}
		val itemsCount = page * itemsPerPage
		if (itemsCount >= maxVoteCount) {
			allSubject.onNext(allItems)
			loading.set(false)
			return
		}
		val enableSingle = safe4RpcBlockChain.getVoteInfo(proposalId, itemsCount, maxVoteCount)
		enableSingle
				.subscribeOn(Schedulers.io())
				.map {
					it.map {
						ProposalVote(it.voter.value, it.voteResult.toInt())
					}
				}
				.doFinally {
					loading.set(false)
				}
				.subscribe( {
					allLoaded.set(it.isEmpty() || it.size < maxVoteCount)
					allItems.addAll(it)
					allSubject.onNext(allItems)

					loadedPageNumber = page
				},{
				}).let {
					disposables.add(it)
				}
	}


	fun loadNext() {
		if (!allLoaded.get()) {
			loadAllItems(loadedPageNumber + 1)
		}
	}

	override fun clear() {
		disposables.clear()
	}

	fun getTops() {
		safe4RpcBlockChain.getTops()
				.subscribeOn(Schedulers.io())
				.map {
					it.map { address ->
						address.value
					}
				}.subscribe( {
					topsAddress.addAll(it)
					topsSubject.onNext(topsAddress)
				}, {

				}).let {
					disposables.add(it)
				}
	}

	fun vote(voteResult: Int): Single<String> {
		return safe4RpcBlockChain.proposalVote(evmKitWrapper.signer!!.privateKey.toHexString(), proposalId, voteResult)
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