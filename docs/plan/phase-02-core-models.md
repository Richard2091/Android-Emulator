# Phase 02: 核心模型和分层

## 阶段目标

建立后续 UI、数据、输入、模拟器共用的稳定模型边界。

## 主要文件

- `app/src/main/java/com/richard/retrohall/domain/game/LocalGame.kt`
- `app/src/main/java/com/richard/retrohall/domain/save/SaveSlot.kt`
- `app/src/main/java/com/richard/retrohall/domain/save/SaveState.kt`
- `app/src/main/java/com/richard/retrohall/domain/settings/UserSettings.kt`
- `app/src/main/java/com/richard/retrohall/domain/input/GameAction.kt`

## 实现任务

- 定义 `LocalGame`。
- 定义 `SaveSlot.Auto` 和 `SaveSlot.Manual(index)`。
- 手动即时存档槽只允许 1、2、3。
- 定义 `SaveState`。
- 定义 `UserSettings` 默认配置。
- 定义统一输入动作 `GameAction`。

## 自动验证命令

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## 阶段完成记录

- 已新增 `SaveSlotTest` 覆盖手动槽位边界。
- 已新增 `UserSettingsTest` 覆盖默认设置。

## 未解决风险

- Room、DataStore、输入映射和模拟器接口还未接入；这些属于后续阶段。
