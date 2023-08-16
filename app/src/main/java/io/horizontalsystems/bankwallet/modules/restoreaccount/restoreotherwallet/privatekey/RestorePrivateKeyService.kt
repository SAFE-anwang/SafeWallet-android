package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.privatekey

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.RestoreBlockchainsModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.RestoreBlockchainsService
import io.horizontalsystems.marketkit.models.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class RestorePrivateKeyService(
    private val accountName: String,
    private val accountType: AccountType,
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
    private val enableCoinService: EnableCoinService,
    private val evmBlockchainManager: EvmBlockchainManager
) : Clearable {

    private val disposables = CompositeDisposable()

    private var internalItems = listOf<RestoreBlockchainsModule.InternalItem>()
    private val enabledCoins = mutableListOf<ConfiguredToken>()

    private var restoreSettingsMap = mutableMapOf<Token, RestoreSettings>()

    val cancelEnableBlockchainObservable = PublishSubject.create<Blockchain>()
    val canRestore = BehaviorSubject.createDefault(false)

    val itemsObservable = BehaviorSubject.create<List<RestoreBlockchainsService.Item>>()
    var items: List<RestoreBlockchainsService.Item> = listOf()
        private set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    private val blockchainTypes = listOf(
        BlockchainType.Bitcoin,
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Zcash,
        BlockchainType.Dash,
        BlockchainType.Safe,
        BlockchainType.BitcoinCash,
        BlockchainType.Litecoin,
        BlockchainType.BinanceChain,
    )

    init {
        enableCoinService.enableCoinObservable
            .subscribeIO { (configuredPlatformCoins, settings) ->
                handleEnableCoin(configuredPlatformCoins, settings)
            }.let {
                disposables.add(it)
            }

        enableCoinService.cancelEnableCoinObservable
            .subscribeIO { fullCoin ->
//                handleCancelEnable(fullCoin)
            }.let { disposables.add(it) }

        syncInternalItems()
        syncState()
    }

    private fun syncInternalItems() {
        val allowedBlockchainTypes = blockchainTypes.filter { it.supports(accountType) }
        val blockchains = marketKit
            .blockchains(allowedBlockchainTypes.map { it.uid })
            .sortedBy { it.type.order }

        val tokens = blockchainTypes
            .map { TokenQuery(it, TokenType.Native) }
            .let { marketKit.tokens(it) }

        internalItems = blockchains.mapNotNull { blockchain ->
            tokens.find { it.blockchain == blockchain }?.let {
                RestoreBlockchainsModule.InternalItem(blockchain, it)
            }
        }
    }

    private fun handleEnableCoin(
        configuredTokens: List<ConfiguredToken>,
        restoreSettings: RestoreSettings
    ) {
        val platformCoin = configuredTokens.firstOrNull()?.token ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsMap[platformCoin] = restoreSettings
        }

        val existingConfiguredPlatformCoins = enabledCoins.filter { it.token == platformCoin }
        val newConfiguredPlatformCoins = configuredTokens.minus(existingConfiguredPlatformCoins)
        val removedConfiguredPlatformCoins = existingConfiguredPlatformCoins.minus(configuredTokens)

        enabledCoins.addAll(newConfiguredPlatformCoins)
        enabledCoins.removeAll(removedConfiguredPlatformCoins)

        syncCanRestore()
        syncState()
    }
    private fun isEnabled(internalItem: RestoreBlockchainsModule.InternalItem): Boolean {
        return enabledCoins.any { it.token == internalItem.token }
    }

    private fun item(internalItem: RestoreBlockchainsModule.InternalItem): RestoreBlockchainsService.Item {
        val enabled = isEnabled(internalItem)
        val hasSettings = enabled && hasSettings(internalItem.token)
        return RestoreBlockchainsService.Item(internalItem.blockchain, enabled, hasSettings)
    }

    private fun hasSettings(token: Token) = token.blockchainType.coinSettingType != null

    private fun syncState() {
        items = internalItems.map { item(it) }
    }

    private fun syncCanRestore() {
        canRestore.onNext(enabledCoins.isNotEmpty())
    }

    private fun getInternalItemByBlockchain(blockchain: Blockchain): RestoreBlockchainsModule.InternalItem? =
        internalItems.firstOrNull { it.blockchain == blockchain }

    fun enable(blockchain: Blockchain, purpose: Int? = null) {
        val internalItem = getInternalItemByBlockchain(blockchain) ?: return

        enableCoinService.enable(internalItem.token.fullCoin, accountType, purpose = purpose)
    }

    fun disable(blockchain: Blockchain) {
        val internalItem = getInternalItemByBlockchain(blockchain) ?: return
        enabledCoins.removeIf { it.token == internalItem.token }

        syncState()
        syncCanRestore()
    }

    fun configure(blockchain: Blockchain) {
        val internalItem = getInternalItemByBlockchain(blockchain) ?: return

        enableCoinService.configure(
            internalItem.token.fullCoin,
            accountType,
            enabledCoins.filter { it.token == internalItem.token })
    }

    fun restore(accountType: AccountType) {
        val account = accountFactory.account(accountName, accountType, AccountOrigin.Restored, true, false)
        accountManager.save(account)

        restoreSettingsMap.forEach { (token, settings) ->
            enableCoinService.save(settings, account, token.blockchainType)
        }

        /*items.filter { it.enabled }.forEach { item ->
            val isEvm = evmBlockchainManager.allBlockchainTypes.contains(item.blockchain.type)
            if (isEvm) {
//                evmBlockchainManager.getEvmAccountManager(item.blockchain.type).markAutoEnable(account)
            }
        }*/

        if (enabledCoins.isEmpty()) return

        val wallets = enabledCoins.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    override fun clear() = disposables.clear()

    data class Item(
        val blockchain: Blockchain,
        val state: ItemState
    )

    sealed class ItemState {
        object Unsupported : ItemState()
        class Supported(val enabled: Boolean, val hasSettings: Boolean) : ItemState()
    }
}
