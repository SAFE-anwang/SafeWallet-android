package io.horizontalsystems.bankwallet.modules.multiswap.providers

object MultiSwapProviderRegistry {
    val allProviders: List<IMultiSwapProvider> = listOf(
        OneInchProvider,
        ThorChainProvider,
        MayaProvider,
        AllBridgeProvider,
        USwapProvider(UProvider.Near),
//        USwapProvider(UProvider.QuickEx),
//        USwapProvider(UProvider.LetsExchange),
//        USwapProvider(UProvider.StealthEx),
//        USwapProvider(UProvider.Swapuz),
        USwapProvider(UProvider.Exolix),
        USwapProvider(UProvider.Cce),
        USwapProvider(UProvider.Barter),
        USwapProvider(UProvider.Circle),
        USwapProvider(UProvider.Pegasus),
        PancakeSwapV3Provider,
        UniswapV3Provider,
        SafeSwapProvider,
    )

    private val providersById: Map<String, IMultiSwapProvider> by lazy {
        allProviders.associateBy { it.id }
    }

    fun isSingleTransactionSwap(providerId: String, tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String): Boolean {
        val provider = providersById[providerId] ?: return false
        return provider.isSingleTransactionSwap(tokenInBlockchainTypeUid, tokenOutBlockchainTypeUid)
    }
}
