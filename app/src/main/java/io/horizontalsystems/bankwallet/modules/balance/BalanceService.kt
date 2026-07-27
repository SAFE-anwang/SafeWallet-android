package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

class BalanceService(
    private val activeWalletRepository: BalanceActiveWalletRepository,
    private val xRateRepository: BalanceXRateRepository,
    private val adapterRepository: BalanceAdapterRepository,
    private val localStorage: ILocalStorage,
    private val connectivityManager: ConnectivityManager,
    private val balanceSorter: BalanceSorter,
    private val accountManager: IAccountManager
) : Clearable {

    val networkAvailable by connectivityManager::isConnected
    val baseCurrency by xRateRepository::baseCurrency

    var sortType = localStorage.sortType
        private set

    var isWatchAccount = false
        private set

    val account: Account?
        get() = accountManager.activeAccount

    private var hideZeroBalances = false

    private var allBalanceItems = listOf<BalanceModule.BalanceItem>()

    private val mutex = Mutex()

    private val _balanceItemsFlow = MutableStateFlow<List<BalanceModule.BalanceItem>?>(null)
    val balanceItemsFlow = _balanceItemsFlow.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var sortAndEmitJob: Job? = null

    // Auto-retry state for NotSynced adapters
    private val retryJobs = ConcurrentHashMap<Wallet, Job>()
    private val retryAttempts = ConcurrentHashMap<Wallet, Int>()
    private var previousNetworkAvailable: Boolean? = null


    fun start() {
        coroutineScope.launch {
            activeWalletRepository.itemsObservable.asFlow().collect { wallets ->
                val filterWallets = wallets.filter { it.token.coin.code != "safeswap-V2" }
                handleWalletsUpdate(filterWallets)
            }
        }
        coroutineScope.launch {
            xRateRepository.itemObservable.asFlow().collect { latestRates ->
                handleXRateUpdate(latestRates)
            }
        }
        coroutineScope.launch {
            adapterRepository.readyObservable.asFlow().collect {
                handleAdaptersReady()
            }
        }
        coroutineScope.launch {
            adapterRepository.updatesObservable.asFlow().collect {
                handleAdapterUpdate(it)
            }
        }
        // Monitor network availability changes for auto re-sync
        coroutineScope.launch {
            connectivityManager.networkAvailabilitySignal.asFlow().collect {
                val current = connectivityManager.isConnected
                if (previousNetworkAvailable == false && current) {
                    onNetworkBecameAvailable()
                }
                previousNetworkAvailable = current
            }
        }
    }

    fun setSortType(value: BalanceSortType) {
        sortType = value
        localStorage.sortType = value

        sortAndEmitItems()
    }

    private fun sortAndEmitItems() {
        sortAndEmitJob?.cancel()
        sortAndEmitJob = coroutineScope.launch {
            allBalanceItems = balanceSorter.sort(allBalanceItems, sortType)

            ensureActive()

            _balanceItemsFlow.update {
                if (hideZeroBalances) {
                    allBalanceItems.filter { it.wallet.token.type.isNative || it.balanceData.total > BigDecimal.ZERO }
                } else {
                    allBalanceItems
                }
            }
        }
    }

    private suspend fun handleAdaptersReady() = mutex.withLock {
        val allBalanceItems = this.allBalanceItems.toMutableList()
        for (i in 0 until allBalanceItems.size) {
            val balanceItem = allBalanceItems[i]

            allBalanceItems[i] = balanceItem.copy(
                balanceData = adapterRepository.balanceData(balanceItem.wallet),
                state = adapterRepository.state(balanceItem.wallet),
                warning = adapterRepository.warning(balanceItem.wallet)
            )
        }

        this.allBalanceItems = allBalanceItems

        sortAndEmitItems()

        // Proactively retry any adapters that are already NotSynced on initial load
        // This handles the case where initial sync failed before subscriptions were set up
        if (networkAvailable) {
            allBalanceItems.forEach { item ->
                if (item.state is AdapterState.NotSynced) {
                    scheduleAutoRetry(item.wallet)
                }
            }
        }
    }

    private suspend fun handleAdapterUpdate(wallet: Wallet) = mutex.withLock {
        val indexOfFirst = allBalanceItems.indexOfFirst { it.wallet == wallet }
        if (indexOfFirst != -1) {
            val allBalanceItems = allBalanceItems.toMutableList()
            val itemToUpdate = allBalanceItems[indexOfFirst]
            val newState = adapterRepository.state(wallet)

            allBalanceItems[indexOfFirst] = itemToUpdate.copy(
                balanceData = adapterRepository.balanceData(wallet),
                state = newState,
                warning = adapterRepository.warning(wallet)
            )

            this.allBalanceItems = allBalanceItems

            sortAndEmitItems()

            // Auto-retry logic: if adapter entered NotSynced and network is available, schedule a retry
            if (networkAvailable && newState is AdapterState.NotSynced) {
                scheduleAutoRetry(wallet)
            } else if (newState is AdapterState.Synced) {
                // Reset retry counter on successful sync
                cancelAutoRetry(wallet)
            }
        }
    }

    private fun scheduleAutoRetry(wallet: Wallet) {
        val attempts = retryAttempts.getOrDefault(wallet, 0)
        if (attempts >= MAX_AUTO_RETRY_ATTEMPTS) return

        // Cancel any existing retry job for this wallet
        retryJobs[wallet]?.cancel()

        val delayMs = minOf(INITIAL_RETRY_DELAY_MS * (1L shl attempts), MAX_RETRY_DELAY_MS)
        retryAttempts[wallet] = attempts + 1

        retryJobs[wallet] = coroutineScope.launch {
            delay(delayMs)
            adapterRepository.refreshByWallet(wallet)
        }
    }

    private fun cancelAutoRetry(wallet: Wallet) {
        retryJobs.remove(wallet)?.cancel()
        retryAttempts.remove(wallet)
    }

    // Called when network transitions from unavailable to available → retry all failed adapters
    private fun onNetworkBecameAvailable() {
        coroutineScope.launch {
            mutex.withLock {
                allBalanceItems.forEach { item ->
                    if (item.state is AdapterState.NotSynced) {
                        adapterRepository.refreshByWallet(item.wallet)
                    }
                }
            }
        }
    }

    private suspend fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) = mutex.withLock {
        val allBalanceItems = allBalanceItems.toMutableList()
        for (i in 0 until allBalanceItems.size) {
            val balanceItem = allBalanceItems[i]

            if (latestRates.containsKey(balanceItem.wallet.coin.uid)) {
                allBalanceItems[i] = balanceItem.copy(coinPrice = latestRates[balanceItem.wallet.coin.uid])
            }
        }

        this.allBalanceItems = allBalanceItems

        sortAndEmitItems()
    }

    private suspend fun handleWalletsUpdate(wallets: List<Wallet>) = mutex.withLock {

        isWatchAccount = accountManager.activeAccount?.isWatchAccount == true
        hideZeroBalances = accountManager.activeAccount?.type?.hideZeroBalances == true

        adapterRepository.setWallet(wallets)
        xRateRepository.setCoinUids(wallets.map { it.coin.uid })
        val latestRates = xRateRepository.getLatestRates()

        val balanceItems = wallets.map { wallet ->
            BalanceModule.BalanceItem(
                wallet = wallet,
                balanceData = adapterRepository.balanceData(wallet),
                state = adapterRepository.state(wallet),
                coinPrice = latestRates[wallet.coin.uid]
            )
        }

        allBalanceItems = balanceItems

        sortAndEmitItems()
    }

    suspend fun refresh() {
        xRateRepository.refresh()
        adapterRepository.refresh()
    }

    override fun clear() {
        retryJobs.values.forEach { it.cancel() }
        retryJobs.clear()
        retryAttempts.clear()
        coroutineScope.cancel()
        adapterRepository.clear()
    }

    fun disable(wallet: Wallet) {
        activeWalletRepository.disable(wallet)
    }

    fun enable(wallet: Wallet) {
        activeWalletRepository.enable(wallet)
    }

    companion object {

        private const val MAX_AUTO_RETRY_ATTEMPTS = 5
        private const val INITIAL_RETRY_DELAY_MS = 2000L
        private const val MAX_RETRY_DELAY_MS = 30000L

        fun getInstance(tag: String): BalanceService {
            return BalanceService(
                BalanceActiveWalletRepository(App.walletManager, App.evmSyncSourceManager),
                BalanceXRateRepository(tag, App.currencyManager, App.marketKit),
                BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao())),
                App.localStorage,
                App.connectivityManager,
                BalanceSorter(),
                App.accountManager
            )

        }
    }
}
