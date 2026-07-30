# Phase 08: libretro 最小集成

## 阶段目标

建立 JNI / C++ host 骨架，为后续真实 libretro core 加载做准备。

## 主要文件

- `app/src/main/java/com/richard/retrohall/emulator/LibretroHost.kt`
- `app/src/main/cpp/CMakeLists.txt`
- `app/src/main/cpp/libretro_host.cpp`

## 实现任务

- Kotlin 可加载 native library。
- JNI 方法 `nativeVersion()` 返回 host 版本字符串。
- 构建包含 C++ 编译。
- Kotlin 暴露 core/ROM/load/run/save/input 最小接口。
- C++ 当前提供安全骨架：路径不存在返回失败，不让 App 崩溃。

## 自动验证命令

```powershell
.\gradlew.bat :app:assembleDebug
```

## 阶段完成记录

- 已扩展 `LibretroHost` 的最小 JNI 方法签名。
- 已实现 `loadCore`、`loadGame`、`runFrame`、`serializeState`、`unserializeState`、`saveSram` 的安全骨架。
- 当前尚未绑定真实 libretro API。

## 未解决风险

- 尚未接入真实 `libretro.h`。
- 尚未通过 `dlopen` 加载真实 core 动态库。
- 尚未实现 video/audio/input callback。
- 尚未记录实际 core 来源和许可证。
