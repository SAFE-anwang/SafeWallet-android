package io.horizontalsystems.bankwallet.modules.nftv2.src721

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.core.App
import kotlinx.parcelize.Parcelize

@Parcelize
data class SRC721ContractInfo(
    val address: String,
    val name: String,
    val symbol: String,
    val burnable: Boolean,
    val creator: String,
    val enabled: Boolean = true,
) : Parcelable

/**
 * 本机发行的 SRC721 合约注册表（MMKV 持久化），
 * 用于 NFT 管理列表和 NFT 资产展示（Safe4NftAdapter）。
 *
 * key 按链隔离：测试网发行的合约不应出现在主网的管理列表中。
 */
object SRC721Storage {

    /** 旧版全局 key，无链标识，仅用于一次性迁移 */
    private const val LEGACY_KEY = "src721_contracts"
    private const val MIGRATED_KEY = "src721_contracts_migrated"

    /** 已删除（本地隐藏）的合约地址 key 前缀 */
    private const val REMOVED_PREFIX = "src721_removed_"

    private val gson = Gson()
    private val listType = object : TypeToken<List<SRC721ContractInfo>>() {}.type
    private val addressSetType = object : TypeToken<Set<String>>() {}.type

    /** 当前链标识：0=主网，1=测试网（与 RedeemStorage / NodeInfo 的约定一致） */
    private fun chainType(): Int = if (App.localStorage.isSafe4TestNet) 1 else 0

    private fun key(): String = "src721_contracts_${chainType()}"

    /**
     * 把旧版全局 key 的数据迁移到当前链，并清理旧 key。
     *
     * 旧数据本身没有链标识，只能归属到执行迁移时所在的链；
     * 通过 [MIGRATED_KEY] 标记保证只执行一次。
     */
    private fun migrateLegacyIfNeeded() {
        val mmkv = MMKV.defaultMMKV() ?: return
        if (!mmkv.getString(MIGRATED_KEY, null).isNullOrEmpty()) return

        val legacy = mmkv.getString(LEGACY_KEY, null)
        if (!legacy.isNullOrEmpty() && mmkv.getString(key(), null).isNullOrEmpty()) {
            mmkv.putString(key(), legacy)
        }
        // 用空串覆盖旧 key，等效于清除（避免依赖额外的 remove API）
        mmkv.putString(LEGACY_KEY, "")
        mmkv.putString(MIGRATED_KEY, "1")
    }

    @Synchronized
    fun list(creator: String? = null): List<SRC721ContractInfo> {
        migrateLegacyIfNeeded()
        val json = MMKV.defaultMMKV()?.getString(key(), null) ?: return emptyList()
        val all: List<SRC721ContractInfo> = try {
            gson.fromJson(json, listType) ?: emptyList()
        } catch (e: Throwable) {
            emptyList()
        }
        return if (creator == null) all
        else all.filter { it.creator.equals(creator, ignoreCase = true) }
    }

    @Synchronized
    fun save(info: SRC721ContractInfo) {
        val all = list().toMutableList()
        all.removeAll { it.address.equals(info.address, ignoreCase = true) }
        all.add(info)
        persist(all)
        // 重新部署 / 重新登记时取消「已删除」标记
        unmarkRemoved(info.address)
    }

    /**
     * 已删除（本地隐藏）的合约地址。
     *
     * 管理列表除了本地注册表，还会并入 insight 接口中 creator 为本账户的合约，
     * 若只从注册表移除，刷新后合约会从接口再次出现，因此需要单独记录删除动作。
     */
    @Synchronized
    fun removedAddresses(): Set<String> {
        val json = MMKV.defaultMMKV()?.getString("$REMOVED_PREFIX${chainType()}", null)
            ?: return emptySet()
        return try {
            val parsed: Set<String>? = gson.fromJson(json, addressSetType)
            parsed ?: emptySet()
        } catch (e: Throwable) {
            emptySet()
        }
    }

    @Synchronized
    fun markRemoved(address: String) {
        val all = removedAddresses().toMutableSet()
        all.add(address.lowercase())
        MMKV.defaultMMKV()?.putString("$REMOVED_PREFIX${chainType()}", gson.toJson(all))
    }

    @Synchronized
    fun unmarkRemoved(address: String) {
        val all = removedAddresses().toMutableSet()
        if (all.remove(address.lowercase())) {
            MMKV.defaultMMKV()?.putString("$REMOVED_PREFIX${chainType()}", gson.toJson(all))
        }
    }

    @Synchronized
    fun setEnabled(address: String, creator: String, enabled: Boolean) {
        val all = list().toMutableList()
        val idx = all.indexOfFirst {
            it.address.equals(address, ignoreCase = true) &&
                    it.creator.equals(creator, ignoreCase = true)
        }
        if (idx >= 0) {
            all[idx] = all[idx].copy(enabled = enabled)
            persist(all)
        }
    }

    @Synchronized
    fun listEnabled(creator: String? = null): List<SRC721ContractInfo> =
        list(creator).filter { it.enabled }

    @Synchronized
    fun remove(address: String, creator: String) {
        val all = list().toMutableList()
        all.removeAll {
            it.address.equals(address, ignoreCase = true) &&
                    it.creator.equals(creator, ignoreCase = true)
        }
        persist(all)
        // 记录删除动作，避免接口重新拉取时该合约又出现
        markRemoved(address)
    }

    private fun persist(all: List<SRC721ContractInfo>) {
        MMKV.defaultMMKV()?.putString(key(), gson.toJson(all))
    }
}
