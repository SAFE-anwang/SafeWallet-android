package io.horizontalsystems.bankwallet.modules.send.evm

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm
import io.horizontalsystems.bankwallet.core.fiat.FiatServiceSendEvm
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.send.SendAmountAdvancedService
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.PriceImpactViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCRequestChain
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeService
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeViewModel
import io.horizontalsystems.bankwallet.modules.sendevm.AmountInputViewModel
import io.horizontalsystems.bankwallet.modules.sendevm.SendAvailableBalanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode


data class SendEvmData(
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
        class Liquidity(val info: UniswapLiquidityInfo) : AdditionalInfo()

        @Parcelize
        class OneInchSwap(val info: OneInchSwapInfo) : AdditionalInfo()

        @Parcelize
        class WalletConnectRequest(val info: WalletConnectInfo) : AdditionalInfo()

        val sendInfo: SendInfo?
            get() = (this as? Send)?.info

        val uniswapInfo: UniswapInfo?
            get() = (this as? Uniswap)?.info

        val oneInchSwapInfo: OneInchSwapInfo?
            get() = (this as? OneInchSwap)?.info

        val walletConnectInfo: WalletConnectInfo?
            get() = (this as? WalletConnectRequest)?.info
    }

    @Parcelize
    data class SendInfo(
        val nftShortMeta: NftShortMeta? = null
    ) : Parcelable

    @Parcelize
    data class NftShortMeta(
        val nftName: String,
        val previewImageUrl: String?
    ) : Parcelable

    @Parcelize
    data class WalletConnectInfo(
        val dAppName: String?,
        val chain: WCRequestChain?
    ) : Parcelable

    @Parcelize
    data class UniswapInfo(
        val estimatedOut: BigDecimal,
        val estimatedIn: BigDecimal,
        val slippage: String? = null,
        val deadline: String? = null,
        val recipientDomain: String? = null,
        val price: String? = null,
        val priceImpact: PriceImpactViewItem? = null,
        val gasPrice: String? = null,
    ) : Parcelable

    @Parcelize
    data class UniswapLiquidityInfo(
        val estimatedOut: BigDecimal,
        val estimatedIn: BigDecimal,
        val slippage: String? = null,
        val deadline: String? = null,
        val recipientDomain: String? = null,
        val price: String? = null,
        val priceImpact: LiquidityMainModule.PriceImpactViewItem? = null,
        val gasPrice: String? = null,
    ) : Parcelable

    @Parcelize
    data class OneInchSwapInfo(
        val tokenFrom: Token,
        val tokenTo: Token,
        val amountFrom: BigDecimal,
        val estimatedAmountTo: BigDecimal,
        val slippage: BigDecimal,
        val recipient: Address?,
        val price: String? = null
    ) : Parcelable
}

object SendEvmModule {

    const val transactionDataKey = "transactionData"
    const val additionalInfoKey = "additionalInfo"
    const val blockchainTypeKey = "blockchainType"
    const val backButtonKey = "backButton"
    const val sendNavGraphIdKey = "sendNavGraphId_key"
    const val sendEntryPointDestIdKey = "sendEntryPointDestIdKey"
    const val transactionToken = "transactionToken"
    const val backNavGraphIdKey = "backNavGraphId"

    @Parcelize
    data class TransactionDataParcelable(
        val toAddress: String,
        val value: BigInteger,
        val input: ByteArray
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(
            transactionData.to.hex,
            transactionData.value,
            transactionData.input
        )
    }


    class Factory(private val wallet: Wallet, private val predefinedAddress: String?) : ViewModelProvider.Factory {
        val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter) ?: throw IllegalArgumentException("SendEthereumAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                EvmKitWrapperHoldingViewModel::class.java -> {
                    EvmKitWrapperHoldingViewModel(adapter.evmKitWrapper) as T
                }
                SendEvmViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val amountService = SendAmountAdvancedService(
                        adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                        wallet.token,
                        amountValidator
                    )
                    val addressService = SendEvmAddressService(predefinedAddress)
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

                    SendEvmViewModel(
                        wallet,
                        wallet.token,
                        adapter,
                        xRateService,
                        amountService,
                        addressService,
                        coinMaxAllowedDecimals
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }


    class WsafeFactory(private val wallet: Wallet) : ViewModelProvider.Factory {
        private val adapter by lazy { App.adapterManager.getAdapterForWallet(wallet) as ISendEthereumAdapter }
        private val service by lazy { SendWsafeService(wallet.token, adapter) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendWsafeViewModel::class.java -> {
                    SendWsafeViewModel(service, listOf(service)) as T
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
}
