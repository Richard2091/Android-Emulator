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
