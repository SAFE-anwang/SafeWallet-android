package io.horizontalsystems.bankwallet.modules.safe4.src20

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.toLowerCase
import com.google.android.exoplayer2.util.Log
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.Safe3TestCoinService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.Constants
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SyncSafe4Tokens{

    fun getTokens() {
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.SafeFour && it.coin.uid == "safe4-coin" && it.token.type == TokenType.Native) {
                safeWallet = it
            }
        }
        if (safeWallet == null) return
        val adapter = (App.adapterManager.getAdapterForWallet(safeWallet) as? ISendEthereumAdapter)  ?: return
        getContract(adapter.evmKitWrapper.evmKit.receiveAddress.hex)
        val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4
        val service = SRC20Service(DeployType.SRC20, rpcBlockchainSafe4.web3j)
        SyncSafe4TokensService(service, adapter.evmKitWrapper.evmKit).getTokens()
    }

    fun getLogo(coinUid: String): String? {
        return MMKV.defaultMMKV()?.getString(coinUid.lowercase(), null)
    }

    fun getContract(address: String) {
        Constants.deployContracts = App.appDatabase.customTokenDao().getAll().filter { it.creator.lowercase() == address }.map { it.address.lowercase() }
    }
}