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

第一版默认不要求公开 Release APK。

如果需要生成 Release APK，必须先确认：

- APK 不包含商业 ROM。
- APK 不包含私有 ROM。
- APK 不包含私有测试资源。
- APK 不包含私有签名配置。
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
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

启动应用：

```powershell
adb shell monkey -p com.richard.retrohall 1
```

查看日志：

```powershell
adb logcat | Select-String "RetroHall"
```

## 发布边界

第一版产物只用于：

- 个人研究
- 本地验证
- 私有设备测试

不得公开分发带 ROM 的 APK。
