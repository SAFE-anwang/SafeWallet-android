package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Transaction
import java.util.concurrent.ConcurrentHashMap

/**
 * 识别 SAFE4 链上 SRC721 的铸造 / 销毁交易。
 *
 * 判定依据是交易 input 的方法选择器，方法 id 在运行时由
 * [ContractMethodHelper.getMethodId] 计算，不硬编码魔数。
 * 同时要求目标合约存在于 `nft/tokens` 索引中，避免与同签名的普通合约调用混淆。
 */
object Safe4NftActionDetector {

    enum class NftAction {
        /** 铸造：mint / adminMint 等 */
        Mint,

        /** 销毁：burn（SRC721Burnable） */
        Burn,
    }

    /** 铸造相关方法签名（SRC721 / ERC721 常见形式） */
    private val mintSignatures = listOf(
        "mint(address,uint256)",
        "mint(address,uint256,uint256)",
        "adminMint(address,uint256)",
        "adminMint(address,uint256,uint256)",
        "mint(uint256)",
        "mint(uint256,uint256)",
        "safeMint(address,uint256)",
        "mintTo(address,uint256)",
    )

    /** 销毁相关方法签名 */
    private val burnSignatures = listOf(
        "burn(uint256)",
        "burn(address,uint256)",
    )

    /** 方法选择器 -> 动作类型，首次使用时计算一次并缓存 */
    private val actionByMethodId: Map<String, NftAction> by lazy {
        val result = ConcurrentHashMap<String, NftAction>()
        mintSignatures.forEach { signature ->
            selector(signature)?.let { result[it] = NftAction.Mint }
        }
        burnSignatures.forEach { signature ->
            selector(signature)?.let { result[it] = NftAction.Burn }
        }
        result
    }

    /** 计算方法选择器，返回形如 "0x40c10f19" 的 hex */
    private fun selector(signature: String): String? = try {
        ContractMethodHelper.getMethodId(signature).toHexString()
    } catch (e: Throwable) {
        null
    }

    /**
     * 判断交易是否为 SAFE4 的 NFT 铸造 / 销毁。
     *
     * 返回 null 表示不是 NFT 动作（含目标合约未收录、input 过短等情况）。
     */
    fun detect(transaction: Transaction): NftAction? {
        val to = transaction.to?.hex ?: return null
        val input = transaction.input?.takeIf { it.size >= 4 } ?: return null

        if (!Safe4NftAssetsService.isNftContract(to)) return null

        return actionByMethodId[input.copyOfRange(0, 4).toHexString()]
    }
}
