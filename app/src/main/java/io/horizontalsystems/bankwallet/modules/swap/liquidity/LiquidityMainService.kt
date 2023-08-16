package io.horizontalsystems.bankwallet.modules.swap.liquidity

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LiquidityMainService(
    tokenFrom: Token?,
    private val providers: List<SwapMainModule.ISwapProvider>,
    private val localStorage: ILocalStorage
) {
    var dex: SwapMainModule.Dex = getDex(tokenFrom)
        private set

    private val _providerUpdatedFlow =
        MutableSharedFlow<SwapMainModule.ISwapProvider>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val providerUpdatedFlow = _providerUpdatedFlow.asSharedFlow()

    val availableProviders: List<SwapMainModule.ISwapProvider>
        get() = providers.filter { it.supports(dex.blockchainType) }

    fun setProvider(provider: SwapMainModule.ISwapProvider) {
        if (dex.provider.id != provider.id) {
            dex = SwapMainModule.Dex(dex.blockchain, provider)
            _providerUpdatedFlow.tryEmit(provider)

            localStorage.setLiquidityProviderId(dex.blockchainType, provider.id)
        }
    }

    private fun getDex(tokenFrom: Token?): SwapMainModule.Dex {
        val blockchain = getBlockchainForToken(tokenFrom)
        val provider = getSwapProvider(blockchain.type) ?: throw IllegalStateException("No provider found for ${blockchain.name}")

        return SwapMainModule.Dex(blockchain, provider)
    }

    private fun getSwapProvider(blockchainType: BlockchainType): SwapMainModule.ISwapProvider? {
        val providerId = localStorage.getLiquidityProviderId(blockchainType)
            ?: LiquidityMainModule.PancakeLiquidityProvider.id

        return providers.firstOrNull { it.id == providerId }
    }

    private fun getBlockchainForToken(token: Token?) = when (token?.blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne -> token.blockchain
        null -> Blockchain(BlockchainType.Ethereum, "Ethereum", null) // todo: find better solution
        else -> throw IllegalStateException("Swap not supported for ${token.blockchainType}")
    }


    fun autoSetProvider1Inch(provider: SwapMainModule.ISwapProvider) {
        if (dex.provider.id != provider.id) {
            dex = SwapMainModule.Dex(dex.blockchain, provider)
//            providerObservable.onNext(provider)
        }
    }
}
