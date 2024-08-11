package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.ActiveAccount
import io.horizontalsystems.bankwallet.entities.VpnServerInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalState
import io.reactivex.Flowable

@Dao
interface ProposalStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(proposalState: ProposalState)

    @Update
    fun update(proposalState: ProposalState)

    //"SELECT `value` FROM SyncerState WHERE `key` = :key"
    @Query("SELECT * FROM ProposalState WHERE `address` = :address AND `proposalId` = :proposalId")
    fun get(address: String, proposalId: Int): ProposalState?

}
