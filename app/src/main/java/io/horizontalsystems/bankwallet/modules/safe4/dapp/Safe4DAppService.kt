package io.horizontalsystems.bankwallet.modules.safe4.dapp

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.anwang.contracts.additions.DAppManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.Safe4Module
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Connect
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.subjects.PublishSubject
import org.web3j.abi.datatypes.Address
import java.io.File
import java.math.BigInteger

class Safe4DAppService : Clearable {

    companion object {
        private const val TAG = "Safe4DAppService"
        private const val DRAFT_KEY = "safe4_dapp_draft"
    }

    private val gson = Gson()
    private val prefs by lazy {
        App.instance.getSharedPreferences("safe4_dapp_prefs", Context.MODE_PRIVATE)
    }

    private val web3j by lazy {
        Connect.connect(Safe4Module.getSafeChain())
    }

    private val dAppManager by lazy {
        DAppManager(web3j, Safe4Module.getSafeChain().id.toLong())
    }

    private val dAppsSubject = PublishSubject.create<List<ManagedDAppItem>>()
    private var cachedDApps = mutableListOf<ManagedDAppItem>()

    init {
        // 1. Load local cache immediately on main thread (fast, no network)
        val wallet = getActiveSafe4Wallet()
        if (wallet != null) {
            cachedDApps.addAll(getStoredDApps(wallet))
        }
        dAppsSubject.onNext(cachedDApps.toList())

        // 2. Then sync chain data in background (may be slow)
        if (wallet != null) {
            Thread {
                try {
                    syncChainDApps()
                } catch (e: Exception) {
                    Log.e(TAG, "init chain sync error", e)
                }
            }.start()
        }
    }

    /**
     * Sync DApps from chain into local cache. Must be called on background thread.
     * Only fetches DApps owned by the current wallet (management page).
     */
    private fun syncChainDApps() {
        val currentWallet = getActiveSafe4Wallet() ?: return

        try {
            val result = fetchMineChainDAppsWithIds()
            val chainDApps = result.dApps
            val allChainIds = result.allIds
            val localDApps = getStoredDApps(currentWallet)
            val merged = mergeChainAndLocal(chainDApps, localDApps, currentWallet)

            // Preserve only chain IDs that exist but getInfo failed (e.g. after logo update).
            // Temp items (non-numeric id = txHash) are NOT preserved — they are replaced by real chain data.
            val mergedIds = merged.map { it.id }.toSet()
            val preservedLocal = localDApps.filter { local ->
                local.id !in mergedIds && local.id in allChainIds  // chain ID with failed getInfo
            }

            cachedDApps.clear()
            cachedDApps.addAll(merged)
            cachedDApps.addAll(preservedLocal)
            saveDApps(currentWallet, cachedDApps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync chain DApps", e)
        }

        dAppsSubject.onNext(cachedDApps.toList())

        // Background fetch logos for all DApps (gitUrl is GitHub URL, actual logo is on-chain)
        cacheAllLogos(cachedDApps.toList())
    }

    /**
     * Get the current wallet's EVM hex address.
     */
    private fun getWalletAddress(): String {
        val wallet = getActiveSafe4Wallet()
            ?: throw IllegalStateException("No active SAFE4 wallet")
        val adapter = App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter
            ?: throw IllegalStateException("No SAFE4 adapter")
        return adapter.evmKitWrapper.evmKit.receiveAddress.hex
    }

    /**
     * Fetch only DApps owned by the current wallet using getMineNum + getMineIDs + getInfo.
     */
    private fun fetchMineChainDApps(): List<com.anwang.types.dapp.DAppInfo> {
        return fetchMineChainDAppsWithIds().dApps
    }

    /**
     * Result containing both successfully fetched DApps and all chain IDs (including failed).
     */
    private data class FetchResult(
        val dApps: List<com.anwang.types.dapp.DAppInfo>,
        val allIds: Set<String>
    )

    /**
     * Fetch DApps and track all IDs even if getInfo fails for some.
     */
    private fun fetchMineChainDAppsWithIds(): FetchResult {
        val address = getWalletAddress()
        val total = dAppManager.getMineNum(Address(address))
        Log.d(TAG, "fetchMineChainDAppsWithIds: total=$total")
        if (total == BigInteger.ZERO) return FetchResult(emptyList(), emptySet())

        val pageSize = BigInteger.valueOf(10)
        val allIds = mutableListOf<BigInteger>()
        var start = BigInteger.ZERO

        while (start < total) {
            val ids = dAppManager.getMineIDs(Address(address), start, pageSize)
            if (ids.isEmpty()) break
            allIds.addAll(ids)
            start += BigInteger.valueOf(ids.size.toLong())
        }

        val dApps = allIds.mapNotNull { id ->
            try {
                dAppManager.getInfo(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get DApp info for id=$id", e)
                null
            }
        }
        Log.d(TAG, "fetchMineChainDAppsWithIds: $dApps")

        return FetchResult(
            dApps = dApps,
            allIds = allIds.map { it.toString() }.toSet()
        )
    }

    /**
     * Fetch all DApps from chain using getNum + getIDs + getInfo.
     * Used by market page to show all DApps.
     */
    fun fetchAllChainDApps(): List<com.anwang.types.dapp.DAppInfo> {
        val total = dAppManager.getNum()
        if (total == BigInteger.ZERO) return emptyList()

        val pageSize = BigInteger.valueOf(50)
        val allIds = mutableListOf<BigInteger>()
        var start = BigInteger.ZERO

        while (start < total) {
            val ids = dAppManager.getIDs(start, pageSize)
            if (ids.isEmpty()) break
            allIds.addAll(ids)
            start += BigInteger.valueOf(ids.size.toLong())
        }

        return allIds.mapNotNull { id ->
            try {
                dAppManager.getInfo(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get DApp info for id=$id", e)
                null
            }
        }
    }

    /**
     * Merge chain DAppInfo list with local ManagedDAppItem list.
     * Chain data provides: id, name, contractAddr, runUrl(=url), officialUrl, officialEmail, description, gitUrl(=iconUrl), isFrozen(=status), keyword, fraudNum
     * Local data provides: category (extra UI fields)
     */
    private fun mergeChainAndLocal(
        chainDApps: List<com.anwang.types.dapp.DAppInfo>,
        localDApps: List<ManagedDAppItem>,
        wallet: Wallet
    ): List<ManagedDAppItem> {
        val localMap = localDApps.associateBy { it.id }

        return chainDApps.mapNotNull { info ->
            try {
                val id = info.id.toString()
                val local = localMap[id]

                ManagedDAppItem(
                    id = id,
                    name = info.name ?: "",
                    url = info.runUrl ?: "",
                    description = info.description ?: "",
                    category = local?.category ?: "",
                    iconUrl = info.gitUrl ?: local?.iconUrl ?: "",
                    contractAddr = info.contractAddr?.toString() ?: "",
                    officialUrl = info.officialUrl ?: "",
                    officialEmail = info.officialEmail ?: "",
                    officialAccount = info.officialAccount?.toString() ?: "",
                    keyword = info.keyword ?: "",
                    status = if (info.isFrozen == true) "frozen" else "active",
                    fraudNum = (info.fraudNum ?: BigInteger.ZERO).toLong(),
                    createdAt = local?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = local?.updatedAt ?: System.currentTimeMillis(),
                    walletAddress = wallet.account.name
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to merge DApp: ${info.id}", e)
                null
            }
        }
    }

    /**
     * Get private key for signing transactions from evmKitWrapper.
     */
    private fun getPrivateKey(): String {
        val evmKitWrapper = App.evmBlockchainManager
            .getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper
            ?: throw IllegalStateException("No evmKitWrapper for SafeFour")

        return evmKitWrapper.signer?.privateKey?.toString(16)
            ?: throw IllegalStateException("No signer available")
    }

    fun getActiveSafe4Wallet(): Wallet? {
        return App.walletManager.activeWallets.firstOrNull { wallet ->
            wallet.token.blockchain.type is BlockchainType.SafeFour
        }
    }

    private fun getDAppsKey(wallet: Wallet): String {
        return "safe4_dapps_${wallet.account.id}"
    }

    private fun getStoredDApps(wallet: Wallet): List<ManagedDAppItem> {
        return try {
            val key = getDAppsKey(wallet)
            val json = prefs.getString(key, null)
            if (json.isNullOrEmpty()) return emptyList()

            val listType = object : TypeToken<List<ManagedDAppItem>>() {}.type
            gson.fromJson(json, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDApps(wallet: Wallet, dApps: List<ManagedDAppItem>) {
        try {
            val key = getDAppsKey(wallet)
            val json = gson.toJson(dApps)
            prefs.edit { putString(key, json) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save DApps", e)
        }
    }

    /**
     * Register DApp on chain via smart contract.
     * Fields mapping:
     *   name → name, contractAddr → contractAddr, url → runUrl, description → description,
     *   iconUrl → gitUrl (stored as metadata on chain), officialUrl → officialUrl, officialEmail → officialEmail
     */
    fun registerDApp(
        name: String,
        url: String,
        description: String,
        iconUrl: String,
        contractAddr: String,
        officialUrl: String,
        officialEmail: String
    ): ManagedDAppItem {
        val wallet = getActiveSafe4Wallet()
            ?: throw IllegalStateException("No active SAFE4 wallet")

        val privateKey = getPrivateKey()

        // Register on chain
        val addrObj = if (contractAddr.isNotBlank()) Address(contractAddr) else Address.DEFAULT
        val txHash = dAppManager.register(
            privateKey,
            name,
            addrObj,
            url,                    // runUrl
            description,
            iconUrl,                // gitUrl - store iconUrl metadata here
            officialUrl,
            officialEmail
        )

        Log.i(TAG, "DApp registered on chain, txHash=$txHash")

        // Return a temporary item (with txHash as id, will be corrected by subsequent refresh)
        val tempItem = ManagedDAppItem(
            id = txHash,
            name = name,
            url = url,
            description = description,
            category = "",
            iconUrl = iconUrl,
            contractAddr = contractAddr,
            officialUrl = officialUrl,
            officialEmail = officialEmail,
            officialAccount = "",
            keyword = "",
            status = "active",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            walletAddress = wallet.account.name
        )

        // Immediately add to in-memory cache and notify UI, so manage page shows the new item right away.
        // Do NOT persist temp item (id=txHash) — chain sync will replace it with the real numeric id.
        cachedDApps.add(0, tempItem)
        dAppsSubject.onNext(cachedDApps.toList())

        // After chain tx, delay 5s then reload from chain with retry (up to 3 times)
        // to ensure the new DApp is indexed and assigned a real numeric id.
        Thread {
            syncChainDAppsWithRetry()
        }.start()

        return tempItem
    }

    /**
     * Sync chain DApps with retry. Waits 5s initially, then retries up to 3 times
     * if the result is empty, with 5s between each attempt.
     */
    private fun syncChainDAppsWithRetry(maxRetries: Int = 3, delayMs: Long = 5000) {
        // Initial delay to let chain index the new transaction
        Thread.sleep(delayMs)

        for (attempt in 1..maxRetries) {
            syncChainDApps()
            val count = cachedDApps.size
            if (count > 0) {
                Log.i(TAG, "Chain sync succeeded after register, dApps count=$count, attempt=$attempt")
                return
            }
            if (attempt < maxRetries) {
                Log.w(TAG, "Chain sync returned empty after register, retry $attempt/$maxRetries after ${delayMs}ms")
                Thread.sleep(delayMs)
            }
        }
        Log.e(TAG, "Chain sync still empty after $maxRetries retries")
    }

    /**
     * Update DApp on chain via individual field setters.
     * Only calls chain APIs for fields that actually changed from the original values.
     */
    fun updateDApp(
        id: String,
        name: String,
        url: String,
        description: String,
        iconUrl: String,
        contractAddr: String,
        officialUrl: String,
        officialEmail: String,
        officialAccount: String,
        keyword: String,
        existing: ManagedDAppItem
    ): ManagedDAppItem {
        val wallet = getActiveSafe4Wallet()
            ?: throw IllegalStateException("No active SAFE4 wallet")

        val privateKey = getPrivateKey()

        // Convert id to BigInteger for chain calls
        val bigIntId = try {
            BigInteger(id)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid DApp id for chain update: $id")
        }

        // Only update fields that actually changed
        if (name != existing.name) {
            dAppManager.setName(privateKey, bigIntId, name)
            Log.i(TAG, "DApp name updated, id=$id")
        }
        if (url != existing.url) {
            dAppManager.setRunUrl(privateKey, bigIntId, url)
            Log.i(TAG, "DApp runUrl updated, id=$id")
        }
        if (description != existing.description) {
            dAppManager.setDescription(privateKey, bigIntId, description)
            Log.i(TAG, "DApp description updated, id=$id")
        }
        if (iconUrl != existing.iconUrl) {
            dAppManager.setGitUrl(privateKey, bigIntId, iconUrl)
            Log.i(TAG, "DApp gitUrl updated, id=$id")
        }
        if (contractAddr != existing.contractAddr && contractAddr.isNotBlank()) {
            dAppManager.setContractAddr(privateKey, bigIntId, Address(contractAddr))
            Log.i(TAG, "DApp contractAddr updated, id=$id")
        }
        if (officialUrl != existing.officialUrl) {
            dAppManager.setOfficialUrl(privateKey, bigIntId, officialUrl)
            Log.i(TAG, "DApp officialUrl updated, id=$id")
        }
        if (officialEmail != existing.officialEmail) {
            dAppManager.setOfficialEmail(privateKey, bigIntId, officialEmail)
            Log.i(TAG, "DApp officialEmail updated, id=$id")
        }
        if (officialAccount != existing.officialAccount && officialAccount.isNotBlank()) {
            dAppManager.setOfficialAccount(privateKey, bigIntId, Address(officialAccount))
            Log.i(TAG, "DApp officialAccount updated, id=$id")
        }
        if (keyword != existing.keyword) {
            dAppManager.setKeyword(privateKey, bigIntId, keyword)
            Log.i(TAG, "DApp keyword updated, id=$id")
        }
        Log.i(TAG, "DApp updated on chain, id=$id")

        // Update local cache
        val index = cachedDApps.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updated = cachedDApps[index].copy(
                name = name,
                url = url,
                description = description,
                iconUrl = iconUrl,
                contractAddr = contractAddr,
                officialUrl = officialUrl,
                officialEmail = officialEmail,
                officialAccount = officialAccount,
                keyword = keyword,
                updatedAt = System.currentTimeMillis()
            )
            cachedDApps[index] = updated
            saveDApps(wallet, cachedDApps)
            dAppsSubject.onNext(cachedDApps.toList())
            return updated
        }

        // If not found locally, refresh from chain
        refresh()
        val refreshed = cachedDApps.find { it.id == id }
            ?: throw IllegalArgumentException("DApp not found after chain update")
        return refreshed
    }

    fun removeDApp(id: String) {
        val wallet = getActiveSafe4Wallet()
            ?: throw IllegalStateException("No active SAFE4 wallet")

        val privateKey = getPrivateKey()
        val bigIntId = try {
            BigInteger(id)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid DApp id for chain removal: $id")
        }

        // Remove from chain
        dAppManager.remove(privateKey, bigIntId)
        Log.i(TAG, "DApp removed from chain, id=$id")

        cachedDApps.removeAll { it.id == id }
        saveDApps(wallet, cachedDApps)
        dAppsSubject.onNext(cachedDApps.toList())
    }

    /**
     * Get the payment amount required for setting a DApp logo.
     */
    fun getLogoPayAmount(): BigInteger {
        return dAppManager.getLogoPayAmount()
    }

    /**
     * Check if a runUrl is already registered on chain.
     */
    fun isRunUrlExists(url: String): Boolean {
        return try {
            dAppManager.existRunUrl(url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check existRunUrl: $url", e)
            false
        }
    }

    /**
     * Update DApp logo on chain. Logo must be <= 128KB.
     */
    fun setLogo(id: String, logoBytes: ByteArray): String {
        val privateKey = getPrivateKey()
        val bigIntId = try {
            BigInteger(id)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid DApp id for setLogo: $id")
        }
        return dAppManager.setLogo(privateKey, bigIntId, logoBytes)
    }

    // region Logo cache

    private val logoCacheDir: File by lazy {
        File(App.instance.cacheDir, "dapp_logos").also { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    private fun getLogoFile(id: String): File = File(logoCacheDir, "${id}.png")

    /**
     * Get DApp logo bytes from chain.
     */
    fun getLogo(id: String): ByteArray? {
        val bigIntId = try {
            BigInteger(id)
        } catch (e: NumberFormatException) {
            return null
        }
        return try {
            dAppManager.getLogo(bigIntId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get logo for id=$id", e)
            null
        }
    }

    /**
     * Get cached logo file path for a DApp. Returns file absolute path if cached, null otherwise.
     */
    fun getCachedLogoPath(id: String): String? {
        val file = getLogoFile(id)
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    /**
     * Get cached logo bytes for a DApp. Returns bytes if cached, null otherwise.
     */
    fun getCachedLogoBytes(id: String): ByteArray? {
        val file = getLogoFile(id)
        return if (file.exists() && file.length() > 0) file.readBytes() else null
    }

    /**
     * Cache logo bytes to local file immediately (e.g. after successful upload).
     */
    fun cacheLogoForId(id: String, bytes: ByteArray) {
        try {
            getLogoFile(id).writeBytes(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache logo for id=$id", e)
        }
    }

    /**
     * Fetch logo from chain and cache to local file. Returns cached file path or null.
     */
    fun fetchAndCacheLogo(id: String): String? {
        // Check cache first
        val cached = getCachedLogoPath(id)
        if (cached != null) return cached

        val bytes = getLogo(id)
        if (bytes != null && bytes.isNotEmpty()) {
            try {
                getLogoFile(id).writeBytes(bytes)
                return getLogoFile(id).absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write logo cache for id=$id", e)
            }
        }
        return null
    }

    /**
     * Background fetch and cache logos for all DApps that don't have cached logos.
     */
    fun cacheAllLogos(dApps: List<ManagedDAppItem>) {
        Thread {
            dApps.forEach { dapp ->
                if (dapp.id.toBigIntegerOrNull() != null) {
                    try {
                        fetchAndCacheLogo(dapp.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to cache logo for DApp ${dapp.id}", e)
                    }
                }
            }
        }.start()
    }

    // endregion

    fun getDApps(): List<ManagedDAppItem> = cachedDApps

    fun getDAppsObservable() = dAppsSubject

    fun refresh() {
        Thread {
            try {
                syncChainDApps()
            } catch (e: Exception) {
                Log.e(TAG, "refresh error", e)
            }
        }.start()
    }

    // region Draft save/load for registration form

    fun saveDraftDApp(state: Safe4DAppModule.RegisterUiState) {
        try {
            val json = gson.toJson(DraftDApp.fromRegisterState(state))
            prefs.edit { putString(DRAFT_KEY, json) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save DApp draft", e)
        }
    }

    fun loadDraftDApp(): Safe4DAppModule.RegisterUiState? {
        return try {
            val json = prefs.getString(DRAFT_KEY, null)
            if (json.isNullOrEmpty()) return null
            val draft = gson.fromJson(json, DraftDApp::class.java)
            draft?.toRegisterState()
        } catch (e: Exception) {
            null
        }
    }

    fun clearDraftDApp() {
        prefs.edit { remove(DRAFT_KEY) }
    }

    // endregion

    override fun clear() {
        // cleanup
    }
}

private data class DraftDApp(
    val name: String = "",
    val url: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val contractAddr: String = "",
    val officialUrl: String = "",
    val officialEmail: String = "",
    val keyword: String = ""
) {
    companion object {
        fun fromRegisterState(state: Safe4DAppModule.RegisterUiState): DraftDApp {
            return DraftDApp(
                name = state.name,
                url = state.url,
                description = state.description,
                iconUrl = state.iconUrl,
                contractAddr = state.contractAddr,
                officialUrl = state.officialUrl,
                officialEmail = state.officialEmail,
                keyword = state.keyword
            )
        }
    }

    fun toRegisterState(): Safe4DAppModule.RegisterUiState {
        return Safe4DAppModule.RegisterUiState(
            name = name,
            url = url,
            description = description,
            iconUrl = iconUrl,
            contractAddr = contractAddr,
            officialUrl = officialUrl,
            officialEmail = officialEmail,
            keyword = keyword
        )
    }
}
