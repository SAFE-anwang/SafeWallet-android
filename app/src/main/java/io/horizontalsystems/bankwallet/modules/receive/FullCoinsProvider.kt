package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.bankwallet.core.sortedByFilter
import io.horizontalsystems.bankwallet.core.supported
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class FullCoinsProvider(
    private val marketKit: MarketKitWrapper,
    val activeAccount: Account
) {
    private var activeWallets = listOf<Wallet>()
    private var predefinedTokens = listOf<Token>()

    private var query: String? = null

    fun setActiveWallets(wallets: List<Wallet>) {
        activeWallets = wallets

        updatePredefinedTokens()
    }

    private fun updatePredefinedTokens() {
        val allowedBlockchainTypes =
            BlockchainType.supported.filter { it.supports(activeAccount.type) }
        val tokenQueries = allowedBlockchainTypes
            .map { it.nativeTokenQueries }
            .flatten()
        val supportedNativeTokens = marketKit.tokens(tokenQueries)
        val activeTokens = activeWallets.map { it.token }
        val activeUids = activeTokens.map { it.coin.uid }
        // 已发行和推广的资产
        // 111 过滤未推广资产， 未推广，但是当前钱包的也显示
        val evmKitManager = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour)
        val receiveAddress = evmKitManager.evmKitWrapper?.evmKit?.receiveAddress?.hex
        val deployToken = App.appDatabase.customTokenDao().getAll()
            .filter {
                it.creator.lowercase() == receiveAddress ||
                    (it.creator.lowercase() != receiveAddress && it.logoURI.isNotEmpty())
            }.map {
                getToken(it)
            }.filter { !activeUids.contains(it.coin.uid) }
        predefinedTokens = activeTokens + supportedNativeTokens + deployToken
    }

    private fun getToken(tokenInfo: CustomToken): Token {
        val tokenQuery = TokenQuery(BlockchainType.SafeFour, TokenType.Eip20(tokenInfo.address.lowercase()))
        val coin = Coin(tokenQuery.customCoinUid, tokenInfo.name, tokenInfo.symbol, tokenInfo.decimals.toInt(), tokenInfo.symbol)
        return Token(
            coin = coin,
            blockchain = Blockchain(BlockchainType.SafeFour, tokenInfo.name, tokenInfo.address),
            type = tokenQuery.tokenType,
            decimals = tokenInfo.decimals.toInt()
        )
    }

    fun setQuery(q: String) {
        query = q
    }

    fun getItems(): List<FullCoin> {
        val tmpQuery = query

        val (customTokens, regularTokens) = predefinedTokens.partition { it.isCustom }

        val fullCoins = if (tmpQuery.isNullOrBlank()) {
            val coinUids = regularTokens.map { it.coin.uid }
            customTokens.map { it.fullCoin } + marketKit.fullCoins(coinUids)
        } else if (isContractAddress(tmpQuery)) {
            val customFullCoins = customTokens
                .filter {
                    val type = it.type
                    type is TokenType.Eip20 && type.address.contains(tmpQuery, true)
                }
                .map { it.fullCoin }

            customFullCoins + marketKit.tokens(tmpQuery).map { it.fullCoin }
        } else {
            val customFullCoins = customTokens
                .filter {
                    val coin = it.coin
                    coin.name.contains(tmpQuery, true) || coin.code.contains(tmpQuery, true)
                }
                .map { it.fullCoin }

            customFullCoins + marketKit.fullCoins(tmpQuery, 100)
        }

        return fullCoins
            .sortedByFilter(tmpQuery ?: "")
            .sortedByDescending { fullCoin ->
                activeWallets.any { it.coin == fullCoin.coin }
            }
    }

    private fun isContractAddress(filter: String) = try {
        AddressValidator.validate(filter)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

}
