package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.SRC20LockedInfo
import java.math.BigInteger

@Dao
interface SRC20LockRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(token: SRC20LockedInfo)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tokens: List<SRC20LockedInfo>)

    @Update
    fun update(tokens: List<SRC20LockedInfo>)

    @Update
    fun update(token: SRC20LockedInfo)

    @Query("DELETE FROM SRC20LockedInfo WHERE id = :id AND contract = :contact")
    fun delete(id: Long, contact: String)

    @Query("DELETE FROM SRC20LockedInfo WHERE id IN (:id) AND contract = :contact")
    fun delete(id: List<Long>, contact: String)


    @Query("SELECT * FROM SRC20LockedInfo WHERE address=:creator " +
            "ORDER BY id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getRecordsPaged(creator: String, limit: Int, offset: Int): List<SRC20LockedInfo>

    @Query("SELECT amount FROM SRC20LockedInfo WHERE address = :address AND contract = :contract")
    fun getLockValue(address: String, contract: String): List<BigInteger>

    @Query("SELECT COUNT(*) as total_count FROM SRC20LockedInfo  WHERE  address = :address AND contract = :contract")
    fun getLockNum(address: String, contract: String): Long

    @Query("DELETE FROM SRC20LockedInfo")
    fun delete()
}
