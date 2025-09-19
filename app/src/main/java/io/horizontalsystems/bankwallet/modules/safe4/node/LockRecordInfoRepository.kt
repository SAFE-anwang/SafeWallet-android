package io.horizontalsystems.bankwallet.modules.safe4.node

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.storage.LockRecordDao

class LockRecordInfoRepository(
    private val lockRecordDao: LockRecordDao
) {

    fun getRecordsPaged(creator: String, currentHeight: Long, limit: Int, offset: Int): List<LockRecordInfo> {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        return lockRecordDao.getRecordsPaged(creator, currentHeight, limit, offset)
    }

    fun getVoteRecordsPaged(creator: String, limit: Int, offset: Int): List<LockRecordInfo> {
        return lockRecordDao.getVoteRecordsPaged(creator, limit, offset)
    }

    fun queryNeedUpdateRecords(creator: String): List<LockRecordInfo> {
        return lockRecordDao.queryNeedUpdateRecords(creator)
    }

    fun getRecordsForEnableWithdraw(creator: String, currentHeight: Long): List<LockRecordInfo>? {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        return lockRecordDao.getRecordsForEnableWithdraw(creator, currentHeight)
    }

    fun getTotal(creator: String): Int {
        return lockRecordDao.getLockRecordTotal(creator)
    }

    fun getVoteTotal(creator: String): Int {
        return lockRecordDao.getVoteLockRecordTotal(creator)
    }

    fun getRecordNum(contract: String, creator: String): Int {
        return lockRecordDao.getLockRecordNum(contract, creator)
    }

    fun save(datas: List<LockRecordInfo>) {
        lockRecordDao.insert(datas.filter { it.id != 0L })
    }

    fun delete(lockId: Long, contract: String) {
        lockRecordDao.delete(lockId, contract)
    }

    fun getRecordIds(contract: String, creator: String): List<Long> {
        return lockRecordDao.getLockedIds(contract, creator)
    }
}