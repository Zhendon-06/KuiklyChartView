# 架构与跨端实现

## 分层

```text
ChartView / ChartEvent
        │ observable viewport + selection
        ▼
ChartSpec / ChartSeries / typed DSL
        │
        ▼
ViewportMath + ScaleMath + geometry
        │
        ├── ChartRenderer (Kuikly Canvas)
        └── ChartHitTester
```

- `api` 包只包含稳定公共模型和 DSL。
- `internal` 包集中纯数学、绘制和命中逻辑。
- `ChartView` 只管理 Kuikly 生命周期、响应式重绘、事件与手势状态。
- Demo 页面使用组件公开 API，不访问内部实现。

## 响应式重绘

Kuikly `CanvasView` 会追踪绘制回调中读取的 `observable`。`ChartView` 在绘制时读取：

- `renderRevision`：运行时 `update {}` 后重绘。
- `viewportState`：平移/缩放后重绘。
- `selectionState`：Tooltip、十字线、扇区偏移后重绘。

组件不需要平台 `invalidate()` 或自定义原生桥。

## 坐标与刻度

- X 轴使用连续分类索引视窗，支持小数边界，因此平移和缩放保持连续。
- Y 轴只统计当前视窗中的有限值。
- 柱系列自动包含零；普通系列可通过 `includeZero` 控制。
- nice-number 刻度按 1/2/5 × 10ⁿ 生成。
- `null` 与非有限值不参与范围计算，并切断折线路径。

## 绘制顺序

1. 背景。
2. 标题、图例、轴与网格。
3. 面积填充。
4. 分组柱。
5. 面积边线与普通折线。
6. 选中十字线与 Tooltip。

混合图因此能保持面积在底层、折线在顶层。

## Kuikly 2.7 跨端兼容

实现只使用各端共有的路径、圆弧、填充、描边、线性渐变和文本 API，明确不使用仅 iOS 支持的径向渐变。

### Web Canvas

Kuikly 2.7 Web 支持 `fillText`，但没有实现 Canvas `font` 和 `textAlign`。组件通过 `measureText` 后手工偏移 X 坐标实现左/中/右对齐；Web 的实际字体仍使用浏览器 Canvas 默认字体。

Kuikly 2.7 Web 支持 `clip`，但未实现 `save/restore`，而 `reset` 不清除裁剪状态。组件不使用 Canvas 裁剪，而是在几何阶段限制可绘制的数据中心，避免响应式重绘后永久裁剪。

### iOS 虚线

Kuikly 2.7 iOS 对空 `setLineDash` 不会清除已有虚线状态。组件使用路径分段手工绘制虚线，从而不污染后续坐标轴和系列线条。

### 手势

- 四端单指平移使用 Kuikly `pan`。
- Android、iOS、HarmonyOS 双指缩放通过透明 `View` 的原始多触点计算距离。
- Kuikly 2.7 Web 原始触摸只提供单触点，因此 Web 使用公开 `zoomIn` / `zoomOut` 控制 API；Demo 已提供按钮。
- 双击重置和点击选择使用 Kuikly 公共基础事件。

## 测试边界

`commonTest` 覆盖：

- 完整/初始/归一化视窗。
- 像素平移与焦点缩放。
- 正数、负数、柱零基线、显式轴范围与空数据刻度。
- 折线、柱、饼 DSL 数据与颜色。
- 笛卡尔最近系列命中与环形图角度/内孔命中。

Canvas 最终绘制由各平台 Kuikly Render 实现负责，仓库通过 Android、JS、iOS 与 OHOS 编译任务验证公共源码兼容性。

