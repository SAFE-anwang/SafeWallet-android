package com.example.pancakeswap.data.model

import java.math.BigInteger

/**
 * 链类型
 */
enum class ChainType(val displayName: String) {
    BSC("BSC Mainnet"),
    ETHEREUM("Ethereum Mainnet"),
    BSC_TESTNET("BSC Testnet"),
    ETHEREUM_SEPOLIA("Ethereum Sepolia")
}

/**
 * DEX 版本
 */
enum class DexVersion(val displayName: String) {
    V2("PancakeSwap V2"),
    V3("PancakeSwap V3")
}

/**
 * 手续费等级 (V3)
 */
enum class FeeAmount(val value: BigInteger, val percent: String) {
    LOWEST(BigInteger.valueOf(100), "0.01%"),
    LOW(BigInteger.valueOf(500), "0.05%"),
    MEDIUM(BigInteger.valueOf(2500), "0.25%"),
    HIGH(BigInteger.valueOf(10000), "1%")
}

/**
 * 链配置
 */
data class ChainConfig(
    val chainType: ChainType,
    val chainId: Long,
    val rpcUrl: String,
    val nativeTokenSymbol: String,
    val nativeTokenName: String,
    val wrappedNativeAddress: String
) {
    companion object {
        val BSC_MAINNET = ChainConfig(
            chainType = ChainType.BSC,
            chainId = 56,
            rpcUrl = "https://bsc-dataseed1.binance.org",
            nativeTokenSymbol = "BNB",
            nativeTokenName = "BNB",
            wrappedNativeAddress = "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c"
        )
        
        val ETHEREUM_MAINNET = ChainConfig(
            chainType = ChainType.ETHEREUM,
            chainId = 1,
            rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/YOUR_API_KEY",
            nativeTokenSymbol = "ETH",
            nativeTokenName = "Ethereum",
            wrappedNativeAddress = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
        )
        
        val BSC_TESTNET = ChainConfig(
            chainType = ChainType.BSC_TESTNET,
            chainId = 97,
            rpcUrl = "https://data-seed-prebsc-1-s1.binance.org:8545",
            nativeTokenSymbol = "tBNB",
            nativeTokenName = "Testnet BNB",
            wrappedNativeAddress = "0xae13d989daC2f0dEbFf460aC112a837C89BAa7cd"
        )
        
        val ETHEREUM_SEPOLIA = ChainConfig(
            chainType = ChainType.ETHEREUM_SEPOLIA,
            chainId = 11155111,
            rpcUrl = "https://sepolia.infura.io/v3/YOUR_API_KEY",
            nativeTokenSymbol = "SepoliaETH",
            nativeTokenName = "Sepolia Ethereum",
            wrappedNativeAddress = "0xfFf9976782d46CC05630D1f6eBAb18b2324d6B14"
        )
    }
}

/**
 * DEX 配置
 */
data class DexConfig(
    val version: DexVersion,
    val chainConfig: ChainConfig,
    val routerAddress: String,
    val factoryAddress: String,
    val positionManagerAddress: String? = null  // V3 only
) {
    companion object {
        // PancakeSwap V2 (BSC)
        fun pancakeV2(chainConfig: ChainConfig) = DexConfig(
            version = DexVersion.V2,
            chainConfig = chainConfig,
            routerAddress = "0x10ED43C718714eb63d5aA57B78B54704E256024E",
            factoryAddress = "0xcA143Ce32Fe78f1f7019d7d551a6402fC5350c73",
            positionManagerAddress = null
        )
        
        // PancakeSwap V3 / Uniswap V3
        fun pancakeV3(chainConfig: ChainConfig) = DexConfig(
            version = DexVersion.V3,
            chainConfig = chainConfig,
            routerAddress = "0x1b81D678ffb9C0263b24A97847620C99d213eB14",
            factoryAddress = "0x0BFbCF9fa4f9C56B0F40a671Ad40E0805A091865",
            positionManagerAddress = "0x46A15B0b27311cedF172AB29E4f4766fbE7F4364"
        )
        
        fun uniswapV3(chainConfig: ChainConfig) = DexConfig(
            version = DexVersion.V3,
            chainConfig = chainConfig,
            routerAddress = "0xE592427A0AEce92De3Edee1F18E0157C05861564",
            factoryAddress = "0x1F98431c8aD98523631AE4a59f267346ea31F984",
            positionManagerAddress = "0xC36442b4a4522E871399CD717aBDD847Ab11FE88"
        )
    }
}