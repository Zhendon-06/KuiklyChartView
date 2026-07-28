# API 文档

包名：`com.guet.liang.kuiklychart`

数据与样式类型位于：`com.guet.liang.kuiklychart.api`

## 组件入口

| API | `series(...)` 默认类型 | 用途 |
| --- | --- | --- |
| `Chart {}` | `LINE` | 混合折线、面积、柱系列 |
| `LineChart {}` | `LINE` | 趋势折线 |
| `BarChart {}` | `BAR` | 分组柱状对比 |
| `AreaChart {}` | `AREA` | 渐变面积趋势 |
| `PieChart {}` | `PIE` | 饼图或环形图 |

所有入口的 receiver 都是公开 `ChartView`，支持 Kuikly 标准 `attr {}`、`ref {}`、`event {}`，图表配置放在 `chart {}`。

## ChartView

| 成员 | 说明 |
| --- | --- |
| `spec: ChartSpec` | 当前图表配置，只读引用 |
| `currentSelection: ChartSelection?` | 当前选中数据 |
| `currentViewport: ChartViewport` | 当前可见分类范围 |
| `chart {}` / `update {}` | 应用或运行时修改配置；`update` 是语义别名 |
| `setViewport(startIndex, endIndex)` | 设置可见分类索引，可使用小数 |
| `resetViewport()` | 恢复完整范围并清除选择 |
| `zoomIn(factor = 1.5f)` | 以中心为锚点放大 |
| `zoomOut(factor = 1.5f)` | 以中心为锚点缩小 |
| `panBy(categoryCount)` | 按分类数量平移，正数向后 |
| `select(seriesIndex, dataIndex)` | 程序化选择数据 |
| `clearSelection()` | 清除 Tooltip、十字线和扇区选择 |

## ChartSpec

### 基础字段

| 字段 | 类型 | 默认值 |
| --- | --- | --- |
| `title` | `String` | `""` |
| `subtitle` | `String` | `""` |
| `emptyText` | `String` | `"No data"` |
| `defaultSeriesType` | `ChartSeriesType` | 由组件入口决定 |
| `labels` | `List<String>` | 空列表 |
| `series` | `List<ChartSeries>` | 空列表 |

### 数据 DSL

| 方法 | 说明 |
| --- | --- |
| `labels(vararg String)` / `labels(List<String>)` | 设置笛卡尔分类标签 |
| `series(name, vararg Float)` | 使用当前组件的默认系列类型 |
| `series(name, List<Float?>)` | 支持 `null` 折线断点 |
| `line(...)` | 显式折线系列 |
| `area(...)` | 显式面积系列 |
| `bars(...)` | 显式柱系列 |
| `pie(name, vararg PieEntry)` | 饼图系列与逐扇区标签/颜色 |
| `clearSeries()` | 运行时替换数据前清空系列 |

### 分组配置

```kotlin
chart {
    theme { }
    axes { x { }; y { } }
    grid { }
    legend { }
    bars { }
    pie { }
    tooltip { }
    interaction { }
}
```

## ChartSeries

| 字段/方法 | 默认值 | 说明 |
| --- | --- | --- |
| `type` | 入口或数据方法决定 | `LINE` / `AREA` / `BAR` / `PIE` |
| `name` | 必填 | 图例与 Tooltip 系列名 |
| `values` / `values(...)` | 必填 | 数据；`null` 产生折线断点 |
| `color` / `color(...)` | 自动调色板 | 系列主色 |
| `pointColors(...)` | 空 | 逐柱/逐扇区颜色，循环使用 |
| `pointLabels(...)` | 空 | 饼图逐扇区标签 |
| `fillColor` / `fill(color, opacity)` | 主色 / `0.24` | 面积填充 |
| `lineWidth` | `2.5f` | 折线描边宽度 |
| `curve` / `smooth()` | `STRAIGHT` | 直线或平滑贝塞尔 |
| `showPoints` / `points(...)` | `true` | 数据点显示与半径 |
| `pointRadius` | `3.5f` | 数据点半径 |
| `showValues` / `valueLabels(...)` | `false` | 数值标签 |
| `valueLabelColor` | 系列色或主题色 | 数值标签颜色 |

## 坐标轴

### CategoryAxisConfig (`axes.x`)

| 字段 | 默认值 |
| --- | --- |
| `visible` | `true` |
| `showLabels` | `true` |
| `maxLabelCount` | `7` |
| `labelFontSize` | `null`，使用主题 |
| `labelColor` | `null`，使用主题 |
| `axisColor` | `null`，使用主题 |
| `formatter: (String, Int) -> String` | 原标签 |

### ValueAxisConfig (`axes.y`)

| 字段 | 默认值 |
| --- | --- |
| `visible` | `true` |
| `showLabels` | `true` |
| `minimum` / `maximum` | `null`，自动范围 |
| `tickCount` | `5` |
| `includeZero` | `false`；柱图始终包含零基线 |
| `labelFontSize` / `labelColor` / `axisColor` | `null`，使用主题 |
| `formatter: (Float) -> String` | 自动 K/M 与小数格式 |

自动刻度使用 1/2/5 × 10ⁿ 的 nice-number 算法，并只统计当前视窗内数据。

## 网格与图例

### ChartGridConfig

| 字段/方法 | 默认值 |
| --- | --- |
| `horizontal` | `true` |
| `vertical` | `false` |
| `color` | `null`，使用主题 |
| `lineWidth` | `1f` |
| `dashPattern` | `[4f, 4f]` |
| `dashed(vararg)` | 设置虚线段 |
| `solid()` | 清空虚线段 |

### ChartLegendConfig

| 字段 | 默认值 |
| --- | --- |
| `position` | `TOP`，可选 `NONE` / `BOTTOM` |
| `fontSize` / `textColor` | `null`，使用主题 |
| `itemSpacing` | `14f` |
| `rowSpacing` | `6f` |

图例根据可用宽度自动换行。饼图图例按扇区生成，其余图表按系列生成。

## 柱与饼配置

### BarChartConfig

| 字段 | 默认值 |
| --- | --- |
| `groupWidthRatio` | `0.72f` |
| `barSpacing` | `3f` |
| `cornerRadius` | `4f` |

### PieChartConfig

| 字段 | 默认值 |
| --- | --- |
| `innerRadiusRatio` | `0f`；大于 0 变为环形图，最大 0.9 |
| `startAngle` | `-π / 2` |
| `sliceSpacingAngle` | `0.012f` 弧度 |
| `selectedOffset` | `7f` |
| `labelMode` | `PERCENT` |
| `minimumLabelPercent` | `0.05f` |
| `holeColor` | `null`，使用图表背景色 |

`PieLabelMode`：`NONE`、`LABEL`、`VALUE`、`PERCENT`、`LABEL_AND_PERCENT`。

## Tooltip 与交互

### ChartTooltipConfig

| 字段 | 默认值 |
| --- | --- |
| `enabled` | `true` |
| `crosshairEnabled` | `true` |
| `showSeriesName` | `true` |
| `valueFormatter` | `null`，使用 Y 轴 formatter |

### ChartInteractionConfig

| 字段 | 默认值 |
| --- | --- |
| `selectionEnabled` | `true` |
| `panEnabled` | `false` |
| `zoomEnabled` | `false` |
| `resetViewportOnDoubleClick` | `true` |
| `minimumVisiblePoints` | `3` |
| `initialVisiblePoints` | `0`，展示全部 |
| `dismissSelectionOnOutsideTap` | `true` |

## 事件

```kotlin
event {
    selectionChanged { selectionOrNull -> }
    pointSelected { selection -> }
    viewportChanged { viewport -> }
}
```

`ChartSelection` 字段：`seriesIndex`、`dataIndex`、`seriesName`、`label`、`value`、`type`。

`ChartViewport` 字段：`startIndex`、`endIndex`，并提供 `visiblePointCount`。

## 主题

`ChartTheme` 公共字段：

- `backgroundColor`
- `textColor`
- `mutedTextColor`
- `axisColor`
- `gridColor`
- `crosshairColor`
- `tooltipBackgroundColor`
- `tooltipTextColor`
- `selectionColor`
- `titleFontSize`
- `subtitleFontSize`
- `labelFontSize`
- `valueFontSize`
- `contentPadding`

`contentPadding(all)` 与 `contentPadding(left, top, right, bottom)` 可快速设置 `ChartInsets`。

