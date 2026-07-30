# Build Version Lock

- JDK: Temurin 17.0.19+10
- Android Gradle Plugin: 8.13.2
- Gradle Wrapper: 8.13
- Kotlin: 2.2.21
- Compose BOM: 2026.05.01
- compileSdk: 36
- targetSdk: 36
- minSdk: 23
- Android SDK Build-Tools: 36.0.0
- Android SDK Platform-Tools: 37.0.0
- NDK: 29.0.14033849 rc4
- CMake: 4.1.2
- 记录日期: 2026-06-07
- 本地 JDK 路径: `D:\data\AI\Tools\jdk-17`
- 本地 Android SDK 路径: `D:\data\AI\Tools\AndroidSdk`
- 网络代理: `127.0.0.1:7897`

## 验证命令

```powershell
& "D:\data\AI\Tools\jdk-17\bin\java.exe" -version
$env:JAVA_HOME="D:\data\AI\Tools\jdk-17"
$env:ANDROID_HOME="D:\data\AI\Tools\AndroidSdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=$env:ANDROID_HOME --list_installed
```

## 验证结果摘要

- JDK 输出 `openjdk version "17.0.19" 2026-04-21`。
- SDK 已安装 `platforms;android-36`、`build-tools;36.0.0`、`platform-tools`、`ndk;29.0.14033849`、`cmake;4.1.2`。
