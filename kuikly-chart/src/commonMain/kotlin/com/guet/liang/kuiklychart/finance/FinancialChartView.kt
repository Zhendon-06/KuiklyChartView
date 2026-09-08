package com.guet.liang.kuiklychart.finance

import com.tencent.kuikly.core.base.attr.CaptureRule
import com.tencent.kuikly.core.base.attr.CaptureRuleDirection
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.event.TouchParams
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.View
import kotlin.math.roundToInt

/** Price, MA/VWAP, time axis and volume share one viewport and one crosshair. */
public class FinancialChartView : ComposeView<ComposeAttr, ComposeEvent>() {
    private var spec = FinancialChartSpec()
    private var revision by observable(0)
    private var start by observable(0)
    private var count by observable(60)
    private var selected by observable(-1)
    public var onSelectionChanged: ((Int?) -> Unit)? = null
    private var plotWidth = 1f
    private var panStart = 0
    private var panX = 0f
    private var panY = 0f
    private var pinchCount = 60
    private var pinchDistance = 0f
    private var moved = false
    private var verticalGesture = false

    override fun createAttr(): ComposeAttr = ComposeAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    public fun chart(init: FinancialChartSpec.() -> Unit) {
        spec = FinancialChartSpec().apply(init).also { config ->
            config.points = config.points.filter(FinancialChartMath::valid)
        }
        count = spec.visibleCount.coerceIn(10, 240).coerceAtMost(spec.points.size.coerceAtLeast(1))
        start = (spec.points.size - count).coerceAtLeast(0)
        selected = -1
        revision++
    }

    public fun zoom(factor: Float) {
        if (!factor.isFinite() || factor <= 0f || spec.mode == FinancialChartMode.INTRADAY) return
        val end = start + count
        count = (count / factor).roundToInt().coerceIn(10, 240).coerceAtMost(spec.points.size.coerceAtLeast(1))
        start = (end - count).coerceIn(0, (spec.points.size - count).coerceAtLeast(0))
        selected = -1
        onSelectionChanged?.invoke(null)
    }

    public fun resetViewport() {
        count = spec.visibleCount.coerceAtLeast(10).coerceAtMost(spec.points.size.coerceAtLeast(1))
        start = (spec.points.size - count).coerceAtLeast(0)
        selected = -1
        onSelectionChanged?.invoke(null)
    }

    override fun body(): ViewBuilder {
        val owner = this
        return {
            View {
                attr { absolutePositionAllZero(); capture(CaptureRule.pan(CaptureRuleDirection.HORIZONTAL)) }
                event {
                    click { params -> if (!owner.moved) owner.selectAt(params.x) }
                    doubleClick { owner.resetViewport() }
                    touchDown(isSync = true) { params ->
                        owner.panX = params.touches.firstOrNull()?.x ?: params.x
                        owner.panY = params.touches.firstOrNull()?.y ?: params.y
                        owner.panStart = owner.start
                        owner.pinchCount = owner.count
                        owner.pinchDistance = owner.distance(params)
                        owner.moved = false
                        owner.verticalGesture = false
                    }
                    touchMove(isSync = true) { params -> owner.move(params) }
                    touchUp(isSync = true) { owner.pinchDistance = 0f }
                    touchCancel(isSync = true) { owner.pinchDistance = 0f }

                }
            Canvas({ attr { absolutePositionAllZero(); touchEnable(false) } }) { context, width, height ->
                owner.plotWidth = (width - 12f).coerceAtLeast(1f)
                if (owner.revision >= 0) FinancialChartRenderer.draw(
                    context, width, height, owner.spec, owner.start, owner.count, owner.selected,
                )
            }
            }
        }
    }

    private fun distance(params: TouchParams): Float {
        if (params.touches.size < 2) return 0f
        val dx = params.touches[0].x - params.touches[1].x
        val dy = params.touches[0].y - params.touches[1].y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun move(params: TouchParams) {
        val distance = distance(params)
        if (distance > 0f) {
            if (pinchDistance <= 0f) { pinchDistance = distance; pinchCount = count }
            zoom(count.toFloat() / pinchCount * distance / pinchDistance.coerceAtLeast(1f))
            moved = true
            return
        }
        val dx = (params.touches.firstOrNull()?.x ?: params.x) - panX
        val dy = (params.touches.firstOrNull()?.y ?: params.y) - panY
        if (kotlin.math.abs(dx) < 4f && kotlin.math.abs(dy) < 4f) return
        if (!moved) verticalGesture = kotlin.math.abs(dy) > kotlin.math.abs(dx)
        moved = true
        if (verticalGesture || spec.mode == FinancialChartMode.INTRADAY) return
        start = (panStart - (dx / plotWidth * count).roundToInt())
            .coerceIn(0, (spec.points.size - count).coerceAtLeast(0))
        selected = -1
        onSelectionChanged?.invoke(null)
    }

    private fun selectAt(x: Float) {
        if (spec.points.isEmpty()) return
        val ratio = ((x - 6f) / plotWidth).coerceIn(0f, 1f)
        val next = if (spec.mode == FinancialChartMode.INTRADAY) {
            val slot = ratio * spec.sessionSlots
            spec.points.indices.minByOrNull { kotlin.math.abs(spec.points[it].slot - slot) } ?: -1
        } else (start + (ratio * count).toInt()).coerceIn(start, (start + count - 1).coerceAtMost(spec.points.lastIndex))
        selected = if (next == selected) -1 else next
        onSelectionChanged?.invoke(selected.takeIf { it >= 0 })
    }
}

public fun ViewContainer<*, *>.FinancialChart(init: FinancialChartView.() -> Unit) {
    addChild(FinancialChartView(), init)
}
