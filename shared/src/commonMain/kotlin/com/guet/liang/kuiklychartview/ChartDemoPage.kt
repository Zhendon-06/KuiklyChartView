package com.guet.liang.kuiklychartview

import com.guet.liang.kuiklychart.AreaChart
import com.guet.liang.kuiklychart.BarChart
import com.guet.liang.kuiklychart.ChartView
import com.guet.liang.kuiklychart.LineChart
import com.guet.liang.kuiklychart.PieChart
import com.guet.liang.kuiklychart.api.ChartLegendPosition
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
    private lateinit var trendChartRef: ViewRef<ChartView>

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
                        }
                        event {
                            selectionChanged { selection ->
                                page.selectionSummary = selection?.let {
                                    "${it.label} · ${it.seriesName}：${it.value.toInt()} 万元"
                                } ?: "已清除折线图选择"
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
                            page.trendChartRef.view?.zoomIn()
                        }
                        DemoButton("缩小") {
                            page.trendChartRef.view?.zoomOut()
                        }
                        DemoButton("向前") {
                            page.trendChartRef.view?.panBy(-2f)
                        }
                        DemoButton("重置") {
                            page.trendChartRef.view?.resetViewport()
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
                        attr {
                            height(292f)
                        }
                        chart {
                            title = "渠道净增长"
                            subtitle = "逐柱颜色 + 正负值标签"
                            labels("自然流量", "广告", "社交", "推荐", "活动")
                            series("新增", 82f, 64f, 91f, 108f, 76f) {
                                pointColors(
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
                        }
                        event {
                            pointSelected { selection ->
                                page.selectionSummary =
                                    "${selection.label} · ${selection.seriesName}：${selection.value.toInt()}"
                            }
                        }
                    }
                }

                DemoCard(
                    title = "平滑面积图",
                    description = "线性渐变填充、平滑曲线、坐标轴与数据点标签",
                ) {
                    AreaChart {
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
                        }
                        event {
                            pointSelected { selection ->
                                page.selectionSummary =
                                    "${selection.label} · 活跃用户：${selection.value.toInt()}k"
                            }
                        }
                    }
                }

                DemoCard(
                    title = "环形图",
                    description = "自定义扇区颜色、百分比标签、自动换行图例与扇区选中",
                ) {
                    PieChart {
                        attr {
                            height(310f)
                        }
                        chart {
                            title = "订单来源"
                            subtitle = "点击扇区查看具体占比"
                            pie(
                                "订单",
                                PieEntry("App", 42f, Color(0xFF2563EBL)),
                                PieEntry("小程序", 27f, Color(0xFF10B981L)),
                                PieEntry("Web", 18f, Color(0xFFF59E0BL)),
                                PieEntry("门店", 13f, Color(0xFFEC4899L)),
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
                        }
                        event {
                            pointSelected { selection ->
                                page.selectionSummary =
                                    "${selection.label}：${selection.value.toInt()}%"
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

    private companion object {
        val PAGE_BACKGROUND = Color(0xFFF1F5F9L)
        val TITLE_COLOR = Color(0xFF0F172AL)
        val MUTED_COLOR = Color(0xFF64748BL)
    }
}

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
            width(62f)
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
