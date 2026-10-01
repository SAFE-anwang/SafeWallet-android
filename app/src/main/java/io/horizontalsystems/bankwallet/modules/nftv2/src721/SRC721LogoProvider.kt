package io.horizontalsystems.bankwallet.modules.nftv2.src721

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * SRC721 合约 logo 的本地缓存与读取。
 *
 * 合约 logo 由合约 owner 通过 `SRC721.setLogo` 付费上传，链上只能通过 `SRC721.logo()`
 * 读回，insight 接口并不下发该数据。此前 App 从未读取过它，导致用户付费上传后
 * 界面上看不到任何变化（上传的图标不生效）。
 *
 * 文件名带内容 hash：logo 更新后文件路径随之变化，既避免旧的图片缓存继续生效，
 * 也便于在更新时清理该合约的历史文件。
 */
object SRC721LogoProvider {

    private const val TAG = "SRC721LogoProvider"

    /** 链上确实没有 logo 的合约，在 [EMPTY_TTL_MS] 内不再重复上链查询 */
    private const val EMPTY_TTL_MS = 10 * 60 * 1000L
    private val emptyChecked = ConcurrentHashMap<String, Long>()

    /** 链标识：0=主网，1=测试网 */
    private fun chainType(): Int = if (App.localStorage.isSafe4TestNet) 1 else 0

    private fun dir(): File {
        val dir = File(App.instance.cacheDir, "src721_logos/${chainType()}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun prefix(contract: String): String = "${contract.lowercase()}_"

    /** 已缓存的 logo 文件绝对路径；无缓存返回 null */
    fun cachedPath(contract: String): String? {
        return try {
            dir().listFiles()
                ?.firstOrNull { it.name.startsWith(prefix(contract)) && it.length() > 0 }
                ?.absolutePath
        } catch (e: Throwable) {
            null
        }
    }

    /** 缓存 logo 字节（覆盖该合约的旧文件），返回文件绝对路径 */
    fun cache(contract: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return try {
            invalidate(contract)
            val file = File(dir(), "${prefix(contract)}${hash(bytes)}.png")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Throwable) {
            Log.e(TAG, "cache logo failed: $contract", e)
            null
        }
    }

    /** 清除该合约的本地 logo 缓存（下次读取会重新上链获取） */
    fun invalidate(contract: String) {
        try {
            dir().listFiles()
                ?.filter { it.name.startsWith(prefix(contract)) }
                ?.forEach { it.delete() }
            emptyChecked.remove(contract.lowercase())
        } catch (e: Throwable) {
            Log.e(TAG, "invalidate logo failed: $contract", e)
        }
    }

    /**
     * 读取合约 logo 并缓存到本地，返回文件绝对路径。
     *
     * 已缓存则直接返回；链上未设置 logo 时返回 null。
     * 注意：链上返回的是原始图片字节，需在 UI 侧解码后展示。
     */
    suspend fun fetchPath(contract: String): String? = withContext(Dispatchers.IO) {
        cachedPath(contract)?.let { return@withContext it }

        val key = contract.lowercase()
        val checkedAt = emptyChecked[key]
        if (checkedAt != null && System.currentTimeMillis() - checkedAt < EMPTY_TTL_MS) {
            return@withContext null
        }

        val web3j = safe4Web3j() ?: return@withContext null

        val bytes = try {
            SRC721Service(web3j, contract).logo()
        } catch (e: Throwable) {
            Log.d(TAG, "read logo failed: $contract, $e")
            null
        }

        if (bytes == null || bytes.isEmpty()) {
            Log.d(TAG, "contract has no logo: $contract")
            emptyChecked[key] = System.currentTimeMillis()
            return@withContext null
        }

        cache(contract, bytes)
    }

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

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("MD5")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
            .take(8)
}
