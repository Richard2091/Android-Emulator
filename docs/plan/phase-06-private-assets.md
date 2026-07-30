# Phase 06: 私有资源注入

## 阶段目标

允许本地 Debug / private build 从 git 外部目录注入 ROM、封面和 libretro core，同时公开仓库不提交这些资源。

## 主要文件

- `app/build.gradle.kts`
- `private-assets.example.json`
- `docs/technical/private-resource-injection.md`

## 实现任务

- Debug source set 增加 `app/build/generated/retrohallPrivateAssets` 作为 assets 来源。
- Gradle task `prepareRetroHallPrivateAssets` 从 `local.properties` / Gradle property `retrohall.privateAssetsDir` 复制私有资源。
- 复制目标为 `app/build/generated/retrohallPrivateAssets/retrohall_private/`。
- 私有目录缺失时不阻塞普通 Debug 构建。

## 本机配置示例

```properties
retrohall.privateAssetsDir=D:\\data\\AI\\Private\\Android-Emulator
```

## 自动验证命令

```powershell
.\gradlew.bat :app:assembleDebug
```

## 阶段完成记录

- 已建立 Gradle copy task。
- 当前没有真实私有资源参与构建。
- 已新增 `PrivateAssetManifest` 和 `PrivateAssetInitializer`。
- App 启动时会尝试读取 `assets/retrohall_private/manifest.json`。
- manifest 缺失时继续使用假数据 seed，不阻塞公开 Debug 构建。

## 未解决风险

- 尚未用真实 NES ROM 验收。
- 公开构建中没有真实 ROM/core，因此只能验证 manifest 缺失降级路径和 manifest 解析逻辑。
