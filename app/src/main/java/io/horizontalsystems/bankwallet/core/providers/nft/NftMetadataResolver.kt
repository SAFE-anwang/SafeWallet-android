package io.horizontalsystems.bankwallet.core.providers.nft

import android.util.Log
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.coroutines.CancellationException
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
 * 支持 http(s) 与 ipfs 链接。
 *
 * 结果做两级缓存：内存 + MMKV 持久化。
 * 页面先渲染缓存里的图片地址，缺失的条目再上链解析；
 * 解析成功会写回缓存并通知调用方刷新页面，因此冷启动也能立刻看到图片。
 */
class NftMetadataResolver(
    private val evmBlockchainManager: EvmBlockchainManager
) {
    data class NftMeta(
        val name: String?,
        val imageUrl: String?,
        val description: String? = null
    )

    private val cache = ConcurrentHashMap<String, NftMeta>()

    /**
     * 失败记录：value 为失败时间戳。
     *
     * 不做永久缓存——合约可能因临时网络问题或节点不可用而失败，
     * 永久标记会导致图标再也无法恢复显示；这里仅在 [FAILED_TTL_MS] 内跳过重试。
     */
    private val failed = ConcurrentHashMap<String, Long>()
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    /**
     * 读缓存：内存优先，未命中则读持久化缓存并提升到内存。
     *
     * 持久化的意义在于冷启动：上次解析好的图片地址能直接拿出来渲染，
     * 页面不用等合约 `tokenURI()` 调用 + 元数据 JSON 请求完成才显示图片。
     */
    fun cached(nftUid: NftUid): NftMeta? {
        val key = cacheKey(nftUid.uid)
        cache[key]?.let { return it }
        val saved = readCachedMeta(key) ?: return null
        cache[key] = saved
        return saved
    }

    /** 获取成功：写入内存与持久化缓存，调用方随后刷新页面 */
    private fun putCache(uid: String, meta: NftMeta) {
        val key = cacheKey(uid)
        cache[key] = meta
        writeCachedMeta(key, meta)
    }

    /**
     * 持久化缓存使用独立 MMKV 实例。
     *
     * 每个 NFT 一个 key，条目数量随用户持有的 NFT 增长，
     * 单独放一个实例可避免把 App 主存储撑大（与 SafeInfoManager 的做法一致）。
     */
    private val storage: MMKV? by lazy { MMKV.mmkvWithID(MMKV_ID, MMKV.SINGLE_PROCESS_MODE) }

    /**
     * 缓存 key 需要带链标识。
     *
     * SAFE4 主网与测试网共用同一个 [BlockchainType]，而 `nftUid` 内不含网络信息，
     * 不做隔离会导致测试网解析出的图片出现在主网（与 `SRC721Storage` 的约定一致）。
     */
    private fun cacheKey(uid: String) = "${chainScope()}_$uid"

    private fun chainScope(): Int = if (App.localStorage.isSafe4TestNet) 1 else 0

    private fun storageKey(key: String) = "$MMKV_PREFIX$key"

    private fun readCachedMeta(key: String): NftMeta? {
        return try {
            val json = storage?.getString(storageKey(key), null) ?: return null
            val obj = JSONObject(json)
            NftMeta(
                name = obj.optString("name").ifBlank { null },
                imageUrl = obj.optString("imageUrl").ifBlank { null },
                description = obj.optString("description").ifBlank { null }
            )
        } catch (e: Throwable) {
            Log.d(TAG, "read cache failed for $key: $e")
            null
        }
    }

    private fun writeCachedMeta(key: String, meta: NftMeta) {
        try {
            val obj = JSONObject()
            meta.name?.takeIf { it.isNotBlank() }?.let { obj.put("name", it) }
            meta.imageUrl?.takeIf { it.isNotBlank() }?.let { obj.put("imageUrl", it) }
            meta.description?.takeIf { it.isNotBlank() }?.let { obj.put("description", it) }
            storage?.putString(storageKey(key), obj.toString())
        } catch (e: Throwable) {
            Log.d(TAG, "write cache failed for $key: $e")
        }
    }

    private fun isRecentlyFailed(uid: String): Boolean {
        val at = failed[uid] ?: return false
        if (System.currentTimeMillis() - at < FAILED_TTL_MS) return true
        failed.remove(uid)
        return false
    }

    suspend fun resolve(nftUid: NftUid, nftType: NftType): NftMeta? {
        // 命中内存/持久化缓存直接返回，避免重复上链
        cached(nftUid)?.let { return it }
        if (isRecentlyFailed(nftUid.uid)) return null

        val mutex = mutexes.getOrPut(nftUid.uid) { Mutex() }
        return mutex.withLock {
            cached(nftUid)?.let { return@withLock it }
            if (isRecentlyFailed(nftUid.uid)) return@withLock null
            val meta = fetchMetadata(nftUid, nftType)
            if (meta != null) {
                // 获取成功：更新内存 + 持久化缓存，由调用方刷新页面
                putCache(nftUid.uid, meta)
                failed.remove(nftUid.uid)
            } else {
                failed[nftUid.uid] = System.currentTimeMillis()
            }
            meta
        }
    }

    private suspend fun fetchMetadata(nftUid: NftUid, nftType: NftType): NftMeta? {
        return try {
            val tokenUri = fetchTokenUri(nftUid, nftType)
            if (tokenUri.isNullOrEmpty()) return null
            val json = fetchJson(resolveUri(tokenUri))
            if (json == null) return null
            val image = sequenceOf(
                "image", "image_url", "image_data", "imageUrl",
                "animation_url", "animationUrl"
            ).map { json.optString(it) }.firstOrNull { it.isNotBlank() }
            NftMeta(
                name = json.optString("name").ifBlank { null },
                imageUrl = image?.let { resolveUri(it) },
                description = json.optString("description").ifBlank { null }
            )
        } catch (e: CancellationException) {
            // 页面退出导致的取消不是「解析失败」，必须向上抛出：
            // 若在这里被吞掉会被记进 failed，并在 FAILED_TTL_MS 内屏蔽重试，
            // 再次进入页面时整屏资产都拿不到名称与图片
            throw e
        } catch (e: Throwable) {
            Log.d(TAG, "fetchMetadata error for ${nftUid.uid}: $e")
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.d(TAG, "fetchTokenUri error: $e")
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            null
        }
    }

    private fun resolveUri(uri: String): String {
        return when {
            uri.startsWith("ipfs://ipfs/") -> "https://gateway.pinata.cloud/ipfs/" + uri.removePrefix("ipfs://ipfs/")
            uri.startsWith("ipfs://") -> "https://gateway.pinata.cloud/ipfs/" + uri.removePrefix("ipfs://")
            // 部分合约直接返回 ipfs 路径（无 scheme），如 "ipfs/Qm..." 或 "Qm..."（CIDv0/CIDv1）
            uri.startsWith("ipfs/") -> "https://gateway.pinata.cloud/ipfs/" + uri.removePrefix("ipfs/")
            uri.startsWith("ar://") -> "https://arweave.net/" + uri.removePrefix("ar://")
            else -> uri
        }
    }

    companion object {
        private const val TAG = "NftMetadataResolver"

        /** 失败后的重试间隔，避免永久屏蔽导致图标无法恢复 */
        private const val FAILED_TTL_MS = 60_000L

        /** 持久化缓存 key 前缀，每个 NFT 一个 key，避免整表序列化 */
        private const val MMKV_PREFIX = "nft_meta_cache_"

        /** 持久化缓存使用的独立 MMKV 实例 ID */
        private const val MMKV_ID = "nft_metadata_cache"
    }
}
