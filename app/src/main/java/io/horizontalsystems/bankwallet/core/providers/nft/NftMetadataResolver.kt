package io.horizontalsystems.bankwallet.core.providers.nft

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigInteger
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * 从合约直接获取 NFT 元数据（图片等）。
 * 流程：调用合约 tokenURI/uri 方法 -> 获取元数据 JSON -> 解析 image 字段。
 * 支持 http(s) 与 ipfs 链接，结果内存缓存。
 */
class NftMetadataResolver(
    private val evmBlockchainManager: EvmBlockchainManager
) {
    data class NftMeta(
        val name: String?,
        val imageUrl: String?
    )

    private val cache = ConcurrentHashMap<String, NftMeta?>()
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    fun cached(nftUid: NftUid): NftMeta? = cache[nftUid.uid]

    suspend fun resolve(nftUid: NftUid, nftType: NftType): NftMeta? {
        cache[nftUid.uid]?.let { return it }

        val mutex = mutexes.getOrPut(nftUid.uid) { Mutex() }
        return mutex.withLock {
            cache[nftUid.uid]?.let { return@withLock it }
            val meta = fetchMetadata(nftUid, nftType)
            cache[nftUid.uid] = meta
            meta
        }
    }

    private suspend fun fetchMetadata(nftUid: NftUid, nftType: NftType): NftMeta? {
        return try {
            val tokenUri = fetchTokenUri(nftUid, nftType) ?: return null
            val json = fetchJson(resolveUri(tokenUri)) ?: return null
            val image = json.optString("image").ifBlank {
                json.optString("image_url").ifBlank { null }
            }
            NftMeta(
                name = json.optString("name").ifBlank { null },
                imageUrl = image?.let { resolveUri(it) }
            )
        } catch (e: Throwable) {
            Log.d("NftMetadataResolver", "fetchMetadata error for ${nftUid.uid}: $e")
            null
        }
    }

    private suspend fun fetchTokenUri(nftUid: NftUid, nftType: NftType): String? = withContext(Dispatchers.IO) {
        try {
            val evmKitManager = evmBlockchainManager.getEvmKitManager(nftUid.blockchainType)
            val account = App.accountManager.activeAccount ?: return@withContext null
            val evmKit = evmKitManager.getEvmKitWrapper(account, nftUid.blockchainType).evmKit

            val tokenId = nftUid.tokenId.toBigIntegerOrNull() ?: return@withContext null
            val contractAddress = Address(nftUid.contractAddress)

            val methodSignature = when (nftType) {
                NftType.Eip721 -> "tokenURI(uint256)"
                NftType.Eip1155 -> "uri(uint256)"
            }
            val methodId = ContractMethodHelper.getMethodId(methodSignature)
            val data = ContractMethodHelper.encodedABI(methodId, listOf(tokenId))

            val response = evmKit.call(contractAddress, data).await()
            parseAbiString(response)
        } catch (e: Throwable) {
            Log.d("NftMetadataResolver", "fetchTokenUri error: $e")
            null
        }
    }

    /**
     * 解析 ABI 编码的 string 返回值：
     * [0..32) offset -> [offset..offset+32) length -> [offset+32..) data
     */
    private fun parseAbiString(data: ByteArray): String? {
        return try {
            if (data.size < 64) return null
            val offset = data.copyOfRange(0, 32).toInt()
            if (offset + 32 > data.size) return null
            val length = data.copyOfRange(offset, offset + 32).toInt()
            if (offset + 32 + length > data.size) return null
            String(data.copyOfRange(offset + 32, offset + 32 + length), Charsets.UTF_8)
        } catch (e: Throwable) {
            null
        }
    }

    private suspend fun fetchJson(url: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            // data:application/json;base64,... 形式
            if (url.startsWith("data:application/json")) {
                val base64Index = url.indexOf(";base64,")
                val jsonStr = if (base64Index >= 0) {
                    val decoded = android.util.Base64.decode(url.substring(base64Index + 8), android.util.Base64.DEFAULT)
                    String(decoded, Charsets.UTF_8)
                } else {
                    url.substring(url.indexOf(',') + 1)
                }
                return@withContext JSONObject(jsonStr)
            }

            val connection = URL(url).openConnection()
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val text = connection.getInputStream().bufferedReader().use { it.readText() }
            JSONObject(text)
        } catch (e: Throwable) {
            null
        }
    }

    private fun resolveUri(uri: String): String {
        return when {
            uri.startsWith("ipfs://ipfs/") -> "https://ipfs.io/ipfs/" + uri.removePrefix("ipfs://ipfs/")
            uri.startsWith("ipfs://") -> "https://ipfs.io/ipfs/" + uri.removePrefix("ipfs://")
            uri.startsWith("ar://") -> "https://arweave.net/" + uri.removePrefix("ar://")
            else -> uri
        }
    }
}
