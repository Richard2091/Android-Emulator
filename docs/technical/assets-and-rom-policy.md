# Assets And ROM Policy

## 目标

保护项目边界，避免把商业 ROM、私有 ROM、测试 ROM 或不适合公开分发的资源提交到 git。

## 可以提交到 git 的内容

- Kotlin / C++ / Gradle 源码
- 文档
- UI 概念图
- 自制占位封面
- 不包含商业内容的测试占位数据
- 空目录占位文件 `.gitkeep`

## 不可以提交到 git 的内容

- 商业 ROM
- 私有 ROM
- 从社区仓库获取的 ROM 数据
- 带 ROM 的 APK
- 私有签名文件
- 私有 keystore 密码
- 本地测试资源压缩包

## 推荐私有目录

本地测试资源放在仓库外：

```text
D:\data\AI\Private\Android-Emulator\
├─ roms/
├─ cores/
└─ covers/
```

Agent 如果需要引用本地 ROM 或 core，应通过本地配置文件读取，但配置文件不得提交真实私有路径。

私有目录应包含：

```text
D:\data\AI\Private\Android-Emulator\
├─ manifest.json
├─ roms/
├─ cores/
│  └─ arm64-v8a/
└─ covers/
```

`manifest.json` 示例结构：

```json
{
  "games": [
    {
      "id": "sample-nes",
      "title": "Sample NES",
      "platform": "NES",
      "category": "Action",
      "rom": "roms/sample.nes",
      "cover": "covers/sample.png"
    }
  ],
  "cores": {
    "NES": {
      "arm64-v8a": "cores/arm64-v8a/fceumm_libretro_android.so"
    }
  }
}
```

## `.gitignore` 必须包含

```text
*.nes
*.sfc
*.smc
*.gb
*.gbc
*.gba
*.srm
*.state
*.sav
*.apk
*.aab
*.keystore
*.jks
local.properties
private-assets/
roms/
cores/
```

## 内置资源策略

第一版文档允许“内置游戏库”，但公开仓库不能提交 ROM。实现时采用两层策略：

1. 开发时使用假游戏索引和占位资源，让 UI 和流程可运行。
2. 本地验证时从 git 外部私有目录复制 ROM、封面和 core 到 Debug / private build 的 generated assets，或通过开发脚本推送到设备 App 私有目录。
3. App 用户界面不暴露 ROM 导入入口，资源准备只发生在开发期或私有安装流程。

推荐构建期注入：

```text
private directory
-> Gradle copy task
-> app/build/generated/retrohallPrivateAssets/
-> debug assets
-> App 首次启动复制到 files/
```

如果私有资源缺失：

- Debug 构建可以继续。
- App 显示假数据或资源缺失提示。
- 最终真实 NES 验收不能通过。

## 打包策略

Debug APK 可用于本地验证，但不得作为公开分发产物上传。

Release APK 如果未来需要生成，必须先确认：

- 不包含 ROM。
- 不包含私有 core 路径。
- 不包含本地测试资源。
- 签名文件不在 git 中。

## Agent 提交前检查

提交前执行：

```powershell
git status --short
git diff --name-only --cached
```

如果暂存区出现以下后缀，必须停止并移除：

- `.nes`
- `.srm`
- `.state`
- `.apk`
- `.aab`
- `.keystore`
- `.jks`
