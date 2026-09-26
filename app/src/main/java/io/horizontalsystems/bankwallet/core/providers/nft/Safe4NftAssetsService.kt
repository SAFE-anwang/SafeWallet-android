package io.horizontalsystems.bankwallet.core.providers.nft

import android.util.Log
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.modules.nftv2.src721.SRC721Service
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

    private val service: Safe4InsightNftApi by lazy {
        val baseUrl = if (Chain.isSafe4TestMode) {
            "https://safe4testnet.anwang.com/"
        } else {
            "https://safe4.anwang.com/"
        }
        APIClient.retrofit(baseUrl, 30).create(Safe4InsightNftApi::class.java)
    }

    val blockchainType: BlockchainType get() = BlockchainType.SafeFour

    /** 合约地址 -> NFT 合约信息（名称 / 符号 / logo），只缓存成功结果 */
    private val tokens = ConcurrentHashMap<String, Safe4NftToken>()

    /** 资产明细缓存，key = "contract:tokenId"，供详情页复用避免重复请求 */
    private val assetCache = ConcurrentHashMap<String, Safe4NftAsset>()

    /** 资产明细缓存命中时，用于回查合集名称 */
    private var tokensLoaded = false

    /** 当前网络下的 NFT 合约列表（含持有数量），失败返回 null 以区分「无数据」 */
    suspend fun fetchAssets(address: String): List<Safe4NftAsset>? = request {
        service.assets(address)
    }

    /** 某合约下的 NFT 资产明细（结果写入缓存，供元数据查询复用） */
    suspend fun fetchAssetsOfToken(
        address: String,
        tokenAddress: String
    ): List<Safe4NftAsset>? = request {
        service.assets(address, tokenAddress)
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
        val result = request { service.tokens() } ?: return null
        result.forEach { token ->
            if (token.address.isNotBlank()) {
                tokens[token.address.lowercase()] = token
            }
        }
        tokensLoaded = true
        return result
    }

    /**
     * 取合集（合约）名称，数据来源为 insight 的 `nft/tokens` 接口。
     *
     * 先查索引，未命中则加载一次全量列表；仍无结果时回退调用合约
     * `name()` / `symbol()`（部分未收录合约只能链上读取）。
     * 返回 null 表示无法获取，由调用方兜底展示地址缩写。
     */
    suspend fun fetchCollectionName(tokenAddress: String, web3j: Web3j? = null): String? {
        tokens[tokenAddress.lowercase()]?.let { return it.displayName }

        if (!tokensLoaded) {
            fetchTokens()
        }
        tokens[tokenAddress.lowercase()]?.let { return it.displayName }

        // 接口未收录该合约，回退链上读取
        val onChain = web3j?.let { readOnChainName(it, tokenAddress) } ?: return null
        tokens[tokenAddress.lowercase()] = Safe4NftToken(
            address = tokenAddress,
            name = onChain,
            symbol = onChain
        )
        return onChain
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
    val isErc721: Boolean get() = tokenType.equals("erc721", true)
    val balance: Int get() = tokenValue?.toIntOrNull() ?: 1
}
