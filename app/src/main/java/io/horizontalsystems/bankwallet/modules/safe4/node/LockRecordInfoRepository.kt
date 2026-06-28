package io.horizontalsystems.bankwallet.modules.safe4.node

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.storage.LockRecordDao

class LockRecordInfoRepository(
    private val lockRecordDao: LockRecordDao
) {

    fun getRecordsPaged(creator: String, currentHeight: Long, limit: Int, offset: Int): List<LockRecordInfo> {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        val chainType = getChainType()
        return lockRecordDao.getRecordsPaged(creator, chainType, limit, offset)
    }

    fun getVoteRecordsPaged(creator: String, limit: Int, offset: Int): List<LockRecordInfo> {
        val chainType = getChainType()
        return lockRecordDao.getVoteRecordsPaged(creator, chainType, limit, offset)
    }

    fun getEnableReleaseVotedRecordsPaged(creator: String, currentHeight: Long, limit: Int, offset: Int): List<LockRecordInfo> {
        val chainType = getChainType()
        return lockRecordDao.getVotedRecordsPaged(creator, chainType, limit, offset)
    }

    fun queryNeedUpdateRecords(creator: String): List<LockRecordInfo> {
        val chainType = getChainType()
        return lockRecordDao.queryNeedUpdateRecords(creator, chainType)
    }

    fun getRecordsForEnableWithdraw(creator: String, currentHeight: Long): List<LockRecordInfo>? {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        val chainType = getChainType()
        return lockRecordDao.getRecordsForEnableWithdraw(creator, chainType, currentHeight)
    }

    fun getEnableWithdrawIds(creator: String, currentHeight: Long, type: Int): List<Long>? {
        Log.d("LockedInfoViewModel", "currentHeight=$currentHeight")
        val chainType = getChainType()
        return lockRecordDao.getEnableWithdrawIds(creator, chainType, currentHeight, type)
    }

    fun getTotal(creator: String): Int {
        val chainType = getChainType()
        return lockRecordDao.getLockRecordTotal(creator, chainType)
    }

    fun getEnableReleaseVoteTotal(creator: String, currentHeight: Long): Int {
        val chainType = getChainType()
        return lockRecordDao.getEnableReleaseVoteTotal(creator, chainType)
    }

    fun getWithdrawEnableCount(creator: String, currentHeight: Long): Long {
        val chainType = getChainType()
        return lockRecordDao.getWithdrawEnableCount(creator, chainType, currentHeight)
    }

    fun getVoteTotal(creator: String): Int {
        val chainType = getChainType()
        return lockRecordDao.getVoteLockRecordTotal(creator, chainType)
    }

    fun getRecordNum(contract: String, creator: String): Int {
        val chainType = getChainType()
        return lockRecordDao.getLockRecordNum(contract, creator, chainType)
    }

    fun save(datas: List<LockRecordInfo>) {
        val chainType = getChainType()
        lockRecordDao.insert(datas.filter { it.id != 0L }.map { it.copy(chainType = chainType) })
    }

    fun update(data: LockRecordInfo) {
        val chainType = getChainType()
        lockRecordDao.update(data.copy(chainType = chainType))
    }

    fun delete(lockId: Long, contract: String) {
        val chainType = getChainType()
        lockRecordDao.delete(lockId, contract, chainType)
    }

    fun delete(lockId: List<Long>, contract: String) {
        val chainType = getChainType()
        lockRecordDao.delete(lockId, contract, chainType)
    }

    fun getRecordIds(contract: String, creator: String): List<Long> {
        val chainType = getChainType()
        return lockRecordDao.getLockedIds(contract, creator, chainType)
    }

    fun getEnableReleaseVoteRecordIds(contract: String, currentHeight: Long): List<Long> {
        val chainType = getChainType()
        return lockRecordDao.getEnableReleaseVoteLockedIds(contract, chainType, currentHeight)
    }

    fun getRecordsVoteLockRecord(creator: String): List<LockRecordInfo> {
        val chainType = getChainType()
        return lockRecordDao.getRecordsVoteLockRecord(creator, chainType)
    }

    private fun getChainType(): Int = if (App.localStorage.isSafe4TestNet) 1 else 0
}