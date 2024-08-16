package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.Redeem
import io.reactivex.Flowable

class RedeemStorage(appDatabase: AppDatabase) {

    private val dao: RedeemDao by lazy {
        appDatabase.redeemDao()
    }

    fun allRedeem(): List<Redeem> {
        return dao.getAll()
    }

     fun save(redeem: Redeem) {
        dao.insert(redeem)
    }

    fun update(redeem: Redeem) {
        dao.update(redeem)
    }

    fun clearAll() {
        dao.clear()
    }
}
