package io.horizontalsystems.bankwallet.core.providers.nft

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.modules.nftv2.src721.SRC721Service
import io.horizontalsystems.bankwallet.modules.nftv2.src721.SRC721Storage
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap

/**
 * SAFE4 链上 NFT（SRC721）数据查询服务。
 *
 * 主网/测试网基址不同，通过 [Chain.isSafe4TestMode] 区分。
 * 负责四类查询：
 * 1. 账户持有的 NFT 合约列表（[fetchAssets]）；
 * 2. 某合约下的资产明细（[fetchAssetsOfToken]）；
 * 3. 全网络 NFT 合约列表（[fetchTokens] / [fetchCollectionName]）；
 * 4. 合集名称与 logo。
 */
object Safe4NftAssetsService {

    private const val TAG = "Safe4NftAssets"

    private const val TESTNET_BASE_URL = "https://safe4testnet.anwang.com/"
    private const val MAINNET_BASE_URL = "https://safe4.anwang.com/"

    /** 合约地址 -> NFT 合约信息（名称 / 符号 / logo），只缓存成功结果 */
    private val tokens = ConcurrentHashMap<String, Safe4NftToken>()

    /** 资产明细缓存，key = "contract:tokenId"，供详情页复用避免重复请求 */
    private val assetCache = ConcurrentHashMap<String, Safe4NftAsset>()

    /** `nft/tokens` 全量索引是否已加载 */
    private var tokensLoaded = false

    /** 已绑定的链标识：0=主网，1=测试网 */
    private var boundChainType: Int = -1
    private var serviceInstance: Safe4InsightNftApi? = null

    /**
     * 取接口实例；链切换时重建并清空缓存。
     *
     * 主网与测试网的数据必须完全隔离，否则测试网的 NFT 会出现在主网列表，
     * 也会让 [Safe4NftActionDetector] 把测试网的合约误判为主网的 NFT 合约。
     */
    @Synchronized
    private fun service(): Safe4InsightNftApi {
        val chainType = if (Chain.isSafe4TestMode) 1 else 0
        if (chainType != boundChainType) {
            tokens.clear()
            assetCache.clear()
            tokensLoaded = false
            val baseUrl = if (chainType == 1) TESTNET_BASE_URL else MAINNET_BASE_URL
            serviceInstance = APIClient.retrofit(baseUrl, 30).create(Safe4InsightNftApi::class.java)
            boundChainType = chainType
        }
        return serviceInstance!!
    }

    val blockchainType: BlockchainType get() = BlockchainType.SafeFour

    /** 当前网络下的 NFT 合约列表（含持有数量），失败返回 null 以区分「无数据」 */
    suspend fun fetchAssets(address: String): List<Safe4NftAsset>? = request {
        service().assets(address)
    }

    /** 某合约下的 NFT 资产明细（结果写入缓存，供元数据查询复用） */
    suspend fun fetchAssetsOfToken(
        address: String,
        tokenAddress: String
    ): List<Safe4NftAsset>? = request {
        service().assets(address, tokenAddress)
    }?.also { assets ->
        assets.forEach { asset ->
            val tokenId = asset.tokenId ?: return@forEach
            assetCache[cacheKey(asset.token, tokenId)] = asset
        }
    }

    /** 读取已缓存的资产明细（不发起网络请求） */
    fun cachedAsset(tokenAddress: String, tokenId: String): Safe4NftAsset? =
        assetCache[cacheKey(tokenAddress, tokenId)]

    /**
     * 拉取全网络 NFT 合约列表并建立「合约地址 -> 合约信息」索引。
     *
     * 一次请求可覆盖所有合集的 name / symbol / logo，
     * 避免逐个合约发链上 name() 调用。成功加载后不再重复请求。
     */
    suspend fun fetchTokens(force: Boolean = false): List<Safe4NftToken>? {
        if (tokensLoaded && !force) {
            return tokens.values.toList()
        }
        val result = request { service().tokens() } ?: return null
        result.forEach { token ->
            if (token.address.isNotBlank()) {
                tokens[token.address.lowercase()] = token
            }
        }
        tokensLoaded = true
        return result
    }

    /**
     * 取合集（合约）名称，按可靠性依次尝试三个来源：
     *
     * 1. insight 的 `nft/tokens` 索引（最权威，一次请求覆盖全网合约）；
     * 2. 本机 `SRC721Storage` 注册表（接口未收录时，自部署合约在本地仍有 name/symbol）；
     * 3. 链上 `name()` / `symbol()`（最后兜底）。
     *
     * 三者都拿不到才返回 null，由调用方兜底展示地址缩写。
     * 注意：`nft/tokens` 只覆盖部分合约，未收录的合约必须靠 2/3 才能拿到名称。
     */
    suspend fun fetchCollectionName(tokenAddress: String, web3j: Web3j? = null): String? {
        // 1. 接口索引
        tokens[tokenAddress.lowercase()]?.displayName?.let { return it }
        if (!tokensLoaded) {
            fetchTokens()
        }
        tokens[tokenAddress.lowercase()]?.displayName?.let { return it }

        // 2. 本机注册表（自部署的合约一定有记录）
        localName(tokenAddress)?.let { return cacheName(tokenAddress, it) }

        // 3. 链上读取（调用方未传 web3j 时自行获取，避免回退逻辑形同虚设）
        val chain = web3j ?: safe4Web3j() ?: return null
        val onChain = readOnChainName(chain, tokenAddress)
            ?: return null
        return cacheName(tokenAddress, onChain)
    }

    /** 把解析到的名称写回索引，后续调用可直接命中 */
    private fun cacheName(tokenAddress: String, name: String): String {
        tokens[tokenAddress.lowercase()] = Safe4NftToken(
            address = tokenAddress,
            name = name,
            symbol = name
        )
        return name
    }

    /** 本机部署记录中的合约名称 */
    private fun localName(tokenAddress: String): String? =
        SRC721Storage.list()
            .firstOrNull { it.address.equals(tokenAddress, ignoreCase = true) }
            ?.name
            ?.takeIf { it.isNotBlank() }

    /**
     * 取 SAFE4 的 web3j，用于接口与本机注册表都未覆盖的合约读取名称。
     * 与 [SRC721LogoProvider] 中的同名方法一致。
     */
    private fun safe4Web3j(): Web3j? {
        return try {
            val account = App.accountManager.activeAccount ?: return null
            val evmKitWrapper = App.evmBlockchainManager
                .getEvmKitManager(BlockchainType.SafeFour)
                .getEvmKitWrapper(account, BlockchainType.SafeFour)
            (evmKitWrapper.evmKit.blockchain as? RpcBlockchainSafe4)?.web3j
        } catch (e: Throwable) {
            Log.d(TAG, "safe4Web3j unavailable: $e")
            null
        }
    }

    /** 合集 logo（接口 logoURI），可为空 */
    fun collectionLogo(tokenAddress: String): String? =
        tokens[tokenAddress.lowercase()]?.logoURI?.takeIf { it.isNotBlank() }

    /**
     * 判断地址是否为已知的 NFT 合约。
     *
     * 依据 `nft/tokens` 索引，用于交易列表区分 NFT 操作与普通合约调用。
     * 索引未加载时返回 false，避免误判。
     */
    fun isNftContract(address: String): Boolean =
        tokens.containsKey(address.lowercase())

    private suspend fun readOnChainName(web3j: Web3j, tokenAddress: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val src721 = SRC721Service(web3j, tokenAddress)
                src721.name().takeIf { it.isNotBlank() }
                    ?: src721.symbol().takeIf { it.isNotBlank() }
            } catch (e: Throwable) {
                Log.d(TAG, "readOnChainName $tokenAddress failed: $e")
                null
            }
        }

    private fun cacheKey(tokenAddress: String, tokenId: String) =
        "${tokenAddress.lowercase()}:${tokenId.lowercase()}"

    private suspend fun <T> request(
        call: suspend () -> Safe4NftResponse<T>
    ): List<T>? = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.status != "1") {
                Log.w(TAG, "request failed: ${response.message}")
                null
            } else {
                response.result
            }
        } catch (e: Throwable) {
            Log.e(TAG, "request error", e)
            null
        }
    }

    private interface Safe4InsightNftApi {
        @GET("insight/api/nft/assets")
        suspend fun assets(
            @Query("address") address: String,
            @Query("tokenAddress") tokenAddress: String? = null
        ): Safe4NftResponse<Safe4NftAsset>

        @GET("insight/api/nft/tokens")
        suspend fun tokens(): Safe4NftResponse<Safe4NftToken>
    }
}

data class Safe4NftResponse<T>(
    val status: String? = "1",
    val message: String? = null,
    val result: List<T> = emptyList()
)

/** `insight/api/nft/tokens` 返回的 NFT 合约信息 */
data class Safe4NftToken(
    val address: String = "",
    val name: String? = null,
    val symbol: String? = null,
    val holders: Int? = null,
    val totalTransfers: Int? = null,
    val totalAssets: Int? = null,
    /** NFT 资产类型，如 erc721 / erc1155 */
    val type: String? = null,
    val logoURI: String? = null,
    /** 是否通过模板合约发行 */
    val template: Boolean? = null,
    val creator: String? = null,
) {
    /** 展示名称：优先 name，回退 symbol */
    val displayName: String?
        get() = name?.takeIf { it.isNotBlank() }
            ?: symbol?.takeIf { it.isNotBlank() }
}

data class Safe4NftAsset(
    val owner: String? = null,
    val token: String = "",
    val tokenType: String? = null,
    val tokenId: String? = null,
    val tokenValue: String? = null,
    val tokenURI: String? = null,
    val tokenImage: String? = null,
    /** 合约级接口返回的持有数量 */
    val count: Int? = null,
) {
    /**
     * 是否为 ERC721。
     *
     * 注意 tokenType 可能缺失：该资产来自 `nft/assets?tokenAddress=` 明细接口，
     * 本身已经是某个 NFT 合约下的资产，若因为缺字段被过滤掉，
     * 会出现「接口调用成功、但整个合集凭空消失」的情况。
     * 因此只有明确是其他标准（如 erc1155）时才排除。
     */
    val isErc721: Boolean
        get() = tokenType.isNullOrBlank() || tokenType.equals("erc721", true)

    val balance: Int get() = tokenValue?.toIntOrNull() ?: 1
}
