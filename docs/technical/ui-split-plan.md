# UI 模块拆解规划

## 目标

消除多对话并行开发时的文件冲突热点，把 `ui/RetroHallApp.kt`（3744 行、110+ 顶层声明）按职责拆成独立文件，使每个页面/组件/导航逻辑拥有独立的文件所有权边界。**纯搬移重构，不改变运行行为，不引入新架构（不引入 ViewModel / DI 框架）。**

## 现状分析

### 文件规模分布（app/src/main/java/com/richard/retrohall）

| 规模 | 文件 |
| --- | --- |
| 3744 行 | `ui/RetroHallApp.kt`（唯一冲突热点） |
| 300-400 行 | `emulator/LibretroEmulatorSession.kt` |
| 100-239 行 | `data/game/*`（4 个客户端 + 仓库）、`data/save/SaveStateRepository.kt`、`emulator/LibretroHost.kt` |
| <100 行 | 其余 data / domain / emulator / input / 装配类 |

除 `ui/RetroHallApp.kt` 外，其余模块已按职责拆分且规模健康，**无需改动**。测试（`src/test`、`androidTest`）均未引用 UI 内部符号，拆分不破坏测试。

### 冲突根源

`ui/` 包目前只有一个文件，容纳了路由、返回导航、全部 5 个页面、全部通用组件、工具函数和主题常量。任何 UI 相关改动（返回逻辑、搜索框、布局调整）都必须修改同一个文件，并行对话必然冲突。

## 拆分原则

1. **纯搬移**：按函数完整移动，不改签名、不改逻辑、不改文案。
2. **可见性**：跨文件使用的符号由 `private` 改为 `internal`（顶层函数在模块内可见）；仅单文件内部使用的保持 `private`。不使用 `public`。
3. **状态集中**：`route`、`selectedHallSection`、`detailSourceSection`、各 `HallFilters`、`LazyGridState`、退出确认状态全部保留在 `RetroHallAppContent`（组合根），通过参数传入各 Screen——与现有模式一致，避免引入 ViewModel。
4. **逐步验证**：每完成一批拆分，运行一次 `:app:compileDebugKotlin`；全部完成后运行 `:app:assembleDebug` 与现有单元测试 `.\gradlew.bat test`。
5. **一次性提交**：拆分作为一个独立 commit（例如 `refactor: split ui package into modules`），避免 diff 与其他功能混在一起。

## 目标结构

```
app/src/main/java/com/richard/retrohall/ui/
├─ RetroHallApp.kt          ← 保留：RetroHallApp、RetroHallAppContent（状态+路由装配+页面分发）、AppRoute
├─ theme.kt                 ← 颜色常量（UiBg..UiLine）、NavIcon、HallIcon、StarIcon、starPath、
│                              gamePalette、gamePaletteForIndex、formatPlayTime、formatFileSize、formatTimestamp
├─ navigation.kt            ← 返回导航：handleHallBack、BackHandler 装配辅助、detailSourceSection 相关
│                              （注：BackHandler 调用点与状态仍在 RetroHallAppContent，此处放可独立测试的辅助函数）
├─ components/
│  ├─ AppShell.kt           ← AppShell、CompactTopNav、CompactNavItem、HallSidebar、SidebarItem、
│  │                          SidebarLogo、RetroConsoleLogo、PageTitle、EmptyPanel、EmptyPanelFrame
│  ├─ Controls.kt           ← HallActionButton、HallToggle、HallSlider、SegmentedChoice、MetaLine、
│  │                          PillButton、GameSegButton、GamePanelButton
│  ├─ Dialogs.kt            ← 通用弹窗模板：HallConfirmDialog、HallDialogButton
│  │                          （页面内的弹窗实例留在各自页面文件，见"横切主题"约定）
│  └─ Feedback.kt           ← TopToast、AppBackground、CoverLoadingIndicator
├─ hall/
│  └─ HallScreen.kt         ← HallScreen、HallFilters、GameSort、HallFilterBar、ToolbarDivider、
│                              ToolbarSelect、SearchBox、GameTile
├─ detail/
│  └─ DetailScreen.kt       ← DetailScreen、DetailLowerSection、ScreenshotPlaceholder、ScreenshotCard、
│                              ScreenshotViewer、DetailInfoItem、CoverArt、CoverState、rememberCoverBitmap
├─ save/
│  └─ SaveManagerScreen.kt  ← SaveManagerScreen、SaveStateRow、SaveStateSlot.displayName
├─ game/
│  └─ GameScreen.kt         ← GameScreen、VirtualPadOverlay、PadSlideGroup、GameFrame、stateLabel、
│                              PauseWatermark、JoyStickPad、drawZone、AbxyPad、RoundKey、
│                              GameSettingsOverlay、gameViewportWidth、GameSettingRow、DefaultGameFrameAspectRatio
└─ settings/
   └─ SettingsScreen.kt     ← SettingsScreen、SettingsVisual/Audio/Control/Game/SystemSection、
                               SettingsSection、SettingRow、GameSpeedOptions、gameSpeedLabel、
                               aspectRatioLabel、controlModeLabel、isSearchRevealKey
```

函数归属以当前 `RetroHallApp.kt` 为准；拆分时若同一函数依赖多个页面共享，归入 `components/` 或 `theme.kt`。

## 执行步骤

前置条件：**工作区干净**（所有并行对话已提交各自改动，`git status` 无未提交文件）。

1. **theme.kt**：搬移颜色常量、工具函数、通用图标组件 → 编译验证。
2. **components/**：搬移 AppShell 系、Controls 系、Feedback 系 → 编译验证。
3. **各页面**：settings → save → detail → hall → game（从小到大，最后处理最大的 GameScreen）→ 每步编译验证。
4. **navigation.kt**：从 `RetroHallAppContent` 提取返回导航辅助逻辑（BackHandler 调用点留在组合根）→ 编译验证。
5. **组合根瘦身**：`RetroHallApp.kt` 仅保留 `RetroHallApp`、`RetroHallAppContent`、`AppRoute`。
6. **全量验证**：`.\gradlew.bat test` + `.\gradlew.bat assembleDebug`。
7. **提交**：独立 commit，仅含本次拆分的文件。

## 多对话协作约定（拆分完成后）

| 关注点 | 文件所有权 | 对应对话/工作 |
| --- | --- | --- |
| 返回导航 | `ui/navigation.kt` + 组合根中的 BackHandler 调用区 | 返回逻辑 |
| 游戏库/搜索框 | `ui/hall/HallScreen.kt` | 搜索框/大厅改动 |
| 详情页 | `ui/detail/DetailScreen.kt` | 截图/详情改动 |
| 游玩页/虚拟按键 | `ui/game/GameScreen.kt` | 模拟器交互改动 |
| 设置页 | `ui/settings/SettingsScreen.kt` | 设置改动 |
| 通用组件 | `ui/components/*` | 需先与相关方确认，改动影响所有页面 |

### 横切主题与共享接口规则（防多会话冲突的关键）

按文件隔离能消除"改不同文件"的写冲突，但三类改动会横切多个文件，必须额外约定：

1. **横切主题的归属**：一个会话若想"统一改弹窗 / 统一改按钮 / 统一改 Toast 样式"，不得直接改各页面文件，只能改共享模板文件（弹窗→`components/Dialogs.kt`、按钮→`components/Controls.kt`、提示→`components/Feedback.kt`）并让页面实例通过参数适配；页面内弹窗实例的**具体内容**（文案、按钮、布局）归该页面文件拥有者。若某个弹窗外观需要全局统一，先抽到 `Dialogs.kt` 成模板，再让各页面传参复用。
2. **共享组件串行化**：`components/*` 与 `theme.kt` 是公共文件，同一时间只允许一个会话修改；改动需同步相关页面会话，合并后再由其他会话续接。建议共享组件按子文件再细分（appshell / controls / dialogs / feedback），进一步缩小互斥粒度。
3. **组合根是公共接口层**：`RetroHallApp.kt`（组合根）中的 `AppRoute`、各 Screen 的函数签名、`RetroHallAppContent` 的装配调用点是所有页面的公共接口。Screen 签名变更时，由该页面文件拥有者**同步修改组合根中的调用点并一次性提交**，减少持有组合根的窗口；禁止单方面改签名不同步调用点导致他人无法编译。

约定：任何对话不得直接编辑他人拥有的文件；若需修改共享组件或 `AppRoute`/组合根签名，先在对话间同步。跨文件状态一律通过 `RetroHallAppContent` 的参数显式传递，禁止引入模块级可变全局状态。

## 其他模块边界（现状已健康，维持不动）

- `data/`：按 assets / bootstrap / cache / db / game / save / settings 拆分，边界清晰。
- `domain/`：按 game / input / save / settings 拆分，纯模型与规则。
- `emulator/`：`LibretroEmulatorSession.kt` 390 行可接受；若后续超过 ~600 行再考虑拆内部类。
- `input/`、`RetroHallDependencies.kt`：规模小，维持现状。

## 风险与对策

| 风险 | 对策 |
| --- | --- |
| `private → internal` 泄漏实现细节 | 统一用 `internal`，禁止 `public`；拆完后可复查暴露面 |
| 搬运遗漏导致编译错误 | 每批拆分后立即编译验证，小步提交式推进 |
| 与并行对话的改动冲突 | 严格遵守"工作区干净后再拆"前置条件；拆分过程独占执行 |
| 拆分 diff 过大难以审查 | 单独 commit；commit message 标注"纯搬移，行为不变" |
| 回归风险 | 拆分前后行为不变是原则；以现有单测 + assembleDebug + 真机冒烟（返回导航、搜索框、详情、游玩暂停/退出）为验收 |
