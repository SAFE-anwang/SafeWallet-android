package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.marketkit.models.BlockchainType

/**
 * 内置 NFT 合集数据（参考 TokenPocket 的预置 NFT 识别）：
 * 为已知合约提供规范的名称与图标；showAlways = true 的合集
 * 即使用户未持有也会以数量 0 展示在 NFT 列表中。
 */
data class BuiltinNftCollection(
    val blockchainType: BlockchainType,
    val contractAddress: String,
    val name: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val showAlways: Boolean = true,
)

object BuiltinNftCollections {

    private val collections = listOf(
        BuiltinNftCollection(
            blockchainType = BlockchainType.BinanceSmartChain,
            contractAddress = "0x46A15B0b27311cedF172AB29E4f4766fbE7F4364",
            name = "Pancake V3 Positions NFT-V1",
        ),
        BuiltinNftCollection(
            blockchainType = BlockchainType.BinanceSmartChain,
            contractAddress = "0xDf7952B35f24aCF7fC0487D01c8d5690a60DBa07",
            name = "Pancake Bunnies",
            imageUrl = "https://static-nft.pancakeswap.com/mainnet/0xDf7952B35f24aCF7fC0487D01c8d5690a60DBa07/collection-sm.png",
            description = "Pancake Bunnies are PancakeSwap's official, home-raised NFT.",
        ),
        BuiltinNftCollection(
            blockchainType = BlockchainType.BinanceSmartChain,
            contractAddress = "0xb66f8289fAbd691F5b5A646db477A87BeA17A284",
            name = "Rewards at 2eth.eu",
        ),
        BuiltinNftCollection(
            blockchainType = BlockchainType.BinanceSmartChain,
            contractAddress = "0x7dc1049211f76324e651fe6584156ab22bd7b47e",
            name = "Bored Collection",
        ),
        BuiltinNftCollection(
            blockchainType = BlockchainType.BinanceSmartChain,
            contractAddress = "0xca635054afce6c12df89b48389e43666447d1733",
            name = "BNB COUPON NFT",
        ),
    )

    private val map = collections.associateBy { it.blockchainType to it.contractAddress.lowercase() }

    fun all(): List<BuiltinNftCollection> = collections

    fun find(blockchainType: BlockchainType, contractAddress: String): BuiltinNftCollection? =
        map[blockchainType to contractAddress.lowercase()]
}
