package io.horizontalsystems.bankwallet.core.adapters.nft

import android.util.Log
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.providers.nft.Safe4NftAssetsService
import io.horizontalsystems.bankwallet.entities.nft.EvmNftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftUid
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
import kotlinx.coroutines.withContext
import java.math.BigInteger

/**
 * SAFE4 SRC721 NFT 适配器。
 *
 * 数据来源为 SAFE4 insight 接口（[Safe4NftAssetsService]）：
 * 1. 先取账户持有的 NFT 合约列表（含数量）；
 * 2. 再逐个合约取资产明细（tokenId / tokenValue），转换为 [EvmNftRecord]。
 *
 * 合集名称取 insight `nft/tokens` 接口（见 [Safe4NftAssetsService.fetchCollectionName]），
 * 写入 [EvmNftRecord.tokenName] 供列表页直接展示；
 * 单个 NFT 的名称与图片由 NftMetadataResolver 依据 tokenURI 解析补充。
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
            val records = mutableListOf<NftRecord>()

            // 先加载全网络合约索引，一次请求拿到所有合集的 name / symbol
            withContext(Dispatchers.IO) {
                Safe4NftAssetsService.fetchTokens()
            }

            val contracts = withContext(Dispatchers.IO) {
                Safe4NftAssetsService.fetchAssets(userAddress)
            }.orEmpty()

            contracts.forEach { contract ->
                val contractAddress = contract.token
                if (contractAddress.isBlank()) return@forEach

                val collectionName = Safe4NftAssetsService
                    .fetchCollectionName(contractAddress)

                val assets = withContext(Dispatchers.IO) {
                    Safe4NftAssetsService.fetchAssetsOfToken(userAddress, contractAddress)
                }.orEmpty()

                assets
                    .filter { it.isErc721 }
                    .forEach { asset ->
                        val tokenId = asset.tokenId ?: return@forEach
                        records.add(
                            EvmNftRecord(
                                blockchainType = BlockchainType.SafeFour,
                                nftType = NftType.Eip721,
                                contractAddress = contractAddress,
                                tokenId = tokenId,
                                tokenName = collectionName,
                                balance = asset.balance
                            )
                        )
                    }

                // 明细接口未返回 tokenId（如仅返回数量）时记录日志便于排查
                if (assets.isEmpty() && (contract.count ?: 0) > 0) {
                    Log.w("Safe4NftAdapter", "no asset detail for $contractAddress")
                }
            }

            recordsFlow.value = records.distinctBy { it.nftUid.uid }
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
