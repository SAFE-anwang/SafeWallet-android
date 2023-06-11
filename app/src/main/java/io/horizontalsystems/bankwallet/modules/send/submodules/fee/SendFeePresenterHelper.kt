package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class SendFeePresenterHelper(
        private val numberFormatter: IAppNumberFormatter,
        private val coin: Token,
        private val baseCurrency: Currency) {

    fun feeAmount(coinAmount: BigDecimal? = null, inputType: AmountInputType, rate: BigDecimal?): String? {
        return when (inputType) {
            AmountInputType.COIN -> coinAmount?.let {
                numberFormatter.formatCoinFull(it, coin.coin.code, 8)
            }
            AmountInputType.CURRENCY -> {
                rate?.let { rateValue ->
                    coinAmount?.times(rateValue)?.let { amount ->
                        numberFormatter.formatFiatShort(amount, baseCurrency.symbol, 2)
                    }
                }
            }
        }
    }

}
