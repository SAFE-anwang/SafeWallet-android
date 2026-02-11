package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.util.Log
import com.anwang.src20.SRC20LockFactory
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.SRC20LockedInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordManager
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawModule
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService.Companion.itemsPerPage
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.models.Chain
import io.reactivex.schedulers.Schedulers
import org.web3j.abi.datatypes.Address
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class SRC20LockedInfoService(
    val rpcBlockchainSafe4: RpcBlockchainSafe4,
    val contractAddress: String
) {

    private val TAG = "SRC20LockedInfoService"

    var maxNum = -1
    private val loading = AtomicBoolean(false)
    private var allLoaded = AtomicBoolean(false)

    private val lockRecordDao by lazy{
        App.appDatabase.src20RecordDao()
    }

    private fun getContract(): Address {
        return Address(contractAddress)
    }

    private fun getAddress(): Address {
        return Address(rpcBlockchainSafe4.address.hex)
    }

    private fun getLockedIDNum(): Long {
        return try {
            return rpcBlockchainSafe4.src20LockFactory.getLockedIDNum(getContract(), getAddress()).toLong()
        } catch (e: Exception) {
            Log.d(TAG, "getLockedIDNum error=$e")
            0
        }
    }

    private fun getCacheRecordNum(): Long {
        return lockRecordDao.getLockNum(contractAddress, rpcBlockchainSafe4.address.hex)
    }

    fun updateLockInfo() {
        val ids = lockRecordDao.getLockId(rpcBlockchainSafe4.address.hex, contractAddress.lowercase())
        ids.forEach {
            val record = getRecordInfo(it.toLong())
            if (record != null && record.id == "0") {
                lockRecordDao.delete(record.id.toLong(), contractAddress)
            }
        }
    }

    fun loadLocked(page: Int) {
        if (loading.get()) return
        loading.set(true)
        var page = page
        if (page == 0) {
            page = getCacheRecordNum().toInt()  / itemsPerPage
        }
        if (maxNum == -1) {
            maxNum = getLockedIDNum().toInt()
        }
        Log.d(TAG, "maxNum=$maxNum, page=$page")
        if (maxNum <= 0) {
            loading.set(false)
            return
        }
        var itemsCount = page * itemsPerPage
        if (maxNum < itemsCount)  itemsCount = maxNum
        if (allLoaded.get()) {
            loading.set(false)
            return
        }
        val ids =
            rpcBlockchainSafe4.src20LockFactory.getLockedIDs(getContract(), getAddress(), itemsCount.toBigInteger(), itemsPerPage.toBigInteger())
        ids.map { id ->
            getRecordInfo(id.toLong())
        }.forEach {
            if (it != null) {
                Log.d(TAG, "lock info=$it")
                lockRecordDao.insert(it)
            }
        }
    }

    private fun getRecordInfo(id: Long): SRC20LockedInfo? {
        return try {
            val record =
                rpcBlockchainSafe4.src20LockFactory.getRecordByID(getContract(), id.toBigInteger())
            return SRC20LockedInfo(
                record.id.toLong().toString(),
                record.addr.value,
                record.amount,
                record.lockDay.toInt(),
                record.startHeight.toLong(),
                record.unlockHeight.toLong(),
                contractAddress.lowercase()
            )
        } catch (e: Exception) {
            Log.d(TAG, "getRecordInfo error=$e")
            null
        }
    }

}