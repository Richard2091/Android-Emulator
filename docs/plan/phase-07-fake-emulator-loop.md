# Phase 07: FakeEmulatorSession 闭环

## 阶段目标

在真实 libretro 前打通详情页开始游戏、游戏页显示、退出大厅的基础流程。

## 主要文件

- `app/src/main/java/com/richard/retrohall/emulator/EmulatorSession.kt`
- `app/src/main/java/com/richard/retrohall/emulator/FakeEmulatorSession.kt`
- `app/src/main/java/com/richard/retrohall/emulator/EmulatorState.kt`
- `app/src/main/java/com/richard/retrohall/ui/RetroHallApp.kt`

## 实现任务

- `FakeEmulatorSession` 支持 load/start/pause/resume/reset/stop。
- 游戏页显示当前游戏名和模拟器状态。
- 退出游戏时调用 `saveSram()` 和 `stop()`。

## 自动验证命令

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

## 阶段完成记录

- 当前 Fake 模拟器流程已接入 UI。
- 已新增暂停菜单 MVP，包含继续、保存、读档、重置、设置、退出。
- 游戏页会根据设置展示虚拟按键状态。

## 未解决风险

- Fake 模拟器不能作为第一版最终验收依据。
- 暂停菜单尚未完成遥控器焦点专项测试。
- 后续仍需真实 libretro。
