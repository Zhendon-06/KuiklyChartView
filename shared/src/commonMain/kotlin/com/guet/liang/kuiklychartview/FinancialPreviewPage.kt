package com.guet.liang.kuiklychartview

import com.guet.liang.kuiklychart.PieChart
import com.guet.liang.kuiklychart.api.ChartLegendPosition
import com.guet.liang.kuiklychart.api.PieEntry
import com.guet.liang.kuiklychart.api.PieLabelMode
import com.guet.liang.kuiklychart.finance.*
import com.guet.liang.kuiklychartview.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** Offline visual verification independent of stock/AI services. Every value is a component example. */
@Page("financial_preview", supportInLocal = true)
internal class FinancialPreviewPage : BasePager() {
    override fun body(): ViewBuilder = {
        attr { backgroundColor(Color(0xFFF5F7F9L)) }
        Scroller {
            attr { absolutePositionAllZero(); padding(top = pagerData.statusBarHeight + 18f, left = 16f, right = 16f, bottom = 30f) }
            Text { attr { text("金融图表组件样例 · 非真实预测"); fontSize(18f); fontWeightBold(); color(Color(0xFF202630L)) } }
            Text { attr { text("历史线、预测虚线、模型区间及环图选择"); fontSize(12f); marginTop(7f); marginBottom(16f); color(Color(0xFF7C8590L)) } }
            View {
                attr { padding(12f); borderRadius(16f); backgroundColor(Color.WHITE) }
                FinancialChart {
                    attr { height(350f) }
                    chart {
                        val history = listOf(14f, 14.2f, 14.1f, 14.5f, 14.35f, 14.8f, 14.6f, 15.0f, 14.9f, 15.3f, 15.1f, 15.5f)
                        val future = listOf(15.6f, 15.9f, 15.7f, 16.0f, 16.2f, 16.1f)
                        points = (history + future).mapIndexed { index, value -> FinancialPoint("2026-09-${(index + 1).toString().padStart(2, '0')}", value, value, value, value) }
                        mode = FinancialChartMode.CLOSE_LINE
                        showVolume = false
                        forecastStartIndex = history.size
                        forecastIntervals = future.mapIndexed { i, value -> history.size + i to FinancialInterval(value - 0.3f - i * 0.05f, value + 0.3f + i * 0.05f) }.toMap()
                    }
                }
            }
            View {
                attr { marginTop(16f); padding(12f); borderRadius(16f); backgroundColor(Color.WHITE) }
                Text { attr { text("资金分布 · 示例数据"); fontSize(16f); color(Color(0xFF202630L)) } }
                PieChart {
                    attr { height(270f) }
                    chart {
                        pie("金额", PieEntry("主力流入", 4.12f, Color(0xFFE93449L)), PieEntry("主力流出", 4.25f, Color(0xFF139D80L)),
                            PieEntry("散户流入", 5.72f, Color(0xFFFFA1B0L)), PieEntry("散户流出", 5.59f, Color(0xFFB4E8DDL)))
                        pie { innerRadiusRatio = 0.62f; labelMode = PieLabelMode.NONE; centerText = "主力 / 散户"; centerSubtext = "点选查看占比" }
                        legend { position = ChartLegendPosition.BOTTOM }
                        tooltip { enabled = false }
                    }
                }
            }
        }
    }
}
