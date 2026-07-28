# KuiklyChartView

KuiklyChartView 是基于 Kuikly `Canvas` 的 Kotlin Multiplatform 图表组件。图表模型、DSL、绘制和交互全部位于 `commonMain`，Android、iOS、Web 与 HarmonyOS 共用同一套实现，不需要注册平台原生图表 View。

## 功能

- 折线图：多系列趋势、直线/平滑曲线、数据点、数值标签。
- 柱状图：分组对比、正负值、逐系列或逐柱颜色、圆角、数值标签。
- 面积图：平滑曲线、跨端线性渐变填充。
- 饼图/环形图：逐扇区颜色、百分比/数值标签、图例、选中偏移。
- 公共能力：标题、图例、X/Y 轴、自动刻度、横纵网格、空数据状态。
- 交互：点选 Tooltip、十字线、横向平移、双击重置、移动端双指缩放。
- 控制 API：`setViewport`、`zoomIn`、`zoomOut`、`panBy`、`select`、`update`。
- 类型安全 DSL：`Chart`、`LineChart`、`BarChart`、`AreaChart`、`PieChart`。

## 模块

```text
kuikly-chart/   可独立发布的纯 KMP 图表库
shared/         @Page("chart_demo") 全功能演示
androidApp/     Android 宿主
iosApp/         iOS 宿主
ohosApp/        HarmonyOS 宿主
docs/           API 与架构文档
```

三端宿主默认打开 `chart_demo`。原脚手架的 `router` 与 `image_adapter` 页面仍保留。

## 快速接入

仓库内模块依赖：

```kotlin
// commonMain
dependencies {
    implementation(project(":kuikly-chart"))
}
```

发布到本机 Maven 后使用：

```shell
./gradlew :kuikly-chart:publishToMavenLocal
```

```kotlin
dependencies {
    implementation("com.guet.liang.kuiklychartview:kuikly-chart:1.0.0")
}
```

最小折线图：

```kotlin
import com.guet.liang.kuiklychart.LineChart
import com.tencent.kuikly.core.base.Color

LineChart {
    attr {
        height(280f)
    }
    chart {
        title = "周活跃用户"
        labels("周一", "周二", "周三", "周四", "周五")
        series("DAU", 12f, 18f, 16f, 25f, 28f) {
            color(Color(0xFF2563EBL))
            smooth()
            points(radius = 3f)
        }
        axes {
            y {
                includeZero = true
                formatter = { value -> "${value.toInt()}k" }
            }
        }
        grid {
            horizontal = true
            dashed(4f, 4f)
        }
        interaction {
            selectionEnabled = true
            panEnabled = true
            zoomEnabled = true
        }
    }
    event {
        pointSelected { selection ->
            println("${selection.label}: ${selection.value}")
        }
    }
}
```

## 图表 DSL

便捷组件的 `series(...)` 会自动采用对应类型：

```kotlin
LineChart { chart { series("趋势", 1f, 3f, 2f) } }
BarChart  { chart { series("销量", 8f, 12f, 10f) } }
AreaChart { chart { series("流量", 3f, 6f, 5f) } }
PieChart  { chart { labels("A", "B"); series("占比", 70f, 30f) } }
```

`Chart` 可以组合折线、面积和柱系列：

```kotlin
Chart {
    chart {
        labels("Q1", "Q2", "Q3", "Q4")
        bars("订单", 42f, 58f, 63f, 79f) { valueLabels() }
        area("目标", 48f, 55f, 68f, 82f) { smooth() }
        line("转化率", 31f, 38f, 45f, 52f) { smooth() }
    }
}
```

逐柱颜色与标签：

```kotlin
BarChart {
    chart {
        labels("自然", "广告", "推荐")
        series("新增", 82f, 64f, 91f) {
            pointColors(Color.BLUE, Color(0xFF7C3AEDL), Color(0xFF10B981L))
            valueLabels()
        }
    }
}
```

环形图：

```kotlin
PieChart {
    chart {
        pie(
            "订单",
            PieEntry("App", 42f, Color(0xFF2563EBL)),
            PieEntry("Web", 31f, Color(0xFF10B981L)),
            PieEntry("门店", 27f, Color(0xFFF59E0BL)),
        )
        pie {
            innerRadiusRatio = 0.5f
            labelMode = PieLabelMode.PERCENT
        }
    }
}
```

完整属性、默认值与运行时 API 见 [API 文档](docs/API.md)。实现取舍与跨端细节见 [架构文档](docs/ARCHITECTURE.md)。

## 手势与视窗

- 点击：选择最接近的折线点/面积点/柱，或命中的饼图扇区。
- 单指横向拖动：在 `panEnabled = true` 时移动可见窗口。
- 双指缩放：在 `zoomEnabled = true` 时支持 Android、iOS、HarmonyOS。
- 双击：默认恢复完整数据范围。
- Web 缩放：Kuikly 2.7 Web 触摸事件只提供单指，使用 `zoomIn()` / `zoomOut()` 按钮降级；Demo 已提供完整控制栏。

```kotlin
private lateinit var chartRef: ViewRef<ChartView>

LineChart {
    ref { chartRef = it }
    // ...
}

chartRef.view?.zoomIn()
chartRef.view?.panBy(2f)
chartRef.view?.resetViewport()
```

## 平台矩阵

| 能力 | Android | iOS | Web | HarmonyOS |
| --- | --- | --- | --- | --- |
| 折线/柱/面积/饼/环 | ✅ | ✅ | ✅ | ✅ |
| 坐标轴/网格/图例/标签 | ✅ | ✅ | ✅ | ✅ |
| Tooltip/十字线/点选 | ✅ | ✅ | ✅ | ✅ |
| 单指平移/双击重置 | ✅ | ✅ | ✅ | ✅ |
| 双指缩放 | ✅ | ✅ | 按钮降级 | ✅ |
| 程序化缩放和平移 | ✅ | ✅ | ✅ | ✅ |

当前工程使用 Kuikly `2.7.0`。Web Canvas 的 `font` 与 `textAlign` 在该版本未实现，组件使用 `measureText` 和手工坐标对齐保证文本布局；字体由浏览器 Canvas 默认字体决定。

## 运行 Demo

Android：

```shell
./gradlew :androidApp:assembleDebug
```

iOS：先用 Gradle 生成 `shared` framework，再从 `iosApp/iosApp.xcworkspace` 运行。

Web：

```shell
./gradlew :shared:jsBrowserDevelopmentWebpack -PpageName=chart_demo
npm install
npm run serve
```

HarmonyOS：

```shell
./gradlew -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64 -PpageName=chart_demo
./ohosApp/runOhosApp.sh
```

## 验证

```shell
./gradlew :kuikly-chart:testDebugUnitTest
./gradlew :kuikly-chart:compileTestKotlinJs
./gradlew :kuikly-chart:compileKotlinJs
./gradlew :kuikly-chart:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
```

`commonTest` 当前覆盖视窗平移/缩放、自动刻度、DSL 数据构建以及笛卡尔/饼图命中测试。

## 参考

- [Kuikly ComposeView](https://kuikly.tds.qq.com/DevGuide/compose-view.html)
- [Kuikly Canvas](https://kuikly.tds.qq.com/API/components/canvas.html)
- [Kuikly 基础事件](https://kuikly.tds.qq.com/API/components/basic-attr-event.html)
- [Kuikly 多模块](https://kuikly.tds.qq.com/DevGuide/multi_module.html)
- [KuiklyChatUI](https://github.com/Kuikly-contrib/KuiklyChatUI)

