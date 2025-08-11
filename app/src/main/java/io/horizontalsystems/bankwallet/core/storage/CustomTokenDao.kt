package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken

@Dao
interface CustomTokenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(token: CustomToken)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tokens: List<CustomToken>)

    @Update
    fun update(tokens: List<CustomToken>)

    @Update
    fun update(token: CustomToken)

    @Query("UPDATE CustomToken SET logoURI = :logoUrl WHERE address = :address")
    fun updateLogo(logoUrl: String, address: String)

    @Query("DELETE FROM CustomToken WHERE address = :address")
    fun delete(address: String)

    @Query("SELECT * FROM CustomToken")
    fun getAll(): List<CustomToken>

    @Query("DELETE FROM CustomToken")
    fun clear()
}
