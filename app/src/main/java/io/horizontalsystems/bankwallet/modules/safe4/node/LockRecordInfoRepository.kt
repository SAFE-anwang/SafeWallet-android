package io.horizontalsystems.bankwallet.modules.safe4.node

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.storage.LockRecordDao

class LockRecordInfoRepository(
    private val lockRecordDao: LockRecordDao
) {

    fun getRecordsPaged(creator: String, currentHeight: Long, limit: Int, offset: Int): List<LockRecordInfo> {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        return lockRecordDao.getRecordsPaged(creator, limit, offset)
    }

    fun getVoteRecordsPaged(creator: String, limit: Int, offset: Int): List<LockRecordInfo> {
        return lockRecordDao.getVoteRecordsPaged(creator, limit, offset)
    }

    fun getEnableReleaseVotedRecordsPaged(creator: String, currentHeight: Long, limit: Int, offset: Int): List<LockRecordInfo> {
        return lockRecordDao.getVotedRecordsPaged(creator, currentHeight, limit, offset)
    }

    fun queryNeedUpdateRecords(creator: String): List<LockRecordInfo> {
        return lockRecordDao.queryNeedUpdateRecords(creator)
    }

    fun getRecordsForEnableWithdraw(creator: String, currentHeight: Long): List<LockRecordInfo>? {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        return lockRecordDao.getRecordsForEnableWithdraw(creator, currentHeight)
    }

    fun getEnableWithdrawIds(creator: String, currentHeight: Long, type: Int): List<Long>? {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        return lockRecordDao.getEnableWithdrawIds(creator, currentHeight, type)
    }

    fun getTotal(creator: String): Int {
        return lockRecordDao.getLockRecordTotal(creator)
    }

    fun getEnableReleaseVoteTotal(creator: String, currentHeight: Long): Int {
        return lockRecordDao.getEnableReleaseVoteTotal(creator, currentHeight)
    }

    fun getWithdrawEnableCount(creator: String, currentHeight: Long): Long {
        return lockRecordDao.getWithdrawEnableCount(creator, currentHeight)
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

    fun update(data: LockRecordInfo) {
        lockRecordDao.update(data)
    }

    fun delete(lockId: Long, contract: String) {
        lockRecordDao.delete(lockId, contract)
    }

    fun delete(lockId: List<Long>, contract: String) {
        lockRecordDao.delete(lockId, contract)
    }

    fun getRecordIds(contract: String, creator: String): List<Long> {
        return lockRecordDao.getLockedIds(contract, creator)
    }

    fun getEnableReleaseVoteRecordIds(contract: String, currentHeight: Long): List<Long> {
        return lockRecordDao.getEnableReleaseVoteLockedIds(contract, currentHeight)
    }

    fun getRecordsVoteLockRecord(creator: String): List<LockRecordInfo> {
        return lockRecordDao.getRecordsVoteLockRecord(creator)
    }
}