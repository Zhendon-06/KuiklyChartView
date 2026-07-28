package com.guet.liang.kuiklychart.internal

import com.guet.liang.kuiklychart.api.ChartSelection
import com.guet.liang.kuiklychart.api.ChartSeriesType
import com.guet.liang.kuiklychart.api.ChartSpec
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

internal object ChartHitTester {
    fun selectionAt(
        spec: ChartSpec,
        geometry: ChartRenderGeometry,
        horizontal: Float,
        vertical: Float,
    ): ChartSelection? {
        return if (geometry.pieSlices.isNotEmpty()) {
            pieSelectionAt(spec, geometry, horizontal, vertical)
        } else {
            cartesianSelectionAt(spec, geometry, horizontal, vertical)
        }
    }

    private fun cartesianSelectionAt(
        spec: ChartSpec,
        geometry: ChartRenderGeometry,
        horizontal: Float,
        vertical: Float,
    ): ChartSelection? {
        if (!geometry.plot.contains(horizontal, vertical) || geometry.categoryWidth <= 0f) {
            return null
        }
        val categoryPosition = geometry.viewport.startIndex +
            (horizontal - geometry.plot.left) / geometry.categoryWidth
        val dataIndex = floor(categoryPosition).toInt().coerceIn(0, (geometry.dataCount - 1).coerceAtLeast(0))
        var selectedSeriesIndex = -1
        var selectedValue = 0f
        var nearestDistance = Float.POSITIVE_INFINITY

        spec.series.forEachIndexed { seriesIndex, chartSeries ->
            if (chartSeries.type == ChartSeriesType.PIE) {
                return@forEachIndexed
            }
            val value = chartSeries.values.getOrNull(dataIndex)
            if (value == null || !value.isFinite()) {
                return@forEachIndexed
            }
            val valueVertical = geometry.scale.verticalPosition(value, geometry.plot)
            val distance = abs(vertical - valueVertical)
            if (distance < nearestDistance) {
                nearestDistance = distance
                selectedSeriesIndex = seriesIndex
                selectedValue = value
            }
        }
        if (selectedSeriesIndex < 0) {
            return null
        }
        val selectedSeries = spec.series[selectedSeriesIndex]
        return ChartSelection(
            selectedSeriesIndex,
            dataIndex,
            selectedSeries.name,
            spec.categoryLabel(dataIndex),
            selectedValue,
            selectedSeries.type,
        )
    }

    private fun pieSelectionAt(
        spec: ChartSpec,
        geometry: ChartRenderGeometry,
        horizontal: Float,
        vertical: Float,
    ): ChartSelection? {
        geometry.pieSlices.forEach { pieSlice ->
            val horizontalDelta = horizontal - pieSlice.centerHorizontal
            val verticalDelta = vertical - pieSlice.centerVertical
            val distance = sqrt(horizontalDelta * horizontalDelta + verticalDelta * verticalDelta)
            if (distance < pieSlice.innerRadius || distance > pieSlice.outerRadius) {
                return@forEach
            }
            val touchAngle = normalizeAngle(atan2(verticalDelta, horizontalDelta))
            val startAngle = normalizeAngle(pieSlice.startAngle)
            val sweep = (pieSlice.endAngle - pieSlice.startAngle)
                .coerceIn(0f, (PI * 2.0).toFloat())
            val angleFromStart = normalizeAngle(touchAngle - startAngle)
            if (angleFromStart <= sweep) {
                val chartSeries = spec.series.getOrNull(pieSlice.seriesIndex) ?: return null
                val value = chartSeries.values.getOrNull(pieSlice.dataIndex) ?: return null
                val label = chartSeries.pointLabels.getOrNull(pieSlice.dataIndex)
                    ?: spec.categoryLabel(pieSlice.dataIndex)
                return ChartSelection(
                    pieSlice.seriesIndex,
                    pieSlice.dataIndex,
                    chartSeries.name,
                    label,
                    value,
                    ChartSeriesType.PIE,
                )
            }
        }
        return null
    }
}

