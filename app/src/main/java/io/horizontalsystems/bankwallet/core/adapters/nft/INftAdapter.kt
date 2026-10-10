package io.horizontalsystems.bankwallet.core.adapters.nft

import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger


interface INftAdapter {
    val userAddress: String
    val nftRecordsFlow: Flow<List<NftRecord>>
    val nftRecords: List<NftRecord>
    fun nftRecord(nftUid: NftUid): NftRecord?
    fun sync()

    /**
     * 用「某个合约」的最新资产结果替换适配器内的该合约记录。
     *
     * 适配器实例常驻内存，若页面单独拉取到更新结果（如进入合集详情页时），
     * 不回写适配器的话，已销毁/已转出的资产会作为旧记录保留下来，
     * 下次进入页面时先显示一次、等接口返回才消失。
     * 默认空实现：不影响未使用该机制的适配器。
     */
    fun updateContractRecords(contractAddress: String, records: List<NftRecord>) {}
    fun transferEip721TransactionData(
        contractAddress: String,
        to: Address,
        tokenId: String
    ): TransactionData?

    fun transferEip1155TransactionData(
        contractAddress: String,
        to: Address,
        tokenId: String,
        value: BigInteger
    ): TransactionData?
}