# Phase 09: 存档和设置系统

## 阶段目标

定义存档路径规则，并为后续真实 SRAM / 即时存档接入提供稳定接口。

## 主要文件

- `app/src/main/java/com/richard/retrohall/domain/save/SavePathResolver.kt`
- `app/src/test/java/com/richard/retrohall/domain/save/SavePathResolverTest.kt`

## 实现任务

- SRAM 路径为 `files/saves/sram/{gameId}.srm`。
- 自动槽路径为 `files/saves/states/{gameId}/auto.state`。
- 手动槽路径为 `files/saves/states/{gameId}/manual-{index}.state`。
- 手动槽 index 只允许 1、2、3。

## 自动验证命令

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*SavePathResolverTest"
```

## 阶段完成记录

- 已完成路径解析纯逻辑和单元测试。
- 设置页已接入 DataStore，可读写基础设置。
- 游戏页已读取设置并应用虚拟按键显示、透明度、大小和画面比例状态。

## 未解决风险

- 尚未接入真实 libretro serialize / unserialize。
- 尚未接入 Room 存档索引。
- DataStore 已接入，但尚未补全 UI 自动化测试。
