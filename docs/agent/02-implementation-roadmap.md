# Implementation Roadmap

## Phase 1: 初始化 Android 工程

目标：在当前仓库创建可编译的单 module Android 工程。

完成标准：

- 存在 `settings.gradle.kts`
- 存在根 `build.gradle.kts`
- 存在 `gradle/libs.versions.toml`
- 存在 `app/build.gradle.kts`
- 存在 `app/src/main/AndroidManifest.xml`
- 存在 `MainActivity`
- App 启动后显示横屏占位页面
- `.\gradlew.bat :app:assembleDebug` 成功

## Phase 2: 核心模型和分层

目标：建立稳定的 package 结构和核心模型。

完成标准：

- 根 package 为 `com.richard.retrohall`
- 建立 `ui`、`domain`、`data`、`emulator`、`input` 包
- 定义 `LocalGame`
- 定义 `SaveSlot`
- 定义 `SaveState`
- 定义 `UserSettings`
- 定义 `GameAction`
- UI 层不直接依赖 Room、DataStore、JNI

## Phase 3: 游戏大厅和详情页 MVP

目标：实现可浏览的横屏游戏大厅，并让所有游戏集合入口统一进入游戏详情页。

完成标准：

- App 启动直接进入大厅
- 大厅左侧显示分类
- 大厅右侧显示游戏封面网格
- 顶部显示搜索入口和设置入口
- 底部显示操作提示
- 使用假数据展示至少 6 个游戏
- 方向键可以移动焦点
- 确认键可以从游戏库、最近、搜索和收藏进入游戏详情页
- 游戏详情页默认聚焦开始游戏
- 返回键可以从游戏详情页回到进入前的列表和焦点位置

## Phase 4: 输入系统

目标：统一触摸、键盘、遥控器、手柄输入。

完成标准：

- `KeyEventMapper` 可将 Android KeyEvent 映射为 `GameAction`
- 虚拟按键可发出 NES 控制动作
- 大厅、游戏页、暂停菜单使用同一套动作分发
- 单元测试覆盖主要按键映射

## Phase 5: 本地数据层

目标：用 Room 和 DataStore 管理游戏库、收藏、最近游玩和设置。

完成标准：

- Room 中存在 `LocalGameEntity` 和 `SaveStateEntity`
- DAO 可查询游戏、分类、收藏、最近游玩
- DataStore 可读写 `UserSettings`
- Repository 对 UI 隐藏 Room/DataStore 细节
- 单元测试覆盖默认设置、收藏切换、最近游玩更新

## Phase 5.5: 私有资源注入

目标：建立公开仓库不提交 ROM、但本地 Debug / private build 可运行真实 NES 游戏的资源准备机制。

完成标准：

- 定义私有资源目录结构
- 定义私有 manifest 格式
- Gradle 或脚本能把私有资源复制到构建生成目录或设备 App 私有目录
- App 可读取注入后的 ROM、封面和 libretro core
- git 状态不包含 `.nes`、`.so` core、`.srm`、`.state`、私有封面资源

## Phase 6: 假模拟器闭环

目标：先不依赖 libretro，打通游戏流程。

完成标准：

- `FakeEmulatorSession` 实现 `EmulatorSession`
- 可从详情页开始游戏并进入游戏页
- 可接收输入动作
- 可暂停、继续、重置、退出
- 可模拟 SRAM 保存、即时存档、读档
- 暂停菜单完整可操作

## Phase 7: libretro 最小集成

目标：实现最小 libretro host，加载一个 NES core 和一个本地 `.nes` 测试文件。

完成标准：

- JNI/C++ 可加载 libretro core 动态库
- 可调用 core 初始化和释放
- 可加载 `.nes` ROM
- 可驱动 `retro_run()`
- 可接收视频帧
- 可接收音频采样
- 可通过 input callback 读取当前输入状态
- 可从私有资源注入目录定位 core 和 ROM
- 出错时可返回大厅且 App 不崩溃

## Phase 8: 存档系统

目标：实现 SRAM 和即时存档。

完成标准：

- SRAM 退出时自动保存
- 自动即时存档槽固定 1 个
- 手动即时存档槽固定 3 个
- 真实模拟器接入后调用 `saveSram`
- 保存即时存档调用 `serializeState`
- 读取即时存档调用 `unserializeState`
- 保存失败会提示
- 读档失败会提示
- 存档路径不依赖公开外部存储

## Phase 9: 设置系统

目标：实现基础设置和高级设置。

完成标准：

- 画面比例可保存并生效
- 滤镜开关可保存并生效
- 声音开关和音量可保存并生效
- 虚拟按键显示、透明度、大小可保存并生效
- 虚拟按键位置可保存并生效
- 手柄连接时隐藏虚拟按键的策略可保存

## Phase 10: 验收和打包

目标：完成可本地安装验证的 Debug APK。

完成标准：

- `.\gradlew.bat clean :app:assembleDebug` 成功
- 单元测试通过
- 手动 QA 清单通过
- 产物不包含公开禁止的 ROM 数据
- 没有本地 ROM 导入口
- 没有游戏源入口
