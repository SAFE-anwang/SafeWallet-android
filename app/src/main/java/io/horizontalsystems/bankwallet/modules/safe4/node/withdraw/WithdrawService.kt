package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import com.anwang.types.accountmanager.RecordUseInfo
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfoRepository
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordManager
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

    private var repository: LockRecordInfoRepository? = null
    private var cancel = false

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

    fun setLockRecordRepository(repository: LockRecordInfoRepository) {
        this.repository = repository
    }

    fun withdraw(lockedId: List<Long>, type: Int = 0): Single<String> {
        var result = Single.just("withdraw fail")
        lockedId.chunked(50).forEach {
            result = evmKitManager.withdrawByIds(it.map { it.toBigInteger() }, type)
        }
        return result
    }

    fun removeVoteOrApproval(lockedId: List<Long>): Single<String> {
        var result = Single.just("withdraw fail")
        lockedId.chunked(50).forEach {
            result = evmKitManager.removeVoteOrApproval(it.map { it.toBigInteger() })
        }
        return result
    }


    fun loadLocked(page: Int) {
        if (loading.get() || cancel) return
        loading.set(true)
        var page = page
        if (page == 0) {
            page = (repository?.getRecordNum(getContract(), evmKitManager.evmKit.receiveAddress.hex) ?: 0) / itemsPerPage

        }
        if (maxNum == -1) {
            maxNum = safe4.getAccountTotalAmount(safe4.address.hex, type).num.toInt()
        }
        Log.d("WithdrawService", "maxNum=$maxNum")
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
                    val info = safe4.getRecordByID(id.toLong(), type)
                    val recordUseInfo = if (type == 0) safe4.getRecordUseInfo(id.toInt()) else null
                    LockedRecord(id, info.amount, info.addr.value,  info.unlockHeight, recordUseInfo)
                }
            }
            .doFinally {
                loading.set(false)
            }
            .subscribe({ records ->
                Log.d("WithdrawService", "type=$type, ${records.map { it.lockedId }}")
                allLoaded.set(records.isEmpty() || records.size < itemsPerPage)
                val infos = records/*.filter {
                    it.recordInfo.frozenAddr.value != zeroAddress && it.recordInfo.votedAddr.value != zeroAddress
                }*/.map {
                    WithdrawModule.WithDrawLockedInfo(it.lockedId.toLong(),
                        it.unlockHeight.toLong(),
                        it.recordInfo?.releaseHeight?.toLong(),
                        NodeCovertFactory.formatSafe(it.amount),
                        it.amount,
                        it.address, it.recordInfo?.votedAddr?.value, it.recordInfo?.frozenAddr?.value,
                        (it.recordInfo?.releaseHeight == BigInteger.ZERO && it.unlockHeight.toLong() < (evmKitManager.evmKit.lastBlockHeight ?: 0))
                                || (it.unlockHeight == BigInteger.ZERO && (it.recordInfo?.releaseHeight?.toLong() ?: 0) < (evmKitManager.evmKit.lastBlockHeight ?: 0)),
                        if (it.recordInfo?.votedAddr?.value == zeroAddress || type > 0) null else it.unlockHeight > BigInteger.ZERO
                    )
                }
                if (infos.isNotEmpty()) {
                    Log.d("WithdrawService", "save=${infos.size}")
                    repository?.save(
                        infos.map {
                            LockRecordInfo(
                                it.id,
                                it.unlockHeight,
                                it.releaseHeight,
                                it.value,
                                it.address,
                                it.address2,
                                it.frozenAddr,
                                getContract(),
                                evmKitManager.evmKit.receiveAddress.hex,
                                type,
                                it.withdrawEnable
                            )
                        }
                    )
                    LockRecordManager.emit()
                    loadNext()
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
        loading.set(false)
        if (!allLoaded.get()) {
            loadLocked(loadedPageNumberLocked + 1)
        }
    }

    private fun getContract(): String {
        return when(type) {
            1 -> safe4.AccountManagerContractAddr4ac6
            2 -> safe4.AccountManagerContractAddr91b2
            else -> safe4.safe4SwapContractAddress
        }
    }

    fun getRecordInfo(id: Long): LockedRecord {
        val info = safe4.getRecordByID(id.toLong(), type)
        val recordUseInfo = safe4.getRecordUseInfo(id.toInt())
        return LockedRecord(info.id, info.amount, info.addr.value, info.unlockHeight, recordUseInfo)
    }

    fun start() {
        loadLocked(0)
    }

    fun deleteLockedInfo() {
        val idList = repository?.getRecordIds(getContract(), evmKitManager.evmKit.receiveAddress.hex)
        idList?.let {ids ->
            ids.forEach {
                try {
                    val record = getRecordInfo(it)
                    if (record.lockedId == BigInteger.ZERO) {
                        repository?.delete(it, getContract())
                    }
                } catch (e: Exception) {
                    android.util.Log.e("deleteLockedInfo", "error=$e")
                }
            }
        }
    }

    override fun clear() {
        disposables.clear()
    }

    fun cancel() {
        this.cancel = true
    }

    companion object {
        const val itemsPerPage = 20
    }
}

data class LockedRecord(
    val lockedId: BigInteger,
    val amount: BigInteger,
    val address: String,
    val unlockHeight: BigInteger,
    val recordInfo: RecordUseInfo?
)