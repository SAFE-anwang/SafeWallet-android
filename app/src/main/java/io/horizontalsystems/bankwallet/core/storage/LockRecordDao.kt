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

    @Query("DELETE FROM LockRecordInfo WHERE id = :id AND contact = :contact")
    fun delete(id: Long, contact: String)

    @Query("DELETE FROM LockRecordInfo WHERE id IN (:id) AND contact = :contact")
    fun delete(id: List<Long>, contact: String)


    @Query("SELECT * FROM LockRecordInfo WHERE creator=:creator " +
            "ORDER BY id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getRecordsPaged(creator: String, limit: Int, offset: Int): List<LockRecordInfo>


    @Query("SELECT * FROM LockRecordInfo WHERE type = 0 AND creator=:creator " +
            "ORDER BY id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getVoteRecordsPaged(creator: String, limit: Int, offset: Int): List<LockRecordInfo>


    @Query("SELECT * FROM LockRecordInfo WHERE type = 0 AND creator=:creator " +
            "AND releaseHeight IS NOT NULL AND releaseHeight > 0")
    fun queryNeedUpdateRecords(creator: String): List<LockRecordInfo>

    //
    @Query("SELECT * FROM LockRecordInfo WHERE " +
            "(releaseHeight IS NULL OR releaseHeight = 0) AND " +
            "unlockHeight<=:currentHeight AND creator=:creator ORDER BY id ASC")
    fun getRecordsForEnableWithdraw(creator: String, currentHeight: Long): List<LockRecordInfo>?

    //
    @Query("SELECT id FROM LockRecordInfo WHERE " +
            "(releaseHeight IS NULL OR releaseHeight = 0) AND " +
            "unlockHeight<=:currentHeight AND creator=:creator AND type=:type")
    fun getEnableWithdrawIds(creator: String, currentHeight: Long, type: Int): List<Long>?

    //
    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo WHERE " +
            "(releaseHeight IS NULL OR releaseHeight = 0) AND " +
            "unlockHeight<=:currentHeight AND creator=:creator")
    fun getWithdrawEnableCount(creator: String, currentHeight: Long): Long

    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo WHERE creator=:creator ")
    fun getLockRecordTotal(creator: String): Int

    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo WHERE creator= :creator AND type = 0")
    fun getVoteLockRecordTotal(creator: String): Int

    @Query("SELECT COUNT(*) FROM LockRecordInfo  WHERE contact = :contact AND creator = :creator")
    fun getLockRecordNum(contact: String, creator: String): Int


    @Query("SELECT * FROM LockRecordInfo WHERE creator= :creator AND releaseHeight>0 AND type = 0")
    fun getRecordsVoteLockRecord(creator: String): List<LockRecordInfo>

    @Query("SELECT id FROM LockRecordInfo WHERE creator=:creator AND contact=:contact ORDER BY id ASC ")
    fun getLockedIds(contact: String, creator: String): List<Long>


    @Query("SELECT * FROM LockRecordInfo WHERE type = 0 AND creator=:creator AND releaseHeight != 0 AND releaseHeight <= :currentHeight " +
            " AND unlockHeight > 0 " +
            "ORDER BY id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getVotedRecordsPaged(creator: String, currentHeight: Long, limit: Int, offset: Int): List<LockRecordInfo>

    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo WHERE  type = 0 AND creator=:creator AND releaseHeight != 0 AND releaseHeight <= :currentHeight" +
            " AND unlockHeight > 0 ")
    fun getEnableReleaseVoteTotal(creator: String, currentHeight: Long): Int

    @Query("SELECT id FROM LockRecordInfo WHERE type = 0 AND creator=:creator AND releaseHeight != 0 AND releaseHeight <= :currentHeight " +
            " AND unlockHeight > 0 ")
    fun getEnableReleaseVoteLockedIds(creator: String, currentHeight: Long): List<Long>

}
