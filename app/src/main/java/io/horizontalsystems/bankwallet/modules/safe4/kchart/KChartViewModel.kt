package io.horizontalsystems.bankwallet.modules.safe4.kchart

import androidx.compose.runtime.MutableState
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.CandleEntry
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KChartViewModel(
    val tokenA: Token,
    val tokenB: Token,
) : ViewModelUiState<ChartUiState>() {

    private val kChartService by lazy { KChartService() }

    private val chartData = MutableStateFlow<List<KChartData>>(emptyList())
    val chartDataState = chartData.asStateFlow()

    init {
        kChartService.itemsObservable.subscribeOn(Schedulers.io())
            .subscribe({
                chartData.value = it
            }, {

            })
        viewModelScope.launch {
            getChartData(TimeOption.THIRTY_MINUTES.label)
        }
    }

    override fun createState(): ChartUiState {
        return ChartUiState()
    }

    fun getChartData(interval: String) {
        val addressA = (tokenA.type as TokenType.Eip20).address
        val addressB = (tokenB.type as TokenType.Eip20).address
        kChartService.getKChartData(addressA, addressB, interval)
    }
}


data class ChartUiState(
    val time: String = "30M"
)