package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import androidx.room.Entity

@Entity(primaryKeys = ["address", "proposalId"])
data class ProposalState(
		val proposalId: Int,
		val address: String,
		val state: Int,
) {
}
