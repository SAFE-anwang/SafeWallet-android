package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigInteger

object SRCLockManager {

    fun syncLockInfo() {
        val customToken = App.appDatabase.customTokenDao().getAll().filter { it.logoURI.isNotBlank() }
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.SafeFour && it.coin.uid == "safe4-coin" && it.token.type == TokenType.Native) {
                safeWallet = it
            }
        }
        if (safeWallet == null) return
        val adapter = (App.adapterManager.getAdapterForWallet(safeWallet) as? ISendEthereumAdapter)  ?: return
        val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
        GlobalScope.launch(Dispatchers.IO) {
            customToken.forEach {
                Log.d("SRC20LockedInfoService", "logo=${it.logoURI}, ${it.address}")
                val src20LockedService = SRC20LockedInfoService(rpcBlockchainSafe4, it.address)
                src20LockedService.updateLockInfo()
                src20LockedService.loadLocked(0)
            }
        }
    }

    fun getLockAmount(contract: String, address: String): BigInteger {
        Log.d("SRC20LockedInfoService", "$contract,$address")
        val lockAmount = App.appDatabase.src20RecordDao().getLockValue(address.lowercase(), contract.lowercase())
        return lockAmount.sumOf { it }
    }

}