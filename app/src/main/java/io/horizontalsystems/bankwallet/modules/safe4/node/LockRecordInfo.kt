package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.room.Entity
import androidx.room.TypeConverters
import io.horizontalsystems.bankwallet.core.storage.BigIntegerConverters
import io.horizontalsystems.bankwallet.core.storage.DatabaseConverters
import java.math.BigInteger

@Entity(primaryKeys = ["id", "contact"])
data class LockRecordInfo(
    val id: Long,
    val unlockHeight: Long?,
    val releaseHeight: Long?,
    @TypeConverters(DatabaseConverters::class)
    val value: BigInteger,
    val address: String?,
    val address2: String?,
    val frozenAddr: String?,
    val contact: String,
    val creator: String,
    val type : Int,
    val withEnable: Boolean = false
)
