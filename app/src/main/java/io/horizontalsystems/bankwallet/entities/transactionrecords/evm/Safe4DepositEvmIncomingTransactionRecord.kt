package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class Safe4DepositEvmIncomingTransactionRecord(
    val transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    spamManager: SpamManager,
    val from: String,
    val value: TransactionValue
) : EvmTransactionRecord(
    transaction = transaction,
    baseToken = baseToken,
    source = source,
    foreignTransaction = true,
    spam = spamManager.isIncomingSpam(value)
) {

    override val mainValue = value

}
