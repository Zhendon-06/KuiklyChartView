package com.guet.liang.kuiklychart.internal

import com.guet.liang.kuiklychart.api.ChartSeriesType
import com.guet.liang.kuiklychart.api.ChartSpec
import com.guet.liang.kuiklychart.api.ChartViewport
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal data class ChartPoint(
    val horizontal: Float,
    val vertical: Float,
)

internal data class ChartRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float
        get() = (right - left).coerceAtLeast(0f)

    val height: Float
        get() = (bottom - top).coerceAtLeast(0f)

    fun contains(horizontal: Float, vertical: Float): Boolean {
        return horizontal in left..right && vertical in top..bottom
    }
}

internal data class ValueScale(
    val minimum: Float,
    val maximum: Float,
    val ticks: List<Float>,
) {
    val range: Float
        get() = (maximum - minimum).coerceAtLeast(0.0001f)

    fun verticalPosition(value: Float, plot: ChartRect): Float {
        val ratio = ((value - minimum) / range).coerceIn(0f, 1f)
        return plot.bottom - ratio * plot.height
    }
}

internal data class PieSliceGeometry(
    val seriesIndex: Int,
    val dataIndex: Int,
    val centerHorizontal: Float,
    val centerVertical: Float,
    val outerRadius: Float,
    val innerRadius: Float,
    val startAngle: Float,
    val endAngle: Float,
)

internal data class ChartRenderGeometry(
    val plot: ChartRect,
    val viewport: ChartViewport,
    val scale: ValueScale,
    val dataCount: Int,
    val pieSlices: List<PieSliceGeometry> = emptyList(),
) {
    val categoryWidth: Float
        get() = if (viewport.visiblePointCount <= 0f) 0f else plot.width / viewport.visiblePointCount

    fun categoryCenter(dataIndex: Int): Float {
        return plot.left + (dataIndex - viewport.startIndex + 0.5f) * categoryWidth
    }
}

internal object ViewportMath {
    fun full(dataCount: Int): ChartViewport {
        return ChartViewport(0f, (dataCount - 1).coerceAtLeast(0).toFloat())
    }

    fun initial(dataCount: Int, initialVisiblePoints: Int): ChartViewport {
        if (dataCount <= 0 || initialVisiblePoints <= 0 || initialVisiblePoints >= dataCount) {
            return full(dataCount)
        }
        val endIndex = (dataCount - 1).toFloat()
        return ChartViewport(endIndex - initialVisiblePoints + 1f, endIndex)
    }

    fun normalize(viewport: ChartViewport, dataCount: Int): ChartViewport {
        if (dataCount <= 1) {
            return full(dataCount)
        }
        val maximumIndex = (dataCount - 1).toFloat()
        val requestedSpan = (viewport.endIndex - viewport.startIndex).coerceIn(0f, maximumIndex)
        val normalizedStart = viewport.startIndex.coerceIn(0f, maximumIndex - requestedSpan)
        return ChartViewport(normalizedStart, normalizedStart + requestedSpan)
    }

    fun pan(
        viewport: ChartViewport,
        horizontalDelta: Float,
        plotWidth: Float,
        dataCount: Int,
    ): ChartViewport {
        if (plotWidth <= 0f || dataCount <= 1) {
            return normalize(viewport, dataCount)
        }
        val indexDelta = -horizontalDelta / plotWidth * viewport.visiblePointCount
        return normalize(
            ChartViewport(
                viewport.startIndex + indexDelta,
                viewport.endIndex + indexDelta,
            ),
            dataCount,
        )
    }

    fun zoom(
        viewport: ChartViewport,
        scale: Float,
        focalRatio: Float,
        dataCount: Int,
        minimumVisiblePoints: Int,
    ): ChartViewport {
        if (dataCount <= 1 || scale <= 0f) {
            return normalize(viewport, dataCount)
        }
        val maximumSpan = (dataCount - 1).toFloat()
        val minimumSpan = (minimumVisiblePoints.coerceIn(1, dataCount) - 1).toFloat()
        val currentSpan = viewport.endIndex - viewport.startIndex
        val targetSpan = (currentSpan / scale).coerceIn(minimumSpan, maximumSpan)
        val clampedFocalRatio = focalRatio.coerceIn(0f, 1f)
        val focalIndex = viewport.startIndex + currentSpan * clampedFocalRatio
        val targetStart = focalIndex - targetSpan * clampedFocalRatio
        return normalize(ChartViewport(targetStart, targetStart + targetSpan), dataCount)
    }
}

internal object ScaleMath {
    fun calculate(spec: ChartSpec, viewport: ChartViewport): ValueScale {
        val visibleStart = floor(viewport.startIndex).toInt().coerceAtLeast(0)
        val visibleEnd = ceil(viewport.endIndex).toInt().coerceAtMost((spec.dataCount() - 1).coerceAtLeast(0))
        var rawMinimum = Float.POSITIVE_INFINITY
        var rawMaximum = Float.NEGATIVE_INFINITY
        var containsBar = false

        spec.series.forEach { chartSeries ->
            if (chartSeries.type == ChartSeriesType.PIE) {
                return@forEach
            }
            containsBar = containsBar || chartSeries.type == ChartSeriesType.BAR
            chartSeries.values.forEachIndexed { dataIndex, value ->
                if (dataIndex in visibleStart..visibleEnd && value != null && value.isFinite()) {
                    rawMinimum = min(rawMinimum, value)
                    rawMaximum = max(rawMaximum, value)
                }
            }
        }

        if (!rawMinimum.isFinite() || !rawMaximum.isFinite()) {
            rawMinimum = 0f
            rawMaximum = 1f
        }
        if (containsBar || spec.axes.y.includeZero) {
            rawMinimum = min(rawMinimum, 0f)
            rawMaximum = max(rawMaximum, 0f)
        }
        if (rawMinimum == rawMaximum) {
            val padding = max(abs(rawMinimum) * 0.1f, 1f)
            rawMinimum -= padding
            rawMaximum += padding
        }

        val requestedTicks = spec.axes.y.tickCount.coerceAtLeast(2)
        val rawRange = (rawMaximum - rawMinimum).coerceAtLeast(0.0001f)
        val step = niceNumber(rawRange / (requestedTicks - 1), true)
        var scaleMinimum = spec.axes.y.minimum ?: floor(rawMinimum / step) * step
        var scaleMaximum = spec.axes.y.maximum ?: ceil(rawMaximum / step) * step
        if (scaleMinimum >= scaleMaximum) {
            scaleMaximum = scaleMinimum + step
        }

        val ticks = mutableListOf<Float>()
        var tickValue = scaleMinimum
        val tickLimit = requestedTicks * 4
        while (tickValue <= scaleMaximum + step * 0.25f && ticks.size < tickLimit) {
            ticks.add(normalizeNearZero(tickValue))
            tickValue += step
        }
        if (ticks.isEmpty() || ticks.last() < scaleMaximum - step * 0.25f) {
            ticks.add(scaleMaximum)
        }
        scaleMinimum = min(scaleMinimum, ticks.first())
        scaleMaximum = max(scaleMaximum, ticks.last())
        return ValueScale(scaleMinimum, scaleMaximum, ticks)
    }

    private fun niceNumber(value: Float, roundResult: Boolean): Float {
        if (value <= 0f || !value.isFinite()) {
            return 1f
        }
        val exponent = floor(log10(value.toDouble())).toInt()
        val fraction = value / 10.0.pow(exponent.toDouble()).toFloat()
        val niceFraction = if (roundResult) {
            when {
                fraction < 1.5f -> 1f
                fraction < 3f -> 2f
                fraction < 7f -> 5f
                else -> 10f
            }
        } else {
            when {
                fraction <= 1f -> 1f
                fraction <= 2f -> 2f
                fraction <= 5f -> 5f
                else -> 10f
            }
        }
        return niceFraction * 10.0.pow(exponent.toDouble()).toFloat()
    }

    private fun normalizeNearZero(value: Float): Float {
        return if (abs(value) < 0.000001f) 0f else value
    }
}

internal fun normalizeAngle(angle: Float): Float {
    val fullCircle = (PI * 2.0).toFloat()
    var normalized = angle % fullCircle
    if (normalized < 0f) {
        normalized += fullCircle
    }
    return normalized
}

