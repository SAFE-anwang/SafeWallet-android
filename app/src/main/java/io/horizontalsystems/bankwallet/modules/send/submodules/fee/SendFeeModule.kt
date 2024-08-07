package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.FeeRateState
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountInfo
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal


object SendFeeModule {

    class InsufficientFeeBalance(val coin: Token, val coinProtocol: String, val feeCoin: Token, val fee: CoinValue) :
            Exception()

    interface IView {
        fun setAdjustableFeeVisible(visible: Boolean)
        fun setPrimaryFee(feeAmount: String?)
        fun setSecondaryFee(feeAmount: String?)
        fun setInsufficientFeeBalanceError(insufficientFeeBalance: InsufficientFeeBalance?)
        fun setFeePriority(priority: FeeRatePriority)
        fun showFeeRatePrioritySelector(feeRates: List<FeeRateInfoViewItem>)
        fun showCustomFeePriority(show: Boolean)
        fun setCustomFeeParams(value: Int, range: IntRange, label: String?)

        fun setLoading(loading: Boolean)
        fun setFee(fee: AmountInfo, convertedFee: AmountInfo?)
        fun setError(error: Exception?)
        fun showLowFeeWarning(show: Boolean)
        fun setLineLockTips(value: String)

    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onChangeFeeRate(feeRatePriority: FeeRatePriority)
        fun onChangeFeeRateValue(value: Int)
        fun onClickFeeRatePriority()
    }

    interface IInteractor {
        val feeRatePriorityList: List<FeeRatePriority>
        val defaultFeeRatePriority: FeeRatePriority?
        fun getRate(coinUid: String): BigDecimal?
        fun syncFeeRate(feeRatePriority: FeeRatePriority)
        fun onClear()
    }

    interface IInteractorDelegate {
        fun didUpdate(feeRate: Int, feeRatePriority: FeeRatePriority)
        fun didReceiveError(error: Exception)
        fun didUpdateExchangeRate(rate: BigDecimal)
    }

    interface IFeeModule {
        val isValid: Boolean
        val feeRateState: FeeRateState
        val feeRate: Long?
        val coinValue: CoinValue
        val currencyValue: CurrencyValue?

        fun setLoading(loading: Boolean)
        fun setFee(fee: BigDecimal)
        fun setError(externalError: Exception?)
        fun setAvailableFeeBalance(availableFeeBalance: BigDecimal)
        fun setInputType(inputType: AmountInputType)
        fun fetchFeeRate()
        fun setBalance(balance: BigDecimal)
        fun setRate(rate: BigDecimal?)
        fun setAmountInfo(sendAmountInfo: SendAmountInfo)
        fun setLineLockTips(value: String)
    }

    interface IFeeModuleDelegate {
        fun onUpdateFeeRate()
    }

    data class FeeRateInfoViewItem(val feeRatePriority: FeeRatePriority, val selected: Boolean)


    class Factory(
            private val token: Token,
            private val sendHandler: SendModule.ISendHandler,
            private val feeModuleDelegate: IFeeModuleDelegate,
            private val customPriorityUnit: CustomPriorityUnit?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendFeeView()
            val feeRateProvider = FeeRateProviderFactory.provider(token.blockchainType)
            val feeCoinData = App.feeCoinProvider.feeTokenData(token)
            val feeCoin = feeCoinData?.first ?: token

            val baseCurrency = App.currencyManager.baseCurrency
            val helper = SendFeePresenterHelper(App.numberFormatter, feeCoin, baseCurrency)
            val interactor = SendFeeInteractor(baseCurrency, App.marketKit, feeRateProvider, feeCoin)

            val presenter = SendFeePresenter(view, interactor, helper, token, baseCurrency, feeCoinData, customPriorityUnit, FeeRateAdjustmentHelper(App.appConfigProvider))

            presenter.moduleDelegate = feeModuleDelegate
            interactor.delegate = presenter
            sendHandler.feeModule = presenter

            return presenter as T
        }
    }

}

class FeeRateAdjustmentInfo(
    var amountInfo: SendAmountInfo,
    var xRate: BigDecimal?,
    val currency: Currency,
    var balance: BigDecimal?
)
