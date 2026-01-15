package io.horizontalsystems.bankwallet.modules.safe4.kchart

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.CandleEntry as MPChartCandleEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import io.horizontalsystems.bankwallet.net.SafeApiKeyService
import java.util.Date
import java.util.Locale

@Composable
fun KChartScreen(
    candleData: List<KChartData>
) {

    AndroidView(
        factory = { ctx ->
            Log.d("KChartScreen", "create")
            createCandleStickChart(ctx, candleData)
        },
        update = { chart ->
            Log.d("KChartScreen", "update")
            updateChartData(chart, candleData)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )
}

private fun createCandleStickChart(context: Context, candleData: List<KChartData>): CombinedChart {
    return CombinedChart(context).apply {
        // 基础设置
        description.isEnabled = false
        setDrawGridBackground(false)
        setTouchEnabled(true)
        isDragEnabled = true
        isDragDecelerationEnabled = true
        dragDecelerationFrictionCoef = 0.9f
        setScaleEnabled(true)
        setPinchZoom(true)
        isDoubleTapToZoomEnabled = true

        // X轴设置
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(true)
            gridColor = Color.LTGRAY
            textColor = Color.DKGRAY
            labelRotationAngle = -45f
            valueFormatter = DateValueFormatter(
                candleData.map { it.timestamp }
            )
        }

        // Y轴设置
        // Y轴设置
        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.LTGRAY
            textColor = Color.DKGRAY
            axisMinimum = getMinPrice(candleData)  * 0.98f
            axisMaximum = getMaxPrice(candleData) * 1.02f
            labelCount = 6
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.2f", value)
                }
            }
        }

        axisRight.isEnabled = false

        // 图例
        legend.isEnabled = false
        isHighlightPerDragEnabled = true
        isHighlightPerTapEnabled = true

        // 添加数据
        setData(createCandleDataSet(candleData))

        // 动画
        animateXY(1000, 1000)
        invalidate()
    }
}

private fun updateChartData(chart: CombinedChart, candleData: List<KChartData>) {
    chart.data = createCandleDataSet(candleData)

    // 更新X轴格式化器
    (chart.xAxis.valueFormatter as? DateValueFormatter)?.let {
        // 如果已经设置了格式化器，需要更新数据
        chart.xAxis.valueFormatter = DateValueFormatter(
            candleData.map { it.timestamp }
        )
    } ?: run {
        chart.xAxis.valueFormatter = DateValueFormatter(
            candleData.map { it.timestamp }
        )
    }

    chart.axisLeft.axisMinimum = getMinPrice(candleData) * 0.98f
    chart.axisLeft.axisMaximum = getMaxPrice(candleData) * 1.02f
    chart.xAxis.labelCount = minOf(6, candleData.size)

    chart.data.notifyDataChanged()
    chart.notifyDataSetChanged()

    chart.invalidate()

    resetChartView(chart, candleData)
}

private fun resetChartView(chart: CombinedChart, candleData: List<KChartData>) {
    // 设置可见范围
    chart.setVisibleXRangeMaximum(20f)
    chart.setVisibleXRangeMinimum(5f)

    // 计算Y轴范围
    val minPrice = candleData.minOfOrNull { it.low } ?: 0f
    val maxPrice = candleData.maxOfOrNull { it.high } ?: 100f

    chart.axisLeft.axisMinimum = minPrice * 0.99f
    chart.axisLeft.axisMaximum = maxPrice * 1.01f

    // 移动到最新数据
    chart.moveViewToX(candleData.size.toFloat())
}

private fun createCandleDataSet(candleData: List<KChartData>): CombinedData {
    val entries = mutableListOf<CandleEntry>()
    val maEntries = mutableListOf<Entry>()

    candleData.forEachIndexed { index, candle ->
        entries.add(
            CandleEntry(
                index.toFloat(),
                candle.high,
                candle.low,
                candle.open,
                candle.close
            )
        )
        maEntries.add(
            Entry(index.toFloat(), (candle.high + candle.low) / 2)
        )
    }

    val dataSet = CandleDataSet(entries, "Price").apply {
        // 颜色设置
        color = Color.rgb(80, 80, 80)
        shadowColor = Color.DKGRAY
        shadowWidth = 0.7f
        decreasingColor = Color.RED
        decreasingPaintStyle = Paint.Style.FILL
        increasingColor = Color.GREEN
        increasingPaintStyle = Paint.Style.FILL
        neutralColor = Color.GRAY
        isHighlightEnabled = true

        // 设置蜡烛样式为真正的蜡烛状
        setDrawValues(false)
        shadowColorSameAsCandle = true
        setDrawHorizontalHighlightIndicator(false)
        setDrawVerticalHighlightIndicator(true)
        highLightColor = Color.YELLOW

        // 确保蜡烛有足够的宽度
        setBarSpace(0.3f)
        setShadowWidth(0.8f)
    }

    val maDataSet = LineDataSet(maEntries, "MA5").apply {
        color = Color.GREEN
        lineWidth = 2f
        setDrawCircles(false) // 不显示圆圈点
        setDrawValues(false)  // 不显示数值
        this.enableDashedLine(2f, 2f,1f)
    }

    // 3. 组合数据
    val combinedData = CombinedData()
    combinedData.setData(CandleData(dataSet))
    combinedData.setData(LineData(maDataSet))
    return combinedData
}

private fun getMinPrice(candleData: List<KChartData>): Float {
    return candleData.minOfOrNull { it.low } ?: 0f
}

private fun getMaxPrice(candleData: List<KChartData>): Float {
    return candleData.maxOfOrNull { it.high } ?: 100f
}

// 创建时间格式化器类
class DateValueFormatter(private val timestamps: List<Long>) : ValueFormatter() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault())

    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        return if (index in timestamps.indices) {
            dateFormat.format(Date(timestamps[index]*1000))
        } else {
            ""
        }
    }
}