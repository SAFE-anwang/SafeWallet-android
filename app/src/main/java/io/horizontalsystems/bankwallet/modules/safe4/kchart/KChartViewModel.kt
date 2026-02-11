package io.horizontalsystems.bankwallet.modules.safe4.kchart

import android.util.Log
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
import kotlin.random.Random

class KChartViewModel(
    val tokenA: Token,
    val tokenB: Token,
) : ViewModelUiState<ChartUiState>() {

    private val kChartService by lazy { KChartService() }

    private var chartData: List<KChartData>? = null

    init {
        kChartService.itemsObservable.subscribeOn(Schedulers.io())
            .subscribe({
                chartData = it
                emitState()
            }, {

            })

        getChartData(TimeOption.THIRTY_MINUTES.label)
    }

    override fun createState(): ChartUiState {
        return ChartUiState(chartData)
    }

    fun getChartData(interval: String) {
        val addressA = (tokenA.type as TokenType.Eip20).address
        val addressB = (tokenB.type as TokenType.Eip20).address
        viewModelScope.launch {
            kChartService.getKChartData(addressA, addressB, interval)
        }
    }
}


data class ChartUiState(
    val data: List<KChartData>? = null
)