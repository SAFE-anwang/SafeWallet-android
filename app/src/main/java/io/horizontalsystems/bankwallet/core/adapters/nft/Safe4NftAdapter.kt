package io.horizontalsystems.bankwallet.core.adapters.nft

import android.util.Log
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.nftv2.src721.SRC721Service
import io.horizontalsystems.bankwallet.modules.nftv2.src721.SRC721Storage
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger

/**
 * SAFE4 SRC721 NFT 适配器：
 * 枚举本机注册的 SRC721 合约（SRC721Storage）中当前账户创建（已铸造）的所有 NFT，
 * 未持有也会显示（balance=0），持有的 balance=1。
 * 转账通过标准 safeTransferFrom(address,address,uint256) 编码实现。
 */
class Safe4NftAdapter(
    private val evmKitWrapper: EvmKitWrapper,
) : INftAdapter {

    override val userAddress = evmKitWrapper.evmKit.receiveAddress.hex

    private val recordsFlow = MutableStateFlow<List<NftRecord>>(emptyList())
    override val nftRecordsFlow: Flow<List<NftRecord>> = recordsFlow
    override val nftRecords: List<NftRecord>
        get() = recordsFlow.value

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun nftRecord(nftUid: NftUid): NftRecord? {
        return recordsFlow.value.firstOrNull { it.nftUid == nftUid }
    }

    override fun sync() {
        scope.launch {
            val web3j = (evmKitWrapper.evmKit.blockchain as? RpcBlockchainSafe4)?.web3j
                ?: return@launch
            val records = mutableListOf<NftRecord>()
            val ownedTokenIds = mutableSetOf<String>()
            SRC721Storage.listEnabled(userAddress).forEach { info ->
                try {
                    val service = SRC721Service(web3j, info.address)
                    // 当前账户持有的 NFT
                    val balance = service.balanceOf(userAddress).toInt()
                    for (i in 0 until balance) {
                        try {
                            val tokenId = service.tokenOfOwnerByIndex(
                                userAddress, BigInteger.valueOf(i.toLong())
                            )
                            ownedTokenIds.add(tokenId.toString())
                            records.add(
                                EvmNftRecord(
                                    blockchainType = BlockchainType.SafeFour,
                                    nftType = NftType.Eip721,
                                    contractAddress = info.address,
                                    tokenId = tokenId.toString(),
                                    tokenName = info.name,
                                    balance = 1
                                )
                            )
                        } catch (e: Throwable) {
                            Log.e("Safe4NftAdapter", "tokenOfOwnerByIndex error: $e")
                        }
                    }
                    // 该合约已铸造的全部 NFT（包括未持有的，balance=0）
                    val totalSupply = runCatching { service.totalSupply().toInt() }.getOrNull() ?: 0
                    for (i in 0 until totalSupply) {
                        try {
                            val tokenId = service.tokenByIndex(BigInteger.valueOf(i.toLong()))
                            val tokenIdStr = tokenId.toString()
                            if (tokenIdStr !in ownedTokenIds) {
                                records.add(
                                    EvmNftRecord(
                                        blockchainType = BlockchainType.SafeFour,
                                        nftType = NftType.Eip721,
                                        contractAddress = info.address,
                                        tokenId = tokenIdStr,
                                        tokenName = info.name,
                                        balance = 0
                                    )
                                )
                            }
                        } catch (e: Throwable) {
                            Log.e("Safe4NftAdapter", "tokenByIndex error: $e")
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("Safe4NftAdapter", "sync ${info.address} error: $e")
                }
            }
            recordsFlow.value = records
        }
    }

    override fun transferEip721TransactionData(
        contractAddress: String,
        to: Address,
        tokenId: String
    ): TransactionData? {
        val tokenIdBigInt = tokenId.toBigIntegerOrNull() ?: return null
        val data = ContractMethodHelper.encodedABI(
            ContractMethodHelper.getMethodId("safeTransferFrom(address,address,uint256)"),
            listOf(
                Address(userAddress),
                to,
                tokenIdBigInt
            )
        )
        return TransactionData(Address(contractAddress), BigInteger.ZERO, data)
    }

    override fun transferEip1155TransactionData(
        contractAddress: String,
        to: Address,
        tokenId: String,
        value: BigInteger
    ): TransactionData? = null
}
