package io.horizontalsystems.bankwallet.modules.safe4.src20.approve

import android.util.Log
import io.horizontalsystems.ethereumkit.models.RpcSource
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.Arrays


class SRC20ApproveService(
    val rpcUrl: String,
    val contractAddress: String
) {
    private val web3j: Web3j by lazy {
        Web3j.build(HttpService(rpcUrl))
    }


    /**
     * 授权代币给某个合约
     */
    @Throws(Exception::class)
    fun approve(
        privateKey: String?,
        spenderAddress: String?,
        amount: BigInteger?,
        address: String
    ): String {
        // 创建凭证
        val credentials: Credentials = Credentials.create(privateKey)


        // 获取nonce
        val ethGetTransactionCount = web3j.ethGetTransactionCount(
            address, DefaultBlockParameterName.PENDING
        ).send()
        val nonce: BigInteger = ethGetTransactionCount.transactionCount


        // 创建授权函数
        val function = Function(
            "approve",
            listOf(
                Address(spenderAddress),
                Uint256(amount)
            ),
            emptyList()
        )


        // 编码函数数据
        val encodedFunction = FunctionEncoder.encode(function)


        // 估算Gas
        val gasLimit: BigInteger = BigInteger.valueOf(100000)
        val gasPrice: BigInteger = web3j.ethGasPrice().send().gasPrice


        // 创建原始交易
        val rawTransaction: RawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            contractAddress,
            encodedFunction
        )


        // 签名交易
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue: String = Numeric.toHexString(signedMessage)


        // 发送交易
        val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()

        if (ethSendTransaction.hasError()) {
            throw RuntimeException("交易失败: " + ethSendTransaction.error.message)
        }

        return ethSendTransaction.transactionHash
    }

    /**
     * 查询授权数量
     */
    @Throws(Exception::class)
    fun allowance(ownerAddress: String, spenderAddress: String): BigInteger {
        val function = Function(
            "allowance",
            listOf(
                Address(ownerAddress),
                Address(spenderAddress)
            ),
            listOf(object : TypeReference<Uint256?>() {})
        )

        val encodedFunction = FunctionEncoder.encode(function)


        // 这里需要调用 eth_call 来查询
        // 具体实现取决于你的 Web3j 版本和调用方式
        val transaction =
            Transaction.createEthCallTransaction(
                ownerAddress, contractAddress, encodedFunction
            )

        val response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send()
        Log.d("scr20approve", "allowance:${response.hasError()}")
        return Numeric.decodeQuantity(response.value)
    }



}