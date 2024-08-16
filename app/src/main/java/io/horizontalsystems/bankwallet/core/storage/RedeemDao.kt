package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.Redeem

@Dao
interface RedeemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(redeemInfo: Redeem)

    @Update
    fun update(redeemInfo: Redeem)

    @Query("SELECT * FROM Redeem")
    fun getAll(): List<Redeem>

    @Query("DELETE FROM Redeem")
    fun clear()
}
