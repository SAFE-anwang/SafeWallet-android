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

    @Query("DELETE FROM ProposalRecordInfo WHERE id = :id AND chainType = :chainType")
    fun delete(id: Long, chainType: Int)


    @Query("SELECT * FROM ProposalRecordInfo WHERE chainType = :chainType ORDER BY id DESC " +
            "LIMIT :limit OFFSET :offset")
    fun getRecordsPaged(chainType: Int, limit: Int, offset: Int): List<ProposalRecordInfo>

    @Query("SELECT * FROM ProposalRecordInfo WHERE creator = :creator AND chainType = :chainType ORDER BY " +
            "id DESC ")
    fun getMineRecordsPaged(creator: String, chainType: Int): List<ProposalRecordInfo>


    @Query("SELECT COUNT(*) as total_count FROM ProposalRecordInfo WHERE chainType = :chainType")
    fun getProposalRecordTotal(chainType: Int): Int

    @Query("SELECT COUNT(*) FROM ProposalRecordInfo  WHERE creator = :creator AND chainType = :chainType")
    fun getMineRecordNum(creator: String, chainType: Int): Int

    @Query("SELECT COUNT(*) FROM ProposalRecordInfo  WHERE newProposal = 1 AND state != 2 AND chainType = :chainType")
    fun getNewProposalRecordNum(chainType: Int): Int

    @Query("UPDATE ProposalRecordInfo SET newProposal = 0 WHERE chainType = :chainType")
    fun updateStatus(chainType: Int)

    @Query("SELECT startPayTime FROM ProposalRecordInfo WHERE chainType = :chainType ORDER BY startPayTime DESC LIMIT 1")
    fun getLocalLastCreateTime(chainType: Int): Long

}
