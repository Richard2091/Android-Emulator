# Retro Hall

一个面向 Android 的本地游戏库与模拟器交互原型。项目使用 Kotlin、Jetpack Compose、Room 和 DataStore 构建游戏库、收藏、游玩记录、即时存档与设置管理等基础能力，并集成 libretro 原生宿主接口。

当前版本已经打通本地数据流程、真实 libretro 核心降级链路与主要交互闭环；后续重点是继续收口分包、拆分大文件、同步文档和清理仓库杂物。

## 已实现

- 游戏大厅、分类筛选、收藏和最近游玩记录
- 游戏详情、游玩会话与累计时长记录
- 即时存档、读档、重置和暂停菜单的交互流程
- Room 本地数据持久化与 DataStore 设置管理
- 虚拟按键、画面比例、音量和显示选项
- libretro 原生宿主接口、JNI/CMake 工程骨架与真实核心兜底链路

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

## 在线游戏库清单

生成器会把本地 `game.json` 中的多来源图片合并起来，并继续尝试 `libretro-thumbnails` 的 `Named_Snaps`、`Named_Boxarts`、`Named_Logos` 等兜底资源；重复图片会按内容去重，不会在输出目录里重复落盘。
当前 Android 工程仍默认忽略 `*.nes` 等私有资源；实际 ROM 资源仓库需要单独确认版权、仓库权限和 `.gitignore` 策略。
生成器会把封面下载并缓存到静态输出目录，再把 `coverUrl` 写成可直接访问的静态地址，方便本地预览和校验。

## 当前边界

界面和业务流程保留 `FakeEmulatorSession` 作为兜底；真实核心路径已经通过 `LibretroHost`、`LibretroEmulatorSession` 和 instrumented tests 验证，但 UI 文件还偏大，仍需要继续拆分和整理。
