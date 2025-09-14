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


    @Query("SELECT * FROM LockRecordInfo WHERE unlockHeight>:currentHeight ORDER BY " +
            "id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getRecordsPaged(currentHeight: Long, limit: Int, offset: Int): List<LockRecordInfo>


    @Query("SELECT * FROM LockRecordInfo WHERE type = 0 ORDER BY " +
            "id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getVoteRecordsPaged(limit: Int, offset: Int): List<LockRecordInfo>

    //
    @Query("SELECT * FROM LockRecordInfo WHERE unlockHeight<:currentHeight ORDER BY id ASC")
    fun getRecordsForEnableWithdraw(currentHeight: Long): List<LockRecordInfo>?

    @Query("SELECT COUNT(*) as total_count FROM LockRecordInfo")
    fun getLockRecordTotal(): Int

    @Query("SELECT COUNT(*) FROM LockRecordInfo  WHERE contact = :contact AND creator = :creator")
    fun getLockRecordNum(contact: String, creator: String): Int
}
