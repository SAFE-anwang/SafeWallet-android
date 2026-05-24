package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import android.util.Log
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Constants
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.MethodID
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Token
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.TokenAmount
import io.horizontalsystems.ethereumkit.models.Chain
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.abs

/**
 * V3 Position data returned from NonfungiblePositionManager.positions(tokenId)
 */
data class V3PositionData(
    val tokenId: BigInteger,
    val token0Address: String,
    val token1Address: String,
    val fee: BigInteger,          // fee tier in hundredths of a basis point (500 = 0.05%)
    val tickLower: BigInteger,
    val tickUpper: BigInteger,
    val liquidity: BigInteger,    // current liquidity in position
    val tokensOwed0: BigInteger,  // unclaimed fees in token0
    val tokensOwed1: BigInteger   // unclaimed fees in token1
)

/**
 * V3 Slot0 data from pool
 */
data class V3Slot0Data(
    val sqrtPriceX96: BigInteger,
    val tick: BigInteger
)

/**
 * Utility class for V3 Position Manager interactions
 */
object LiquidityV3Utils {

    private val TAG = "LiquidityV3Utils"

    /**
     * Get the NonfungiblePositionManager address for a chain
     */
    fun getPositionManager(chain: Chain): String {
        return when (chain) {
            Chain.BinanceSmartChain -> Constants.DEX.PANCAKE_V3_POSITION_MANAGER_ADDRESS
            Chain.Ethereum -> Constants.DEX.UNISWAP_V3_POSITION_MANAGER_ADDRESS
            Chain.SafeFour -> Constants.DEX.SAFESWAP_V3_POSITION_MANAGER_ADDRESS
            else -> Constants.DEX.UNISWAP_V3_POSITION_MANAGER_ADDRESS
        }
    }

    /**
     * Get the V3 Factory address for a chain
     */
    private fun getV3Factory(chain: Chain): String {
        return when (chain) {
            Chain.BinanceSmartChain -> "0x0BFbCF9fa4f9C56B0F40a671Ad40E0805A091865"
            Chain.Ethereum -> "0x1F98431c8aD98523631AE4a59f267346ea31F984"
            Chain.SafeFour -> "0x0BFbCF9fa4f9C56B0F40a671Ad40E0805A091865" // use same as BSC for now
            else -> "0x1F98431c8aD98523631AE4a59f267346ea31F984"
        }
    }

    /**
     * Query the number of positions owned by an address
     */
    fun balanceOf(web3j: Web3j, owner: String, chain: Chain): BigInteger {
        val positionManager = getPositionManager(chain)
        val function = Function(
            "balanceOf",
            listOf(Address(owner)),
            listOf(object : TypeReference<Uint256>() {})
        )
        val encodedFunction = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(owner, positionManager, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send()
        val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
        return if (result.isNotEmpty()) (result[0] as Uint256).value else BigInteger.ZERO
    }

    /**
     * Get tokenId of position at a given index
     */
    fun tokenOfOwnerByIndex(web3j: Web3j, owner: String, index: BigInteger, chain: Chain): BigInteger {
        val positionManager = getPositionManager(chain)
        val function = Function(
            "tokenOfOwnerByIndex",
            listOf(Address(owner), Uint256(index)),
            listOf(object : TypeReference<Uint256>() {})
        )
        val encodedFunction = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(owner, positionManager, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send()
        val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
        return if (result.isNotEmpty()) (result[0] as Uint256).value else BigInteger.ZERO
    }

    /**
     * Query position details. Returns null if position doesn't exist.
     */
    fun positions(web3j: Web3j, tokenId: BigInteger, chain: Chain): V3PositionData? {
        val positionManager = getPositionManager(chain)
        val function = Function(
            "positions",
            listOf(Uint256(tokenId)),
            listOf(
                object : TypeReference<Uint96>() {},       // nonce
                object : TypeReference<Address>() {},       // operator
                object : TypeReference<Address>() {},       // token0
                object : TypeReference<Address>() {},       // token1
                object : TypeReference<Uint24>() {},        // fee
                object : TypeReference<Int24>() {},         // tickLower
                object : TypeReference<Int24>() {},         // tickUpper
                object : TypeReference<Uint128>() {},       // liquidity
                object : TypeReference<Uint256>() {},       // feeGrowthInside0LastX128
                object : TypeReference<Uint256>() {},       // feeGrowthInside1LastX128
                object : TypeReference<Uint128>() {},       // tokensOwed0
                object : TypeReference<Uint128>() {}        // tokensOwed1
            )
        )
        try {
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                    "0x0000000000000000000000000000000000000000",
                    positionManager,
                    encodedFunction
                ),
                DefaultBlockParameterName.LATEST
            ).send()
            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.size < 12) return null
            val liquidity = (result[7] as Uint128).value
            // If liquidity is 0, position might be closed
            if (liquidity == BigInteger.ZERO) return null

            return V3PositionData(
                tokenId = tokenId,
                token0Address = (result[2] as Address).value,
                token1Address = (result[3] as Address).value,
                fee = (result[4] as Uint24).value,
                tickLower = (result[5] as Int24).value,
                tickUpper = (result[6] as Int24).value,
                liquidity = liquidity,
                tokensOwed0 = (result[10] as Uint128).value,
                tokensOwed1 = (result[11] as Uint128).value
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error querying position $tokenId: ${e.message}")
            return null
        }
    }

    /**
     * Query pool's slot0 for sqrtPrice and current tick
     */
    fun slot0(web3j: Web3j, poolAddress: String): V3Slot0Data? {
        val function = Function(
            "slot0",
            emptyList(),
            listOf(
                object : TypeReference<Uint160>() {},  // sqrtPriceX96
                object : TypeReference<Int24>() {},     // tick
                object : TypeReference<Uint16>() {},    // observationIndex
                object : TypeReference<Uint16>() {},    // observationCardinality
                object : TypeReference<Uint16>() {},    // observationCardinalityNext
                object : TypeReference<Uint8>() {},     // feeProtocol
                object : TypeReference<Bool>() {}       // unlocked
            )
        )
        try {
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                    "0x0000000000000000000000000000000000000000",
                    poolAddress,
                    encodedFunction
                ),
                DefaultBlockParameterName.LATEST
            ).send()
            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.size < 2) return null
            return V3Slot0Data(
                sqrtPriceX96 = (result[0] as Uint160).value,
                tick = (result[1] as Int24).value
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error querying slot0 for pool $poolAddress: ${e.message}")
            return null
        }
    }

    /**
     * Convert tick to sqrtPriceX96
     */
    fun tickToSqrtPriceX96(tick: BigInteger): BigInteger {
        // Simplified: price = 1.0001^tick
        // For display purposes we'll use the actual slot0 query instead
        return BigInteger.ZERO
    }

    /**
     * Calculate token amounts from liquidity and price range (simplified V3 math)
     */
    fun calculateTokenAmountsFromLiquidity(
        liquidity: BigInteger,
        tickLower: BigInteger,
        tickUpper: BigInteger,
        sqrtPriceX96: BigInteger,
        token0Decimals: Int,
        token1Decimals: Int
    ): Pair<BigDecimal, BigDecimal> {
        // Calculate sqrt prices at boundaries
        val sqrtPLower = tickToSqrtPrice(tickLower)
        val sqrtPUpper = tickToSqrtPrice(tickUpper)
        val sqrtP = sqrtPriceX96

        // Clamp sqrtP to the range
        val sqrtPrice = when {
            sqrtP < sqrtPLower -> sqrtPLower
            sqrtP > sqrtPUpper -> sqrtPUpper
            else -> sqrtP
        }

        // amount0 = L * (sqrtPUpper - sqrtPrice) / (sqrtPrice * sqrtPUpper) * 10^dec0 / 2^96
        // amount1 = L * (sqrtPrice - sqrtPLower) / 2^96 * 10^dec1

        val Q96 = BigInteger.valueOf(2).pow(96)

        // amount0 calculation with higher precision
        val sqrtDiff0 = sqrtPUpper.subtract(sqrtPrice)
        val numerator0 = liquidity.multiply(sqrtDiff0)
        val denominator0 = sqrtPrice.multiply(sqrtPUpper)
        
        val amount0Scaled = numerator0.multiply(BigInteger.TEN.pow(token0Decimals))
        val amount0WithQ96 = amount0Scaled.divide(denominator0)

        val amount0 = BigDecimal(amount0WithQ96).divide(BigDecimal(Q96), token0Decimals + 2, RoundingMode.DOWN)
            .setScale(token0Decimals, RoundingMode.DOWN)

        // amount1 calculation
        val sqrtDiff1 = sqrtPrice.subtract(sqrtPLower)
        val amount1Raw = liquidity.multiply(sqrtDiff1).multiply(BigInteger.TEN.pow(token1Decimals))
        val amount1 = BigDecimal(amount1Raw).divide(BigDecimal(Q96), token1Decimals + 2, RoundingMode.DOWN)
            .setScale(token1Decimals, RoundingMode.DOWN)

        return Pair(amount0, amount1)
    }

    /**
     * Convert tick to sqrt price (Q64.96 format)
     */
    private fun tickToSqrtPrice(tick: BigInteger): BigInteger {
        val tickVal = tick.toLong()
        val absTick = abs(tickVal)
        
        var sqrtPrice = BigInteger("0")
        
        // Use precomputed values for common tick ranges
        if (absTick and 0x1 != 0L) sqrtPrice = BigInteger("FFFCB933BD6FAD37AA2D162D1A594001", 16)
        // Simplified - for production, use full tick math library
        // For now approximate using 1.0001^tick formula
        val pow1_0001 = powBase1_0001(tickVal)
        sqrtPrice = multiplyShift(pow1_0001, 96)

        return if (tickVal < 0) {
            BigInteger("2").pow(192).divide(sqrtPrice)
        } else {
            sqrtPrice
        }
    }

    private fun powBase1_0001(tickVal: Long): BigInteger {
        // Very simplified: use BigDecimal for now
        val base = BigDecimal("1.0001")
        val result = base.pow(abs(tickVal).toInt())
        // Convert to Q192 format (shift left by 192 binary = scale by 2^192)
        val resultScaled = result.multiply(BigDecimal.valueOf(2).pow(192))
        return resultScaled.toBigInteger()
    }

    private fun multiplyShift(value: BigInteger, shift: Int): BigInteger {
        return value.divide(BigInteger.valueOf(2).pow(192 - shift))
    }

    /**
     * Encode decreaseLiquidity call for V3 position removal
     */
    fun encodeDecreaseLiquidity(
        tokenId: BigInteger,
        liquidity: BigInteger,
        amount0Min: BigInteger,
        amount1Min: BigInteger,
        deadline: BigInteger
    ): String {
        val inputParameters: MutableList<org.web3j.abi.datatypes.Type<*>> = mutableListOf()
        inputParameters.add(Uint256(tokenId))
        inputParameters.add(Uint128(liquidity))
        inputParameters.add(Uint256(amount0Min))
        inputParameters.add(Uint256(amount1Min))
        inputParameters.add(Uint256(deadline))
        return FunctionEncoder.encode(
            MethodID.generate("decreaseLiquidity((uint256,uint128,uint256,uint256,uint256))"),
            inputParameters
        )
    }

    /**
     * Encode collect call for V3 position fee/token collection
     */
    fun encodeCollect(
        tokenId: BigInteger,
        recipient: String,
        amount0Max: BigInteger,
        amount1Max: BigInteger
    ): String {
        val inputParameters: MutableList<org.web3j.abi.datatypes.Type<*>> = mutableListOf()
        inputParameters.add(Uint256(tokenId))
        inputParameters.add(Address(recipient))
        inputParameters.add(Uint128(amount0Max))
        inputParameters.add(Uint128(amount1Max))
        return FunctionEncoder.encode(
            MethodID.generate("collect((uint256,address,uint128,uint128))"),
            inputParameters
        )
    }

}
