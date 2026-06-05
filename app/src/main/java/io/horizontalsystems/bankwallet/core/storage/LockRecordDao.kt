package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfo

@Dao
interface LockRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(token: LockRecordInfo)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tokens: List<LockRecordInfo>)

    @Update
    fun update(tokens: List<LockRecordInfo>)

    @Update
    fun update(token: LockRecordInfo)

    @Query("DELETE FROM LockRecordInfo WHERE id = :id AND contact = :contact AND chainType = :chainType")
    fun delete(id: Long, contact: String, chainType: Int)

    @Query("DELETE FROM LockRecordInfo WHERE id IN (:id) AND contact = :contact AND chainType = :chainType")
    fun delete(id: List<Long>, contact: String, chainType: Int)


    @Query("SELECT * FROM LockRecordInfo WHERE creator=:creator AND chainType = :chainType " +
            "ORDER BY id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getRecordsPaged(creator: String, chainType: Int, limit: Int, offset: Int): List<LockRecordInfo>


    @Query("SELECT * FROM LockRecordInfo WHERE type = 0 AND creator=:creator AND chainType = :chainType " +
            "ORDER BY id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getVoteRecordsPaged(creator: String, chainType: Int, limit: Int, offset: Int): List<LockRecordInfo>


    @Query("SELECT * FROM LockRecordInfo WHERE type = 0 AND creator=:creator AND chainType = :chainType " +
            "AND releaseHeight IS NOT NULL AND releaseHeight > 0")
    fun queryNeedUpdateRecords(creator: String, chainType: Int): List<LockRecordInfo>

    //
    @Query("SELECT * FROM LockRecordInfo WHERE " +
            "(releaseHeight IS NULL OR releaseHeight = 0) AND " +
            "unlockHeight<=:currentHeight AND creator=:creator AND chainType = :chainType ORDER BY id ASC")
    fun getRecordsForEnableWithdraw(creator: String, chainType: Int, currentHeight: Long): List<LockRecordInfo>?

    //
    @Query("SELECT id FROM LockRecordInfo WHERE " +
            "(releaseHeight IS NULL OR releaseHeight = 0) AND " +
            "unlockHeight<=:currentHeight AND creator=:creator AND type=:type AND chainType = :chainType")
    fun getEnableWithdrawIds(creator: String, chainType: Int, currentHeight: Long, type: Int): List<Long>?

    //
    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo WHERE " +
            "(releaseHeight IS NULL OR releaseHeight = 0) AND " +
            "unlockHeight<=:currentHeight AND creator=:creator AND chainType = :chainType")
    fun getWithdrawEnableCount(creator: String, chainType: Int, currentHeight: Long): Long

    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo WHERE creator=:creator AND chainType = :chainType ")
    fun getLockRecordTotal(creator: String, chainType: Int): Int

    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo WHERE creator= :creator AND type = 0 AND chainType = :chainType")
    fun getVoteLockRecordTotal(creator: String, chainType: Int): Int

    @Query("SELECT COUNT(*) FROM LockRecordInfo  WHERE contact = :contact AND creator = :creator AND chainType = :chainType")
    fun getLockRecordNum(contact: String, creator: String, chainType: Int): Int


    @Query("SELECT * FROM LockRecordInfo WHERE creator= :creator AND releaseHeight>0 AND type = 0 AND chainType = :chainType")
    fun getRecordsVoteLockRecord(creator: String, chainType: Int): List<LockRecordInfo>

    @Query("SELECT id FROM LockRecordInfo WHERE creator=:creator AND contact=:contact AND chainType = :chainType ORDER BY id ASC ")
    fun getLockedIds(contact: String, creator: String, chainType: Int): List<Long>


    @Query("SELECT * FROM LockRecordInfo WHERE type = 0 AND creator=:creator AND chainType = :chainType AND releaseHeight != 0 " +
            /*" AND unlockHeight > 0 " +*/
            "ORDER BY id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getVotedRecordsPaged(creator: String, chainType: Int, limit: Int, offset: Int): List<LockRecordInfo>

    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo WHERE  type = 0 AND creator=:creator AND chainType = :chainType AND releaseHeight != 0")
    fun getEnableReleaseVoteTotal(creator: String, chainType: Int): Int

    @Query("SELECT id FROM LockRecordInfo WHERE type = 0 AND creator=:creator AND chainType = :chainType AND releaseHeight != 0 AND releaseHeight <= :currentHeight ")
    fun getEnableReleaseVoteLockedIds(creator: String, chainType: Int, currentHeight: Long): List<Long>

}
