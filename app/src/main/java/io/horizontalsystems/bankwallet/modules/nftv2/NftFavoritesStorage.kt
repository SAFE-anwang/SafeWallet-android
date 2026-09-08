package io.horizontalsystems.bankwallet.modules.nftv2

import com.tencent.mmkv.MMKV

/**
 * NFT 合集收藏列表（MMKV 持久化），key = "chainUid:contractAddress.lowercase"
 */
object NftFavoritesStorage {

    private const val KEY = "nft_favorites"

    private fun mmkv() = MMKV.defaultMMKV()

    @Synchronized
    fun isFavorite(blockchainUid: String, contractAddress: String): Boolean {
        val key = composeKey(blockchainUid, contractAddress)
        val set = mmkv()?.getStringSet(KEY, emptySet()) ?: return false
        return key in set
    }

    @Synchronized
    fun setFavorite(blockchainUid: String, contractAddress: String, favorite: Boolean) {
        val mkv = mmkv() ?: return
        val key = composeKey(blockchainUid, contractAddress)
        val set = mkv.getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (favorite) set.add(key) else set.remove(key)
        mkv.putStringSet(KEY, set)
    }

    @Synchronized
    fun toggle(blockchainUid: String, contractAddress: String): Boolean {
        val current = isFavorite(blockchainUid, contractAddress)
        setFavorite(blockchainUid, contractAddress, !current)
        return !current
    }

    @Synchronized
    fun all(): Set<String> {
        return mmkv()?.getStringSet(KEY, emptySet()) ?: emptySet()
    }

    fun composeKey(blockchainUid: String, contractAddress: String): String =
        "$blockchainUid:${contractAddress.lowercase()}"

    fun parseKey(key: String): Pair<String, String>? {
        val parts = key.split(":")
        if (parts.size < 2) return null
        return parts[0] to parts[1]
    }
}
