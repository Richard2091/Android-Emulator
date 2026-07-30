# Phase 04: 统一输入系统

## 阶段目标

把 Android KeyEvent 映射为内部统一动作 `GameAction`。

## 主要文件

- `app/src/main/java/com/richard/retrohall/input/KeyEventMapper.kt`
- `app/src/test/java/com/richard/retrohall/input/KeyEventMapperTest.kt`

## 实现任务

- DPAD 上下左右映射为方向动作。
- DPAD_CENTER / ENTER 映射为 `Confirm`。
- BACK / ESCAPE 映射为 `Back`。
- MENU / BUTTON_MODE 映射为 `Menu`。
- 常见手柄按钮映射为 NES 动作或确认动作。

## 自动验证命令

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*KeyEventMapperTest"
```

## 阶段完成记录

- 已覆盖导航键、系统键和常见手柄按键单元测试。

## 未解决风险

- 当前 UI 尚未全局接管 `onKeyEvent` 分发。
- 不同手柄的 A/B 键值可能需要真实设备补映射。
