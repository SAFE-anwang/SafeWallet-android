package io.horizontalsystems.bankwallet.modules.safe4.safeprice

data class MarketPrice(
    val address: String,
    val decimals: Int,
    val name: String,
    val symbol: String,
    val price: String,
    val change: String,
    val logoURI: String,
    val usdtReserves: String,
)