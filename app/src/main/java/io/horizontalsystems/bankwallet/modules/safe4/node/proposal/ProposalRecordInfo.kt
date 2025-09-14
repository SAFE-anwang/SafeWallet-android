package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import androidx.room.Entity
import androidx.room.TypeConverters
import io.horizontalsystems.bankwallet.core.storage.BigIntegerConverters
import io.horizontalsystems.bankwallet.core.storage.DatabaseConverters
import java.math.BigInteger

@Entity(primaryKeys = ["id"])
data class ProposalRecordInfo(
    val id: Int,
    val creator: String,
    val title: String,
    @TypeConverters(DatabaseConverters::class)
    val payAmount: BigInteger,
    val payTimes: Long,
    val startPayTime: Long,
    val endPayTime: Long,
    val description: String,
    val state: Int,
    val createHeight: Long,
    val updateHeight: Long,
    val newProposal: Int = 0
)
