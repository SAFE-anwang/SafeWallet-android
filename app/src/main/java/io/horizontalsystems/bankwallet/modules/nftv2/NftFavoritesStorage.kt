package io.horizontalsystems.bankwallet.modules.nftv2

import com.tencent.mmkv.MMKV

/**
 * NFT 合集收藏列表（MMKV 持久化）。
 *
 * 存储按**账户地址**隔离：每个钱包使用独立的 MMKV key（见 [storageKey]），
 * 避免在 A 钱包收藏后 B 钱包也显示为已收藏（以及互相取消）。
 * 集合内元素 key = "chainUid:contractAddress.lowercase"。
 */
object NftFavoritesStorage {

    private const val KEY_PREFIX = "nft_favorites"

    private fun mmkv() = MMKV.defaultMMKV()

    /** 按账户地址区分存储 key，保证不同钱包的收藏互不影响 */
    private fun storageKey(account: String): String = "${KEY_PREFIX}_${account.lowercase()}"

    @Synchronized
    fun isFavorite(account: String, blockchainUid: String, contractAddress: String): Boolean {
        val set = mmkv()?.getStringSet(storageKey(account), emptySet()) ?: return false
        return composeKey(blockchainUid, contractAddress) in set
    }

    @Synchronized
    fun setFavorite(account: String, blockchainUid: String, contractAddress: String, favorite: Boolean) {
        val mkv = mmkv() ?: return
        val storageKey = storageKey(account)
        val key = composeKey(blockchainUid, contractAddress)
        val set = mkv.getStringSet(storageKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (favorite) set.add(key) else set.remove(key)
        mkv.putStringSet(storageKey, set)
    }

    @Synchronized
    fun toggle(account: String, blockchainUid: String, contractAddress: String): Boolean {
        val current = isFavorite(account, blockchainUid, contractAddress)
        setFavorite(account, blockchainUid, contractAddress, !current)
        return !current
    }

    /** 指定账户的全部收藏 */
    @Synchronized
    fun all(account: String): Set<String> {
        return mmkv()?.getStringSet(storageKey(account), emptySet()) ?: emptySet()
    }

    fun composeKey(blockchainUid: String, contractAddress: String): String =
        "$blockchainUid:${contractAddress.lowercase()}"

    fun parseKey(key: String): Pair<String, String>? {
        val parts = key.split(":")
        if (parts.size < 2) return null
        return parts[0] to parts[1]
    }
}
