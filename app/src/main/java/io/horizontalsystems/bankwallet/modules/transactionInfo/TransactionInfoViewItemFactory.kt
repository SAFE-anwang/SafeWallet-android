package io.horizontalsystems.bankwallet.modules.transactionInfo

import com.anwang.utils.Safe4Contract
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App.Companion.numberFormatter
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionRecord
import io.horizontalsystems.bankwallet.core.adapters.TonAdapter.Companion.getAmount
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionRecord
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.TonHelper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.providers.Translator.getString
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.monero.MoneroIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.monero.MoneroOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCreationTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.Safe4DepositEvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.Safe4DepositEvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.SwapTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.UnknownSwapTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronApproveTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.zcash.ZcashShieldingTransactionRecord
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Address
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Amount
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.ContactItem
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.NftAmount
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.SentToSelf
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.SpeedUpCancel
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Transaction
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Value
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionViewItemFactoryHelper.getSwapEventSectionItems
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionViewItemFactoryHelper.zeroAddress
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.ethereumkit.core.Safe4Web3jUtils
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.marketkit.SafeExtend.isSafeFourCustomCoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import kotlin.math.min

class TransactionInfoViewItemFactory(
    private val numberFormatter: IAppNumberFormatter,
    private val translator: Translator,
    private val dateHelper: DateHelper,
    private val evmLabelManager: EvmLabelManager,
    private val resendEnabled: Boolean,
    private val contactsRepo: ContactsRepository,
    private val blockchainType: BlockchainType
) {
    fun getViewItemSections(transactionItem: TransactionInfoItem): List<List<TransactionInfoViewItem>> {
        val transaction = transactionItem.record
        val rates = transactionItem.rates
        val nftMetadata = transactionItem.nftMetadata

        val status = transaction.status(transactionItem.lastBlockInfo?.height)
        val itemSections = mutableListOf<List<TransactionInfoViewItem>>()
        val miscItemsSection = mutableListOf<TransactionInfoViewItem>()

        var sentToSelf = false

        if (transactionItem.record.spam) {
            itemSections.add(listOf(TransactionInfoViewItem.WarningMessage(Translator.getString(R.string.TransactionInfo_SpamWarning))))
        }

        when (transaction) {
            is StellarTransactionRecord -> {
                when (val transactionType = transaction.type) {
                    is StellarTransactionRecord.Type.Receive -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getReceiveSectionItems(
                                value = transactionType.value,
                                fromAddress = transactionType.from,
                                coinPrice = rates[transactionType.value.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                blockchainType = blockchainType,
                            )
                        )

                        if (transactionType.accountCreated) {
                            itemSections.add(
                                listOf(
                                    Value(
                                        Translator.getString(R.string.Transactions_OperationType),
                                        Translator.getString(R.string.Transactions_OperationType_CreateAccount)
                                    )
                                )
                            )
                        }
                    }

                    is StellarTransactionRecord.Type.Send -> {
                        sentToSelf = transactionType.sentToSelf
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getSendSectionItems(
                                value = transactionType.value,
                                toAddress = transactionType.to,
                                coinPrice = rates[transactionType.value.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                sentToSelf = transactionType.sentToSelf,
                                nftMetadata = nftMetadata,
                                blockchainType = blockchainType,
                            )
                        )

                        if (transactionType.accountCreated) {
                            itemSections.add(
                                listOf(
                                    Value(
                                        Translator.getString(R.string.Transactions_OperationType),
                                        Translator.getString(R.string.Transactions_OperationType_CreateAccount)
                                    )
                                )
                            )
                        }
                    }

                    is StellarTransactionRecord.Type.ChangeTrust -> {
                        itemSections.add(
                            listOf(
                                Value(
                                    Translator.getString(R.string.Transactions_OperationType),
                                    Translator.getString(R.string.Transactions_OperationType_ChangeTrust)
                                )
                            )
                        )
                    }

                    is StellarTransactionRecord.Type.Unsupported -> {
                        itemSections.add(
                            listOf(
                                Value(
                                    Translator.getString(R.string.Transactions_OperationType),
                                    transactionType.type
                                )
                            )
                        )
                    }
                }

                addMemoItem(transaction.memo, miscItemsSection)
            }

            is ContractCreationTransactionRecord -> {
                itemSections.add(TransactionViewItemFactoryHelper.getContractCreationItems(transaction))
            }

            is TonTransactionRecord -> {
                transaction.actions.forEach { action ->
                    itemSections.add(
                        TonHelper.getViewItemsForAction(
                            action,
                            rates,
                            blockchainType,
                            transactionItem.hideAmount,
                            true
                        )
                    )
                }

//            feeViewItem = record.fee.map { .fee(title: "tx_info.fee".localized, value: feeString(transactionValue: $0, rate: _rate($0))) }
            }

            is EvmIncomingTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

            is Safe4DepositEvmIncomingTransactionRecord ->
                itemSections.add(
                    getReceiveSectionItemsSafe4(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        input = transaction.transaction.input?.toHexString()
                    )
                )

            is TronIncomingTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

            is EvmOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        nftMetadata = nftMetadata,
                        blockchainType = blockchainType,
                    )
                )
            }

            is Safe4DepositEvmOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    getSendSectionItemsSafe4(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        nftMetadata = nftMetadata,
                        transaction.transaction.input?.toHexString()
                    )
                )
            }

            is TronOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        nftMetadata = nftMetadata,
                        blockchainType = blockchainType,
                    )
                )
            }

            is SwapTransactionRecord -> {
                itemSections.add(
                    getSwapEventSectionItems(
                        valueIn = transaction.valueIn,
                        valueOut = transaction.valueOut,
                        rates = rates,
                        amount = transaction.amountIn,
                        hideAmount = transactionItem.hideAmount,
                        hasRecipient = transaction.recipient != null
                    )
                )

                itemSections.add(
                    TransactionViewItemFactoryHelper.getSwapDetailsSectionItems(
                        rates,
                        transaction.exchangeAddress,
                        transaction.valueOut,
                        transaction.valueIn
                    )
                )
            }

            is UnknownSwapTransactionRecord -> {
                itemSections.add(
                    getSwapEventSectionItems(
                        valueIn = transaction.valueIn,
                        valueOut = transaction.valueOut,
                        amount = null,
                        rates = rates,
                        hideAmount = transactionItem.hideAmount,
                        hasRecipient = false
                    )
                )

                itemSections.add(
                    TransactionViewItemFactoryHelper.getSwapDetailsSectionItems(
                        rates,
                        transaction.exchangeAddress,
                        transaction.valueOut,
                        transaction.valueIn,
                    )
                )
            }

            is ApproveTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getApproveSectionItems(
                        value = transaction.value,
                        coinPrice = rates[transaction.value.coinUid],
                        spenderAddress = transaction.spender,
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

            is TronApproveTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getApproveSectionItems(
                        value = transaction.value,
                        coinPrice = rates[transaction.value.coinUid],
                        spenderAddress = transaction.spender,
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

            is ContractCallTransactionRecord -> {
                itemSections.add(
                    TransactionViewItemFactoryHelper.getContractMethodSectionItems(
                        transaction.method,
                        transaction.contractAddress,
                        transaction.blockchainType
                    )
                )

                for (event in transaction.outgoingEvents) {
                    if (event.address == Safe4Contract.Safe3ContractAddr) continue
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = event.value,
                            toAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (event in transaction.incomingEvents) {
                    if (event.address == Safe4Contract.Safe3ContractAddr) continue
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = event.value,
                            fromAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            is TronContractCallTransactionRecord -> {
                itemSections.add(
                    TransactionViewItemFactoryHelper.getContractMethodSectionItems(
                        transaction.method,
                        transaction.contractAddress,
                        transaction.blockchainType
                    )
                )

                for (event in transaction.outgoingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = event.value,
                            toAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (event in transaction.incomingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = event.value,
                            fromAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            is ExternalContractCallTransactionRecord -> {
                for (event in transaction.outgoingEvents) {
                    if (event.address == Safe4Contract.Safe3ContractAddr) continue
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = event.value,
                            toAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (event in transaction.incomingEvents) {
                    if (event.address == Safe4Contract.Safe3ContractAddr) continue
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = event.value,
                            fromAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            is TronExternalContractCallTransactionRecord -> {
                for (event in transaction.outgoingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = event.value,
                            toAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (event in transaction.incomingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = event.value,
                            fromAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            is TronTransactionRecord -> {
                itemSections.add(
                    listOf(
                        Transaction(
                            transaction.transaction.contract?.label
                                ?: Translator.getString(R.string.Transactions_ContractCall),
                            "",
                            TransactionViewItem.Icon.Platform(transaction.blockchainType).iconRes
                        )
                    )
                )
            }

            is BitcoinIncomingTransactionRecord -> {
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                        toAddress = transaction.to
                    )
                )

                miscItemsSection.addAll(
                    TransactionViewItemFactoryHelper.getBitcoinSectionItems(
                        transaction,
                        transactionItem.lastBlockInfo
                    )
                )
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is BitcoinOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        blockchainType = blockchainType,
                    )
                )

                miscItemsSection.addAll(
                    TransactionViewItemFactoryHelper.getBitcoinSectionItems(
                        transaction,
                        transactionItem.lastBlockInfo
                    )
                )
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is MoneroIncomingTransactionRecord -> {
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                        toAddress = transaction.to
                    )
                )
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is MoneroOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        blockchainType = blockchainType,
                    )
                )
                addMemoItem(transaction.memo, miscItemsSection)
                if (!transaction.txKey.isNullOrEmpty()) {
                    miscItemsSection.add(TransactionInfoViewItem.TransactionSecretKey(transaction.txKey))
                }
            }

            is ZcashShieldingTransactionRecord -> {
                itemSections.add(
                    listOf(
                        Transaction(
                            Translator.getString(transaction.direction.title),
                            "",
                            transaction.direction.icon
                        )
                    )
                )
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = null,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = true,
                        blockchainType = blockchainType,
                    )
                )

                miscItemsSection.addAll(
                    TransactionViewItemFactoryHelper.getBitcoinSectionItems(
                        transaction,
                        transactionItem.lastBlockInfo
                    )
                )
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is SolanaIncomingTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        nftMetadata = nftMetadata,
                        blockchainType = blockchainType,
                    )
                )

            is SolanaOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        nftMetadata = nftMetadata,
                        blockchainType = blockchainType,
                    )
                )
            }

            is SolanaUnknownTransactionRecord -> {
                for (transfer in transaction.outgoingTransfers) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = transfer.value,
                            toAddress = transfer.address,
                            coinPrice = rates[transfer.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (transfer in transaction.incomingTransfers) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = transfer.value,
                            fromAddress = transfer.address,
                            coinPrice = rates[transfer.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            else -> {}
        }

        if (sentToSelf) {
            miscItemsSection.add(SentToSelf)
        }
        if (miscItemsSection.isNotEmpty()) {
            itemSections.add(miscItemsSection)
        }

        itemSections.add(TransactionViewItemFactoryHelper.getStatusSectionItems(transaction, status, rates, blockchainType))

        if (resendEnabled) {
            when (transaction) {
                is EvmTransactionRecord -> {
                    if (!transaction.foreignTransaction && status == TransactionStatus.Pending) {
                        itemSections.add(
                            listOf(
                                SpeedUpCancel(
                                    transactionHash = transaction.transactionHash,
                                    blockchainType = transaction.blockchainType
                                )
                            )
                        )
                        itemSections.add(listOf(TransactionInfoViewItem.Description(Translator.getString(R.string.TransactionInfo_SpeedUpDescription))))
                    }
                }

                is BitcoinOutgoingTransactionRecord -> {
                    if (transaction.replaceable) {
                        itemSections.add(
                            listOf(
                                SpeedUpCancel(
                                    transactionHash = transaction.transactionHash,
                                    blockchainType = transaction.blockchainType
                                )
                            )
                        )
                        itemSections.add(listOf(TransactionInfoViewItem.Description(Translator.getString(R.string.TransactionInfo_SpeedUpDescription))))
                    }
                }
            }
        }
        itemSections.add(TransactionViewItemFactoryHelper.getExplorerSectionItems(transactionItem.explorerData))

        return itemSections
    }

    private fun getContact(address: String?): Contact? {
        return contactsRepo.getContactsFiltered(blockchainType, addressQuery = address).firstOrNull()
    }

    private fun addMemoItem(
        memo: String?,
        miscItemsSection: MutableList<TransactionInfoViewItem>,
    ) {
        if (!memo.isNullOrBlank()) {
            miscItemsSection.add(
                TransactionViewItemFactoryHelper.getMemoItem(memo)
            )
        }
    }


    private fun getReceiveSectionItemsSafe4(
        value: TransactionValue,
        fromAddress: String?,
        coinPrice: CurrencyValue?,
        hideAmount: Boolean,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata> = mapOf(),
        input: String? = null
    ): List<TransactionInfoViewItem> {
        val mint = fromAddress == zeroAddress
        val title: String = if (mint) getString(R.string.Transactions_Mint) else getString(R.string.Transactions_Receive)

        val amount: TransactionInfoViewItem
        val rate: TransactionInfoViewItem?

        when (value) {
            is TransactionValue.NftValue -> {
                amount = getNftAmount(title, value, true, hideAmount, nftMetadata[value.nftUid])
                rate = null
            }

            else -> {
                amount = getAmount(coinPrice, value, true, hideAmount, AmountType.Received)
                rate = getHistoricalRate(coinPrice, value)
            }
        }

        val items: MutableList<TransactionInfoViewItem> = mutableListOf(
            amount
        )

        if (!mint && fromAddress != null) {
            val contact = getContact(fromAddress)
            val realAddress = Safe4Web3jUtils.depositDecode(input) ?: fromAddress
            items.add(
                Address(
                    getString(R.string.TransactionInfo_From),
                    realAddress,
                    contact == null, blockchainType,
                    StatSection.AddressTo,
                    realAddress != Safe4Contract.AccountManagerContractAddr
                )
            )
            contact?.let {
                items.add(
                    ContactItem(it)
                )
            }
        }

        rate?.let { items.add(it) }

        return items
    }

    private fun getSendSectionItemsSafe4(
        value: TransactionValue,
        toAddress: String?,
        coinPrice: CurrencyValue?,
        hideAmount: Boolean,
        sentToSelf: Boolean = false,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata> = mapOf(),
        input: String? = null
    ): List<TransactionInfoViewItem> {
        val burn = toAddress == zeroAddress

        val title: String = if (burn) getString(R.string.Transactions_Burn) else getString(R.string.Transactions_Send)

        val amount: TransactionInfoViewItem
        val rate: TransactionInfoViewItem?

        when (value) {
            is TransactionValue.NftValue -> {
                amount = getNftAmount(title, value, if (sentToSelf) null else false, hideAmount, nftMetadata[value.nftUid])
                rate = null
            }

            else -> {
                amount = getAmount(coinPrice, value, if (sentToSelf) null else false, hideAmount, AmountType.Sent)
                rate = getHistoricalRate(coinPrice, value)
            }
        }

        val items: MutableList<TransactionInfoViewItem> = mutableListOf(amount)

        if (!burn && toAddress != null) {
            val contact = getContact(toAddress)
            val realAddress = Safe4Web3jUtils.depositDecode(input) ?: toAddress
            items.add(
                Address(
                    getString(R.string.TransactionInfo_To),
                    realAddress,
                    contact == null,
                    blockchainType,
                    StatSection.AddressTo,
                    realAddress != Safe4Contract.AccountManagerContractAddr
                )
            )

            contact?.let {
                items.add(ContactItem(it))
            }
        }

        rate?.let { items.add(it) }

        return items
    }

    private fun getNftAmount(
        title: String,
        value: TransactionValue.NftValue,
        incoming: Boolean?,
        hideAmount: Boolean,
        nftMetadata: NftAssetBriefMetadata?
    ): TransactionInfoViewItem {
        val valueFormatted = if (hideAmount) "*****" else value.decimalValue.let { decimalValue ->
            val sign = when {
                incoming == null -> ""
                decimalValue < BigDecimal.ZERO -> "-"
                decimalValue > BigDecimal.ZERO -> "+"
                else -> ""
            }
            val valueWithCoinCode = numberFormatter.formatCoinFull(decimalValue.abs(), value.coinCode, 8)
            "$sign$valueWithCoinCode"
        }

        val nftName = nftMetadata?.name ?: value.tokenName?.let {
            when (value.nftUid) {
                is NftUid.Evm -> "$it #${value.nftUid.tokenId}"
                is NftUid.Solana -> it
            }
        } ?: "#${value.nftUid.tokenId}"

        return NftAmount(
            title,
            ColoredValue(valueFormatted, getAmountColor(incoming)),
            nftName,
            nftMetadata?.previewImageUrl,
            R.drawable.icon_24_nft_placeholder,
            null,
        )
    }

    private fun getHistoricalRate(
        rate: CurrencyValue?,
        transactionValue: TransactionValue,
    ): TransactionInfoViewItem {
        val rateValue = if (rate == null) {
            "---"
        } else {
            val rateFormatted = numberFormatter.formatFiatFull(rate.value, rate.currency.symbol)
            translator.getString(
                R.string.Balance_RatePerCoin,
                rateFormatted,
                transactionValue.coinCode
            )
        }
        return Value(getString(R.string.TransactionInfo_HistoricalRate), rateValue)
    }

    private fun getAmountColor(incoming: Boolean?): ColorName {
        return when (incoming) {
            true -> ColorName.Remus
            false -> ColorName.Grey
            else -> ColorName.Leah
        }
    }

    private fun getString(resId: Int): String {
        return translator.getString(resId)
    }

    private fun getAmount(
        rate: CurrencyValue?,
        value: TransactionValue,
        incoming: Boolean?,
        hideAmount: Boolean,
        amountType: AmountType,
        amount: SwapTransactionRecord.Amount? = null,
        hasRecipient: Boolean = false,
    ): TransactionInfoViewItem {
        val valueInFiat = if (hideAmount) "*****" else rate?.let {
            value.decimalValue?.let { decimalValue ->
                numberFormatter.formatFiatFull(
                    (it.value * decimalValue).abs(),
                    it.currency.symbol
                )
            }
        } ?: "---"
        val fiatValueColored = ColoredValue(valueInFiat, ColorName.Grey)
        val coinValueFormatted = if (hideAmount) "*****" else value.decimalValue?.let { decimalValue ->
            val sign = when (incoming) {
                true -> "+"
                false -> "-"
                else -> ""
            }
            val valueWithCoinCode = numberFormatter.formatCoinFull(decimalValue.abs(), value.coinCode, 8)
            if (amount is SwapTransactionRecord.Amount.Extremum && incoming != null) {
                val suffix = if (incoming) getString(R.string.Swap_AmountMin) else getString(R.string.Swap_AmountMax)
                "$sign$valueWithCoinCode $suffix"
            } else {
                "$sign$valueWithCoinCode"
            }
        } ?: "---"

        val color = if (hasRecipient && incoming == true) {
            ColorName.Leah
        } else {
            getAmountColor(incoming)
        }

        val coinValueColored = ColoredValue(coinValueFormatted, color)
        val coinUid = if (value is TransactionValue.CoinValue && (value.token.coin.uid.isSafeFourCustomCoin() || !value.token.isCustom)) {
            value.token.coin.uid
        } else {
            null
        }
        return Amount(
            coinValueColored,
            fiatValueColored,
            value.coinIconUrl,
            value.alternativeCoinIconUrl,
            value.coinIconPlaceholder,
            coinUid,
            value.badge,
            amountType,
        )
    }

}
