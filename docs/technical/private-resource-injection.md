# Private Resource Injection

## 目标

解决第一版的资源边界：

- App 体验上是“内置游戏库”，用户打开即可选择游戏。
- 公开仓库不提交 ROM、私有封面、私有 core。
- 公开 APK 可以发布带 libretro core 的产物，但不发布带 ROM 的产物。
- 本地 Debug / private build 可以注入私有资源，完成真实 NES 验收。

## 私有目录

默认私有目录：

```text
D:\data\AI\Private\Android-Emulator\
```

目录结构：

```text
D:\data\AI\Private\Android-Emulator\
├─ manifest.json
├─ roms/
│  └─ sample.nes
├─ covers/
│  └─ sample.png
└─ cores/
   └─ arm64-v8a/
      └─ fceumm_libretro_android.so
```

## manifest 格式

公开仓库只提交 `private-assets.example.json`，不提交真实 `manifest.json`。

示例：

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

## 构建期注入

推荐流程：

```text
私有目录
-> Gradle copy task
-> app/build/generated/retrohallPrivateAssets/
-> debug assets
-> App 首次启动复制到 files/
```

Debug source set 可把生成目录作为 assets；Release source set 只挂载 release core assets：

```kotlin
android {
    sourceSets {
        getByName("debug") {
            assets.srcDir(layout.buildDirectory.dir("generated/retrohallPrivateAssets"))
        }
        getByName("release") {
            assets.srcDir(layout.buildDirectory.dir("generated/retrohallReleaseCoreAssets"))
        }
    }
}
```

Gradle copy task 的职责：

- 读取 `local.properties` 中的 `retrohall.privateAssetsDir`
- 检查 `manifest.json` 是否存在
- Debug 构建复制 `manifest.json`、`roms/`、`covers/`、`cores/`
- Release 构建写入 `games: []` 的 manifest，并只复制 `cores/**/*.so`
- Debug 目标目录为 `app/build/generated/retrohallPrivateAssets/retrohall_private/`
- Release 目标目录为 `app/build/generated/retrohallReleaseCoreAssets/retrohall_private/`
- 私有目录缺失时不阻止普通 Debug 构建，但记录 warning

## 运行期初始化

App 首次启动：

```text
读取 assets/retrohall_private/manifest.json
-> 复制 ROM 到 files/roms/
-> 复制 cover 到 files/covers/
-> 复制 core 到 files/cores/{abi}/
-> 写入 Room 游戏索引
-> 大厅展示真实游戏
```

如果 `manifest.json` 不存在：

- App 可以展示假数据或资源缺失提示。
- 不显示本地 ROM 导入入口。
- 不显示游戏源入口。
- 最终真实 NES 验收不能通过。

## local.properties

本机配置：

```properties
retrohall.privateAssetsDir=D:\\data\\AI\\Private\\Android-Emulator
```

`local.properties` 不提交 git。

## 验收标准

- 公开仓库没有 `.nes` 文件。
- 公开仓库没有私有 core `.so` 文件。
- Debug / private build 可读取私有 manifest。
- Release build 可打包 `assets/retrohall_private/cores/{abi}/fceumm_libretro_android.so`。
- App 首次启动能把私有资源复制到 App 私有目录。
- 至少一个 NES 游戏可以从大厅启动并进入真实 libretro 流程。
