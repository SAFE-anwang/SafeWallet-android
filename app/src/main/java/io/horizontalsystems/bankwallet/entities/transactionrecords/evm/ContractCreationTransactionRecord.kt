package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class ContractCreationTransactionRecord(
    // 暴露 transaction：列表页需要用 input 前缀区分 NFT 发行与 SRC20 发行
    val transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    protected: Boolean
) : EvmTransactionRecord(transaction, baseToken, source)
