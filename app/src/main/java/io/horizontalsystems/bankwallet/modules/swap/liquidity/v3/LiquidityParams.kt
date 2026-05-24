package com.example.pancakeswap.data.model

import io.horizontalsystems.uniswapkit.models.Token
import java.math.BigInteger

/**
 * 添加流动性参数（通用）
 */
sealed class LiquidityParams {
    
    /**
     * V2 参数
     */
    data class V2(
        val dexConfig: DexConfig,
        val tokenA: Token,
        val tokenB: Token,
        val amountADesired: BigInteger,
        val amountBDesired: BigInteger,
        val amountAMin: BigInteger,
        val amountBMin: BigInteger,
        val recipient: String,
        val deadline: BigInteger
    ) : LiquidityParams()
    
    /**
     * V3 参数
     */
    data class V3(
        val dexConfig: DexConfig,
        val tokenA: Token,
        val tokenB: Token,
        val fee: FeeAmount,
        val tickLower: BigInteger,
        val tickUpper: BigInteger,
        val amount0Desired: BigInteger,
        val amount1Desired: BigInteger,
        val amount0Min: BigInteger,
        val amount1Min: BigInteger,
        val recipient: String,
        val deadline: BigInteger
    ) : LiquidityParams()
}

/**
 * 流动性添加结果
 */
data class LiquidityResult(
    val success: Boolean,
    val transactionHash: String? = null,
    val tokenId: BigInteger? = null,  // V3 only
    val liquidity: BigInteger? = null,
    val amount0: BigInteger? = null,
    val amount1: BigInteger? = null,
    val errorMessage: String? = null
)

/**
 * 授权状态
 */
data class ApprovalState(
    val token: Token,
    val spender: String,
    val requiredAmount: BigInteger,
    val currentAllowance: BigInteger,
    val isApproved: Boolean = currentAllowance >= requiredAmount
)

/**
 * 授权请求
 */
data class ApprovalRequest(
    val id: String,
    val token: Token,
    val spender: String,
    val amount: BigInteger,
    val chainConfig: ChainConfig,
    val dexVersion: DexVersion
)