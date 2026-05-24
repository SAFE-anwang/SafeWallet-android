package com.example.pancakeswap.contract.v3

import android.util.Log
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.utils.Numeric
import com.example.pancakeswap.data.model.LiquidityParams
import com.example.pancakeswap.data.model.LiquidityResult
import kotlinx.coroutines.delay
import java.math.BigInteger

class NonfungiblePositionManagerV3(
    private val positionManagerAddress: String,
    private val web3j: Web3j,
    private val credentials: Credentials,
    private val gasProvider: ContractGasProvider
) {
    
    companion object {
        const val FUNC_MINT = "mint"
        const val TRANSFER_EVENT_SIGNATURE = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
    }
    
    suspend fun mint(params: LiquidityParams.V3): LiquidityResult {
        return try {
            val function = buildMintFunction(params)
            val encodedFunction = FunctionEncoder.encode(function)
            val txHash = sendTransaction(positionManagerAddress, encodedFunction)
            
            if (txHash == null) {
                return LiquidityResult(success = false, errorMessage = "Failed to send transaction")
            }
            
            val receipt = waitForTransactionReceipt(txHash)
            
            if (receipt == null || !receipt.isStatusOK) {
                return LiquidityResult(
                    success = false,
                    transactionHash = txHash,
                    errorMessage = "Transaction failed"
                )
            }
            
            val tokenId = parseTokenIdFromReceipt(receipt)
            
            LiquidityResult(
                success = true,
                transactionHash = txHash,
                tokenId = tokenId
            )
        } catch (e: Exception) {
            LiquidityResult(success = false, errorMessage = e.message)
        }
    }

    suspend fun mint(data: String): LiquidityResult {
        return try {
            /*val function = buildMintFunction(params)
            val encodedFunction = FunctionEncoder.encode(function)*/
            val txHash = sendTransaction(positionManagerAddress, data)

            if (txHash == null) {
                return LiquidityResult(success = false, errorMessage = "Failed to send transaction")
            }

            val receipt = waitForTransactionReceipt(txHash)

            if (receipt == null || !receipt.isStatusOK) {
                return LiquidityResult(
                    success = false,
                    transactionHash = txHash,
                    errorMessage = "Transaction failed"
                )
            }

            val tokenId = parseTokenIdFromReceipt(receipt)

            LiquidityResult(
                success = true,
                transactionHash = txHash,
                tokenId = tokenId
            )
        } catch (e: Exception) {
            LiquidityResult(success = false, errorMessage = e.message)
        }
    }

    private fun buildMintFunction(params: LiquidityParams.V3): Function {
        val token0 = params.tokenA.address.eip55
        val token1 = params.tokenB.address.eip55

        // 确保 token0 < token1（按地址排序）
        val (actualToken0, actualToken1) = if (token0.lowercase() < token1.lowercase()) {
            Pair(token0, token1)
        } else {
            Pair(token1, token0)
        }
        
        val (actualAmount0, actualAmount1) = if (token0.lowercase() < token1.lowercase()) {
            Pair(params.amount0Desired, params.amount1Desired)
        } else {
            Pair(params.amount1Desired, params.amount0Desired)
        }
        
        val (actualAmount0Min, actualAmount1Min) = if (token0.lowercase() < token1.lowercase()) {
            Pair(params.amount0Min, params.amount1Min)
        } else {
            Pair(params.amount1Min, params.amount0Min)
        }
        
        return Function(
            FUNC_MINT,
            listOf(
                org.web3j.abi.datatypes.Address(actualToken0),
                org.web3j.abi.datatypes.Address(actualToken1),
                Uint256(params.fee.value),
                Uint256(params.tickLower),
                Uint256(params.tickUpper),
                Uint256(actualAmount0),
                Uint256(actualAmount1),
                Uint256(actualAmount0Min),
                Uint256(actualAmount1Min),
                org.web3j.abi.datatypes.Address(params.recipient),
                Uint256(params.deadline)
            ),
            listOf(
                TypeReference.create(Uint256::class.java),
                TypeReference.create(org.web3j.abi.datatypes.generated.Uint128::class.java),
                TypeReference.create(Uint256::class.java),
                TypeReference.create(Uint256::class.java)
            )
        )
    }
    
    private fun parseTokenIdFromReceipt(receipt: TransactionReceipt): BigInteger? {
        receipt.logs.forEach { log ->
            val logObj = log as? org.web3j.protocol.core.methods.response.Log
            if (logObj?.topics?.isNotEmpty() == true && 
                logObj.topics[0] == TRANSFER_EVENT_SIGNATURE &&
                logObj.topics.size >= 4) {
                return BigInteger(logObj.topics[3].substring(2), 16)
            }
        }
        return null
    }
    
    private suspend fun sendTransaction(to: String, data: String): String? {
        val nonce = web3j.ethGetTransactionCount(
            credentials.address,
            org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send().transactionCount
        
        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasProvider.getGasPrice(FUNC_MINT),
            gasProvider.getGasLimit(FUNC_MINT),
            to,
            data
        )
        
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        
        val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
        Log.d("addLiquidity", "errpr=${ethSendTransaction.error?.message}")
        
        return if (ethSendTransaction.hasError()) null else ethSendTransaction.transactionHash
    }
    
    private suspend fun waitForTransactionReceipt(txHash: String, maxAttempts: Int = 60): TransactionReceipt? {
        var attempts = 0
        while (attempts < maxAttempts) {
            val receipt = web3j.ethGetTransactionReceipt(txHash).send()
            if (receipt.transactionReceipt.isPresent) {
                return receipt.transactionReceipt.get()
            }
            delay(2000)
            attempts++
        }
        return null
    }
}