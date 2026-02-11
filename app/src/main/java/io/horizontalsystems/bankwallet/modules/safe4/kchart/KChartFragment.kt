package io.horizontalsystems.bankwallet.modules.safe4.kchart

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.icu.text.SimpleDateFormat
import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.getInputX
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.Locale

class KChartFragment : BaseComposeFragment() {

    private val input by lazy {
        arguments?.getInputX<KChartToken>()!!
    }
    
    val viewModel by lazy {  KChartViewModel(input.tokenA, input.tokenB) }

    @Composable
    override fun GetContent(navController: NavController) {
        ChartScreen(navController, viewModel)
    }
}

@Composable
fun ChartScreen(navController: NavController, viewModel: KChartViewModel) {
    val uiState = viewModel.uiState


    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.KChart),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {

        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it)
                    ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TimeSwitchButtons() {
                    viewModel.getChartData(it.label)
                }
                uiState.data?.let { data ->
                    if (data.isNotEmpty()) {
                        KChartScreen(data)
                    } else {
                        Text(stringResource(R.string.No_Data_KChart),
                            modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSwitchButtons(
    modifier: Modifier = Modifier,
//    selectedTime: TimeOption,
    onTimeSelected: (TimeOption) -> Unit
) {
    var selectedTime by remember { mutableStateOf(TimeOption.THIRTY_MINUTES) }
    // 定义按钮数据
    val timeOptions = listOf(
        TimeOption.THIRTY_MINUTES,
        TimeOption.FOUR_HOURS,
        TimeOption.ONE_DAY
    )

    // 使用Card作为按钮组容器
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .height(44.dp),
        ) {
            timeOptions.forEachIndexed { index, option ->
                val isSelected = option == selectedTime
                val shape = when (index) {
                    0 -> MaterialTheme.shapes.medium.copy(
                        topEnd = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp)
                    )
                    2 -> MaterialTheme.shapes.medium.copy(
                        topStart = CornerSize(0.dp),
                        bottomStart = CornerSize(0.dp)
                    )
                    else -> MaterialTheme.shapes.medium.copy(
                        topStart = CornerSize(0.dp),
                        topEnd = CornerSize(0.dp),
                        bottomStart = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp)
                    )
                }

                TimeButton(
                    text = option.label,
                    isSelected = isSelected,
                    shape = shape,
                    onClick = {
                        selectedTime = option
                        onTimeSelected(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeButton(
    text: String,
    isSelected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .width(90.dp)
            .fillMaxHeight(),
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

enum class TimeOption(val label: String) {
    THIRTY_MINUTES("30M"),
    FOUR_HOURS("4H"),
    ONE_DAY("1D")
}

@Parcelize
data class KChartToken(
    val tokenA: Token,
    val tokenB: Token,
): Parcelable {

}
