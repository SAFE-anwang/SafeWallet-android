package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalRecordInfo

@Dao
interface ProposalRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tokens: List<ProposalRecordInfo>)

    @Update
    fun update(tokens: List<ProposalRecordInfo>)

    @Update
    fun update(token: ProposalRecordInfo)

    @Query("DELETE FROM ProposalRecordInfo WHERE id = :id")
    fun delete(id: Long)


    @Query("SELECT * FROM ProposalRecordInfo ORDER BY id DESC " +
            "LIMIT :limit OFFSET :offset")
    fun getRecordsPaged(limit: Int, offset: Int): List<ProposalRecordInfo>

    @Query("SELECT * FROM ProposalRecordInfo WHERE creator = :creator ORDER BY " +
            "id DESC ")
    fun getMineRecordsPaged(creator: String): List<ProposalRecordInfo>


    @Query("SELECT COUNT(*) as total_count FROM ProposalRecordInfo")
    fun getProposalRecordTotal(): Int

    @Query("SELECT COUNT(*) FROM ProposalRecordInfo  WHERE creator = :creator")
    fun getMineRecordNum(creator: String): Int

    @Query("SELECT COUNT(*) FROM ProposalRecordInfo  WHERE newProposal = 1")
    fun getNewProposalRecordNum(): Int

    @Query("UPDATE ProposalRecordInfo SET newProposal = 0")
    fun updateStatus()
}
