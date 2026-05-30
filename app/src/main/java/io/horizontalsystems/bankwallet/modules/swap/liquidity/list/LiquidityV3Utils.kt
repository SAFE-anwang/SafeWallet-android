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
     * Get the V3 pool address for a token pair + fee tier by calling factory.getPool()
     */
    fun getPool(web3j: Web3j, token0: String, token1: String, fee: BigInteger, chain: Chain): String? {
        val factory = getV3Factory(chain)
        val function = Function(
            "getPool",
            listOf(Address(token0), Address(token1), Uint24(fee)),
            listOf(object : TypeReference<Address>() {})
        )
        return try {
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                    "0x0000000000000000000000000000000000000000",
                    factory,
                    encodedFunction
                ),
                DefaultBlockParameterName.LATEST
            ).send()
            val result = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (result.isNotEmpty()) (result[0] as Address).value else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pool for $token0/$token1 fee=$fee: ${e.message}")
            null
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
     * Calculate token amounts from liquidity and price range using Uniswap V3 math.
     *
     * Formula (all sqrt prices in Q64.96 format):
     *   amount0 = L * 2^96 * (sb - sp) / (sp * sb)   when sp is within [sa, sb]
     *   amount1 = L * (sp - sa) / 2^96
     *
     * Returns raw amounts (before dividing by token decimals).
     */
    fun calculateTokenAmountsFromLiquidity(
        liquidity: BigInteger,
        tickLower: BigInteger,
        tickUpper: BigInteger,
        sqrtPriceX96: BigInteger,
        token0Decimals: Int,
        token1Decimals: Int
    ): Pair<BigDecimal, BigDecimal> {
        val sqrtPLower = tickToSqrtPrice(tickLower)
        val sqrtPUpper = tickToSqrtPrice(tickUpper)
        val sqrtP = sqrtPriceX96

        // Clamp current price to the position's range
        val sqrtPrice = when {
            sqrtP < sqrtPLower -> sqrtPLower
            sqrtP > sqrtPUpper -> sqrtPUpper
            else -> sqrtP
        }

        val Q96 = BigInteger.ONE.shiftLeft(96)

        // --- amount0 = L * Q96 * (sb - sp) / (sp * sb) ---
        val sqrtDiff0 = sqrtPUpper.subtract(sqrtPrice)
        val numerator0 = liquidity.multiply(Q96).multiply(sqrtDiff0)
        val denominator0 = sqrtPrice.multiply(sqrtPUpper)

        val amount0Raw = if (denominator0 > BigInteger.ZERO) numerator0.divide(denominator0) else BigInteger.ZERO
        val amount0 = BigDecimal(amount0Raw).divide(BigDecimal.TEN.pow(token0Decimals), token0Decimals, RoundingMode.DOWN)

        // --- amount1 = L * (sp - sa) / Q96 ---
        val sqrtDiff1 = sqrtPrice.subtract(sqrtPLower)
        val numerator1 = liquidity.multiply(sqrtDiff1)
        val amount1Raw = numerator1.divide(Q96)
        val amount1 = BigDecimal(amount1Raw).divide(BigDecimal.TEN.pow(token1Decimals), token1Decimals, RoundingMode.DOWN)

        return Pair(amount0, amount1)
    }

    /**
     * Convert tick to sqrt price (Q64.96 format) using the Uniswap V3 formula:
     * sqrtPriceX96 = 1.0001^(tick/2) * 2^96
     */
    private fun tickToSqrtPrice(tick: BigInteger): BigInteger {
        val tickVal = tick.toLong()
        val absTick = abs(tickVal)

        // 1.0001^(|tick|/2) using BigDecimal for precision
        val halfTick = absTick / 2
        val isOdd = absTick % 2 != 0L

        // Compute 1.0001^halfTick using BigDecimal with sufficient precision
        val ONE = BigDecimal("1.0001")
        val Q96 = BigDecimal.valueOf(2).pow(96)
        val powerScale = 30 // extra precision to avoid rounding errors

        val halfPrice = ONE.pow(halfTick.toInt())
        // If absTick is odd, multiply by sqrt(1.0001)
        val basePrice = if (isOdd) {
            halfPrice.multiply(BigDecimal("1.00004999875006249609400374031529"), java.math.MathContext(40))
        } else {
            halfPrice
        }

        // Multiply by 2^96
        val sqrtPriceX96 = basePrice.multiply(Q96).setScale(0, RoundingMode.DOWN).toBigInteger()

        return if (tickVal < 0) {
            val Q192 = BigInteger.ONE.shiftLeft(192)
            Q192.divide(sqrtPriceX96)
        } else {
            sqrtPriceX96
        }
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
