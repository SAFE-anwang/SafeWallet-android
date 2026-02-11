package io.horizontalsystems.bankwallet.modules.safe4.kchart

data class KChartData(
    val open: Float,
    val close: Float,
    val high: Float,
    val low: Float,
    val timestamp: Long,
    val volumes: String,
)