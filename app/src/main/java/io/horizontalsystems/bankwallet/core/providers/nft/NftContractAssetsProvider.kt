package io.horizontalsystems.bankwallet.core.providers.nft

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap

/**
 * 获取 NFT 合约下的可用资产（参考 TokenPocket）：
 * 1. 优先使用 Reservoir 索引 API（/tokens/v6），返回带名称与图片的资产列表；
 * 2. 未命中时回退到 ERC721 Enumerable 链上枚举（totalSupply + tokenByIndex）。
 * 结果按合约做内存缓存。
 */
class NftContractAssetsProvider(
    private val evmBlockchainManager: EvmBlockchainManager
) {

    data class ContractAsset(
        val tokenId: String,
        val name: String?,
        val imageUrl: String?,
    )

    private val cache = ConcurrentHashMap<String, List<ContractAsset>>()
    private val services = ConcurrentHashMap<BlockchainType, ReservoirTokensApi?>()

    suspend fun availableAssets(
        blockchainType: BlockchainType,
        contractAddress: String,
        limit: Int = 50
    ): List<ContractAsset> {
        val cacheKey = "${blockchainType.uid}:${contractAddress.lowercase()}"
        cache[cacheKey]?.let { return it }

        val assets = fetchFromReservoir(blockchainType, contractAddress, limit)
            ?: fetchFromEnumerable(blockchainType, contractAddress, limit)
            ?: emptyList()

        cache[cacheKey] = assets
        return assets
    }

    private suspend fun fetchFromReservoir(
        blockchainType: BlockchainType,
        contractAddress: String,
        limit: Int
    ): List<ContractAsset>? {
        val service = reservoirService(blockchainType) ?: return null
        return try {
            val response = service.tokens(contractAddress, limit)
            response.tokens.mapNotNull { entry ->
                val tokenId = entry.token.tokenId ?: return@mapNotNull null
                // 名称与图片均为空的条目视为无效数据
                if (entry.token.name == null && entry.token.image == null) return@mapNotNull null
                ContractAsset(
                    tokenId = tokenId,
                    name = entry.token.name,
                    imageUrl = entry.token.image
                )
            }
        } catch (e: Throwable) {
            Log.d("NftContractAssets", "reservoir error for $contractAddress: $e")
            null
        }
    }

    private fun reservoirService(blockchainType: BlockchainType): ReservoirTokensApi? {
        if (!services.containsKey(blockchainType)) {
            val retrofit: Retrofit? = when (blockchainType) {
                BlockchainType.Ethereum -> APIClient.retrofit("https://api.reservoir.tools/", 30)
                BlockchainType.BinanceSmartChain -> APIClient.retrofit("https://api-bsc.reservoir.tools/", 30)
                BlockchainType.Polygon -> APIClient.retrofit("https://api-polygon.reservoir.tools/", 30)
                BlockchainType.ArbitrumOne -> APIClient.retrofit("https://api-arbitrum.reservoir.tools/", 30)
                BlockchainType.Optimism -> APIClient.retrofit("https://api-optimism.reservoir.tools/", 30)
                BlockchainType.Base -> APIClient.retrofit("https://api-base.reservoir.tools/", 30)
                else -> null
            }
            services[blockchainType] = retrofit?.create(ReservoirTokensApi::class.java)
        }
        return services[blockchainType]
    }

    private suspend fun fetchFromEnumerable(
        blockchainType: BlockchainType,
        contractAddress: String,
        limit: Int
    ): List<ContractAsset>? = withContext(Dispatchers.IO) {
        try {
            val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchainType)
            val account = App.accountManager.activeAccount ?: return@withContext null
            val evmKit = evmKitManager.getEvmKitWrapper(account, blockchainType).evmKit
            val contract = Address(contractAddress)

            // totalSupply()
            val totalSupplyData = evmKit.call(
                contract,
                ContractMethodHelper.encodedABI(ContractMethodHelper.getMethodId("totalSupply()"), listOf())
            ).await()
            val totalSupply = parseUint256(totalSupplyData)?.toInt() ?: return@withContext null
            if (totalSupply <= 0) return@withContext emptyList()

            // tokenByIndex(i)
            val tokenByIndexMethodId = ContractMethodHelper.getMethodId("tokenByIndex(uint256)")
            val count = minOf(totalSupply, limit)
            val assets = mutableListOf<ContractAsset>()
            for (i in 0 until count) {
                try {
                    val data = evmKit.call(
                        contract,
                        ContractMethodHelper.encodedABI(tokenByIndexMethodId, listOf(BigInteger.valueOf(i.toLong())))
                    ).await()
                    parseUint256(data)?.let { tokenId ->
                        assets.add(ContractAsset(tokenId.toString(), null, null))
                    }
                } catch (e: Throwable) {
                    break
                }
            }
            assets
        } catch (e: Throwable) {
            Log.d("NftContractAssets", "enumerable error for $contractAddress: $e")
            null
        }
    }

    private fun parseUint256(data: ByteArray): BigInteger? {
        return try {
            if (data.size < 32) null else BigInteger(1, data.copyOfRange(0, 32))
        } catch (e: Throwable) {
            null
        }
    }

    private interface ReservoirTokensApi {
        @GET("tokens/v6")
        suspend fun tokens(
            @Query("contract") contract: String,
            @Query("limit") limit: Int,
            @Query("includeAttributes") includeAttributes: Boolean = false
        ): TokensResponse
    }

    data class TokensResponse(val tokens: List<TokenEntry>, val continuation: String?)
    data class TokenEntry(val token: TokenInfo)
    data class TokenInfo(val tokenId: String?, val name: String?, val image: String?)
}
