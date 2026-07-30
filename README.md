# Retro Hall

一个面向 Android 的本地游戏库与模拟器交互原型。项目使用 Kotlin、Jetpack Compose、Room 和 DataStore 构建游戏库、收藏、游玩记录、即时存档与设置管理等基础能力，并预留 libretro 原生宿主接口。

当前版本重点验证应用架构和本地数据流程；真实模拟器核心接入仍处于后续阶段。

## 已实现

- 游戏大厅、分类筛选、收藏和最近游玩记录
- 游戏详情、游玩会话与累计时长记录
- 即时存档、读档、重置和暂停菜单的交互流程
- Room 本地数据持久化与 DataStore 设置管理
- 虚拟按键、画面比例、音量和显示选项
- libretro 原生宿主接口与 JNI/CMake 工程骨架

## 技术栈

- Kotlin、Jetpack Compose、Material 3
- Room、DataStore、KSP
- CMake、JNI、libretro 宿主接口
- JUnit、Robolectric

## 本地运行

需要 Android Studio、Android SDK 和 JDK 17。

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

## 私有资源约定

本仓库不包含 ROM、BIOS、模拟器核心二进制、存档、密钥或其他私有资源。

- 使用 `private-assets.example.json` 了解可选的私有资源清单格式。
- 私有资源文件应通过 `retrohall.privateAssetsDir` 指向仓库外目录。
- 请仅使用你有权使用的游戏文件与模拟器核心。

## 当前边界

界面和业务流程目前通过 `FakeEmulatorSession` 验证；`LibretroHost` 已提供原生接口定义，但尚未完成真实核心加载、音视频渲染和输入循环集成。
