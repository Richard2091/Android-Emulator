# Phase 10: 本地 QA 和交付整理

## 阶段目标

完成本地可验证 Debug APK，不包含任何部署内容。

## 自动验证命令

```powershell
.\gradlew.bat clean :app:testDebugUnitTest :app:assembleDebug
```

有设备或模拟器时再执行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 本地验收清单

- App 启动后进入横屏大厅。
- 大厅可浏览分类、收藏、最近、搜索结果。
- 任意游戏入口都先进入详情页。
- 详情页开始游戏进入游戏页。
- 暂停菜单可继续、重置、保存、读档、退出。
- 设置保存并生效。
- 无本地 ROM 导入口、无游戏源入口、无账号、无云同步。
- 公开仓库不包含 ROM、私有 core、私有封面、存档或签名文件。

## 当前未完成项

- 尚未接入真实 libretro core。
- 尚未通过真实 NES ROM 验收。
- 尚未执行 connected Android 测试。
