package com.example.pancakeswap.contract.common

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.utils.Numeric
import kotlinx.coroutines.delay
import java.math.BigInteger

class ERC20Contract(
    private val contractAddress: String,
    private val web3j: Web3j,
    private val credentials: Credentials,
    private val gasProvider: ContractGasProvider
) {
    
    companion object {
        const val FUNC_APPROVE = "approve"
        const val FUNC_ALLOWANCE = "allowance"
        const val FUNC_BALANCE_OF = "balanceOf"
        const val FUNC_DECIMALS = "decimals"
        const val FUNC_SYMBOL = "symbol"
    }
    
    /**
     * 授权代币
     */
    suspend fun approve(spender: String, amount: BigInteger): String? {
        val function = Function(
            FUNC_APPROVE,
            listOf(
                org.web3j.abi.datatypes.Address(spender),
                Uint256(amount)
            ),
            emptyList()
        )
        
        return sendTransaction(function)
    }
    
    /**
     * 查询授权额度
     */
    suspend fun allowance(owner: String, spender: String): BigInteger {
        val function = Function(
            FUNC_ALLOWANCE,
            listOf(
                org.web3j.abi.datatypes.Address(owner),
                org.web3j.abi.datatypes.Address(spender)
            ),
            listOf(object : org.web3j.abi.TypeReference<Uint256>() {})
        )
        
        val result = callFunction(function)
        return if (result.isNotEmpty()) (result[0] as Uint256).value else BigInteger.ZERO
    }
    
    /**
     * 查询余额
     */
    suspend fun balanceOf(address: String): BigInteger {
        val function = Function(
            FUNC_BALANCE_OF,
            listOf(org.web3j.abi.datatypes.Address(address)),
            listOf(object : org.web3j.abi.TypeReference<Uint256>() {})
        )
        
        val result = callFunction(function)
        return if (result.isNotEmpty()) (result[0] as Uint256).value else BigInteger.ZERO
    }
    
    /**
     * 查询小数位数
     */
    suspend fun decimals(): Int {
        val function = Function(
            FUNC_DECIMALS,
            emptyList(),
            listOf(object : org.web3j.abi.TypeReference<org.web3j.abi.datatypes.generated.Uint8>() {})
        )
        
        val result = callFunction(function)
        return if (result.isNotEmpty()) {
            (result[0] as org.web3j.abi.datatypes.generated.Uint8).value.toInt()
        } else {
            18
        }
    }
    
    private suspend fun sendTransaction(function: Function): String? {
        val encodedFunction = FunctionEncoder.encode(function)
        val nonce = getNonce()
        
        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasProvider.getGasPrice(function.name),
            gasProvider.getGasLimit(function.name),
            contractAddress,
            encodedFunction
        )
        
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        
        val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
        
        return if (ethSendTransaction.hasError()) {
            null
        } else {
            ethSendTransaction.transactionHash
        }
    }
    
    private suspend fun callFunction(function: Function): List<Type<*>> {
        val encodedFunction = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(
                credentials.address,
                contractAddress,
                encodedFunction
            ),
            org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send()
        
        return FunctionReturnDecoder.decode(response.value, function.outputParameters)
    }
    
    private suspend fun getNonce(): BigInteger {
        return web3j.ethGetTransactionCount(
            credentials.address,
            org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send().transactionCount
    }
    
    suspend fun waitForReceipt(txHash: String, maxAttempts: Int = 30): TransactionReceipt? {
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