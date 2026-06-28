package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.storage.ProposalRecordDao

class ProposalRecordRepository(
    private val proposalRecordDao: ProposalRecordDao
) {

    private fun getChainType(): Int = if (App.localStorage.isSafe4TestNet) 1 else 0

    fun getRecordsPaged(limit: Int, offset: Int): List<ProposalRecordInfo> {
        val chainType = getChainType()
        return proposalRecordDao.getRecordsPaged(chainType, limit, offset)
    }

    fun getMineRecordsPaged(creator: String): List<ProposalRecordInfo> {
        val chainType = getChainType()
        return proposalRecordDao.getMineRecordsPaged(creator, chainType)
    }

    fun getTotal(): Int {
        val chainType = getChainType()
        return proposalRecordDao.getProposalRecordTotal(chainType)
    }

    fun getMineNum(creator: String): Int {
        val chainType = getChainType()
        return proposalRecordDao.getMineRecordNum(creator, chainType)
    }

    fun getNewProposalRecordNum(): Int {
        val chainType = getChainType()
        return proposalRecordDao.getNewProposalRecordNum(chainType)
    }

    fun save(datas: List<ProposalRecordInfo>) {
        val chainType = getChainType()
        proposalRecordDao.insert(datas.map { it.copy(chainType = chainType) })
    }

    fun updateStatus() {
        val chainType = getChainType()
        proposalRecordDao.updateStatus(chainType)
    }

    fun getLocalLastCreateTime(): Long {
        val chainType = getChainType()
        return proposalRecordDao.getLocalLastCreateTime(chainType)
    }
}