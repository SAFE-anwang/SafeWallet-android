package io.horizontalsystems.bankwallet.modules.sendevm

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm
import io.horizontalsystems.bankwallet.core.fiat.FiatServiceSendEvm
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.safe42usdt.Safe42UsdtConvertSendViewModel
import io.horizontalsystems.bankwallet.modules.safe4.safe42usdt.Safe42UsdtConvertService
import io.horizontalsystems.bankwallet.modules.safe4.safe42wsafe.Safe4ConvertSendViewModel
import io.horizontalsystems.bankwallet.modules.safe4.safe42wsafe.Safe4ConvertService
import io.horizontalsystems.bankwallet.modules.safe4.usdt2safe.SendUsdtToSafeService
import io.horizontalsystems.bankwallet.modules.safe4.usdt2safe.SendUsdtToSafeViewModel
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeService
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeViewModel
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

/*data class SendEvmData(
    val transactionData: TransactionData,
    val additionalInfo: AdditionalInfo? = null,
    val warnings: List<Warning> = listOf()
) {
    sealed class AdditionalInfo : Parcelable {
        @Parcelize
        class Send(val info: SendInfo) : AdditionalInfo()

        @Parcelize
        class Uniswap(val info: UniswapInfo) : AdditionalInfo()

        @Parcelize
        class OneInchSwap(val info: OneInchSwapInfo) : AdditionalInfo()

        val sendInfo: SendInfo?
            get() = (this as? Send)?.info

        val uniswapInfo: UniswapInfo?
            get() = (this as? Uniswap)?.info

        val oneInchSwapInfo: OneInchSwapInfo?
            get() = (this as? OneInchSwap)?.info
    }

    @Parcelize
    data class SendInfo(
        val domain: String?
    ) : Parcelable

    @Parcelize
    data class UniswapInfo(
        val estimatedOut: BigDecimal,
        val estimatedIn: BigDecimal,
        val slippage: String? = null,
        val deadline: String? = null,
        val recipientDomain: String? = null,
        val price: String? = null,
        val priceImpact: UniswapModule.PriceImpactViewItem? = null,
        val gasPrice: String? = null,
    ) : Parcelable

    @Parcelize
    data class OneInchSwapInfo(
        val coinFrom: Token,
        val coinTo: Token,
        val amountFrom: BigDecimal,
        val estimatedAmountTo: BigDecimal,
        val slippage: BigDecimal,
        val recipient: Address?
    ) : Parcelable
}*/

object SendEvmModule {

    const val walletKey = "walletKey"
    const val transactionDataKey = "transactionData"
    const val additionalInfoKey = "additionalInfo"

    @Parcelize
    data class TransactionDataParcelable(
        val toAddress: String,
        val value: BigInteger,
        val input: ByteArray,
        val nonce: Long? = null,
        val safe4Swap: Int = 0,
        val isSafeUsdt: Boolean = false,
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(
            transactionData.to.hex,
            transactionData.value,
            transactionData.input,
            safe4Swap = transactionData.safe4Swap,
            isSafeUsdt = transactionData.isSafeUsdt
        )
    }


    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        private val adapter by lazy { App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter }
        private val service by lazy { SendEvmService(wallet.token, adapter) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmViewModel::class.java -> {
                    SendEvmViewModel(service, listOf(service)) as T
                }
                AmountInputViewModel::class.java -> {
                    val switchService = AmountTypeSwitchServiceSendEvm()
                    val fiatService = FiatServiceSendEvm(switchService, App.currencyManager, App.marketKit)
                    switchService.add(fiatService.toggleAvailableObservable)

                    AmountInputViewModel(
                        service,
                        fiatService,
                        switchService,
                        clearables = listOf(service, fiatService, switchService)
                    ) as T
                }
                SendAvailableBalanceViewModel::class.java -> {
                    val coinService = EvmCoinService(wallet.token, App.currencyManager, App.marketKit)
                    SendAvailableBalanceViewModel(service, coinService, listOf(service, coinService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }


    class WsafeFactory(private val wallet: Wallet, private val isSafe4: Boolean) : ViewModelProvider.Factory {
        private val adapter by lazy { App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter }
        private val service by lazy { SendWsafeService(wallet.token, adapter, isSafe4) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendWsafeViewModel::class.java -> {
                    SendWsafeViewModel(service, listOf(service), isSafe4) as T
                }
                AmountInputViewModel::class.java -> {
                    val switchService = AmountTypeSwitchServiceSendEvm()
                    val fiatService = FiatServiceSendEvm(switchService, App.currencyManager, App.marketKit)
                    switchService.add(fiatService.toggleAvailableObservable)
                    AmountInputViewModel(
                        service,
                        fiatService,
                        switchService,
                        clearables = listOf(service, fiatService, switchService)
                    ) as T
                }
                SendAvailableBalanceViewModel::class.java -> {
                    val coinService = EvmCoinService(wallet.token, App.currencyManager, App.marketKit)
                    SendAvailableBalanceViewModel(service, coinService, listOf(service, coinService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }


    class Safe4Factory(private val wallet: Wallet, private val chain: Chain) : ViewModelProvider.Factory {
        private val adapter by lazy {App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter }
        private val service by lazy { Safe4ConvertService(wallet.token, adapter, chain) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                Safe4ConvertSendViewModel::class.java -> {
                    Safe4ConvertSendViewModel(service, listOf(service)) as T
                }
                AmountInputViewModel::class.java -> {
                    val switchService = AmountTypeSwitchServiceSendEvm()
                    val fiatService = FiatServiceSendEvm(switchService, App.currencyManager, App.marketKit)
                    switchService.add(fiatService.toggleAvailableObservable)

                    AmountInputViewModel(
                        service,
                        fiatService,
                        switchService,
                        clearables = listOf(service, fiatService, switchService)
                    ) as T
                }
                SendAvailableBalanceViewModel::class.java -> {
                    val coinService = EvmCoinService(wallet.token, App.currencyManager, App.marketKit)
                    SendAvailableBalanceViewModel(service, coinService, listOf(service, coinService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }


    class Safe4ToUsdtFactory(private val wallet: Wallet, private val chain: Chain) : ViewModelProvider.Factory {
        private val adapter by lazy {App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter }
        private val service by lazy { Safe42UsdtConvertService(wallet.token, adapter, chain) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                Safe42UsdtConvertSendViewModel::class.java -> {
                    Safe42UsdtConvertSendViewModel(service, listOf(service)) as T
                }
                AmountInputViewModel::class.java -> {
                    val switchService = AmountTypeSwitchServiceSendEvm()
                    val fiatService = FiatServiceSendEvm(switchService, App.currencyManager, App.marketKit)
                    switchService.add(fiatService.toggleAvailableObservable)

                    AmountInputViewModel(
                        service,
                        fiatService,
                        switchService,
                        clearables = listOf(service, fiatService, switchService)
                    ) as T
                }
                SendAvailableBalanceViewModel::class.java -> {
                    val coinService = EvmCoinService(wallet.token, App.currencyManager, App.marketKit)
                    SendAvailableBalanceViewModel(service, coinService, listOf(service, coinService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }


    class UsdtToSafeFactory(private val wallet: Wallet, private val chain: Chain) : ViewModelProvider.Factory {
        private val adapter by lazy { App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter }
        private val service by lazy { SendUsdtToSafeService(wallet.token, adapter) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendUsdtToSafeViewModel::class.java -> {
                    SendUsdtToSafeViewModel(service, listOf(service)) as T
                }
                AmountInputViewModel::class.java -> {
                    val switchService = AmountTypeSwitchServiceSendEvm()
                    val fiatService = FiatServiceSendEvm(switchService, App.currencyManager, App.marketKit)
                    switchService.add(fiatService.toggleAvailableObservable)
                    AmountInputViewModel(
                        service,
                        fiatService,
                        switchService,
                        clearables = listOf(service, fiatService, switchService)
                    ) as T
                }
                SendAvailableBalanceViewModel::class.java -> {
                    val coinService = EvmCoinService(wallet.token, App.currencyManager, App.marketKit)
                    SendAvailableBalanceViewModel(service, coinService, listOf(service, coinService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    @Parcelize
    data class Input(
            val safeWallet: Wallet,
            val wSafeWallet: Wallet,
            val isEth: Boolean,
            val isMatic: Boolean,
            val isSafe4: Boolean = false
    ) : Parcelable

    @Parcelize
    data class InputSafe4(
            val safeWallet: Wallet,
            val chain: Chain,
            val isEth: Boolean,
            val isMatic: Boolean
    ) : Parcelable

    @Parcelize
    data class InputSafe4ToUsdt(
        val safeWallet: Wallet,
        val chain: Chain,
        val title: Int
    ) : Parcelable

    @Parcelize
    data class InputUsdtToSafe4(
        val usdtWallet: Wallet,
        val safeWallet: Wallet,
        val chain: Chain,
        val title: Int
    ) : Parcelable
}
