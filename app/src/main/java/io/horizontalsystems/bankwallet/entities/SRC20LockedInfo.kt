package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import java.math.BigInteger

@Entity(primaryKeys = ["id", "contract"])
data class SRC20LockedInfo(
    val id: String,
    val address: String,
    val amount: BigInteger,
    val lockDay: Int,
    val startHeight: Long,
    val unlockHeight: Long,
    val contract: String
)
