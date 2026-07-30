# Phase 03: 大厅、搜索、收藏、最近和详情页

## 阶段目标

实现可浏览的横屏游戏大厅，并让游戏集合入口统一进入详情页。

## 主要文件

- `app/src/main/java/com/richard/retrohall/data/assets/FakeGameCatalog.kt`
- `app/src/main/java/com/richard/retrohall/ui/RetroHallApp.kt`

## 实现任务

- 提供至少 6 个 NES 假游戏。
- 提供“全部、收藏、最近游玩”和普通分类。
- 大厅显示分类、游戏网格、搜索入口、设置入口、底部操作提示。
- 游戏卡片点击进入详情页，不直接启动游戏。
- 详情页展示封面占位、标题、平台、分类、收藏状态和累计游玩时长。
- 详情页“开始游戏”进入游戏页。

## 自动验证命令

```powershell
.\gradlew.bat :app:assembleDebug
```

## 阶段完成记录

- 当前为 Compose MVP 实现，搜索按钮暂时在“搜索/清除搜索”之间切换本地标题过滤。
- 当前收藏和最近游玩仍为假数据/内存数据，持久化属于后续 Room 阶段。

## 未解决风险

- 尚未实现完整焦点恢复。
- 尚未实现真实搜索输入框。
- 尚未连接 Room 数据层。
