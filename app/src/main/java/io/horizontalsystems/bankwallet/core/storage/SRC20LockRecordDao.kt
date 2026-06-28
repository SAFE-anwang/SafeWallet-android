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

    @Query("DELETE FROM SRC20LockedInfo WHERE id = :id AND contract = :contact AND chainType = :chainType")
    fun delete(id: Long, contact: String, chainType: Int)

    @Query("DELETE FROM SRC20LockedInfo WHERE id IN (:id) AND contract = :contact AND chainType = :chainType")
    fun delete(id: List<Long>, contact: String, chainType: Int)


    @Query("SELECT * FROM SRC20LockedInfo WHERE address=:creator AND chainType = :chainType " +
            "ORDER BY id ASC " +
            "LIMIT :limit OFFSET :offset")
    fun getRecordsPaged(creator: String, chainType: Int, limit: Int, offset: Int): List<SRC20LockedInfo>

    @Query("SELECT amount FROM SRC20LockedInfo WHERE address = :address AND contract = :contract AND chainType = :chainType")
    fun getLockValue(address: String, contract: String, chainType: Int): List<BigInteger>

    @Query("SELECT COUNT(*) as total_count FROM SRC20LockedInfo  WHERE  address = :address AND contract = :contract AND chainType = :chainType")
    fun getLockNum(address: String, contract: String, chainType: Int): Long

    @Query("DELETE FROM SRC20LockedInfo WHERE chainType = :chainType")
    fun delete(chainType: Int)

    @Query("SELECT id FROM SRC20LockedInfo WHERE address = :address AND contract = :contract AND chainType = :chainType")
    fun getLockId(address: String, contract: String, chainType: Int): List<String>

}
