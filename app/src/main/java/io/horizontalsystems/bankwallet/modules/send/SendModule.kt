package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.linelock.LineLockSendHandler
import io.horizontalsystems.bankwallet.modules.safe4.linelock.LineLockSendInteractor
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.SendSafeConvertHandler
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.SendSafeConvertInteractor
import io.horizontalsystems.bankwallet.modules.send.safe.SendSafeHandler
import io.horizontalsystems.bankwallet.modules.send.safe.SendSafeInteractor
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.CustomPriorityUnit
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.Single
import java.math.BigDecimal

object SendModule {

    data class AmountData(val primary: AmountInfo, val secondary: AmountInfo?)

    sealed class AmountInfo {
        abstract val approximate: Boolean

        data class CoinValueInfo(val coinValue: CoinValue, override val approximate: Boolean = false) : AmountInfo()
        data class CurrencyValueInfo(val currencyValue: CurrencyValue, override val approximate: Boolean = false) : AmountInfo()

        val value: BigDecimal
            get() = when (this) {
                is CoinValueInfo -> coinValue.value
                is CurrencyValueInfo -> currencyValue.value
            }

        val decimal: Int
            get() = when (this) {
                is CoinValueInfo -> coinValue.decimal
                is CurrencyValueInfo -> currencyValue.currency.decimal
            }

        fun getFormatted(): String {
            val prefix = if (approximate) "~" else ""
            return prefix + when (this) {
                is CoinValueInfo -> coinValue.getFormattedFull()
                is CurrencyValueInfo -> App.numberFormatter.formatFiatFull(
                    currencyValue.value, currencyValue.currency.symbol
                )
            }
        }

        fun getFormattedPlain(): String {
            val prefix = if (approximate) "~" else ""
            return prefix + when (this) {
                is CoinValueInfo -> {
                    App.numberFormatter.formatCoinFull(value, coinValue.coin.code, coinValue.decimal)
                }

                is CurrencyValueInfo -> {
                    App.numberFormatter.formatFiatFull(currencyValue.value, currencyValue.currency.symbol)
                }
            }
        }

    }


    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendView()
            val interactor: ISendInteractor = SendInteractor()
            val router = SendRouter()
            val presenter = SendPresenter(interactor, router)

            val handler: ISendHandler = when (val adapter = App.adapterManager.getAdapterForWallet(wallet)) {
                /*is ISendBitcoinAdapter -> {
                    val bitcoinInteractor = SendBitcoinInteractor(adapter, App.localStorage)
                    val handler = SendBitcoinHandler(bitcoinInteractor, wallet.coinType)

                    bitcoinInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler
                    presenter.hodlerModuleDelegate = handler
                    presenter.customPriorityUnit = CustomPriorityUnit.Satoshi

                    handler
                }
                is ISendDashAdapter -> {
                    val dashInteractor = SendDashInteractor(adapter)
                    val handler = SendDashHandler(dashInteractor)

                    dashInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }*/
                is ISendSafeAdapter -> {
                    val safeInteractor = SendSafeInteractor(adapter)
                    val handler = SendSafeHandler(safeInteractor)

                    safeInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    presenter.hodlerModuleDelegate = handler

                    handler
                }
                /*is ISendBinanceAdapter -> {
                    val binanceInteractor = SendBinanceInteractor(adapter)
                    val handler = SendBinanceHandler(binanceInteractor)

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }
                is ISendZcashAdapter -> {
                    val zcashInteractor = SendZcashInteractor(adapter)
                    val handler = SendZcashHandler(zcashInteractor)

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }*/
                else -> {
                    throw Exception("No adapter found!")
                }
            }

            presenter.view = view
            presenter.handler = handler

            view.delegate = presenter
            handler.delegate = presenter
            interactor.delegate = presenter

            return presenter as T
        }
    }

    class Factory2(
        private val safeAdapter: ISendSafeAdapter,
        private val handler: SendSafeHandler,
        private val safeInteractor: SendSafeInteractor
        ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendView()
            val interactor: ISendInteractor = SendInteractor()
            val router = SendRouter()
            val presenter = SendPresenter(interactor, router)

            val handler: ISendHandler = when (safeAdapter) {
                /*is ISendBitcoinAdapter -> {
                    val bitcoinInteractor = SendBitcoinInteractor(adapter, App.localStorage)
                    val handler = SendBitcoinHandler(bitcoinInteractor, wallet.coinType)

                    bitcoinInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler
                    presenter.hodlerModuleDelegate = handler
                    presenter.customPriorityUnit = CustomPriorityUnit.Satoshi

                    handler
                }
                is ISendDashAdapter -> {
                    val dashInteractor = SendDashInteractor(adapter)
                    val handler = SendDashHandler(dashInteractor)

                    dashInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }*/
                is ISendSafeAdapter -> {
//                    val safeInteractor = SendSafeInteractor(adapter)
//                    val handler = SendSafeHandler(safeInteractor)

                    safeInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    presenter.hodlerModuleDelegate = handler

                    handler
                }
                /*is ISendBinanceAdapter -> {
                    val binanceInteractor = SendBinanceInteractor(adapter)
                    val handler = SendBinanceHandler(binanceInteractor)

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }
                is ISendZcashAdapter -> {
                    val zcashInteractor = SendZcashInteractor(adapter)
                    val handler = SendZcashHandler(zcashInteractor)

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }*/
                else -> {
                    throw Exception("No adapter found!")
                }
            }

            presenter.view = view
            presenter.handler = handler

            view.delegate = presenter
            handler.delegate = presenter
            interactor.delegate = presenter

            return presenter as T
        }
    }

    class SafeConvertFactory(private val wallet: Wallet, private val ethAdapter: ISendEthereumAdapter) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendView()
            val interactor: ISendInteractor = SendInteractor()
            val router = SendRouter()
            val presenter = SendPresenter(interactor, router)

            val handler: ISendHandler = when (val adapter = App.adapterManager.getAdapterForWallet(wallet)) {
                is ISendSafeAdapter -> {
                    val safeInteractor = SendSafeConvertInteractor(adapter)
                    val handler = SendSafeConvertHandler(safeInteractor, ethAdapter)

                    safeInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    presenter.hodlerModuleDelegate = handler

                    handler
                }
                else -> {
                    throw Exception("No adapter found!")
                }
            }

            presenter.view = view
            presenter.handler = handler

            view.delegate = presenter
            handler.delegate = presenter
            interactor.delegate = presenter

            return presenter as T
        }
    }


    class LineLockFactory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendView()
            val interactor: ISendInteractor = SendInteractor()
            val router = SendRouter()
            val presenter = SendPresenter(interactor, router)

            val handler: ISendHandler = when (val adapter = App.adapterManager.getAdapterForWallet(wallet)) {
                is ISendSafeAdapter -> {
                    val safeInteractor = LineLockSendInteractor(adapter)
                    val handler = LineLockSendHandler(safeInteractor)

                    safeInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    presenter.hodlerModuleDelegate = handler

                    handler
                }
                else -> {
                    throw Exception("No adapter found!")
                }
            }

            presenter.view = view
            presenter.handler = handler

            view.delegate = presenter
            handler.delegate = presenter
            interactor.delegate = presenter

            return presenter as T
        }
    }

    interface IRouter {
        fun closeWithSuccess()
    }

    interface IView {
        var delegate: IViewDelegate

        fun loadInputItems(inputs: List<Input>)
        fun setSendButtonEnabled(actionState: SendPresenter.ActionState)
        fun showConfirmation(confirmationViewItems: List<SendConfirmationViewItem>)
        fun showErrorInToast(error: Throwable)
    }

    interface IViewDelegate {
        var view: IView
        val handler: ISendHandler

        fun onViewDidLoad()
        fun onModulesDidLoad()
        fun onProceedClicked()
        fun onSendConfirmed(logger: AppLogger)
        fun onClear()
    }

    interface ISendInteractor {
        var delegate: ISendInteractorDelegate

        fun send(sendSingle: Single<Unit>, logger: AppLogger)
        fun clear()
    }


    interface ISendInteractorDelegate {
        fun sync()
        fun didSend()
        fun didFailToSend(error: Throwable)
    }

    interface ISendHandler {
        var amountModule: SendAmountModule.IAmountModule?
        var addressModule: SendAddressModule.IAddressModule?
        var feeModule: SendFeeModule.IFeeModule?
        var memoModule: SendMemoModule.IMemoModule?
        var hodlerModule: SendHodlerModule.IHodlerModule?

        val inputItems: List<Input>
        var delegate: ISendHandlerDelegate

        fun sync()
        fun onModulesDidLoad()
        fun onClear() {}

        @Throws
        fun confirmationViewItems(): List<SendConfirmationViewItem>
        fun sendSingle(logger: AppLogger): Single<Unit>
    }

    interface ISendHandlerDelegate {
        fun onChange(isValid: Boolean, amountError: Throwable?, addressError: Throwable?)
    }

    sealed class Input {
        object Amount : Input()
        class Address(val editable: Boolean = false) : Input()
        object Fee : Input()
        class Memo(val maxLength: Int, val hidden: Boolean = false) : Input()
        object ProceedButton : Input()
        object Hodler : Input()
    }

    /*enum class InputType {
        COIN, CURRENCY;

        fun reversed(): InputType {
            return if (this == COIN) CURRENCY else COIN
        }
    }*/

    abstract class SendConfirmationViewItem

    data class SendConfirmationAmountViewItem(
        val coinValue: CoinValue,
        val currencyValue: CurrencyValue?,
        val receiver: Address,
        val locked: Boolean = false,
        val wsafeHex: String = ""
    ) : SendConfirmationViewItem()

    data class SendConfirmationFeeViewItem(
        val coinValue: CoinValue,
        val currencyValue: CurrencyValue?,
    ) : SendConfirmationViewItem()

    data class SendConfirmationTotalViewItem(
        val primaryInfo: AmountInfo,
        val secondaryInfo: AmountInfo?
    ) : SendConfirmationViewItem()

    data class SendConfirmationMemoViewItem(val memo: String?) : SendConfirmationViewItem()

    data class SendConfirmationLockTimeViewItem(val lockTimeInterval: LockTimeInterval) : SendConfirmationViewItem()


    interface ISendSafeInteractor {
        fun fetchAvailableBalance(address: String?)
        fun fetchMinimumAmount(address: String?): BigDecimal?
        fun fetchFee(amount: BigDecimal, address: String?)
        fun validate(address: String)
        fun send(amount: BigDecimal, address: String, logger: AppLogger, lockTimeInterval: LockTimeInterval ?, reverseHex: String ?): Single<Unit>
        fun clear()
    }

    interface ISendSafeInteractorDelegate {
        fun didFetchAvailableBalance(availableBalance: BigDecimal)
        fun didFetchFee(fee: BigDecimal)
    }

}


sealed class SendResult {
    object Sending : SendResult()
    object Sent : SendResult()
    class Failed(val caution: HSCaution) : SendResult()
}

object SendErrorFetchFeeRateFailed : HSCaution(
    TranslatableString.ResString(R.string.Send_Error_FetchFeeRateFailed),
    Type.Error
)

object SendWarningLowFee : HSCaution(
    TranslatableString.ResString(R.string.Send_Warning_LowFee),
    Type.Warning,
    TranslatableString.ResString(R.string.Send_Warning_LowFee_Description)
)

class SendErrorInsufficientBalance(coinCode: Any) : HSCaution(
    TranslatableString.ResString(R.string.Swap_ErrorInsufficientBalance),
    Type.Error,
    TranslatableString.ResString(
        R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
        coinCode
    )
)

class SendErrorMinimumSendAmount(amount: Any) : HSCaution(
    TranslatableString.ResString(R.string.Send_Error_MinimumAmount, amount)
)
