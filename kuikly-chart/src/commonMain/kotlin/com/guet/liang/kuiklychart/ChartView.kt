package com.guet.liang.kuiklychart

import com.guet.liang.kuiklychart.api.ChartSelection
import com.guet.liang.kuiklychart.api.ChartSeriesType
import com.guet.liang.kuiklychart.api.ChartSpec
import com.guet.liang.kuiklychart.api.ChartViewport
import com.guet.liang.kuiklychart.internal.ChartHitTester
import com.guet.liang.kuiklychart.internal.ChartRenderGeometry
import com.guet.liang.kuiklychart.internal.ChartRenderer
import com.guet.liang.kuiklychart.internal.ViewportMath
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.CaptureRule
import com.tencent.kuikly.core.base.attr.CaptureRuleDirection
import com.tencent.kuikly.core.base.event.EventName
import com.tencent.kuikly.core.base.event.PanGestureParams
import com.tencent.kuikly.core.base.event.TouchParams
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.View
import kotlin.math.sqrt

/** Event callbacks emitted by every chart convenience component. */
public class ChartEvent : ComposeEvent() {
    /** Called for both selection and selection clearing. */
    public fun selectionChanged(handler: (ChartSelection?) -> Unit) {
        registerEvent(SELECTION_CHANGED) { value -> handler(value as? ChartSelection) }
    }

    /** Called only after a point, bar, or pie slice is selected. */
    public fun pointSelected(handler: (ChartSelection) -> Unit) {
        registerEvent(POINT_SELECTED) { value ->
            (value as? ChartSelection)?.let(handler)
        }
    }

    /** Called whenever a gesture or public method changes the visible range. */
    public fun viewportChanged(handler: (ChartViewport) -> Unit) {
        registerEvent(VIEWPORT_CHANGED) { value ->
            (value as? ChartViewport)?.let(handler)
        }
    }

    internal companion object {
        const val SELECTION_CHANGED = "chartSelectionChanged"
        const val POINT_SELECTED = "chartPointSelected"
        const val VIEWPORT_CHANGED = "chartViewportChanged"
    }
}

/**
 * Kuikly Canvas chart component shared by line, bar, area, pie, and mixed charts.
 * Configure it with [chart], style it with the regular Kuikly `attr` block, and
 * observe interactions with the `event` block.
 */
public class ChartView internal constructor(
    defaultSeriesType: ChartSeriesType,
) : ComposeView<ComposeAttr, ChartEvent>() {
    public val spec: ChartSpec = ChartSpec(defaultSeriesType)

    private var renderRevision by observable(0)
    private var viewportState by observable(ChartViewport(0f, 0f))
    private var selectionState by observable<ChartSelection?>(null)
    private var lastRenderGeometry: ChartRenderGeometry? = null
    private var configuredDataCount: Int = 0

    private var gestureMode: GestureMode = GestureMode.NONE
    private var gestureStartHorizontal: Float = 0f
    private var gestureStartViewport: ChartViewport = ChartViewport(0f, 0f)
    private var pinchStartDistance: Float = 0f
    private var gestureMoved: Boolean = false
    private var suppressNextClick: Boolean = false

    /** Current selected datum, or `null` when no tooltip is visible. */
    public val currentSelection: ChartSelection?
        get() = selectionState

    /** Current visible range in category-index coordinates. */
    public val currentViewport: ChartViewport
        get() = viewportState

    override fun createAttr(): ComposeAttr = ComposeAttr()

    override fun createEvent(): ChartEvent = ChartEvent()

    /** Applies chart data and grouped visual/interaction configuration. */
    public fun chart(init: ChartSpec.() -> Unit) {
        val previousDataCount = spec.dataCount()
        spec.apply(init)
        configuredDataCount = spec.dataCount()
        if (previousDataCount <= 1 && configuredDataCount > 1 && lastRenderGeometry != null) {
            setViewportInternal(ViewportMath.full(configuredDataCount), emitEvent = false)
        } else {
            setViewportInternal(
                ViewportMath.normalize(viewportState, configuredDataCount),
                emitEvent = false,
            )
        }
        renderRevision += 1
    }

    /** Alias for [chart], intended for runtime data replacement through a view ref. */
    public fun update(init: ChartSpec.() -> Unit) {
        chart(init)
    }

    /** Shows the full data range and clears the active selection. */
    public fun resetViewport() {
        setViewportInternal(ViewportMath.full(spec.dataCount()), emitEvent = true)
        clearSelection()
    }

    /** Sets a visible category range. Fractional indices are supported. */
    public fun setViewport(startIndex: Float, endIndex: Float) {
        setViewportInternal(ChartViewport(startIndex, endIndex), emitEvent = true)
    }

    /** Zooms in around the current viewport center. */
    public fun zoomIn(factor: Float = 1.5f) {
        zoomBy(factor)
    }

    /** Zooms out around the current viewport center. */
    public fun zoomOut(factor: Float = 1.5f) {
        if (factor > 0f) {
            zoomBy(1f / factor)
        }
    }

    /** Pans by a number of category slots; positive values move forward. */
    public fun panBy(categoryCount: Float) {
        val requestedViewport = ChartViewport(
            viewportState.startIndex + categoryCount,
            viewportState.endIndex + categoryCount,
        )
        setViewportInternal(requestedViewport, emitEvent = true)
    }

    /** Selects a datum programmatically. */
    public fun select(seriesIndex: Int, dataIndex: Int) {
        val chartSeries = spec.series.getOrNull(seriesIndex) ?: return
        val value = chartSeries.values.getOrNull(dataIndex) ?: return
        setSelection(
            ChartSelection(
                seriesIndex,
                dataIndex,
                chartSeries.name,
                chartSeries.pointLabels.getOrNull(dataIndex) ?: spec.categoryLabel(dataIndex),
                value,
                chartSeries.type,
            ),
        )
    }

    /** Clears tooltip, crosshair, and selected-slice state. */
    public fun clearSelection() {
        setSelection(null)
    }

    override fun created() {
        super.created()
        configuredDataCount = spec.dataCount()
        viewportState = ViewportMath.initial(
            configuredDataCount,
            spec.interaction.initialVisiblePoints,
        )
    }

    override fun body(): ViewBuilder {
        val chartView = this
        return {
            Canvas({
                attr {
                    absolutePositionAllZero()
                    touchEnable(false)
                }
            }) { canvasContext, width, height ->
                val currentRevision = chartView.renderRevision
                if (currentRevision >= 0) {
                    chartView.lastRenderGeometry = ChartRenderer.render(
                        canvasContext,
                        width,
                        height,
                        chartView.spec,
                        chartView.viewportState,
                        chartView.selectionState,
                    )
                }
            }
            View {
                attr {
                    absolutePositionAllZero()
                    backgroundColor(Color.TRANSPARENT)
                    capture(
                        CaptureRule.click(),
                        CaptureRule.doubleClick(),
                        CaptureRule.pan(CaptureRuleDirection.HORIZONTAL),
                    )
                }
                event {
                    click { clickParams ->
                        chartView.handleClick(clickParams.x, clickParams.y)
                    }
                    doubleClick {
                        if (chartView.spec.interaction.resetViewportOnDoubleClick) {
                            chartView.resetViewport()
                        }
                    }
                    register(
                        EventName.PAN.value,
                        { rawParams ->
                            chartView.handlePan(PanGestureParams.decode(rawParams))
                        },
                        isSync = true,
                    )
                    touchDown(isSync = true) { touchParams ->
                        chartView.handleTouchDown(touchParams)
                    }
                    touchMove(isSync = true) { touchParams ->
                        chartView.handleTouchMove(touchParams)
                    }
                    touchUp(isSync = true) {
                        if (chartView.gestureMode == GestureMode.PINCH) {
                            chartView.finishGesture()
                        }
                    }
                    touchCancel(isSync = true) {
                        chartView.finishGesture()
                    }
                }
            }
        }
    }

    private fun handleClick(horizontal: Float, vertical: Float) {
        if (suppressNextClick) {
            suppressNextClick = false
            return
        }
        if (!spec.interaction.selectionEnabled) {
            return
        }
        val geometry = lastRenderGeometry ?: return
        val selection = ChartHitTester.selectionAt(spec, geometry, horizontal, vertical)
        if (selection != null || spec.interaction.dismissSelectionOnOutsideTap) {
            setSelection(selection)
        }
    }

    private fun handleTouchDown(touchParams: TouchParams) {
        val touchPoints = touchPoints(touchParams)
        if (spec.interaction.zoomEnabled && touchPoints.size >= 2) {
            gestureMoved = false
            gestureStartViewport = viewportState
            gestureMode = GestureMode.PINCH
            pinchStartDistance = distance(touchPoints[0], touchPoints[1]).coerceAtLeast(1f)
        }
    }

    private fun handleTouchMove(touchParams: TouchParams) {
        val touchPoints = touchPoints(touchParams)
        if (spec.interaction.zoomEnabled && touchPoints.size >= 2) {
            if (gestureMode != GestureMode.PINCH) {
                gestureStartViewport = viewportState
                pinchStartDistance = distance(touchPoints[0], touchPoints[1]).coerceAtLeast(1f)
                gestureMode = GestureMode.PINCH
            }
            val geometry = lastRenderGeometry ?: return
            val currentDistance = distance(touchPoints[0], touchPoints[1]).coerceAtLeast(1f)
            val focalHorizontal = (touchPoints[0].horizontal + touchPoints[1].horizontal) / 2f
            val focalRatio = if (geometry.plot.width <= 0f) {
                0.5f
            } else {
                (focalHorizontal - geometry.plot.left) / geometry.plot.width
            }
            val nextViewport = ViewportMath.zoom(
                gestureStartViewport,
                currentDistance / pinchStartDistance,
                focalRatio,
                spec.dataCount(),
                spec.interaction.minimumVisiblePoints,
            )
            gestureMoved = gestureMoved || currentDistance != pinchStartDistance
            setViewportInternal(nextViewport, emitEvent = true)
            return
        }

    }

    private fun handlePan(panParams: PanGestureParams) {
        if (!spec.interaction.panEnabled || gestureMode == GestureMode.PINCH) {
            return
        }
        when (panParams.state) {
            "start" -> {
                gestureMode = GestureMode.PAN
                gestureStartHorizontal = panParams.x
                gestureStartViewport = viewportState
                gestureMoved = false
            }

            "move" -> {
                if (gestureMode != GestureMode.PAN) {
                    gestureMode = GestureMode.PAN
                    gestureStartHorizontal = panParams.x
                    gestureStartViewport = viewportState
                    return
                }
                val geometry = lastRenderGeometry ?: return
                val horizontalDelta = panParams.x - gestureStartHorizontal
                gestureMoved = gestureMoved || kotlin.math.abs(horizontalDelta) > 3f
                val nextViewport = ViewportMath.pan(
                    gestureStartViewport,
                    horizontalDelta,
                    geometry.plot.width,
                    spec.dataCount(),
                )
                setViewportInternal(nextViewport, emitEvent = true)
            }

            "end" -> finishGesture()
        }
    }

    private fun finishGesture() {
        suppressNextClick = gestureMoved
        gestureMode = GestureMode.NONE
        gestureMoved = false
    }

    private fun zoomBy(scale: Float) {
        if (scale <= 0f) {
            return
        }
        val nextViewport = ViewportMath.zoom(
            viewportState,
            scale,
            0.5f,
            spec.dataCount(),
            spec.interaction.minimumVisiblePoints,
        )
        setViewportInternal(nextViewport, emitEvent = true)
    }

    private fun setViewportInternal(viewport: ChartViewport, emitEvent: Boolean) {
        val normalizedViewport = ViewportMath.normalize(viewport, spec.dataCount())
        if (normalizedViewport == viewportState) {
            return
        }
        viewportState = normalizedViewport
        if (selectionState != null) {
            setSelection(null)
        }
        if (emitEvent) {
            emit(ChartEvent.VIEWPORT_CHANGED, normalizedViewport)
        }
    }

    private fun setSelection(selection: ChartSelection?) {
        if (selection == selectionState) {
            return
        }
        selectionState = selection
        emit(ChartEvent.SELECTION_CHANGED, selection)
        if (selection != null) {
            emit(ChartEvent.POINT_SELECTED, selection)
        }
    }

    private fun touchPoints(touchParams: TouchParams): List<GesturePoint> {
        if (touchParams.touches.isEmpty()) {
            return listOf(GesturePoint(touchParams.x, touchParams.y))
        }
        return touchParams.touches.map { touch -> GesturePoint(touch.x, touch.y) }
    }

    private fun distance(first: GesturePoint, second: GesturePoint): Float {
        val horizontalDelta = first.horizontal - second.horizontal
        val verticalDelta = first.vertical - second.vertical
        return sqrt(horizontalDelta * horizontalDelta + verticalDelta * verticalDelta)
    }

    private enum class GestureMode {
        NONE,
        PAN,
        PINCH,
    }

    private data class GesturePoint(
        val horizontal: Float,
        val vertical: Float,
    )
}

/** Adds a generic chart that can combine line, area, and bar series. */
public fun ViewContainer<*, *>.Chart(init: ChartView.() -> Unit) {
    addChild(ChartView(ChartSeriesType.LINE), init)
}

/** Adds a chart whose `series(...)` shorthand renders lines. */
public fun ViewContainer<*, *>.LineChart(init: ChartView.() -> Unit) {
    addChild(ChartView(ChartSeriesType.LINE), init)
}

/** Adds a chart whose `series(...)` shorthand renders grouped bars. */
public fun ViewContainer<*, *>.BarChart(init: ChartView.() -> Unit) {
    addChild(ChartView(ChartSeriesType.BAR), init)
}

/** Adds a chart whose `series(...)` shorthand renders a filled area. */
public fun ViewContainer<*, *>.AreaChart(init: ChartView.() -> Unit) {
    addChild(ChartView(ChartSeriesType.AREA), init)
}

/** Adds a pie or donut chart. Global labels map to `series(...)` values. */
public fun ViewContainer<*, *>.PieChart(init: ChartView.() -> Unit) {
    addChild(ChartView(ChartSeriesType.PIE), init)
}
