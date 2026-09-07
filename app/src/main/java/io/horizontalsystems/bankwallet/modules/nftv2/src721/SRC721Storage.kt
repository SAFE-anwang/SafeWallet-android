package io.horizontalsystems.bankwallet.modules.nftv2.src721

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
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
 */
object SRC721Storage {

    private const val KEY = "src721_contracts"

    private val gson = Gson()
    private val listType = object : TypeToken<List<SRC721ContractInfo>>() {}.type

    @Synchronized
    fun list(creator: String? = null): List<SRC721ContractInfo> {
        val json = MMKV.defaultMMKV()?.getString(KEY, null) ?: return emptyList()
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
    }

    private fun persist(all: List<SRC721ContractInfo>) {
        MMKV.defaultMMKV()?.putString(KEY, gson.toJson(all))
    }
}
