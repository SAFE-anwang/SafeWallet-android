package io.horizontalsystems.bankwallet.modules.safe4.linelocksafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.bankwallet.modules.balance.BalanceCache
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.balance.token.TokenBalanceService
import io.horizontalsystems.bankwallet.modules.balance.token.TokenBalanceViewModel
import io.horizontalsystems.bankwallet.modules.balance.token.TokenTransactionsService
import io.horizontalsystems.bankwallet.modules.safe4.linelocksafe.lockinfo.LineLockInfoViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeType
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.bankwallet.modules.transactions.NftMetadataService
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordRepository
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSyncStateRepository
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsRateRepository
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigDecimal
import java.math.RoundingMode

object LineLockSafeModule {

    class Factory(val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

            val amountValidator = AmountValidator()
            val coinMaxAllowedDecimals = wallet.token.decimals

            val amountService = SendAmountService(
                amountValidator,
                wallet.token.coin.code,
                adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                wallet.token.type.isNative
            )
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val addressService = SendEvmAddressService(adapter.evmKitWrapper.evmKit.receiveAddress.hex)
            val rpcBlockchainSafe4 = adapter.evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4

            return LineLockSendSafeViewModel(
                wallet,
                amountService,
                addressService,
                adapter.evmKitWrapper.evmKit,
                coinMaxAllowedDecimals,
                xRateService,
                rpcBlockchainSafe4,
                App.connectivityManager
            ) as T
        }
    }


    class LinLockInfoFactory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val tokenTransactionsService = TokenTransactionsService(
                wallet,
                TransactionRecordRepository(App.transactionAdapterManager),
                TransactionsRateRepository(App.currencyManager, App.marketKit),
                TransactionSyncStateRepository(App.transactionAdapterManager),
                App.contactsRepository,
                NftMetadataService(App.nftMetadataManager),
                App.spamManager
            )

            return LineLockInfoViewModel(
                wallet,
                tokenTransactionsService,
                TransactionViewItemFactory(App.evmLabelManager, App.contactsRepository, App.balanceHiddenManager),
            ) as T
        }
    }

    data class LineLockSafeUiState(
        val sendEnable: Boolean,
        val availableBalance: BigDecimal,
        val amountCaution: HSCaution?,
        val fiatMaxAllowedDecimals: Int,
        val lockAmount: String?,
        val startMonth: String?,
        val intervalMonth: String?,
        val tips: String? = null,
        val addressError: Throwable? = null,
    )

    data class LineLockInfoUiState(
        val lockedAmount: String,
        val transactions: List<LineLockInfo>?
    )

    data class  LineLockInfo(
        val value: String,
        val days: Int,
        val address: String
    )
}