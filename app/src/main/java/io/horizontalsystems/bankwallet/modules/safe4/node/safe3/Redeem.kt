package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import androidx.room.Entity
import java.math.BigInteger

@Entity(primaryKeys = ["address"])
data class Redeem(
		val address: String,
		val existAvailable: Boolean,
		val existLocked: Boolean,
		val existMasterNode: Boolean,
		val success: Boolean
)
