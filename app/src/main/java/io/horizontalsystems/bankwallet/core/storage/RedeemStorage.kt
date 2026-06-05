package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.Redeem

class RedeemStorage(appDatabase: AppDatabase) {

    private val dao: RedeemDao by lazy {
        appDatabase.redeemDao()
    }

    fun allRedeem(): List<Redeem> {
        val chainType = getChainType()
        return dao.getAll(chainType)
    }

     fun save(redeem: Redeem) {
        val chainType = getChainType()
        dao.insert(redeem.copy(chainType = chainType))
    }

    fun update(redeem: Redeem) {
        dao.update(redeem)
    }

    fun clearAll() {
        val chainType = getChainType()
        dao.clear(chainType)
    }

    private fun getChainType(): Int = if (App.localStorage.isSafe4TestNet) 1 else 0
}
