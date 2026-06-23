package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.Redeem

@Dao
interface RedeemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(redeemInfo: Redeem)

    @Update
    fun update(redeemInfo: Redeem)

    @Query("SELECT * FROM Redeem WHERE chainType = :chainType")
    fun getAll(chainType: Int): List<Redeem>

    @Query("DELETE FROM Redeem WHERE chainType = :chainType")
    fun clear(chainType: Int)
}
