package io.horizontalsystems.bankwallet.modules.safe4.src20

import androidx.compose.runtime.collectAsState
import com.google.android.exoplayer2.util.Log
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.core.managers.AdapterManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.Safe3TestCoinService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.Constants
import io.horizontalsystems.marketkit.SafeExtend
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenEntity
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SyncSafe4TokensService(
    val srC20Service: SRC20Service,
    val evmKit: EthereumKit
) {

    var cache: List<CustomToken>? = null
    private val customTokenSubject = PublishSubject.create<List<CustomToken>?>()
    val customTokenObservable: Observable<List<CustomToken>?> get() = customTokenSubject

    fun getCache() {
        cache = App.appDatabase.customTokenDao().getAll().filter { it.creator.lowercase() == evmKit.receiveAddress.hex }
        cache?.let {
            customTokenSubject.onNext(it)
        }
    }

    fun getTokens() {
        val service = Safe3TestCoinService()
        service.getToken()
            .map { tokens ->
                tokens.forEach {
                    saveLogo(it)
                }
                (App.adapterManager as AdapterManager).preloadAdapters()
                // 保存已推广资产
                tokens.filter { it.creator.lowercase() != evmKit.receiveAddress.hex }
                    .forEach { tokenInfo ->
                        if (tokenInfo.logoURI != null && tokenInfo.logoURI.isNotBlank()) {
                            App.appDatabase.customTokenDao().insert(tokenInfo)
                        }
                    }
                val filter = tokens.filter { it.creator.lowercase() == evmKit.receiveAddress.hex }
                Constants.deployContracts = filter.map { it.address.lowercase() }
                val address = filter.map { it.address }
                // 删除已经不存在的资产
                cache?.let {
                    it.forEach {
                        if (!address.contains(it.address)) {
                            App.appDatabase.customTokenDao().delete(it.address)
                        }
                    }
                }
                // 更新资产LOGO
                filter.forEach {
                    if (it.logoURI.isNotEmpty()) {
                        App.appDatabase.customTokenDao().updateLogo(it.logoURI, it.address)
                    }
                }
                val result = filter.map { token ->
                    val cacheToken = getCacheToken(token.address)
                    if (cacheToken == null || cacheToken.version.isNullOrEmpty()) {
                        val version = srC20Service.getVersion(token.address, token.chainId)
                        if (version != null) {
                            token.copy(version = version)
                        } else {
                            token
                        }
                    } else {
                        cacheToken
                    }
                }
                result
            }
            .subscribeIO({
                App.appDatabase.customTokenDao().insert(it)
                customTokenSubject.onNext(it)
                it.forEach {
                    addToken(it)
                }
            }) {
                Log.d("DeployList", "error=$it")
            }?.let {

            }
    }

    fun addToken(tokenInfo: CustomToken) {
        val tokenQuery = TokenQuery(BlockchainType.SafeFour, TokenType.Eip20(tokenInfo.address.lowercase()))
        val coin = Coin(tokenQuery.customCoinUid, tokenInfo.name, tokenInfo.symbol, tokenInfo.decimals.toInt(), tokenInfo.symbol)
        val token = Token(
            coin = coin,
            blockchain = Blockchain(BlockchainType.SafeFour, tokenInfo.name, tokenInfo.address),
            type = tokenQuery.tokenType,
            decimals = tokenInfo.decimals.toInt()
        )
        val account = App.accountManager.activeAccount ?: return
        val wallet = Wallet(token, account)
        App.marketKit.removeTokenEntity("safe4-coin", tokenInfo.address)
        App.marketKit.insertTokenEntity(
            TokenEntity(tokenQuery.customCoinUid, "safe4-coin", "eip20", tokenInfo.decimals.toInt(), tokenInfo.address)
        )
        App.marketKit.insertCoin(coin)

//        App.walletManager.save(listOf(wallet))
    }

    private fun saveLogo(tokenInfo: CustomToken) {
        val tokenQuery = TokenQuery(BlockchainType.SafeFour, TokenType.Eip20(tokenInfo.address.lowercase()))
        SafeExtend.deployCoinHash[tokenQuery.customCoinUid] = tokenInfo.name
        if (tokenInfo.logoURI != null && tokenInfo.logoURI.isNotBlank()) {
            MMKV.defaultMMKV()?.putString(tokenQuery.customCoinUid.lowercase(), tokenInfo.logoURI)
        }
    }

    private fun getCacheToken(address: String): CustomToken? {
        if (cache.isNullOrEmpty())  return null
        return cache?.find { it.address == address }
    }
}