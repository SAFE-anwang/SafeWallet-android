package com.example.pancakeswap.service

import com.example.pancakeswap.contract.v3.NonfungiblePositionManagerV3
import com.example.pancakeswap.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

class LiquidityService {
    
    private lateinit var web3j: Web3j
    private lateinit var credentials: Credentials
    
    private val gasProvider = StaticGasProvider(
        BigInteger.valueOf(5_000_000_000),  // 5 Gwei
        BigInteger.valueOf(3_000_000)        // 3,000,000 gas limit
    )
    
    /**
     * 初始化
     */
    suspend fun initialize(privateKey: String, rpcUrl: String) = withContext(Dispatchers.IO) {
        web3j = Web3j.build(HttpService(rpcUrl))
        credentials = Credentials.create(privateKey)
    }
    
    /**
     * 添加 V2 流动性
     */
    /*suspend fun addLiquidityV2(params: LiquidityParams.V2): LiquidityResult = withContext(Dispatchers.IO) {
        try {
            val router = PancakeSwapV2Router(
                routerAddress = params.dexConfig.routerAddress,
                web3j = web3j,
                credentials = credentials,
                gasProvider = gasProvider,
                wrappedNativeAddress = params.dexConfig.chainConfig.wrappedNativeAddress
            )
            router.addLiquidity(params)
        } catch (e: Exception) {
            LiquidityResult(success = false, errorMessage = e.message)
        }
    }*/
    
    /**
     * 添加 V3 流动性
     */
    suspend fun addLiquidityV3(params: LiquidityParams.V3): LiquidityResult = withContext(Dispatchers.IO) {
        try {
            val positionManager = NonfungiblePositionManagerV3(
                positionManagerAddress = params.dexConfig.positionManagerAddress!!,
                web3j = web3j,
                credentials = credentials,
                gasProvider = gasProvider
            )
            positionManager.mint(params)
        } catch (e: Exception) {
            LiquidityResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * 添加 V3 流动性
     */
    suspend fun addLiquidityV3(data: ByteArray, positionManagerAddress: String): LiquidityResult = withContext(Dispatchers.IO) {
        try {
            val positionManager = NonfungiblePositionManagerV3(
                positionManagerAddress = positionManagerAddress,
                web3j = web3j,
                credentials = credentials,
                gasProvider = gasProvider
            )
            positionManager.mint(data.toHexString())
        } catch (e: Exception) {
            LiquidityResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * 关闭连接
     */
    fun shutdown() {
        if (::web3j.isInitialized) {
            web3j.shutdown()
        }
    }
}