package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.RoundingMode

class SendAmountPresenterHelper(
    private val numberFormatter: IAppNumberFormatter,
    private val coin: Token,
    private val baseCurrency: Currency,
    private val coinDecimal: Int,
    private val currencyDecimal: Int) {

    fun getAmount(coinAmount: BigDecimal?, inputType: AmountInputType, rate: BigDecimal?): String {
        val amount = when (inputType) {
            AmountInputType.COIN -> {
                coinAmount?.setScale(coinDecimal, RoundingMode.DOWN)
            }
            AmountInputType.CURRENCY -> {
                rate?.let { coinAmount?.times(it) }?.let {
                    val scale = if (it >= BigDecimal(1000)) 0 else currencyDecimal

                    it.setScale(scale, RoundingMode.DOWN)
                }
            }
        } ?: BigDecimal.ZERO

        return if (amount > BigDecimal.ZERO) amount.stripTrailingZeros().toPlainString() else ""
    }


    fun getHint(coinAmount: BigDecimal? = null, inputType: AmountInputType, rate: BigDecimal?): String? {
        return when (inputType) {
            AmountInputType.CURRENCY -> {
                numberFormatter.formatCoinFull(coinAmount ?: BigDecimal.ZERO, coin.coin.code, 8)
            }
            AmountInputType.COIN -> {
                rate?.let {
                    numberFormatter.formatFiatShort(coinAmount?.times(it) ?: BigDecimal.ZERO, baseCurrency.symbol, 2)
                }
            }
        }
    }

    fun getAvailableBalance(coinAmount: BigDecimal? = null, inputType: AmountInputType, rate: BigDecimal?): String? {
        return when (inputType) {
            AmountInputType.CURRENCY -> {
                rate?.let {
                    numberFormatter.formatFiatShort(coinAmount?.times(it) ?: BigDecimal.ZERO, baseCurrency.symbol, 2)
                }
            }
            AmountInputType.COIN -> {
                numberFormatter.formatCoinFull(coinAmount ?: BigDecimal.ZERO, coin.coin.code,8)
            }
        }
    }

    fun getAmountPrefix(inputType: AmountInputType, rate: BigDecimal?): String? {
        return when {
            inputType == AmountInputType.CURRENCY && rate != null -> baseCurrency.symbol
            else -> null
        }
    }

    fun getCoinAmount(amount: BigDecimal?, inputType: AmountInputType, rate: BigDecimal?): BigDecimal? {
        return when (inputType) {
            AmountInputType.CURRENCY -> rate?.let { amount?.divide(it, coinDecimal, RoundingMode.CEILING) }
            else -> amount
        }
    }

    fun decimal(inputType: AmountInputType) = if (inputType == AmountInputType.COIN) coinDecimal else currencyDecimal

}
