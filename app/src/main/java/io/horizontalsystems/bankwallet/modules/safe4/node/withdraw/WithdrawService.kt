package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import com.anwang.types.accountmanager.RecordUseInfo
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class WithdrawService(
    val safe4: RpcBlockchainSafe4,
    val evmKitManager: EvmKitWrapper,
    val type: Int = 0
): Clearable {

    val zeroAddress = "0x0000000000000000000000000000000000000000"

    private val disposables = CompositeDisposable()

    private val itemsSubject = BehaviorSubject.create<NodeInfo>()
    val itemsObservable: Observable<NodeInfo> get() = itemsSubject

    private var loadedPageNumberLocked = 0
    private val loading = AtomicBoolean(false)
    private var allLoaded = AtomicBoolean(false)

    private val lockedIdsItemsLocked = CopyOnWriteArrayList<WithdrawModule.WithDrawLockedInfo>()

    private val itemsSubjectAvailable = PublishSubject.create<List<WithdrawModule.WithDrawLockedInfo>>()
    val itemsObservableAvailable: Observable<List<WithdrawModule.WithDrawLockedInfo>> get() = itemsSubjectAvailable
    var maxNum = -1

    fun getNodeInfo(isSuperNode: Boolean) {
        val nodeInfo = if (isSuperNode) {
            val superNodeInfo = safe4.superNodeInfo(safe4.address.hex)
            NodeCovertFactory.covertSuperNode(superNodeInfo, safe4.address)
        } else {
            val masterNodeInfo = safe4.masterNodeInfo(safe4.address.hex)
            NodeCovertFactory.covertMasterNode(masterNodeInfo, safe4.address)
        }
        itemsSubject.onNext(nodeInfo)
    }

    fun withdraw(lockedId: List<Int>, type: Int = 0): Single<String> {
        var result = Single.just("withdraw fail")
        lockedId.chunked(50).forEach {
            result = evmKitManager.withdrawByIds(it.map { it.toBigInteger() }, type)
        }
        return result
    }


    fun loadLocked(page: Int) {
        if (loading.get()) return
        loading.set(true)
        if (maxNum == -1) {
            maxNum = safe4.getAccountTotalAmount(safe4.address.hex, type).num.toInt()
        }
        Log.d("locked total", "maxNum=$maxNum")
        if (maxNum <= 0) {
            loading.set(false)
            itemsSubjectAvailable.onNext(lockedIdsItemsLocked)
            return
        }
        var itemsCount = page * itemsPerPage
        if (maxNum < itemsCount)  itemsCount = maxNum
        if (allLoaded.get()) {
            loading.set(false)
            return
        }
        // already vote
        val disableSingle =
            safe4.getTotalIDs(type, safe4.address.hex, itemsCount, itemsPerPage)

        disableSingle
            .subscribeOn(Schedulers.io())
            .map {
                it.map { id ->
                    val info = safe4.getRecordByID(id.toInt(), type)
                    val recordUseInfo = if (type == 0) safe4.getRecordUseInfo(id.toInt()) else null
                    LockedRecord(id, info.amount,  info.unlockHeight, recordUseInfo)
                }
            }
            .doFinally {
                loading.set(false)
            }
            .subscribe({ records ->
                allLoaded.set(records.isEmpty() || records.size < itemsPerPage)
                val infos = records/*.filter {
                    it.recordInfo.frozenAddr.value != zeroAddress && it.recordInfo.votedAddr.value != zeroAddress
                }*/.map {
                    WithdrawModule.WithDrawLockedInfo(it.lockedId.toInt(),
                        it.unlockHeight.toLong(),
                        it.recordInfo?.releaseHeight?.toLong(),
                        NodeCovertFactory.formatSafe(it.amount),
                        it.amount,
                        it.recordInfo?.votedAddr?.value, null,
                        (it.recordInfo?.releaseHeight == BigInteger.ZERO && it.unlockHeight.toLong() < (evmKitManager.evmKit.lastBlockHeight ?: 0))
                                || (it.unlockHeight == BigInteger.ZERO && (it.recordInfo?.releaseHeight?.toLong() ?: 0) < (evmKitManager.evmKit.lastBlockHeight ?: 0)),
                        if (it.recordInfo?.votedAddr?.value == zeroAddress || type > 0) null else it.unlockHeight > BigInteger.ZERO
                    )
                }
                lockedIdsItemsLocked.addAll(infos)
                itemsSubjectAvailable.onNext(lockedIdsItemsLocked)

                loadedPageNumberLocked = page
            }, {
            }).let {
                disposables.add(it)
            }
    }

    fun loadNext() {
        if (!allLoaded.get()) {
            loadLocked(loadedPageNumberLocked + 1)
        }
    }

    override fun clear() {
        disposables.clear()
    }

    companion object {
        const val itemsPerPage = 20
    }
}

data class LockedRecord(
    val lockedId: BigInteger,
    val amount: BigInteger,
    val unlockHeight: BigInteger,
    val recordInfo: RecordUseInfo?
)