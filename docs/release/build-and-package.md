# Build And Package

## Debug APK

本地验证使用 Debug APK。

构建命令：

```powershell
.\gradlew.bat clean :app:assembleDebug
```

产物路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Debug APK 仅用于个人研究和本地验证。

## Release APK

`0.1.0` 公开发布使用 Release APK。

构建命令：

```powershell
.\gradlew.bat clean :app:assembleRelease
```

产物路径：

```text
app/build/outputs/apk/release/app-release.apk
```

如果未提供 release 签名配置，构建会直接失败，不会产出可分发 APK。

签名配置通过本地 `local.properties` 或环境变量读取：

- `retrohall.release.storeFile`
- `retrohall.release.storePassword`
- `retrohall.release.keyAlias`
- `retrohall.release.keyPassword`

或对应环境变量：

- `RETROHALL_RELEASE_STORE_FILE`
- `RETROHALL_RELEASE_STORE_PASSWORD`
- `RETROHALL_RELEASE_KEY_ALIAS`
- `RETROHALL_RELEASE_KEY_PASSWORD`

Release APK 必须先确认：

- APK 不包含商业 ROM。
- APK 不包含私有 ROM。
- APK 不包含私有测试资源。
- APK 不包含签名文件。
- 许可证边界已检查。

## 签名

签名文件不得提交到 git。

禁止提交：

- `.jks`
- `.keystore`
- keystore 密码
- signing config 私有明文

签名配置通过本地 `local.properties` 或环境变量读取。

## 安装命令

连接设备后：

```powershell
adb install -r -d app\build\outputs\apk\debug\app-debug.apk
```

启动应用：

```powershell
adb shell am start -W -n com.richard.retrohall/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
```

查看日志：

```powershell
adb logcat | Select-String "RetroHall"
```

## 发布边界

Debug APK 只用于：

- 个人研究
- 本地验证
- 私有设备测试

Release APK 用于公开下载，但不得打入 ROM、私有资源或签名材料。
