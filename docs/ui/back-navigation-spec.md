# 返回导航设计规范

## 目标

为软件定义一套完整、可预测的系统返回键（Back）行为，覆盖所有页面、弹窗、覆盖层与侧栏切换场景，让安卓端用户始终能以"从哪来、回哪去"的方式操作：

- 从列表进入详情，返回回到来源列表（游戏库 / 最近 / 收藏）。
- 从详情进入游玩，返回回到详情。
- 任何弹窗 / 覆盖层打开时，返回先收起弹窗，而不是退出页面或软件。
- 大厅内从最近、收藏切回游戏库，再按返回才进入"再次返回退出"确认。
- 设置页返回直接回到游戏库。

## 现状分析

应用是单 Activity + Jetpack Compose，未使用 Navigation 组件，页面切换通过顶层状态 `route`（`RetroHallApp.kt:210` 的 `AppRoute`）手动完成。现状问题：

1. 没有注册任何 `BackHandler` / `onBackPressedDispatcher`，系统返回键在详情、存档管理、游玩、设置等页面都会**直接退出 Activity**，不满足逐级返回。
2. 详情页没有记录来源列表，返回无法回到"最近"或"收藏"对应的列表。
3. 游戏内设置面板 `GameSettingsOverlay`（`RetroHallApp.kt:2581`）是普通 `Box` 覆盖层，返回键会穿透直接退出。
4. 大厅筛选下拉 `ToolbarSelect`（`RetroHallApp.kt:1274`）使用 `Popup`，未设置 `dismissOnBackPress`，返回键不会收起下拉。
5. 大厅搜索栏 `searchVisible`（`RetroHallApp.kt:678`）展开时返回键不会收起搜索栏。
6. 三个 `Dialog`（删除确认 `RetroHallApp.kt:1631`、截图查看器 `RetroHallApp.kt:1845`、清理缓存确认 `RetroHallApp.kt:3403`）默认 `dismissOnBackPress = true`，返回键关闭弹窗的行为已正确，需保持。

## 返回处理模型

采用"层叠式返回栈 + 顶层状态机"两层方案：

- **覆盖层优先**：弹窗、覆盖层、展开的搜索栏在任何时候都优先消费返回键（收起自身），不向下传播。`Dialog` 由系统天然消费；`Popup`、游戏内设置面板、搜索栏需显式注册返回处理。
- **页面级返回**：由顶层统一调度，按当前 `route` 与记录的状态决定去向，形成一条逻辑返回栈：
  `游戏库 → 详情 → 存档管理` 以及 `游戏库 → 详情 → 游玩`。
- **大厅 section 回退**：大厅内部的"最近 / 收藏 / 游戏库"切换视为同页 section 切换，返回在 section 层面逐级回退。
- **退出确认**：只有停留在"游戏库"时，返回才触发二次确认退出。

## 状态定义

新增顶层状态 `detailSourceSection`（`RetroHallAppContent` 内，与 `selectedHallSection` 并列）：

```kotlin
// 记录进入详情的来源列表，用于详情页返回时还原列表
var detailSourceSection by rememberSaveable { mutableStateOf("游戏库") }
```

- 打开详情（`onOpenGame`）时写入来源：`detailSourceSection = selectedHallSection`。
- 显式点击侧栏 / 顶栏切换（`onSelectLibrary` / `onSelectRecent` / `onSelectFavorites`）不改写该状态。
- 从存档管理返回详情、从游玩返回详情时，`detailSourceSection` 保持不变。

## 返回行为定义

约定：`返回` 指按下系统返回键（含手柄 B / ESC 映射，见 `KeyEventMapper.kt:16`）。

### 按页面

| 当前场景 | 返回行为 |
| --- | --- |
| 大厅 · 游戏库 | 首次返回：提示"再按一次返回键退出软件"；3 秒内再次返回：退出软件 |
| 大厅 · 最近 / 收藏 | 切回"游戏库"section，不清空筛选与滚动位置 |
| 大厅 · 搜索栏展开 | 收起搜索栏（不改变 section、不触发退出确认） |
| 详情 | 回到来源列表，即 `detailSourceSection` 对应的 section（游戏库 / 最近 / 收藏），保留该列表的筛选条件和滚动位置 |
| 存档管理 | 回到对应游戏的详情页 |
| 游玩（运行中） | 暂停游戏，画面显示"已暂停"水印 |
| 游玩（已暂停） | 退出游戏：保存 SRAM、停止会话、记录游玩时长，回到对应游戏的详情页 |
| 游玩 · 游戏内设置面板 | 收起设置面板，停留在当前暂停状态 |
| 设置 | 回到游戏库 section |

### 按弹窗 / 覆盖层

| 弹窗 | 返回行为 |
| --- | --- |
| 详情 · 删除确认（Dialog） | 关闭弹窗（系统默认，保持） |
| 详情 · 截图查看器（Dialog） | 关闭查看器（已显式 `dismissOnBackPress = true`，保持） |
| 设置 · 清理缓存确认（Dialog） | 关闭弹窗（系统默认，保持） |
| 大厅 · 筛选下拉（Popup） | 收起下拉（需新增返回处理） |
| 游玩 · 游戏内设置面板（Box 覆盖层） | 收起面板（需新增返回处理） |
| 大厅 · 搜索栏 | 收起搜索栏（需新增返回处理） |

### 返回链路总览

```
退出软件 ←（二次确认）← 游戏库
                        ↑
                   最近 / 收藏（切回游戏库）
                        ↑
                       详情 ←── 存档管理
                        ↑
                       游玩 ←── 游戏内设置面板 / 暂停水印
设置 ──返回──→ 游戏库
```

## 实现要点

### 1. 页面级返回状态机（顶层）

在 `RetroHallAppContent` 的 `when (route)` 外层注册 `BackHandler`，按当前 `route` 调度：

```kotlin
BackHandler {
    when (route) {
        is AppRoute.Hall -> handleHallBack()      // 搜索栏 → 非游戏库 section → 二次退出
        is AppRoute.Detail -> openHall(detailSourceSection)
        is AppRoute.SaveManager -> route = AppRoute.Detail(current.game)
        is AppRoute.Game -> handleGameBack()      // 由 GameScreen 内部 BackHandler 处理，见下
        AppRoute.Settings -> openHall("游戏库")
    }
}
```

`handleHallBack` 需访问大厅内部状态（搜索栏展开、筛选下拉），建议改为在 `HallScreen` 内部注册 `BackHandler`（见第 2 点），顶层 `BackHandler` 仅处理页面级去向。

### 2. 覆盖层优先：在各 Screen 内部注册

利用 Compose `BackHandler` 后注册先处理的 LIFO 语义，让"覆盖层 → 页面"自然形成返回优先级：

- `HallScreen` 内部注册，处理顺序：筛选下拉展开 → 搜索栏展开 → section 回退 / 二次退出确认。
- `GameScreen` 内部注册，处理顺序：游戏内设置面板 → 暂停 → 退出游玩。注册时返回行为不落到顶层 Hall 分支。
- 三个 `Dialog` 无需改动（系统已消费返回键，且不会穿透到下层 `BackHandler`）。

### 3. 二次退出确认

在 `RetroHallAppContent` 增加状态：

```kotlin
var exitConfirmTick by remember { mutableStateOf(0L) }   // 首次返回的时间戳
```

- 首次返回：记录时间戳并显示 `TopToast` "再按一次返回键退出软件"。
- 3 秒内再次返回：调用 `context.finish()`（或 `activity.onBackPressedDispatcher` 默认行为）退出。
- 超过 3 秒：重置时间戳，重新开始确认。

### 4. 保留列表状态

来源列表的筛选与滚动位置由现有 `rememberSaveable` 的 `HallFilters` 与 `LazyGridState` 天然保留（`RetroHallApp.kt:255` 起），返回还原 section 后无需额外恢复。

## 边界情况

- **实体键盘 / 手柄**：`KeyEventMapper` 将 `KEYCODE_BACK` / `KEYCODE_ESCAPE` 映射为 `GameAction.Back`，但该映射目前未被消费链路使用。游玩页内应避免让返回键同时作为游戏操作键，保持"返回 = 系统导航"，后续接入按键映射时需在文档中同步此约束。
- **启动游玩失败**：`onStart` 失败时不切换 `route`，停留在详情页，返回行为不受影响。
- **详情页内显式切换列表**：详情页侧栏点击"最近 / 收藏"直接打开对应列表，属于显式跳转，后续返回仍回到来源 section（仅返回路径依赖 `detailSourceSection`）。
- **全屏沉浸模式**：系统导航栏为瞬态隐藏，返回手势 / 返回键触发时应用应保持沉浸，不因返回操作重新显示系统栏。

## 验收清单

- [ ] 游戏库 → 详情 → 返回：回到游戏库，筛选与滚动位置不变。
- [ ] 最近 / 收藏 → 详情 → 返回：回到最近 / 收藏列表，而非游戏库。
- [ ] 详情 → 游玩 → 返回：暂停 → 再返回：退出游玩回到详情。
- [ ] 游戏内设置面板打开时返回：收起面板，不退出。
- [ ] 筛选下拉打开时返回：收起下拉，不退出。
- [ ] 搜索栏展开时返回：收起搜索栏。
- [ ] 删除确认 / 截图查看器 / 清理缓存确认打开时返回：仅关闭弹窗。
- [ ] 最近 / 收藏页返回：切回游戏库。
- [ ] 设置页返回：回到游戏库。
- [ ] 游戏库页首次返回：提示"再按一次返回键退出软件"，3 秒内再按退出，超时后重新计数。