package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.ActiveAccount
import io.horizontalsystems.bankwallet.entities.VpnServerInfo
import io.reactivex.Flowable

@Dao
interface VpnServerInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vpnServerInfo: VpnServerInfo)

    @Update
    fun update(vpnServerInfo: VpnServerInfo)

    @Query("SELECT * FROM VpnServerInfo")
    fun getAll(): List<VpnServerInfo>

    @Query("DELETE FROM VpnServerInfo")
    fun clear()
}
