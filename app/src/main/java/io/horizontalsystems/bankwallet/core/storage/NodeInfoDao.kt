package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.SRC20LockedInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import java.math.BigInteger

@Dao
interface NodeInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(token: NodeInfo)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tokens: List<NodeInfo>)

    @Update
    fun update(tokens: List<NodeInfo>)

    @Update
    fun update(token: NodeInfo)

    @Query("DELETE FROM NodeInfo WHERE id = :id")
    fun delete(id: Long)



    @Query("DELETE FROM NodeInfo WHERE type=:type")
    fun deleteNodeInfoList(type: Int)


    @Query("SELECT * FROM NodeInfo WHERE type=:type ORDER BY sortOrder ASC")
    fun getNodeInfoList(type: Int): List<NodeInfo>

}
