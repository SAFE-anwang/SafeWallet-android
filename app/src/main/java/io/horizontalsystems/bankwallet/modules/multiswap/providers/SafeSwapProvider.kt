package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.marketkit.models.BlockchainType

object SafeSwapProvider : BaseUniswapProvider() {
    override val id = "safe"
    override val title = "SafeSwap"
    override val url = "https://safecoreswap.com/"
    override val icon = R.drawable.ic_safe_20

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.Ethereum || blockchainType == BlockchainType.BinanceSmartChain
    }
}
