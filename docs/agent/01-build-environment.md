# Build Environment

## 目标环境

项目默认在 Windows 本地开发，使用 Android Studio 或 Gradle Wrapper 构建。

推荐基线：

- JDK：17
- Android Gradle Plugin：使用 Android Studio 当前稳定模板生成的版本
- Gradle：使用 Android Gradle Plugin 兼容的 Gradle Wrapper
- Kotlin：使用 Android Gradle Plugin 兼容的稳定版本
- compileSdk：优先使用本机已安装的最新稳定 Android SDK
- targetSdk：与 compileSdk 保持一致
- minSdk：23
- NDK：使用 Android Studio SDK Manager 安装的固定 NDK 版本，并写入 `app/build.gradle.kts`
- UI：Jetpack Compose

初始化工程时必须把实际版本写入：

- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`

后续 Agent 不得在没有构建失败证据的情况下随意升级或降级版本。

初始化时先探测本机环境，再锁定版本。不要在文档中临时猜版本。

## 推荐 Gradle 文件

项目使用 Kotlin DSL：

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

## 常用命令

在项目根目录执行：

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

如果没有连接 Android 设备或模拟器，`connectedDebugAndroidTest` 可以暂时跳过，但必须在结果说明中写明原因。

## Android SDK 要求

必须安装：

- Android SDK Platform，优先使用本机已安装的最新稳定版本
- Android SDK Build-Tools
- Android SDK Platform-Tools
- Android Emulator
- Android NDK
- CMake

Agent 可以用以下命令检查本机 SDK：

```powershell
Get-ChildItem $env:ANDROID_HOME
Get-ChildItem "$env:ANDROID_HOME\platforms"
Get-ChildItem "$env:ANDROID_HOME\ndk"
```

如果 `ANDROID_HOME` 不存在，检查常见路径：

```powershell
Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk"
```

初始化工程前记录实际采用版本：

```powershell
java -version
Get-ChildItem "$env:ANDROID_HOME\platforms" | Select-Object Name
Get-ChildItem "$env:ANDROID_HOME\ndk" | Select-Object Name
```

版本锁定记录写入本文件或新增 `docs/agent/build-version-lock.md`：

```markdown
# Build Version Lock

- JDK:
- Android Gradle Plugin:
- Gradle Wrapper:
- Kotlin:
- compileSdk:
- targetSdk:
- minSdk:
- NDK:
- CMake:
- 记录日期:
- 验证命令:
- 验证结果:
```

## 成功标准

环境文档对应的工程初始化完成后，必须满足：

- `.\gradlew.bat :app:assembleDebug` 成功。
- `app/build/outputs/apk/debug/app-debug.apk` 存在。
- `.\gradlew.bat :app:testDebugUnitTest` 可以执行。
- NDK 和 CMake 配置没有阻塞普通 Kotlin/Compose 构建。

## 版本变更规则

只有以下情况允许调整版本：

- 官方模板生成的版本与本机 SDK 不兼容。
- 构建失败明确指向版本兼容问题。
- Android Studio 或 Gradle 输出明确要求升级或降级。

调整后必须记录：

- 调整前版本。
- 调整后版本。
- 触发原因。
- 验证命令结果。
