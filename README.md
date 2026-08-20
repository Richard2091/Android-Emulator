# Retro Hall

Retro Hall 是一个面向 Android 的复古游戏大厅与模拟器交互原型，基于 Kotlin、Jetpack Compose、Room、DataStore、JNI、CMake 和 libretro 构建，目标是在手机与电视上提供统一的游戏浏览、启动、输入、存档和设置体验。

## 功能

- 游戏大厅、搜索和分类筛选
- 收藏和最近游玩
- 游戏详情、封面、截图和元数据展示
- libretro 游戏启动流程
- 触摸、手柄和电视遥控器输入映射
- 暂停菜单、即时存档、读档和 SRAM 保存
- 画面比例、滤镜、声音、虚拟按键和缓存设置
- 游戏库与模拟器核心均消费数据源资源，不依赖私有资源注入

## 快速开始

### 环境要求

- Android Studio
- Android SDK
- JDK 17

### 常用命令

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat :app:assembleDebug
```

更多构建与验证说明见 [docs/README.md](docs/README.md) 和 [docs/testing/test-strategy.md](docs/testing/test-strategy.md)。

## 资源边界

公开仓库只保留应用源码、文档、测试和公开资源规范，不提交商业 ROM、私有 ROM、私有封面、存档、签名材料或带 ROM 的 APK。

本地真实运行依赖仓库外的私有资源目录，样例结构如下：

```text
Private/Android-Emulator/
├─ manifest.json
├─ roms/
├─ covers/
└─ cores/
   └─ arm64-v8a/
```

样例配置：

```properties
retrohall.privateAssetsDir=Private/Android-Emulator
```

详细说明见 [docs/technical/private-resource-injection.md](docs/technical/private-resource-injection.md) 和 [docs/technical/assets-and-rom-policy.md](docs/technical/assets-and-rom-policy.md)。

## 资源仓库

资源仓库分为两个独立仓库：

- `RetroGame`：游戏目录仓库，维护分类清单、全部清单、每个游戏自己的 `game.json`、封面、截图、简介和可合法分发的游戏文件。
- `RetroGame-Cores`：核心资源仓库，维护核心清单、核心二进制、架构、版本、许可证、来源、体积和哈希。

`RetroGame` 的分类建议至少拆成 `fc`、`sfc`、`gba`、`nds`、`md`、`arcade`、`dos`、`java`、`flash`、`h5`。主机类优先走 No-Intro / libretro-database / libretro-thumbnails，街机优先走 MAME software lists，DOS / Java / Flash / H5 走各自独立的运行时和授权字段，`页游` 不进入仓库。

大厅里的类型筛选必须直接跟随 `catalog/index.v2.json` 的 `categories`，不能单独维护一份类型表；总入口新增或调整分类时，筛选项要一起变。

读取方式：

- 应用先读取 `catalog/index.v2.json`
- “全部”分类读取 `catalog/all/manifest.list.v2.json`
- 单独分类读取 `catalog/<category>/manifest.list.v2.json`
- 详情页再读取 `games/<category>/<slug>/game.json`
- 核心管理读取 `RetroGame-Cores` 的 `catalog/core-manifest.v1.json`

完整规范见 [docs/technical/resource-repository-spec.md](docs/technical/resource-repository-spec.md)。

## 项目结构

```text
app/
├─ src/main/java/com/richard/retrohall/
│  ├─ data/
│  ├─ domain/
│  ├─ emulator/
│  ├─ input/
│  └─ ui/
├─ src/main/cpp/
└─ build.gradle.kts
docs/
├─ agent/
├─ technical/
├─ testing/
├─ ui/
└─ release/
scripts/
```

详细结构见 [docs/technical/project-structure.md](docs/technical/project-structure.md)。

## 文档

- [docs/README.md](docs/README.md)
- [docs/requirements.md](docs/requirements.md)
- [docs/design.md](docs/design.md)
- [docs/technical/project-structure.md](docs/technical/project-structure.md)
- [docs/technical/data-model.md](docs/technical/data-model.md)
- [docs/technical/libretro-integration.md](docs/technical/libretro-integration.md)
- [docs/technical/private-resource-injection.md](docs/technical/private-resource-injection.md)
- [docs/technical/resource-repository-spec.md](docs/technical/resource-repository-spec.md)
- [docs/testing/test-strategy.md](docs/testing/test-strategy.md)
- [docs/release/build-and-package.md](docs/release/build-and-package.md)

## 许可

本仓库代码采用 MIT License，详见 [LICENSE](LICENSE)。

公开发布前检查：

- APK 内不包含未授权游戏本体
- 核心二进制的许可证和来源已记录
- 私有资源目录、私有路径和签名文件没有进入 git
- 下载入口只面向授权明确且清单声明为 `public` 的资源
