package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SendFeeInteractor(
    private val baseCurrency: Currency,
    private val marketKit: MarketKitWrapper,
    private val feeRateProvider: IFeeRateProvider?,
    private val platformCoin: Token)
    : SendFeeModule.IInteractor {

    var delegate: SendFeeModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    init {
        if (!platformCoin.isCustom) {
            marketKit.coinPriceObservable(platformCoin.coin.uid, baseCurrency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { marketInfo ->
                    delegate?.didUpdateExchangeRate(marketInfo.value)
                }
                .let {
                    disposables.add(it)
                }
        }
    }

    override val feeRatePriorityList: List<FeeRatePriority> = feeRateProvider?.feeRatePriorityList ?: listOf()

    override val defaultFeeRatePriority: FeeRatePriority? = feeRateProvider?.defaultFeeRatePriority

    override fun getRate(coinUid: String): BigDecimal? {
        return marketKit.coinPrice(coinUid, baseCurrency.code)?.value
    }

    override fun syncFeeRate(feeRatePriority: FeeRatePriority) {
        if (feeRateProvider == null)
            return

        GlobalScope.launch {
            try {
                val feeRate = feeRateProvider.getFeeRate(feeRatePriority)
                withContext(Dispatchers.Main) {
                    delegate?.didUpdate(feeRate.toBigInteger(), feeRatePriority)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    delegate?.didReceiveError(e)
                }
            }
        }
    }

    override fun onClear() {
        disposables.dispose()
    }
}
