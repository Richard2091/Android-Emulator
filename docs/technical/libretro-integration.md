# Libretro Integration

## 目标

在 Android App 内实现最小 libretro host，加载成熟 FC / NES libretro core，并通过 Kotlin 接口暴露模拟器能力。

第一版不改造 RetroArch 前端，不做多机种模拟，不做专业模拟器高级功能。

## Kotlin 侧接口

UI 只依赖以下接口：

```kotlin
interface EmulatorSession {
    fun load(game: LocalGame)
    fun start()
    fun pause()
    fun resume()
    fun reset()
    fun stop()
    fun sendInput(action: GameAction, pressed: Boolean)
    fun saveSram()
    fun saveState(slot: SaveSlot)
    fun loadState(slot: SaveSlot)
}
```

## 分层边界

Android / Kotlin 侧负责：

- 大厅和游戏页生命周期
- 文件路径
- 输入状态
- 设置读取
- 存档触发
- 错误提示

JNI / C++ 侧负责：

- 加载 libretro core 动态库
- 绑定 libretro API 函数
- 设置 environment、video、audio、input 回调
- 加载 ROM
- 驱动 `retro_run()`
- 调用 serialize / unserialize
- 释放 core

libretro core 负责：

- NES 模拟执行
- 生成视频帧
- 生成音频采样
- 读取输入状态
- 提供 SRAM 和即时存档能力

## Native 文件

Native 层文件：

```text
app/src/main/cpp/
├─ CMakeLists.txt
└─ libretro_host.cpp
```

Kotlin 门面：

```text
app/src/main/java/com/richard/retrohall/emulator/LibretroHost.kt
```

## NES core 选择

第一版默认候选 core：

- 名称：FCEUmm libretro Android core
- 用途：FC / NES 模拟
- ABI 优先级：`arm64-v8a`，后续补 `armeabi-v7a`、`x86_64`
- 许可证：实现前必须记录所下载 core 的实际许可证文本和来源链接

如果 Agent 改用其他 NES core，必须先更新本文档，写明：

- core 名称
- 来源
- 版本或 commit
- 许可证
- Android ABI 支持情况
- 选择原因

`libretro.h` 来源必须记录在 `docs/release/known-risks.md` 或新增许可证说明中，不能复制未知来源头文件。

## 私有资源路径

构建期私有资源推荐结构：

```text
D:\data\AI\Private\Android-Emulator\
├─ manifest.json
├─ roms/
│  └─ sample.nes
├─ covers/
│  └─ sample.png
└─ cores/
   └─ arm64-v8a/
      └─ fceumm_libretro_android.so
```

打包到 Debug / private build 后的 assets 结构：

```text
assets/retrohall_private/
├─ manifest.json
├─ roms/
├─ covers/
└─ cores/
   └─ arm64-v8a/
```

首次启动时复制到 App 私有目录：

```text
files/
├─ roms/
├─ covers/
└─ cores/
   └─ arm64-v8a/
```

core 从 App 私有目录加载，避免直接从 assets 中 `dlopen`。

## 最小 JNI 方法

`LibretroHost.kt` 至少暴露：

```kotlin
class LibretroHost {
    external fun nativeVersion(): String
    external fun loadCore(corePath: String): Boolean
    external fun unloadCore()
    external fun loadGame(romPath: String): Boolean
    external fun runFrame(): Boolean
    external fun pause()
    external fun reset()
    external fun serializeState(path: String): Boolean
    external fun unserializeState(path: String): Boolean
    external fun saveSram(path: String): Boolean
    external fun setInputState(actionName: String, pressed: Boolean)
}
```

## 视频输出

第一版可采用最小策略：

- C++ 接收 video callback。
- 将最新视频帧复制到 native buffer。
- Kotlin 侧通过安全接口拉取最新帧。
- UI 使用 Compose 可承载的 Android View 或 Bitmap 进行显示。
- host 至少支持 `RETRO_PIXEL_FORMAT_XRGB8888`。
- 如 core 请求 `RETRO_PIXEL_FORMAT_RGB565`，host 可以转换为 Android 可显示格式。

优化不属于第一阶段目标。第一阶段以可见画面和稳定生命周期为优先。

## 音频输出

第一版可采用最小策略：

- C++ 接收 audio callback。
- Kotlin 或 native 层用 Android AudioTrack 输出 PCM。
- 采样率从 `retro_get_system_av_info()` 获取。
- 音频格式按 libretro 常规 16-bit stereo PCM 处理。
- 音频失败不应导致 App 崩溃。
- 声音关闭时不写入 AudioTrack。

## 线程和生命周期

- 模拟循环运行在专用后台线程，不在 Compose UI 线程调用 `retro_run()`。
- `pause()` 停止继续调度新帧，但保留 core 状态。
- `resume()` 恢复帧循环。
- `stop()` 先保存 SRAM，再停止线程，再释放 core。
- Activity pause 时尽量暂停模拟器并保存 SRAM。
- Activity destroy 时释放 native 资源。

## 输入状态

Kotlin 侧维护当前按键状态：

```text
GameAction -> pressed Boolean
```

libretro input callback 查询当前状态，并映射到 NES joypad：

| GameAction | NES |
| --- | --- |
| Up | Up |
| Down | Down |
| Left | Left |
| Right | Right |
| NesA | A |
| NesB | B |
| Start | Start |
| Select | Select |

`Confirm`、`Back`、`Menu` 不直接传给 NES，除非当前场景明确转换为 NES 控制动作。

## SRAM 和即时存档

- SRAM 使用 libretro memory API 或 core 支持的保存机制。
- 即时存档使用 serialize / unserialize。
- 保存路径由 `SavePathResolver` 生成。
- 保存失败必须返回 false，Kotlin 侧显示短提示。

## 降级方案

如果 libretro 暂时无法完成，必须保留并使用 `FakeEmulatorSession`：

- 可从详情页开始游戏并进入游戏页面。
- 可接收输入。
- 可暂停、继续、重置、退出。
- 可模拟保存 SRAM。
- 可模拟保存和读取即时存档。
- 可显示当前状态和最近输入。

这样可以让大厅、输入、暂停菜单、设置、存档 UI 先完成。

## 错误处理

libretro 失败时：

- 不让 App 崩溃。
- 停止当前 `EmulatorSession`。
- 显示短提示：`游戏加载失败` 或 `模拟器运行失败`。
- 日志记录 gameId、corePath、romPath、错误阶段。
- 返回大厅或保留在可退出的错误页。
