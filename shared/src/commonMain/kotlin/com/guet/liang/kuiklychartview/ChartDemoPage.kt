package com.guet.liang.kuiklychartview

import com.guet.liang.kuiklychart.AreaChart
import com.guet.liang.kuiklychart.BarChart
import com.guet.liang.kuiklychart.ChartView
import com.guet.liang.kuiklychart.LineChart
import com.guet.liang.kuiklychart.PieChart
import com.guet.liang.kuiklychart.api.ChartAnimationEasing
import com.guet.liang.kuiklychart.api.ChartLegendPosition
import com.guet.liang.kuiklychart.api.ChartSelection
import com.guet.liang.kuiklychart.api.PieEntry
import com.guet.liang.kuiklychart.api.PieLabelMode
import com.guet.liang.kuiklychartview.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("chart_demo", supportInLocal = true)
internal class ChartDemoPage : BasePager() {
    private var selectionSummary by observable("点击任意数据点、柱或扇区查看详情")
    private var viewportSummary by observable("折线图初始展示最近 6 个月")
    private var animationSummary by observable("基础数据 · 点击按钮观察五类图平滑过渡")
    private var barPaletteIndex by observable(0)
    private var dataFrameIndex: Int = 0
    private var trendChartRef: ViewRef<ChartView>? = null
    private var barChartRef: ViewRef<ChartView>? = null
    private var areaChartRef: ViewRef<ChartView>? = null
    private var pieChartRef: ViewRef<ChartView>? = null
    private var donutChartRef: ViewRef<ChartView>? = null
    private var coordinatingSelection: Boolean = false

    override fun body(): ViewBuilder {
        val page = this
        return {
            attr {
                backgroundColor(PAGE_BACKGROUND)
            }
            Scroller {
                attr {
                    flex(1f)
                    paddingLeft(14f)
                    paddingRight(14f)
                    paddingTop(page.pagerData.statusBarHeight + 18f)
                    paddingBottom(30f)
                    backgroundColor(PAGE_BACKGROUND)
                }

                Text {
                    attr {
                        text("Kuikly Chart")
                        fontSize(28f)
                        fontWeightBold()
                        color(TITLE_COLOR)
                    }
                }
                Text {
                    attr {
                        text("一套 Kotlin DSL，多端共享 Canvas 渲染")
                        marginTop(5f)
                        fontSize(14f)
                        color(MUTED_COLOR)
                    }
                }
                View {
                    attr {
                        marginTop(14f)
                        padding(12f)
                        borderRadius(10f)
                        backgroundColor(Color(0xFFEFF6FFL))
                    }
                    Text {
                        attr {
                            text(page.selectionSummary)
                            fontSize(12f)
                            color(Color(0xFF1D4ED8L))
                        }
                    }
                    Text {
                        attr {
                            text(page.viewportSummary)
                            marginTop(4f)
                            fontSize(11f)
                            color(Color(0xFF3B82F6L))
                        }
                    }
                }

                DemoCard(
                    title = "数据更新过渡动画",
                    description = "统一切换五类图的数据源，折线、柱、面积、饼和环形图同步插值",
                ) {
                    Text {
                        attr {
                            text(page.animationSummary)
                            fontSize(11f)
                            color(Color(0xFF7C3AEDL))
                        }
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            marginTop(10f)
                        }
                        DemoButton("切换动画数据") {
                            page.applyDataFrame(page.dataFrameIndex + 1)
                        }
                        DemoButton("恢复初始数据") {
                            page.applyDataFrame(0)
                            page.trendChartRef?.view?.resetViewport()
                        }
                    }
                }

                DemoCard(
                    title = "趋势折线图",
                    description = "坐标轴、网格、多系列、Tooltip、十字线、拖拽平移与双指缩放",
                ) {
                    LineChart {
                        ref {
                            page.trendChartRef = it
                        }
                        attr {
                            height(292f)
                        }
                        chart {
                            title = "月度营收趋势"
                            subtitle = "单位：万元 · 横向拖动，移动端双指缩放"
                            labels(
                                "1月", "2月", "3月", "4月", "5月", "6月",
                                "7月", "8月", "9月", "10月", "11月", "12月",
                            )
                            line("2025", 32f, 38f, 35f, 46f, 52f, 49f, 61f, 68f, 66f, 78f, 84f, 91f) {
                                color(Color(0xFF2563EBL))
                                smooth()
                                points(radius = 3.5f)
                            }
                            line("2026", 41f, 44f, 51f, 55f, 63f, 67f, 74f, 81f, 88f, 94f, 102f, 116f) {
                                color(Color(0xFF10B981L))
                                smooth()
                                points(radius = 3.5f)
                            }
                            axes {
                                x {
                                    maxLabelCount = 6
                                }
                                y {
                                    tickCount = 5
                                    includeZero = true
                                    formatter = { value -> "${value.toInt()}w" }
                                }
                            }
                            grid {
                                horizontal = true
                                vertical = true
                                dashed(4f, 4f)
                            }
                            interaction {
                                selectionEnabled = true
                                panEnabled = true
                                zoomEnabled = true
                                minimumVisiblePoints = 3
                                initialVisiblePoints = 6
                            }
                            tooltip {
                                enabled = true
                                crosshairEnabled = true
                            }
                            animation {
                                durationMillis = 650
                                easing = ChartAnimationEasing.EASE_IN_OUT
                            }
                        }
                        event {
                            selectionChanged { selection ->
                                page.handleSelection(
                                    page.trendChartRef,
                                    selection,
                                    selection?.let {
                                        "${it.label} · ${it.seriesName}：${it.value.toInt()} 万元"
                                    },
                                )
                            }
                            viewportChanged { viewport ->
                                page.viewportSummary =
                                    "可见范围：${formatViewportIndex(viewport.startIndex)} - ${formatViewportIndex(viewport.endIndex)}"
                            }
                        }
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            marginTop(10f)
                        }
                        DemoButton("放大") {
                            page.trendChartRef?.view?.zoomIn()
                        }
                        DemoButton("缩小") {
                            page.trendChartRef?.view?.zoomOut()
                        }
                        DemoButton("左移") {
                            page.trendChartRef?.view?.panBy(-1f)
                        }
                        DemoButton("右移") {
                            page.trendChartRef?.view?.panBy(1f)
                        }
                        DemoButton("重置") {
                            page.trendChartRef?.view?.resetViewport()
                        }
                    }
                    Text {
                        attr {
                            text("Web 可使用上方按钮缩放；双击图表也可重置视窗")
                            marginTop(7f)
                            fontSize(10f)
                            color(MUTED_COLOR)
                        }
                    }
                }

                DemoCard(
                    title = "分组柱状图",
                    description = "多系列对比、逐柱自定义颜色、负数基线与数值标签",
                ) {
                    BarChart {
                        ref {
                            page.barChartRef = it
                        }
                        attr {
                            height(292f)
                        }
                        chart {
                            title = "渠道净增长"
                            subtitle = "逐柱颜色 + 正负值标签"
                            labels("自然流量", "广告", "社交", "推荐", "活动")
                            series("新增", 82f, 64f, 91f, 108f, 76f) {
                                barColors(
                                    Color(0xFF2563EBL),
                                    Color(0xFF7C3AEDL),
                                    Color(0xFFDB2777L),
                                    Color(0xFFEA580CL),
                                    Color(0xFF059669L),
                                )
                                valueLabels()
                            }
                            series("流失", -22f, -31f, -18f, -27f, -16f) {
                                color(Color(0xFF94A3B8L))
                                valueLabels(color = Color(0xFF64748BL))
                            }
                            axes {
                                x {
                                    maxLabelCount = 5
                                }
                                y {
                                    tickCount = 6
                                    includeZero = true
                                }
                            }
                            grid {
                                horizontal = true
                                solid()
                            }
                            bars {
                                groupWidthRatio = 0.7f
                                barSpacing = 4f
                                cornerRadius = 5f
                            }
                            interaction {
                                selectionEnabled = true
                            }
                            animation {
                                durationMillis = 650
                                easing = ChartAnimationEasing.EASE_IN_OUT
                            }
                        }
                        event {
                            selectionChanged { selection ->
                                page.handleSelection(
                                    page.barChartRef,
                                    selection,
                                    selection?.let {
                                        "${it.label} · ${it.seriesName}：${it.value.toInt()}"
                                    },
                                )
                            }
                        }
                    }
                    Text {
                        attr {
                            text(
                                "当前配色：${BAR_COLOR_PALETTES[page.barPaletteIndex].name} · " +
                                    "点击按钮运行时调用 barColors(...) 切换逐柱颜色",
                            )
                            marginTop(8f)
                            fontSize(10f)
                            color(MUTED_COLOR)
                        }
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            marginTop(8f)
                        }
                        DemoButton("商务蓝紫") {
                            page.applyBarPalette(0)
                        }
                        DemoButton("活力暖色") {
                            page.applyBarPalette(1)
                        }
                        DemoButton("清新青绿") {
                            page.applyBarPalette(2)
                        }
                    }
                }

                DemoCard(
                    title = "平滑面积图",
                    description = "线性渐变填充、平滑曲线、坐标轴与数据点标签",
                ) {
                    AreaChart {
                        ref {
                            page.areaChartRef = it
                        }
                        attr {
                            height(280f)
                        }
                        chart {
                            title = "活跃用户"
                            subtitle = "最近 7 日 DAU"
                            labels("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                            series("DAU", 12f, 18f, 16f, 25f, 28f, 34f, 31f) {
                                color(Color(0xFF7C3AEDL))
                                fill(Color(0xFF8B5CF6L), opacity = 0.34f)
                                smooth()
                                points(radius = 3f)
                                valueLabels(color = Color(0xFF6D28D9L))
                            }
                            axes {
                                y {
                                    includeZero = true
                                    formatter = { value -> "${value.toInt()}k" }
                                }
                            }
                            grid {
                                horizontal = true
                                dashed(3f, 4f)
                            }
                            legend {
                                position = ChartLegendPosition.NONE
                            }
                            interaction {
                                selectionEnabled = true
                            }
                            animation {
                                durationMillis = 650
                                easing = ChartAnimationEasing.EASE_IN_OUT
                            }
                        }
                        event {
                            selectionChanged { selection ->
                                page.handleSelection(
                                    page.areaChartRef,
                                    selection,
                                    selection?.let {
                                        "${it.label} · 活跃用户：${it.value.toInt()}k"
                                    },
                                )
                            }
                        }
                    }
                }

                DemoCard(
                    title = "饼图与环形图",
                    description = "普通饼图、环形图、自定义扇区颜色、百分比标签与自动换行图例",
                ) {
                    PieChart {
                        ref {
                            page.pieChartRef = it
                        }
                        attr {
                            height(250f)
                        }
                        chart {
                            title = "客户结构 · 饼图"
                            pie(
                                "客户",
                                PieEntry("新客户", 46f, Color(0xFF2563EBL)),
                                PieEntry("复购客户", 32f, Color(0xFF10B981L)),
                                PieEntry("会员", 22f, Color(0xFFF59E0BL)),
                            )
                            pie {
                                labelMode = PieLabelMode.LABEL_AND_PERCENT
                                minimumLabelPercent = 0.06f
                            }
                            legend {
                                position = ChartLegendPosition.NONE
                            }
                            tooltip {
                                valueFormatter = { value -> "${value.toInt()}%" }
                            }
                            interaction {
                                selectionEnabled = true
                            }
                            animation {
                                durationMillis = 650
                                easing = ChartAnimationEasing.EASE_IN_OUT
                            }
                        }
                        event {
                            selectionChanged { selection ->
                                page.handleSelection(
                                    page.pieChartRef,
                                    selection,
                                    selection?.let { "${it.label}：${it.value.toInt()}%" },
                                )
                            }
                        }
                    }
                    PieChart {
                        ref {
                            page.donutChartRef = it
                        }
                        attr {
                            height(330f)
                            marginTop(8f)
                        }
                        chart {
                            title = "订单来源 · 环形图"
                            subtitle = "点击扇区查看具体占比"
                            pie(
                                "订单",
                                PieEntry("Android App", 30f, Color(0xFF2563EBL)),
                                PieEntry("iOS App", 22f, Color(0xFF10B981L)),
                                PieEntry("微信小程序", 18f, Color(0xFFF59E0BL)),
                                PieEntry("桌面 Web", 13f, Color(0xFFEC4899L)),
                                PieEntry("线下门店", 10f, Color(0xFF8B5CF6L)),
                                PieEntry("第三方渠道", 7f, Color(0xFF06B6D4L)),
                            )
                            pie {
                                innerRadiusRatio = 0.5f
                                selectedOffset = 8f
                                labelMode = PieLabelMode.PERCENT
                                minimumLabelPercent = 0.06f
                            }
                            legend {
                                position = ChartLegendPosition.TOP
                            }
                            tooltip {
                                valueFormatter = { value -> "${value.toInt()}%" }
                            }
                            interaction {
                                selectionEnabled = true
                            }
                            animation {
                                durationMillis = 650
                                easing = ChartAnimationEasing.EASE_IN_OUT
                            }
                        }
                        event {
                            selectionChanged { selection ->
                                page.handleSelection(
                                    page.donutChartRef,
                                    selection,
                                    selection?.let { "${it.label}：${it.value.toInt()}%" },
                                )
                            }
                        }
                    }
                }

                Text {
                    attr {
                        text("Kuikly Chart · Android / iOS / Web / HarmonyOS")
                        marginTop(8f)
                        marginBottom(20f)
                        fontSize(11f)
                        color(MUTED_COLOR)
                        textAlignCenter()
                    }
                }
            }
        }
    }

    private fun applyDataFrame(requestedIndex: Int) {
        val normalizedIndex = ((requestedIndex % DATA_FRAMES.size) + DATA_FRAMES.size) % DATA_FRAMES.size
        val frame = DATA_FRAMES[normalizedIndex]
        dataFrameIndex = normalizedIndex

        trendChartRef?.view?.update {
            series.getOrNull(0)?.values(frame.trend2025)
            series.getOrNull(1)?.values(frame.trend2026)
        }
        barChartRef?.view?.update {
            series.getOrNull(0)?.values(frame.barAdded)
            series.getOrNull(1)?.values(frame.barLost)
        }
        areaChartRef?.view?.update {
            series.getOrNull(0)?.values(frame.areaUsers)
        }
        pieChartRef?.view?.update {
            series.getOrNull(0)?.values(frame.pieCustomers)
        }
        donutChartRef?.view?.update {
            series.getOrNull(0)?.values(frame.donutOrders)
        }
        animationSummary = "动态数据 ${normalizedIndex + 1}/${DATA_FRAMES.size} · 650ms ease-in-out"
    }

    private fun applyBarPalette(requestedIndex: Int) {
        val normalizedIndex =
            ((requestedIndex % BAR_COLOR_PALETTES.size) + BAR_COLOR_PALETTES.size) % BAR_COLOR_PALETTES.size
        val palette = BAR_COLOR_PALETTES[normalizedIndex]
        barPaletteIndex = normalizedIndex
        barChartRef?.view?.update(animated = false) {
            series.getOrNull(0)?.barColors(palette.colors)
        }
    }

    private fun handleSelection(
        source: ViewRef<ChartView>?,
        selection: ChartSelection?,
        selectedSummary: String?,
    ) {
        if (coordinatingSelection) {
            return
        }
        if (selection == null) {
            selectionSummary = "点击任意数据点、柱或扇区查看详情"
            return
        }
        coordinatingSelection = true
        listOf(trendChartRef, barChartRef, areaChartRef, pieChartRef, donutChartRef).forEach { chartRef ->
            if (chartRef !== source) {
                chartRef?.view?.clearSelection()
            }
        }
        coordinatingSelection = false
        selectionSummary = selectedSummary ?: "${selection.label}：${selection.value}"
    }

    private companion object {
        val PAGE_BACKGROUND = Color(0xFFF1F5F9L)
        val TITLE_COLOR = Color(0xFF0F172AL)
        val MUTED_COLOR = Color(0xFF64748BL)
        val BAR_COLOR_PALETTES = listOf(
            DemoBarPalette(
                name = "商务蓝紫",
                colors = listOf(
                    Color(0xFF2563EBL),
                    Color(0xFF7C3AEDL),
                    Color(0xFFDB2777L),
                    Color(0xFFEA580CL),
                    Color(0xFF059669L),
                ),
            ),
            DemoBarPalette(
                name = "活力暖色",
                colors = listOf(
                    Color(0xFFB91C1CL),
                    Color(0xFFEA580CL),
                    Color(0xFFF59E0BL),
                    Color(0xFFCA8A04L),
                    Color(0xFFBE123CL),
                ),
            ),
            DemoBarPalette(
                name = "清新青绿",
                colors = listOf(
                    Color(0xFF0F766EL),
                    Color(0xFF0891B2L),
                    Color(0xFF0284C7L),
                    Color(0xFF16A34AL),
                    Color(0xFF65A30DL),
                ),
            ),
        )
        val DATA_FRAMES = listOf(
            DemoDataFrame(
                trend2025 = listOf(32f, 38f, 35f, 46f, 52f, 49f, 61f, 68f, 66f, 78f, 84f, 91f),
                trend2026 = listOf(41f, 44f, 51f, 55f, 63f, 67f, 74f, 81f, 88f, 94f, 102f, 116f),
                barAdded = listOf(82f, 64f, 91f, 108f, 76f),
                barLost = listOf(-22f, -31f, -18f, -27f, -16f),
                areaUsers = listOf(12f, 18f, 16f, 25f, 28f, 34f, 31f),
                pieCustomers = listOf(46f, 32f, 22f),
                donutOrders = listOf(30f, 22f, 18f, 13f, 10f, 7f),
            ),
            DemoDataFrame(
                trend2025 = listOf(35f, 40f, 39f, 49f, 57f, 53f, 65f, 72f, 70f, 82f, 89f, 96f),
                trend2026 = listOf(44f, 48f, 54f, 59f, 67f, 71f, 79f, 85f, 92f, 100f, 108f, 121f),
                barAdded = listOf(95f, 72f, 86f, 116f, 88f),
                barLost = listOf(-19f, -28f, -24f, -22f, -20f),
                areaUsers = listOf(15f, 21f, 20f, 29f, 31f, 37f, 35f),
                pieCustomers = listOf(39f, 37f, 24f),
                donutOrders = listOf(28f, 25f, 17f, 15f, 9f, 6f),
            ),
            DemoDataFrame(
                trend2025 = listOf(28f, 36f, 42f, 43f, 50f, 56f, 59f, 71f, 74f, 80f, 87f, 94f),
                trend2026 = listOf(39f, 47f, 49f, 58f, 61f, 70f, 76f, 83f, 91f, 97f, 105f, 119f),
                barAdded = listOf(76f, 83f, 99f, 102f, 91f),
                barLost = listOf(-25f, -21f, -20f, -30f, -14f),
                areaUsers = listOf(10f, 17f, 23f, 22f, 30f, 33f, 39f),
                pieCustomers = listOf(42f, 29f, 29f),
                donutOrders = listOf(32f, 20f, 19f, 11f, 12f, 6f),
            ),
        )
    }
}

private data class DemoBarPalette(
    val name: String,
    val colors: List<Color>,
)

private data class DemoDataFrame(
    val trend2025: List<Float?>,
    val trend2026: List<Float?>,
    val barAdded: List<Float?>,
    val barLost: List<Float?>,
    val areaUsers: List<Float?>,
    val pieCustomers: List<Float?>,
    val donutOrders: List<Float?>,
)

private fun ViewContainer<*, *>.DemoCard(
    title: String,
    description: String,
    content: ViewBuilder,
) {
    View {
        attr {
            marginTop(16f)
            padding(14f)
            borderRadius(14f)
            backgroundColor(Color.WHITE)
        }
        Text {
            attr {
                text(title)
                fontSize(18f)
                fontWeightBold()
                color(Color(0xFF0F172AL))
            }
        }
        Text {
            attr {
                text(description)
                marginTop(4f)
                marginBottom(8f)
                fontSize(11f)
                color(Color(0xFF64748BL))
            }
        }
        content()
    }
}

private fun ViewContainer<*, *>.DemoButton(
    title: String,
    onClick: () -> Unit,
) {
    View {
        attr {
            flex(1f)
            height(30f)
            marginRight(7f)
            borderRadius(8f)
            backgroundColor(Color(0xFFE2E8F0L))
            allCenter()
        }
        Text {
            attr {
                text(title)
                fontSize(11f)
                fontWeightSemiBold()
                color(Color(0xFF334155L))
            }
        }
        event {
            click {
                onClick()
            }
        }
    }
}

private fun formatViewportIndex(index: Float): String {
    val rounded = kotlin.math.round(index * 10f) / 10f
    return if (rounded == rounded.toInt().toFloat()) {
        (rounded.toInt() + 1).toString()
    } else {
        (rounded + 1f).toString()
    }
}
