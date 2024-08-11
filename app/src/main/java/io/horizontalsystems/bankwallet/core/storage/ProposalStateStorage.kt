package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalState
import io.reactivex.Flowable

class ProposalStateStorage(appDatabase: AppDatabase) {

    private val dao: ProposalStateDao by lazy {
        appDatabase.proposalStateDao()
    }

    fun get(address: String, proposalId: Int): ProposalState? {
        return dao.get(address, proposalId)
    }

     fun save(proposalState: ProposalState) {
        dao.insert(proposalState)
    }

    fun update(proposalState: ProposalState) {
        dao.update(proposalState)
    }

}
