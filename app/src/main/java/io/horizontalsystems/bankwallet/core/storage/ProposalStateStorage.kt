package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalState

class ProposalStateStorage(appDatabase: AppDatabase) {

    private val dao: ProposalStateDao by lazy {
        appDatabase.proposalStateDao()
    }

    fun get(address: String, proposalId: Int): ProposalState? {
        val chainType = getChainType()
        return dao.get(address, proposalId, chainType)
    }

     fun save(proposalState: ProposalState) {
        val chainType = getChainType()
        dao.insert(proposalState.copy(chainType = chainType))
    }

    fun update(proposalState: ProposalState) {
        dao.update(proposalState)
    }

    private fun getChainType(): Int = if (App.localStorage.isSafe4TestNet) 1 else 0
}
