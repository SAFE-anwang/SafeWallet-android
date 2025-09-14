package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.storage.LockRecordDao
import io.horizontalsystems.bankwallet.core.storage.ProposalRecordDao

class ProposalRecordRepository(
    private val proposalRecordDao: ProposalRecordDao
) {

    fun getRecordsPaged(limit: Int, offset: Int): List<ProposalRecordInfo> {
        return proposalRecordDao.getRecordsPaged(limit, offset)
    }

    fun getMineRecordsPaged(creator: String): List<ProposalRecordInfo> {
        return proposalRecordDao.getMineRecordsPaged(creator)
    }

    fun getTotal(): Int {
        return proposalRecordDao.getProposalRecordTotal()
    }

    fun getMineNum(creator: String): Int {
        return proposalRecordDao.getMineRecordNum(creator)
    }

    fun getNewProposalRecordNum(): Int {
        return proposalRecordDao.getNewProposalRecordNum()
    }

    fun save(datas: List<ProposalRecordInfo>) {
        proposalRecordDao.insert(datas)
    }

    fun updateStatus() {
        proposalRecordDao.updateStatus()
    }

}