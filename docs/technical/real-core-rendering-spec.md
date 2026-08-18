# 真实核心渲染方案（FCEUmm）

## 1. 目标

用真实 libretro core（默认 FCEUmm）替换 `FakeEmulatorSession` 兜底，打通"能玩"闭环：
核心文件加载 → 视频帧显示 → 音频播放 → 帧率与显示同步 → 超宽屏布局。

本方案只涉及渲染/输出/核心相关改动，不改变大厅、存档、设置的数据结构。

## 1.1 实施状态（2026-08 已验证）

| 能力 | 状态 | 实证 |
| --- | --- | --- |
| FCEUmm 核心加载 | ✅ | `LibretroCoreInstrumentedTest`：dlopen + loadGame 成功 |
| 魂斗罗 ROM 运行 | ✅ | 同上，180/180 帧运行成功，帧非黑 |
| 视频帧渲染到屏幕 | ✅ | 截图实证：中央区域蓝(25%)/绿(4%)/红棕(10%)，两帧 56% 像素变化（游戏动态运行） |
| 音频输出 | ✅ | 核心链路测试 drainAudio 输出 16384 bytes |
| UI 全流程（搜索→详情→开始→核心启动） | ✅ | `GameplayFlowTest` 单跑通过，核心加载日志实证 |
| 游戏速度设置（0.5x~2x） | ✅ | 设置面板 + AudioTrack.setPlaybackRate |
| 私有游戏直接"开始" | ✅ | 修复 `RomDownloadManager.isDownloaded` 识别本地注入 ROM |

实测环境：Pixel 6 API 35 模拟器（x86_64）、FCEUmm nightly core、魂斗罗(J)。

## 1.2 游玩界面按钮验证（2026-08 二次验证）

| 按钮 | 验证方式 | 结果 |
| --- | --- | --- |
| 方向键（上下左右） | native 输入日志 | ✅ 四向均触发 |
| A / B / X / Y | native 输入日志（X→NesA, Y→NesB） | ✅ |
| 开始 / 选择 | native 输入日志 + 暂停/恢复画面实证 | ✅ 修复瞬时按键未被采样的问题 |
| 暂停 / 继续 | 帧发布停止/恢复 | ✅ |
| 设置 | 面板渲染日志 + 截图 | ✅ |
| 继续游戏 | 帧恢复 + 面板关闭 | ✅ |
| 保存即时存档 | 存档文件生成 | ✅ |
| 读取即时存档 | 读档后正常运行 | ✅ |
| 重置游戏 | 修复后帧恢复 | ✅ 修复 reset 未恢复帧循环的 bug |
| 游戏速度 0.5x~2x | 帧发布频率变化 | ✅ |
| 退出游戏 | 帧停止 + 回详情页 | ✅ |

修复项：
- 瞬时按键（开始/选择）改为保持按下 120ms（`tapKey`），否则帧循环采样不到按下状态。
- `LibretroEmulatorSession.reset()` 补充 `running=true` 与帧循环/音频循环重启，修复暂停后重置游戏卡死。
- 像素格式转换统一输出 `R,G,B,A` 字节序（`Bitmap.copyPixelsFromBuffer` 对 ARGB_8888 期望的字节序）：
  - RGB565 按标准布局 `bit11-15=R, bit5-10=G, bit0-4=B` 转换（此前 R/B 位提取写反导致人物肤色显示为蓝色）。
  - 0RGB1555 按标准布局 `bit14-10=R, bit9-5=G, bit4-0=B` 转换。
  - XRGB8888 从 core 的 `B,G,R,X`（小端）重排为 `R,G,B,A`（此前直接 memcpy 保持 B,G,R 会再被 Bitmap 按 R,G,B 读取导致 R/B 交换）。

颜色链路验证（`LibretroColorTest` + `BitmapByteOrderTest`）：
- `Bitmap.copyPixelsFromBuffer` 对 ARGB_8888 期望 R,G,B,A 字节序（已实测确认）。
- 修复前：魂斗罗标题画面红色元素（KONAMI 标志/角色皮肤）显示为蓝色；修复后红/黄/白系正常。

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
