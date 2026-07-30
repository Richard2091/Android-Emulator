# Phase 05: 本地数据层

## 阶段目标

建立 Room 和 DataStore 的最小可编译数据层。

## 主要文件

- `app/src/main/java/com/richard/retrohall/data/db/`
- `app/src/main/java/com/richard/retrohall/data/settings/UserSettingsStore.kt`

## 实现任务

- Room 定义 `LocalGameEntity` 和 `SaveStateEntity`。
- DAO 支持全部、分类、收藏、最近、收藏更新和游玩统计更新。
- DataStore 支持基础 `UserSettings` 读写。
- Repository 接入 UI，用 Room Flow 驱动大厅游戏列表。

## 自动验证命令

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

## 阶段完成记录

- 已新增 `GameRepository`，封装游戏 seed、收藏、最近游玩和累计时长。
- 已新增 `RetroHallDependencies`，集中创建 Room、Repository 和 DataStore。
- UI 已改为从 Repository Flow 读取游戏列表。
- 首次启动数据库为空时，使用 `FakeGameCatalog` 写入 Room。

## 未解决风险

- 已有 Repository 的 Robolectric 数据库测试，但尚未覆盖所有 DAO 分支。
- 尚未引入 ViewModel，当前 Compose 直接订阅 Repository。
