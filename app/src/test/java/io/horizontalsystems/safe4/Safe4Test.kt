package io.horizontalsystems.safe4

import com.google.android.exoplayer2.util.Log
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService

class Safe4Test {

    @Test
    fun getBalance() {
        val web3j = Web3j.build(HttpService("http://172.104.162.94:8545"))
        val ethBalance = web3j.ethGetBalance("0x929455e1fe152709A7D71D4257f10E082d092442", DefaultBlockParameterName.LATEST)
                .send()

        val balance = ethBalance.balance
        print("balance=$balance")
    }

    @Test
    fun getTransactionCount() {
        val web3j = Web3j.build(HttpService("http://172.104.162.94:8545"))
        val ethBalance = web3j.ethGetTransactionCount("0x929455e1fe152709A7D71D4257f10E082d092442", DefaultBlockParameterName.LATEST)
                .send()

        val balance = ethBalance.transactionCount
        print("transactionCount=$balance")
    }

    @Test
    fun send() {
        val web3j = Web3j.build(HttpService("http://172.104.162.94:8545"))
        val sendTransaction = web3j.ethSendRawTransaction("0xf86c0c843b9aca008252089426c6d52b9b9758750ba9536f824377f5362fa9918901158e460913d00000801ca0c2731c4aa433e4d6970df51d06e7baa8363ff66ece2c012b7a087674eb8251b8a031f221d6d710222ba2f2af896637e6ead6779af9425fe1af21d752425afab297")
                .send()
        val hash = sendTransaction.transactionHash
        print("hash=$hash, ${if (sendTransaction.hasError()) sendTransaction.error.message else ""}")
    }

}