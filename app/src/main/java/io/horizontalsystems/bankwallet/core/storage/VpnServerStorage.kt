package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.*
import io.reactivex.Flowable

class VpnServerStorage(appDatabase: AppDatabase) {

    private val dao: VpnServerInfoDao by lazy {
        appDatabase.vpnServerDao()
    }

    fun allVpnServer(): List<VpnServerInfo> {
        return dao.getAll()
    }

     fun save(vpnInfo: VpnServerInfo) {
        dao.insert(vpnInfo)
    }

    fun update(vpnInfo: VpnServerInfo) {
        dao.update(vpnInfo)
    }

    fun clearAll() {
        dao.clear()
    }
}
