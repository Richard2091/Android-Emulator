# Task Breakdown

本文档用于指导 Agent 按任务顺序自动开发。每个任务完成后必须执行验证命令，再进入下一个任务。

## Task 001: 初始化 Android 工程

目标：创建最小可编译 Android 工程。

创建文件：

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/richard/retrohall/MainActivity.kt`

完成标准：

- 使用单 module：`app`
- 使用 Kotlin + Compose
- `applicationId` 为 `com.richard.retrohall`
- Activity 强制横屏
- App 启动后显示占位大厅

验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

预期结果：

- 命令成功退出。
- 存在 `app/build/outputs/apk/debug/app-debug.apk`。

禁止事项：

- 不加入 ROM。
- 不实现真实模拟器。
- 不创建多 module。

## Task 002: 建立核心包结构和模型

目标：创建项目基础分层和领域模型。

创建文件：

- `app/src/main/java/com/richard/retrohall/domain/game/LocalGame.kt`
- `app/src/main/java/com/richard/retrohall/domain/save/SaveSlot.kt`
- `app/src/main/java/com/richard/retrohall/domain/save/SaveState.kt`
- `app/src/main/java/com/richard/retrohall/domain/settings/UserSettings.kt`
- `app/src/main/java/com/richard/retrohall/domain/input/GameAction.kt`

模型要求：

- `LocalGame` 包含 `id`、`title`、`platform`、`category`、`coverPath`、`romPath`、`favorite`、`lastPlayedAt`、`totalPlayTimeMillis`
- `SaveSlot` 区分 `Auto` 和 `Manual(index)`
- `SaveState` 包含 `gameId`、`slot`、`filePath`、`createdAt`、`updatedAt`
- `UserSettings` 包含画面、声音、虚拟按键和自动存档配置
- `GameAction` 包含 `Up`、`Down`、`Left`、`Right`、`Confirm`、`Back`、`Menu`、`NesA`、`NesB`、`Start`、`Select`

验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Task 003: 实现大厅假数据

目标：提供可用于 UI 开发的本地假游戏列表。

创建文件：

- `app/src/main/java/com/richard/retrohall/data/assets/FakeGameCatalog.kt`

要求：

- 至少 6 个游戏条目
- 至少 3 个分类
- `platform` 固定为 `NES`
- `romPath` 使用私有目录占位路径，不指向公开 ROM

验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Task 003A: 实现私有资源注入规范

目标：让本地 Debug / private build 可以使用 git 外部的 ROM、封面和 libretro core，同时公开仓库不提交这些资源。

创建文件：

- `docs/technical/private-resource-injection.md`
- `private-assets.example.json`

实现要求：

- 私有目录默认使用 `D:\data\AI\Private\Android-Emulator\`
- manifest 示例只包含假路径和格式，不包含真实 ROM 路径
- Gradle 或脚本从私有目录复制资源到 build 生成目录或设备 App 私有目录
- `.gitignore` 屏蔽 `.nes`、`.srm`、`.state`、私有 core `.so`、private assets 目录

完成标准：

- 公开仓库没有 ROM。
- Agent 知道如何准备本地真实 NES 验收资源。
- 私有资源注入失败时，App 可以提示资源缺失，而不是展示 ROM 导入入口。

## Task 004: 实现游戏大厅 UI

目标：实现横屏大厅主界面。

创建文件：

- `app/src/main/java/com/richard/retrohall/ui/hall/HallScreen.kt`
- `app/src/main/java/com/richard/retrohall/ui/hall/HallViewModel.kt`
- `app/src/main/java/com/richard/retrohall/ui/components/GameCoverTile.kt`
- `app/src/main/java/com/richard/retrohall/ui/components/BottomActionBar.kt`

完成标准：

- 左侧分类列表
- 右侧游戏网格
- 顶部搜索和设置入口
- 底部操作提示
- 焦点高亮清晰
- 点击或确认键可选择游戏并进入详情页
- 搜索入口可进入本地标题过滤界面
- 游戏卡片可切换收藏
- 最近游玩分类可显示最近打开的游戏

验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

## Task 005: 实现导航骨架和游戏详情页

目标：大厅、游戏详情页、游戏页、设置页之间可以切换。

创建文件：

- `app/src/main/java/com/richard/retrohall/ui/RetroHallApp.kt`
- `app/src/main/java/com/richard/retrohall/ui/detail/GameDetailScreen.kt`
- `app/src/main/java/com/richard/retrohall/ui/game/GameScreen.kt`
- `app/src/main/java/com/richard/retrohall/ui/settings/SettingsScreen.kt`

完成标准：

- App 启动进入大厅
- 从大厅、最近、搜索和收藏确认游戏进入游戏详情页
- 详情页显示封面、标题、平台、分类、收藏状态、最近游玩时间和累计游玩时长
- 详情页默认聚焦开始游戏
- 从详情页开始游戏进入游戏页
- 详情页 Back 返回进入前的列表位置
- 游戏页返回大厅
- 大厅进入设置页
- 设置页返回大厅

验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

## Task 006: 实现统一输入映射

目标：把 Android KeyEvent 映射为 `GameAction`。

创建文件：

- `app/src/main/java/com/richard/retrohall/input/KeyEventMapper.kt`
- `app/src/test/java/com/richard/retrohall/input/KeyEventMapperTest.kt`

完成标准：

- DPAD 上下左右映射到方向动作
- DPAD_CENTER 和 ENTER 映射到 `Confirm`
- BACK 映射到 `Back`
- MENU 映射到 `Menu`
- BUTTON_A 映射到 `NesA`
- BUTTON_B 映射到 `NesB`
- BUTTON_START 映射到 `Start`
- BUTTON_SELECT 映射到 `Select`

验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*KeyEventMapperTest"
```

## Task 007: 实现虚拟按键

目标：手机横屏游戏页显示虚拟方向键和 NES 按键。

创建文件：

- `app/src/main/java/com/richard/retrohall/ui/game/VirtualPad.kt`

完成标准：

- 显示方向键
- 显示 A、B、Start、Select
- 按下和松开分别发出 pressed true/false
- 可通过 `UserSettings.virtualPadVisibility` 控制显示方式

验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

## Task 008: 实现 Room 数据层

目标：把游戏库和存档索引迁移到 Room。

创建文件：

- `app/src/main/java/com/richard/retrohall/data/db/RetroHallDatabase.kt`
- `app/src/main/java/com/richard/retrohall/data/db/LocalGameEntity.kt`
- `app/src/main/java/com/richard/retrohall/data/db/SaveStateEntity.kt`
- `app/src/main/java/com/richard/retrohall/data/db/LocalGameDao.kt`
- `app/src/main/java/com/richard/retrohall/data/db/SaveStateDao.kt`

完成标准：

- 可查询全部游戏
- 可按分类查询
- 可查询收藏
- 可查询最近游玩
- 可更新收藏状态
- 可更新最近游玩时间和累计时长
- 可保存和查询即时存档索引
- 从详情页开始游戏时更新最近游玩时间
- 退出游戏时更新累计游玩时长

验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Task 009: 实现 DataStore 设置

目标：保存和读取用户设置。

创建文件：

- `app/src/main/java/com/richard/retrohall/data/settings/UserSettingsStore.kt`

完成标准：

- 首次启动返回默认设置
- 设置变更写入 DataStore
- 设置流可被 UI 订阅

验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Task 010: 实现 FakeEmulatorSession

目标：在 libretro 前打通游戏流程。

创建文件：

- `app/src/main/java/com/richard/retrohall/emulator/EmulatorSession.kt`
- `app/src/main/java/com/richard/retrohall/emulator/FakeEmulatorSession.kt`
- `app/src/main/java/com/richard/retrohall/emulator/EmulatorState.kt`

完成标准：

- 支持 load/start/pause/resume/reset/stop
- 支持 sendInput
- 支持 saveSram
- 支持 saveState/loadState
- 游戏页可显示当前状态和最近输入

验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

## Task 011: 实现暂停菜单

目标：游戏中可打开暂停菜单并执行基础动作。

创建文件：

- `app/src/main/java/com/richard/retrohall/ui/pause/PauseMenu.kt`

完成标准：

- 包含继续游戏、保存即时存档、读取即时存档、重置游戏、画面设置、声音设置、退出到大厅
- 支持焦点操作
- 继续游戏调用 `resume`
- 重置游戏调用 `reset`
- 退出调用 `saveSram` 后 `stop`

验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

## Task 012: 实现 libretro JNI 骨架

目标：创建 C++ host 的最小骨架，不要求立即完成真实 NES 运行。

创建文件：

- `app/src/main/cpp/CMakeLists.txt`
- `app/src/main/cpp/libretro_host.cpp`
- `app/src/main/java/com/richard/retrohall/emulator/LibretroHost.kt`

完成标准：

- Kotlin 可加载 native library
- JNI 方法可返回 host 版本字符串
- 构建包含 C++ 编译

验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

## Task 013: 实现 libretro 最小运行

目标：加载 libretro core 和 `.nes` 文件，驱动模拟循环。

修改文件：

- `app/src/main/cpp/libretro_host.cpp`
- `app/src/main/java/com/richard/retrohall/emulator/LibretroHost.kt`

完成标准：

- 可加载 core 动态库
- core 默认候选为 FCEUmm libretro Android core；如果改用其他 NES core，必须记录名称、来源、版本、许可证和选择原因
- `libretro.h` 来源必须记录，并随许可证说明保存
- 支持 ABI 目录：`arm64-v8a` 优先，后续可补 `armeabi-v7a`、`x86_64`
- 可设置 environment/video/audio/input 回调
- 可加载 ROM
- 可调用 `retro_run`
- 可接收 XRGB8888 或 RGB565 视频帧
- 可接收 16-bit stereo PCM 音频采样
- 可释放 core
- 失败时返回错误结果，不让 App 崩溃

验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

## Task 014: 实现存档系统

目标：实现 SRAM 和即时存档路径、索引、真实 libretro 调用和失败处理。

创建文件：

- `app/src/main/java/com/richard/retrohall/domain/save/SavePathResolver.kt`
- `app/src/test/java/com/richard/retrohall/domain/save/SavePathResolverTest.kt`

完成标准：

- SRAM 路径为 `files/saves/sram/{gameId}.srm`
- 自动槽路径为 `files/saves/states/{gameId}/auto.state`
- 手动槽路径为 `files/saves/states/{gameId}/manual-{index}.state`
- 手动槽 index 只允许 1、2、3
- 退出游戏时调用真实 `saveSram`
- 保存即时存档时调用真实 `serializeState`
- 读取即时存档时调用真实 `unserializeState`
- 保存失败不更新 `SaveStateEntity`
- 读档失败不改变当前游戏状态

验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*SavePathResolverTest"
```

## Task 015: 实现设置页

目标：实现基础设置和高级设置 UI。

修改文件：

- `app/src/main/java/com/richard/retrohall/ui/settings/SettingsScreen.kt`

完成标准：

- 可调整画面比例
- 可调整滤镜开关
- 可调整声音开关和音量
- 可调整虚拟按键显示方式、透明度、大小
- 可调整虚拟按键位置
- 可调整手柄连接时是否隐藏虚拟按键
- 设置变更写入 DataStore

验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
```

## Task 016: 最终验收

目标：验证第一版闭环。

验证命令：

```powershell
.\gradlew.bat clean :app:testDebugUnitTest :app:assembleDebug
```

手动验证：

- 启动 App 直接进入大厅
- 方向键可移动焦点
- 确认键可进入游戏详情页
- 详情页开始游戏可进入游戏页
- 至少一个通过私有资源注入准备的 NES 游戏可用真实 libretro core 启动
- 游戏页可输出真实视频画面和音频
- 游戏页可打开暂停菜单
- 可保存和读取即时存档
- 可退出回大厅
- 设置可保存并生效
- 项目中没有提交 ROM 文件
- App 中没有本地 ROM 导入口
- App 中没有游戏源入口
