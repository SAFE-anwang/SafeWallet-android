package io.horizontalsystems.bankwallet.modules.safe4.node

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.storage.LockRecordDao

class LockRecordInfoRepository(
    private val lockRecordDao: LockRecordDao
) {

    fun getRecordsPaged(currentHeight: Long, limit: Int, offset: Int): List<LockRecordInfo> {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        return lockRecordDao.getRecordsPaged(currentHeight, limit, offset)
    }

    fun getVoteRecordsPaged(limit: Int, offset: Int): List<LockRecordInfo> {
        return lockRecordDao.getVoteRecordsPaged(limit, offset)
    }

    fun getRecordsForEnableWithdraw(currentHeight: Long): List<LockRecordInfo>? {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        return lockRecordDao.getRecordsForEnableWithdraw(currentHeight)
    }

    fun getTotal(): Int {
        return lockRecordDao.getLockRecordTotal()
    }

    fun getRecordNum(contract: String, creator: String): Int {
        return lockRecordDao.getLockRecordNum(contract, creator)
    }

    fun save(datas: List<LockRecordInfo>) {
        lockRecordDao.insert(datas)
    }

    fun delete(lockId: Long, contract: String) {
        lockRecordDao.delete(lockId, contract)
    }

}