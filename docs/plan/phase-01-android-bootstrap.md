# Phase 01: Android 工程初始化

## 阶段目标

创建可编译的单 module Android 工程，启动后显示横屏大厅占位页。

## 前置条件

- JDK 17 可用。
- Android SDK Platform 36 可用。
- Android SDK Build-Tools 36.0.0 可用。
- Android Gradle Plugin 与 Gradle Wrapper 版本锁定。

## 主要文件

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/richard/retrohall/MainActivity.kt`
- `app/src/main/java/com/richard/retrohall/ui/RetroHallApp.kt`
- `.gitignore`

## 实现任务

- 使用单 module：`app`。
- 使用 Kotlin + Jetpack Compose。
- `applicationId` 固定为 `com.richard.retrohall`。
- Activity 强制横屏。
- 启动后显示横屏大厅占位页。
- `.gitignore` 屏蔽构建产物、`local.properties`、ROM、存档、私有 core 和签名文件。

## 自动验证命令

```powershell
.\gradlew.bat :app:assembleDebug
```

## 手动验收清单

- APK 可生成。
- 首屏不是登录页、引导页或空白页。
- Manifest 中 Activity 固定横屏。

## 阶段完成记录

- 使用便携 JDK：`D:\data\AI\Tools\jdk-17`。
- 使用便携 Android SDK：`D:\data\AI\Tools\AndroidSdk`。
- 系统代理为 `127.0.0.1:7897`，网络下载命令使用该代理。

## 未解决风险

- 本机未安装 Android Studio；当前依赖命令行 SDK 和 Gradle Wrapper 构建。
- 尚未连接 Android 设备或模拟器，无法执行安装和 connected UI 测试。
