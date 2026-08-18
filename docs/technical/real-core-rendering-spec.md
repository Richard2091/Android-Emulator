# 真实核心渲染方案（FCEUmm）

## 1. 目标

用真实 libretro core（默认 FCEUmm）替换 `FakeEmulatorSession` 兜底，打通"能玩"闭环：
核心文件加载 → 视频帧显示 → 音频播放 → 帧率与显示同步 → 超宽屏布局。

本方案只涉及渲染/输出/核心相关改动，不改变大厅、存档、设置的数据结构。

## 2. 核心选型

| 决策 | 结论 | 理由 |
| --- | --- | --- |
| 默认核心 | FCEUmm | libretro 生态 NES 事实默认，mapper 支持最全，兼容性最好，维护活跃，4 ABI 齐全 |
| 兼容性回退 | Mesen | 精度最高，作为"某游戏 FCEUmm 跑不动"时的备选 |
| 不采用 | QuickNES | 快但兼容差，不适合做主核心 |
| 体积 | ~1-3MB/core | 对 APK 无感；ROM/封面是私有资源不走公开 APK |

### 核心抽象：CoreDescriptor

将 `CorePathResolver` 中写死的候选文件名列表提升为一等公民，为多核心/多平台预留：

```kotlin
data class CoreDescriptor(
    val id: String,                 // "fceumm"
    val displayName: String,        // "FCEUmm"
    val platforms: Set<String>,     // setOf("FC", "NES")
    val candidateSoNames: List<String>, // listOf("fceumm_libretro_android.so")
    val fallbackCoreId: String? = null, // 兼容性回退链
)
```

- 解析顺序保持现有逻辑：ABI 优先 → core 候选列表顺序 → 文件存在且非空。
- 新增私有资源 manifest 侧：`cores` 两级结构（platform → abi → path）已具备，无需改动，只需补充 `coreId` 映射。
- 每个核心必须记录许可证文本与来源链接（沿用 `docs/technical/libretro-integration.md` 的要求）。

## 3. 分层边界

| 层 | 职责 |
| --- | --- |
| libretro core | NES 模拟、生成帧、生成 PCM、输入查询、存档 |
| C++ host（`libretro_host.cpp`） | 帧缓冲、PCM ring buffer、输入掩码、serialize/SRAM |
| JNI 门面（`LibretroHost.kt`） | 暴露 loadCore/runFrame/getFrame/drainAudio 等 |
| `LibretroEmulatorSession` | 帧循环、音频线程、状态机、错误处理 |
| UI（`GameScreen`） | Bitmap 显示、等比缩放、虚拟按键布局、设置项 |

现有 `EmulatorSession` 接口保持不变，UI 不感知核心差异。

## 4. 视频渲染链路

### 4.1 Native 侧

- `video_refresh_callback(data, width, height, pitch)` 改为真实实现：
  - 按当前 `pixel_format`（`SET_PIXEL_FORMAT` 请求）拷贝到 host 帧缓冲。
  - 支持 `XRGB8888` 与 `RGB565` 两种格式；`0RGB1555` 若 core 请求则转换到 XRGB8888。
  - 帧尺寸随游戏可能切换（width/height 动态更新）。
- 新增 JNI 拉帧接口（host 缓存最近一帧，避免回调中做耗时代码）：

```kotlin
data class NativeFrame(
    val width: Int,
    val height: Int,
    val pixelFormat: Int,   // 枚举: XRGB8888 / RGB565
    val buffer: ByteArray,  // 已按 pitch 展平
)
external fun pollFrame(): NativeFrame?
```

- 失败策略：native 返回 null / 空帧时 Kotlin 显示最后成功帧（不黑屏闪断）。

### 4.2 Kotlin 侧

- 帧循环中每 `runFrame()` 成功后立即 `pollFrame()`，用 `runCatching` 兜底。
- `RGB565 → ARGB_8888` 转换在 native 或 Kotlin 完成，256×240 量级开销可忽略。
- `Bitmap.createBitmap(w, h, ARGB_8888)` → `asImageBitmap()` → Compose `Image`，用 `remember` + 帧计数触发重绘。
- 显示走 Compose 而非 SurfaceView（无额外 View 层，保持现有 UI 结构）。

### 4.3 缩放与比例

| 选项 | 行为 |
| --- | --- |
| Original | 按核心报告 aspect_ratio（NES 通常 4:3）等比缩放，黑边填充 |
| FourThree | 固定 4:3 等比 |
| Fullscreen | 全屏拉伸（默认不推荐，标注"变形"） |
| Overscan | 显示隐藏的边缘行（约 ±8px），由 FCEUmm core option 控制 |

- 所有等比模式不变形、不裁剪、不强制拉伸：中央画面按目标区域 fit，余量黑边。
- 超宽屏（21:9 等）不扩充游戏视野（NES 帧恒为 256×240），两侧黑边区域留给虚拟按键与系统 UI。

### 4.4 超宽屏布局

- 画面区域按 `Box(contentAlignment = Center)` 放置，画面 fit 后余量自动形成左右黑边。
- 虚拟按键从文字占位升级为可点击按钮，水平排布在画面两侧黑边区；画面满宽（Fullscreen）时虚拟按键浮层叠加。
- 现有 `virtualPadScale / Opacity` 设置继续生效。

## 5. 音频渲染链路

### 5.1 Native 侧

- `audio_sample_batch_callback(const int16_t *data, size_t frames)` 写入固定容量 ring buffer（约 4096 帧 stereo）。
- 单采样回调 `audio_sample_callback` 同样入 ring buffer（按帧打包）。
- 采样率/声道数从 `retro_get_system_av_info()` 的 `timing.sample_rate` 与 `timing.fps` 获取，随 core 报告配置。

### 5.2 Kotlin 侧

- `AudioTrack` 使用 `MODE_STREAM`，配置取 av_info（FCEUmm 典型 44100Hz / 16bit / stereo）。
- 专用音频线程循环 `drainAudio(buffer)` → `AudioTrack.write()`，静音（`audioEnabled=false`）时不写。
- 下溢策略：缓冲不足时写静音帧补齐（避免 AudioTrack 空转异常），不做阻塞等待。

### 5.3 同步策略（第一版）

- 第一版以视频帧循环为时钟源（`runFrame` → 帧间隔 = `1 / av_info.timing.fps`，如 60.0988Hz → 约 16.64ms），音频 ring buffer 兜底下溢。
- 后续升级为 audio 驱动主循环：仅当 AudioTrack 消费位置落后时才推进 `retro_run()`，音画严格同步。

## 6. 帧率与显示同步

### 6.1 原则

- 模拟帧率恒定：NES 硬件时钟固定 ~60.0988Hz（NTSC），不可改。改变它会破坏游戏速度。
- 用户可调的是**显示同步**与**游戏速度**，不碰模拟帧率。

### 6.2 设置项

| 设置项 | 取值 | 实现 |
| --- | --- | --- |
| 显示刷新率 | 自动 / 60 / 120 / 144 / 165 | 见 6.3 |
| 游戏速度 | 0.5x / 1x / 1.5x / 2x | 帧间隔 = `(1/fps) / speed`，音频按同比例变速输出 |
| 跳帧（高级） | 开 / 关 | 强制 N 帧跳 1 帧，省电场景，标注会掉帧 |

### 6.3 显示刷新率同步

- 120Hz：60.0988 × 2 的近似整数倍，每模拟帧由显示端重复显示 2 次，仅需保证帧节奏均匀，近乎免费。
- 144 / 165Hz：非整数倍，用 vsync（Choreographer 或 SurfaceFlinger）+ 帧间隔抖动对齐，RetroArch 式 frame-time 同步；画面平滑但本质仍 60fps 模拟。
- 自动：按当前 display 刷新率选择策略。

### 6.4 与 run-ahead 的关系（二期）

- 高刷低延迟体验的正解是 run-ahead（提前计算 N 帧降输入延迟），与帧率独立，列入二期亮点，不在本方案范围。

## 7. 涉及文件

| 文件 | 改动 |
| --- | --- |
| `app/src/main/cpp/libretro_host.cpp` | video 回调拷贝帧缓冲；audio 回调写 ring buffer；新增 `pollFrame` / `drainAudio` / `setFrameInterval` JNI |
| `app/src/main/java/.../emulator/LibretroHost.kt` | 新增 native 方法与 `NativeFrame` 数据类 |
| `app/src/main/java/.../emulator/CorePathResolver.kt` | 引入 `CoreDescriptor` 列表替换硬编码候选名 |
| `app/src/main/java/.../emulator/LibretroEmulatorSession.kt` | 帧循环用 av_info 帧间隔；帧回调上抛；音频线程启动/停止；游戏速度换算 |
| `app/src/main/java/.../emulator/EmulatorSessionFactory.kt` | 按 CoreDescriptor 解析 + fallback 链，提示语带 core 名 |
| `app/src/main/java/.../ui/RetroHallApp.kt`（GameScreen） | Bitmap 帧显示、等比 fit、两侧虚拟按键、Overscan 提示 |
| `app/src/main/java/.../data/settings/UserSettings.kt` | 新增 `displayRefreshRate`、`gameSpeed`、`overscanEnabled`、`frameSkip` |
| `app/src/main/java/.../data/settings/UserSettingsStore.kt` | 新设置项持久化 |
| `docs/technical/libretro-integration.md` | 更新 FCEUmm 实际来源/许可证记录 |
| 私有资源（不入库） | `cores/{abi}/fceumm_libretro_android.so` + manifest 更新 |

## 8. 落地顺序

| 阶段 | 内容 | 出口标准 |
| --- | --- | --- |
| A | 获取 FCEUmm `.so`，更新 manifest，dlopen 冒烟 | logcat 出现 "core loaded" |
| B | 视频帧链路（native 缓冲 → JNI → Bitmap → Compose） | 游戏画面可见且比例正确 |
| C | 音频链路（ring buffer → AudioTrack） | 有声音且无爆音/下溢 |
| D | 帧率同步 + 超宽屏布局 + 新设置项 | 120Hz 平滑，21:9 两侧虚拟按键可用 |
| E | 存档/读档/重置用真实 core 验收 | master-plan 最终验收项通过 |

## 9. 风险与对策

| 风险 | 对策 |
| --- | --- |
| FCEUmm 与 NDK/API 版本不兼容导致 dlopen 失败 | Phase A 先做 dlopen 冒烟，失败即换版本/换核心（Nestopia/Mesen） |
| core 请求 `0RGB1555` 或未知 pixel format | host 统一转换到 XRGB8888，不直接透传 |
| 音频下溢/爆音 | ring buffer 静音补齐 + 后续升级 audio 驱动主循环 |
| 帧节奏不均匀（sleep 不精确） | 用 av_info.fps 计算帧间隔 + SystemClock 校准，替代固定 16ms sleep |
| 超宽屏画面拉伸 | 所有非 Fullscreen 模式强制 fit + 黑边，不提供拉伸插值 |
