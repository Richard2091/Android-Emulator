# Known Risks

## ROM 版权风险

风险：商业 ROM 或社区 ROM 被提交到公开仓库或打入公开 APK。

控制方式：

- `.gitignore` 屏蔽常见 ROM 和存档后缀。
- ROM 放在 git 外部私有目录。
- 提交前检查暂存文件。
- 打包前检查 APK 内容。

## libretro core 许可证风险

风险：不同 libretro core 使用不同许可证，可能影响分发方式。

控制方式：

- 记录所用 core 名称、版本、来源、许可证。
- 记录 `libretro.h` 来源、版本或 commit、许可证。
- 默认候选 core 为 FCEUmm libretro Android core；实际采用前必须核对许可证文本和 Android ABI 支持。
- 第一版只做个人研究和本地验证。
- 公开分发前重新审查许可证。

### 已采用 core 记录（2026-08）

| core | 来源 | 许可证 | 校验 |
| --- | --- | --- | --- |
| FCEUmm (`fceumm_libretro_android.so`) | libretro buildbot nightly，`https://buildbot.libretro.com/nightly/android/latest/{abi}/`，`fceumm_libretro_android.so.zip` | GPL-2.0（libretro 分支） | 已用于魂斗罗实机验证，4 ABI 均可用 |
| Mesen (`mesen_libretro_android.so`) | 同上（备用/回退） | GPL-3.0 | 仅 arm64-v8a 下载，未实机验证 |

- `libretro.h` 来自 libretro 官方头（`app/src/main/cpp/libretro.h`），对应 RETRO_API_VERSION。
- 分发前需核对 buildbot 具体 commit 的许可证文本。

## instrumented 测试需分开运行

风险：`LibretroCoreInstrumentedTest` 与 `GameplayFlowTest` 共享进程内的 native 宿主全局状态
（`g_host`），并发运行时帧循环与直接 native 调用会互相干扰，导致音频断言偶发失败。

控制方式：

- 两个测试分别通过 `-Pandroid.testInstrumentationRunnerArguments.class=...` 单独运行。
- 二者单独运行均稳定通过。
- 后续可改为不同 `runnerBuilder` 进程隔离。

## 输入注入可靠性风险（调试用）

风险：`adb shell input text` 在模拟器上偶发附加尾随空格，导致搜索词不匹配。

控制方式：

- 调试时优先用 `adb shell input keyevent <KEYCODE 序列>` 输入 ASCII。
- 中文搜索在自动化测试中用 Compose `performTextInput`（支持 Unicode）。
- 私有资源标题当前为中文"魂斗罗"。

## NDK 编译复杂度

风险：CMake、NDK、ABI、动态库加载路径导致构建或运行失败。

控制方式：

- 先实现 `FakeEmulatorSession`。
- 再实现 JNI 版本字符串。
- 再加载 core。
- 再加载 ROM。
- 最后接视频、音频、存档。

## Android TV 焦点风险

风险：Compose 页面触摸可用，但遥控器焦点不可用。

控制方式：

- 所有主要控件必须可聚焦。
- 每个页面手动验证方向键。
- 暂停菜单和设置页必须能只靠遥控器完成。

## 手柄键值差异

风险：不同手柄 A/B、Start/Select 键值不同。

控制方式：

- 第一版支持 Android 常见 `KEYCODE_BUTTON_*`。
- 记录未识别键值日志。
- 后续根据真实设备补映射。

## 音频延迟风险

风险：AudioTrack 缓冲配置不当导致延迟或爆音。

控制方式：

- 第一版以稳定输出为目标。
- 声音失败不影响画面和输入。
- 设置中提供声音开关。

## 性能风险

风险：视频帧复制到 Bitmap 的方式性能不足。

控制方式：

- 第一版先保证可见画面。
- 如果帧率不足，再切换到 SurfaceView/OpenGL 路径。
- 不在大厅阶段提前优化渲染。

## 工程收口风险

风险：`RetroHallApp.kt` 仍然偏大，若继续在单文件内堆页面、控制器和辅助控件，后续维护会迅速失控。

控制方式：

- 继续按页面和职责拆分 `ui/` 下的文件。
- 应用启动编排、数据预热和业务同步不要再放回 Compose 入口。
- 设计文档、README 和代码结构变更保持同步，避免再次漂移。
