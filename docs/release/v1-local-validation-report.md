# V1 本地验收报告

## 验证范围

本报告记录 Android 复古游戏大厅第一版的本地构建、单元测试和手动验收状态。

## 当前结论

- 已完成 Android Debug 构建链路。
- 已完成 `clean assembleDebug connectedDebugAndroidTest`、应用安装和启动验证。
- `connectedDebugAndroidTest` 已在 `codex_pixel_6_api35(AVD) - 15` 上通过，4/4 通过。
- 已完成 Room / DataStore / FakeEmulatorSession / JNI / 私有资源注入 / 真实 libretro core 的本地冒烟验证。
- 真实 NES ROM 验收在私有资源注入环境中完成本地验证；公开仓库不分发 ROM/core，也不把它们当成可公开复用的交付物。
- 未执行部署，且本项目计划不包含部署步骤。

## 已执行验证命令

```powershell
.\gradlew.bat clean assembleDebug connectedDebugAndroidTest
adb install -r -d app\build\outputs\apk\debug\app-debug.apk
adb shell am start -W -n com.richard.retrohall/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
```

## connected 测试

`connectedDebugAndroidTest` 已在 `codex_pixel_6_api35(AVD) - 15` 上执行，4 个测试全部通过。
