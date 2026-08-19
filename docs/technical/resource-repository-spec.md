# 资源仓库规范

## 目标

本文档定义 Retro Hall 模拟器项目对外部资源仓库的要求，方便维护者创建自己的游戏资源仓库和核心资源仓库。

资源仓库不是 Android 模拟器工程的一部分。Android 工程只负责读取清单、缓存资源、选择核心和启动游戏；游戏本体、封面、截图、简介、外部来源和模拟器核心由独立资源仓库维护，并通过 GitHub Pages 或等价静态站点发布。

本文档描述的是后续多平台资源仓库改造目标，不自动覆盖 v0.1 / 第一版边界。第一版仍不暴露游戏源入口和 ROM 导入入口；后续即使开放在线下载，也只允许对授权明确且 `availability = public` 的文件显示下载入口。

## 仓库分工

建议至少拆成两个仓库：

```text
RetroGame
RetroGame-Cores
```

`RetroGame` 负责游戏目录：

- 分类入口
- 分类列表清单
- 全部列表清单
- 单个游戏文件夹
- 游戏详情 `game.json`
- 封面、截图、标志、说明书等媒体
- 游戏本体或合法下载指针
- 标准名、哈希、地区、版本、来源和授权信息

`RetroGame-Cores` 负责模拟器核心：

- 核心列表清单
- 按平台、核心、架构组织的 `.so`
- 核心版本、许可证、来源、哈希和体积
- GitHub Pages 下载地址

不要把核心二进制塞进 Android 模拟器仓库；不要把游戏目录和核心目录混在同一套清单里。

## 发布原则

源数据可以拆细，发布产物必须方便客户端读取。

推荐原则：

- 分类清单按平台或运行时拆分。
- “全部”列表由分类清单自动汇总生成。
- 详情不做单个巨大详情清单，直接放进每个游戏自己的文件夹。
- 列表清单只放首屏展示需要的轻量字段。
- 游戏详情、哈希、截图、来源、授权和游戏本体放到游戏文件夹。
- 核心清单和游戏下载清单分开。
- 旧版 `manifest.v1.json` 可以保留为兼容投影，但不要手工维护两份数据。

## URL 解析规则

所有清单中的 URL 字段都支持三种形式：

- `https://...` 或 `http://...`：绝对 URL，客户端原样使用。
- `/path/to/file`：站点根相对 URL，客户端按当前资源仓库 Pages 根地址解析。
- `path/to/file` 或 `../path/to/file`：当前 JSON 文件目录相对 URL，客户端按当前 JSON 文件所在目录解析。

除非字段另有说明，推荐使用当前 JSON 文件目录相对 URL。这样单个游戏文件夹可以整体移动，`game.json`、封面、截图和游戏本体之间的引用仍然有效。

示例：

- `catalog/index.v2.json` 中的 `listUrl: "fc/manifest.list.v2.json"` 指向 `catalog/fc/manifest.list.v2.json`。
- `catalog/fc/manifest.list.v2.json` 中的 `detailUrl: "../../games/fc/example-game/game.json"` 指向 `games/fc/example-game/game.json`。
- `games/fc/example-game/game.json` 中的 `cover.png` 指向同目录下的 `cover.png`。
- `RetroGame-Cores/catalog/core-manifest.v1.json` 中的 `../cores/nes/fceumm/...` 指向核心仓库根目录下的 `cores/nes/fceumm/...`。

## 游戏资源仓库结构

推荐结构：

```text
RetroGame/
├─ catalog/
│  ├─ index.v2.json
│  ├─ search-index.v2.json
│  ├─ all/
│  │  └─ manifest.list.v2.json
│  ├─ fc/
│  │  └─ manifest.list.v2.json
│  ├─ sfc/
│  │  └─ manifest.list.v2.json
│  ├─ gba/
│  │  └─ manifest.list.v2.json
│  ├─ nds/
│  │  └─ manifest.list.v2.json
│  ├─ md/
│  │  └─ manifest.list.v2.json
│  ├─ arcade/
│  │  └─ manifest.list.v2.json
│  ├─ dos/
│  │  └─ manifest.list.v2.json
│  ├─ java/
│  │  └─ manifest.list.v2.json
│  ├─ flash/
│  │  └─ manifest.list.v2.json
│  └─ h5/
│     └─ manifest.list.v2.json
├─ games/
│  ├─ fc/
│  │  └─ example-game/
│  │     ├─ game.json
│  │     ├─ cover.png
│  │     ├─ screenshots/
│  │     │  ├─ 01.png
│  │     │  └─ 02.png
│  │     └─ roms/
│  │        └─ example-game.nes
│  ├─ gba/
│  ├─ java/
│  ├─ flash/
│  └─ h5/
├─ legacy/
│  └─ manifest.v1.json
├─ scripts/
└─ README.md
```

目录约定：

- `catalog/index.v2.json` 是总入口。
- `catalog/all/manifest.list.v2.json` 给“全部”分类使用，必须自动生成。
- `catalog/<category>/manifest.list.v2.json` 给具体分类使用。
- `catalog/search-index.v2.json` 给全局搜索使用。
- `games/<category>/<slug>/game.json` 是单个游戏的权威详情。
- `games/<category>/<slug>/cover.*` 是该游戏默认封面。
- `games/<category>/<slug>/screenshots/` 放截图。
- `games/<category>/<slug>/roms/`、`packages/` 或 `content/` 放可合法分发的游戏文件。

如果游戏本体不能公开分发，`game.json` 只允许记录文件身份、来源页面和用户自备说明，不允许给出未授权直链。

## 分类入口清单

`catalog/index.v2.json` 是总入口和分类筛选的唯一来源。游戏列表页的类型筛选必须直接读取这里的 `categories`，不得再维护一份独立的类型枚举。总入口新增、删除或重命名分类时，列表页筛选项必须同步变化。

`catalog/index.v2.json` 示例：

```json
{
  "schemaVersion": 2,
  "catalogId": "retrogame",
  "catalogName": "RetroGame",
  "generatedAt": "2026-08-19T00:00:00Z",
  "defaultCategoryId": "all",
  "categories": [
    {
      "id": "all",
      "displayName": "全部",
      "runtimeFamily": "mixed",
      "listUrl": "all/manifest.list.v2.json",
      "gameCount": 1200,
      "updatedAt": "2026-08-19T00:00:00Z"
    },
    {
      "id": "fc",
      "displayName": "FC",
      "runtimeFamily": "libretro",
      "platformIds": ["nes"],
      "listUrl": "fc/manifest.list.v2.json",
      "gameCount": 300,
      "updatedAt": "2026-08-19T00:00:00Z"
    },
    {
      "id": "java",
      "displayName": "Java",
      "runtimeFamily": "j2me",
      "platformIds": ["j2me"],
      "listUrl": "java/manifest.list.v2.json",
      "gameCount": 80,
      "updatedAt": "2026-08-19T00:00:00Z"
    }
  ],
  "searchIndexUrl": "search-index.v2.json"
}
```

分类建议：

| 分类 ID | 显示名 | 运行时家族 |
| --- | --- | --- |
| `all` | 全部 | `mixed` |
| `fc` | FC | `libretro` |
| `sfc` | SFC | `libretro` |
| `gba` | GBA | `libretro` |
| `nds` | NDS | `libretro` |
| `md` | MD | `libretro` |
| `arcade` | 街机 | `libretro` 或 `mame` |
| `dos` | DOS | `dosbox` |
| `java` | Java | `j2me` |
| `flash` | Flash | `ruffle` |
| `h5` | H5 | `webview` 或 `html5` |

不要把外跳聚合型“页游”混入 `h5`。`h5` 只收可以明确定位入口、资源包、运行方式和授权状态的 HTML5 游戏或 Web 应用；外跳到第三方渠道、无法稳定复现资源包和授权状态的页游入口必须排除。

`categories[].id`、列表清单里的 `categoryId`、本地 `LocalGame.categoryId` 必须保持同一套取值；“全部”是特殊入口，但也必须来自总入口清单并参与 UI 渲染。

## 列表清单

`catalog/<category>/manifest.list.v2.json` 只放列表页需要的字段。

示例：

```json
{
  "schemaVersion": 2,
  "categoryId": "fc",
  "categoryName": "FC",
  "generatedAt": "2026-08-19T00:00:00Z",
  "games": [
    {
      "id": "fc-example-game",
      "slug": "example-game",
      "categoryId": "fc",
      "primaryPlatformId": "nes",
      "platformName": "FC",
      "runtimeFamily": "libretro",
      "title": {
        "zh": "示例游戏",
        "en": "Example Game"
      },
      "coverUrl": "../../games/fc/example-game/cover.png",
      "detailUrl": "../../games/fc/example-game/game.json",
      "tags": ["动作"],
      "releaseYear": 1985,
      "availability": {
        "binary": "public"
      }
    }
  ]
}
```

列表清单不要放：

- 长简介
- 大量截图
- 完整哈希表
- 多地区版本细节
- 核心下载地址
- 复杂授权说明
- 大体积内嵌数据

## 游戏详情文件

每个游戏目录内必须有 `game.json`。

示例：

```json
{
  "schemaVersion": 2,
  "id": "fc-example-game",
  "slug": "example-game",
  "categoryId": "fc",
  "platformIds": ["nes"],
  "primaryPlatformId": "nes",
  "runtimeFamily": "libretro",
  "title": {
    "zh": "示例游戏",
    "en": "Example Game"
  },
  "alternateTitles": ["Example Game (World)"],
  "description": {
    "zh": "这里放完整中文简介。",
    "en": "Full English description goes here."
  },
  "media": {
    "cover": {
      "url": "cover.png",
      "source": "manual",
      "licenseHint": "unknown"
    },
    "screenshots": [
      {
        "url": "screenshots/01.png",
        "source": "manual",
        "licenseHint": "unknown"
      }
    ],
    "logos": []
  },
  "files": [
    {
      "id": "main",
      "kind": "rom",
      "role": "primary",
      "path": "roms/example-game.nes",
      "mime": "application/octet-stream",
      "size": 40960,
      "hashes": {
        "crc32": "00000000",
        "md5": "00000000000000000000000000000000",
        "sha1": "0000000000000000000000000000000000000000",
        "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
      },
      "headers": {
        "headerlessCrc32": "00000000"
      },
      "availability": "public"
    }
  ],
  "runtime": {
    "family": "libretro",
    "requiredCorePlatformId": "nes",
    "recommendedCoreIds": ["fceumm", "mesen"]
  },
  "links": {
    "officialPageUrl": "",
    "sourcePageUrl": "",
    "downloadPageUrl": ""
  },
  "legal": {
    "copyrightStatus": "homebrew",
    "license": "MIT",
    "licenseUrl": "",
    "rightsHolder": "",
    "notes": ""
  },
  "sources": [
    {
      "name": "No-Intro",
      "kind": "identity",
      "url": "https://no-intro.org/"
    }
  ],
  "updatedAt": "2026-08-19T00:00:00Z"
}
```

`runtime.family` 决定详情页、下载按钮和启动器如何解释 `files[]`。不同运行时不要强行共用 `rom` 字段。

运行时字段建议：

| `runtime.family` | 适用分类 | 必填或推荐字段 |
| --- | --- | --- |
| `libretro` | FC、SFC、GBA、NDS、MD | `requiredCorePlatformId`、`recommendedCoreIds`、文件 `crc32`、`md5`、`sha1`、`sha256`；NES 建议额外保存 `headerlessCrc32` |
| `mame` | 街机 | `machineId`、`romSet`、`parentSet`、`biosSet`、`mameVersion`、`recommendedCoreIds` |
| `dosbox` | DOS | `packageId`、`launchCommand`、`workingDirectory`、`dosboxConfigUrl`、内容包 `sha256` |
| `j2me` | Java | `midletName`、`midletVendor`、`midletVersion`、`screenSize`、`keyMapId`、JAR `sha256` |
| `ruffle` | Flash | `flashpointId`、`swfEntry`、`ruffleCompatibility`、SWF 或资源包 `sha256` |
| `html5` | H5 | `entryUrl`、`assetManifestUrl`、`buildHash`、`offlineMode`、资源包 `sha256` |

运行时示例：

```json
{
  "runtime": {
    "family": "mame",
    "machineId": "dino",
    "romSet": "dino.zip",
    "parentSet": "",
    "biosSet": "",
    "mameVersion": "0.267",
    "recommendedCoreIds": ["mame2003-plus", "fbneo"]
  }
}
```

```json
{
  "runtime": {
    "family": "dosbox",
    "packageId": "death-rally",
    "workingDirectory": "content/death-rally",
    "launchCommand": "RALLY.EXE",
    "dosboxConfigUrl": "dosbox.conf"
  }
}
```

```json
{
  "runtime": {
    "family": "j2me",
    "midletName": "Example Java Game",
    "midletVendor": "Unknown",
    "midletVersion": "1.0.0",
    "screenSize": "240x320",
    "keyMapId": "numeric-keypad"
  }
}
```

```json
{
  "runtime": {
    "family": "ruffle",
    "flashpointId": "",
    "swfEntry": "content/game.swf",
    "ruffleCompatibility": "unknown"
  }
}
```

```json
{
  "runtime": {
    "family": "html5",
    "entryUrl": "content/index.html",
    "assetManifestUrl": "content/asset-manifest.json",
    "buildHash": "",
    "offlineMode": "packaged"
  }
}
```

`files[].kind` 建议值包括 `rom`、`rom_set`、`package`、`jar`、`swf`、`html5_bundle`、`manual`、`patch`。客户端只对 `availability = public` 的文件显示下载入口，并且下载后必须校验 `sha256`。

`availability` 建议值：

| 值 | 含义 |
| --- | --- |
| `public` | 可公开下载或镜像 |
| `metadata_only` | 只提供元数据 |
| `media_only` | 只提供媒体 |
| `private` | 仅本地私有资源 |
| `blocked` | 不允许提供 |

`copyrightStatus` 建议值：

| 值 | 含义 |
| --- | --- |
| `public_domain` | 公共领域 |
| `homebrew` | 自制或独立作品 |
| `licensed` | 已获得授权 |
| `freeware` | 免费软件，但仍需遵守原授权 |
| `shareware` | 共享软件，通常只适合跳转下载页 |
| `unknown` | 权利状态不明确 |

## 核心资源仓库结构

推荐结构：

```text
RetroGame-Cores/
├─ catalog/
│  └─ core-manifest.v1.json
├─ cores/
│  ├─ nes/
│  │  ├─ fceumm/
│  │  │  ├─ arm64-v8a/
│  │  │  │  └─ fceumm_libretro_android.so
│  │  │  └─ armeabi-v7a/
│  │  └─ mesen/
│  ├─ snes/
│  ├─ gba/
│  └─ nds/
├─ licenses/
└─ README.md
```

`catalog/core-manifest.v1.json` 示例：

```json
{
  "schemaVersion": 1,
  "catalogId": "retrogame-cores",
  "catalogName": "RetroGame 核心仓库",
  "generatedAt": "2026-08-19T00:00:00Z",
  "cores": [
    {
      "id": "fceumm",
      "displayName": "FCEUmm",
      "platformIds": ["nes"],
      "runtimeFamily": "libretro",
      "version": "0.0.0",
      "license": "GPL-2.0-or-later",
      "licenseUrl": "../licenses/fceumm.txt",
      "sourceUrl": "https://github.com/libretro/libretro-fceumm",
      "defaultForPlatform": true,
      "files": [
        {
          "abi": "arm64-v8a",
          "url": "../cores/nes/fceumm/arm64-v8a/fceumm_libretro_android.so",
          "fileName": "fceumm_libretro_android.so",
          "size": 1234567,
          "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
          "minSdk": 23
        }
      ]
    }
  ]
}
```

核心文件要求：

- 每个文件必须有 `sha256`。
- 发布产物中的 `size` 必须大于 0，`sha256` 必须是实际文件的 SHA-256，不能使用空值或示例占位值。
- 每个核心必须写明许可证和来源。
- 每个平台可以有多个核心。
- 每个平台必须有默认核心。
- App 删除核心时只删下载目录，不删除随 APK 内置的基础核心。
- App 选择核心时按 `平台 -> 当前选择 -> 默认核心 -> 内置核心` 顺序回退。

## Android 工程改造要求

Android 工程需要支持以下能力：

1. 资源仓库配置
   - 支持配置游戏目录入口 URL。
   - 支持配置核心目录入口 URL。
   - 默认指向维护者提供的 GitHub Pages。
   - 旧 `FC_ROMS` URL 保留兼容回退。

2. 分类列表读取
   - 启动后读取 `catalog/index.v2.json`。
   - 用户选择“全部”时读取 `catalog/all/manifest.list.v2.json`。
   - 用户选择具体分类时读取对应 `catalog/<category>/manifest.list.v2.json`。
   - 列表数据写入 Room 或本地缓存。

3. 游戏详情读取
   - 列表项必须包含 `detailUrl`。
   - 进入详情页时按需拉取 `game.json`。
   - 详情本地缓存，离线时可使用最近一次缓存。

4. 媒体加载
   - 封面优先使用列表清单中的 `coverUrl`。
   - 详情页截图从 `game.json` 的 `media.screenshots` 读取。
   - 相对路径按当前 `game.json` 所在目录解析。

5. 游戏文件处理
   - `public` 文件允许 App 下载。
   - `metadata_only`、`private`、`blocked` 文件不显示直接下载按钮。
   - 下载完成后校验 `sha256`，失败必须删除临时文件。

6. 核心管理
   - 设置页新增“核心管理”卡片。
   - 卡片右侧展示当前所选平台的当前核心。
   - 点击后打开核心管理弹窗。
   - 弹窗支持按平台查看核心。
   - 支持在线下载、选择、删除已下载核心。
   - 当前核心选择写入 DataStore。
   - 启动游戏时优先使用用户选择核心。

7. 运行时分发
   - `libretro` 类游戏走现有 `EmulatorSession`。
   - `j2me`、`dosbox`、`ruffle`、`html5` 类游戏先进入未支持提示或对应运行器。
   - 不要把 Java、DOS、Flash、H5 硬塞进 libretro 核心逻辑。

## Android 代码改造任务

建议分阶段做：

### 阶段一：文档与兼容配置

- 新增本文档。
- 更新文档索引。
- 把 `FC_ROMS` 命名改为可配置资源仓库名。
- 保留旧 URL 兼容回退。

### 阶段二：游戏目录 v2 读取

- 新增 `ResourceCatalogClient`。
- 新增 `CategoryCatalog`、`GameListItem`、`GameDetail` 领域模型。
- `GitHubGameCatalogClient` 改为兼容层或逐步废弃。
- 支持 `catalog/index.v2.json` 和分类列表清单。
- 支持 `detailUrl` 按需读取。

### 阶段三：Room 与缓存升级

- `LocalGame` 增加 `categoryId`、`platformId`、`runtimeFamily`、`detailUrl`。
- Room schema 增加对应字段和迁移。
- 封面缓存支持相对 URL 解析。
- 游戏详情缓存到 `metadata-cache/details/`。

### 阶段四：资源下载升级

- `RomDownloadManager` 改名或新增 `ContentDownloadManager`。
- 支持 `files[]` 多文件下载。
- 下载后校验 `sha256`。
- 按 `availability` 控制下载按钮。

### 阶段五：核心仓库接入

- 新增 `CoreCatalogClient`。
- 新增 `CoreRepository`。
- 新增 `CoreDownloadManager`。
- 新增 `CoreSelectionStore`。
- `CoreDescriptor` 从清单生成，不再只用硬编码常量。
- `CorePathResolver` 支持用户选择、下载核心、内置核心回退。

### 阶段六：设置页核心管理

- `SettingsScreen` 新增“核心管理”卡片。
- 新增 `CoreManagerDialog`。
- 弹窗内支持平台筛选、下载、选择、删除。
- 所有按钮、提示、菜单必须使用中文。

### 阶段七：分类差异化 UI

- 大厅按 `categoryId` 展示分类。
- 每个分类可声明自己的列表展示策略。
- 详情页按 `runtimeFamily` 展示不同字段和操作。
- 未支持运行时显示明确提示。

## 信息来源分级

信息源按用途分级。不要把游戏池、元数据源、媒体源、授权来源和下载来源混为一谈。

`yikm.net` 可作为分类和游戏池参考，但不是权威标准名来源、不是授权来源，也不应作为公开本体下载来源。采集时必须排除“页游”和 `CS:S`，保留站内 H5 分类中可明确定位入口和资源包的 HTML5 游戏。

### 身份、标准名和哈希

| 信息源 | 适用分类 | 可提供字段 | 自动化建议 | 限制 |
| --- | --- | --- | --- | --- |
| No-Intro | FC、SFC、GBA、NDS、MD 等主机类 | 标准名、地区、版本、序列号、CRC、MD5、SHA1 | 用文件哈希优先匹配，中文名只做候选别名 | 汉化、改版、盗版合集通常不在标准条目中 |
| libretro-database | FC、SFC、GBA、NDS、MD 等主机类 | No-Intro 投影、平台 DAT、部分元数据 | 适合生成标准身份和哈希索引 | 不是简介和封面的主来源 |
| MAME software lists / `listxml` | 街机 | `machineId`、`romSet`、父集、克隆集、BIOS、ROM CRC/SHA1 | 街机必须按 set 匹配，不按单 ROM 文件匹配 | MAME 版本变化会影响 set 关系 |
| Hasheous | 多平台主机类 | 哈希到游戏身份、媒体候选 | 可作为自动匹配补充 | 需要缓存结果，不能作为唯一来源 |
| RetroAchievements | 多平台主机类 | 游戏 ID、标准名、成就生态哈希 | 可辅助用户库匹配 | 目标是成就生态，不覆盖所有版本 |
| Redump | 光盘平台扩展 | 光盘镜像身份和哈希 | 后续扩展 PS1、SS、DC 时使用 | 当前分类暂不是主线 |
| TOSEC | 计算机、老平台、杂项 | 变体命名和哈希 | 可作为补充索引 | 命名复杂，授权不等于可分发 |

### 封面、截图和简介

| 信息源 | 适用分类 | 可提供字段 | 自动化建议 | 限制 |
| --- | --- | --- | --- | --- |
| libretro-thumbnails | FC、SFC、GBA、NDS、MD 等主机类 | 封面、截图、标题图 | 标准名命中后自动取 `Named_Boxarts`、`Named_Snaps`、`Named_Titles` | 对中文改版覆盖不足 |
| ScreenScraper | 多平台 | 封面、截图、简介、发行信息 | 适合补全详情页 | 通常需要账号/API 配额 |
| IGDB | 多平台 | 简介、发行信息、封面、截图、厂商 | 适合生成英文基础详情 | API 授权和请求限制需单独处理 |
| TheGamesDB | 多平台 | 封面、截图、平台、简介 | 适合作为媒体补充 | 中文内容有限 |
| MobyGames | 多平台，DOS 特别有用 | 简介、发行信息、截图、封面 | DOS、PC、老游戏详情优先考虑 | 公开 API 和授权边界要遵守 |
| Progetto-SNAPS | 街机 | MAME 截图、标题图、面板等 | 街机媒体优先来源 | 需要按 MAME 名称和版本匹配 |
| SteamGridDB / LaunchBox Games Database / OpenVGDB | 补充源 | 封面、网格图、别名、平台信息 | 只做候选补充 | 不作为哈希权威来源 |

### 合法可分发内容

只有授权明确的内容才允许 `availability = public`。商业游戏、权利不明资源、站点在线可玩内容默认 `metadata_only`、`media_only` 或 `private`。

| 信息源 | 适用分类 | 可提供字段 | 处理方式 |
| --- | --- | --- | --- |
| Homebrew Hub、nesdev homebrew-db | FC | 自制游戏条目、源码或发布包、许可证 | 可作为公开内容池，下载前记录许可证和 SHA256 |
| gbadev games | GBA | 自制游戏、demo、源码或构建包 | 可作为 GBA 公开内容池 |
| SGDK 示例和 homebrew | MD | Genesis / Mega Drive 自制内容 | 记录源码来源、构建产物和许可证 |
| PVSnesLib 示例和 homebrew | SFC | SFC 自制内容 | 记录源码来源、构建产物和许可证 |
| ScummVM 官方免费游戏 | DOS / 冒险游戏运行时 | 官方声明可免费下载的游戏 | 只收官方明确可下载条目 |
| Freedoom | DOS / Doom 引擎类 | 自由 WAD、许可证、版本 | 可作为 Doom 类公开内容池 |
| js13kGames | H5 | HTML5 游戏源码、构建包、许可证 | 适合 H5 分类的公开样例 |
| DOS Games Archive、Internet Archive、eXoDOS | DOS | 详情、截图、包线索、启动配置参考 | 仅在授权明确时提供下载；否则只记录来源页面 |

### 特殊平台处理

- DOS：记录内容包、`launchCommand`、工作目录、`dosbox.conf` 和包 SHA256。商业 DOS 游戏默认不提供本体直链。
- Java：以实际 JAR 为准，解析 `META-INF/MANIFEST.MF` 中的 `MIDlet-Name`、`MIDlet-Vendor`、`MIDlet-Version`，再保存分辨率、键位和 JAR SHA256。Kahvibreak 与 J2ME 索引只做参考，不能当作统一哈希权威。
- Flash：优先用 Flashpoint 做标准条目、简介、截图和运行线索，用 Ruffle 作为运行时。SWF 或资源包必须逐项确认授权并保存 SHA256。
- H5：优先收源码或构建包授权清晰的 HTML5 游戏。详情记录 `entryUrl`、资源清单、构建哈希和离线策略；外跳页游聚合不进入仓库。

### 核心来源

核心资源仓库必须优先使用官方或可复现构建来源。

| 信息源 | 可提供字段 | 要求 |
| --- | --- | --- |
| libretro 官方核心仓库 | 源码、许可证、核心名称、平台支持 | 每个核心写明 `sourceUrl`、`license`、`version` |
| libretro buildbot 或维护者自建构建产物 | Android `.so`、ABI、下载 URL | 发布前计算实际 `size` 和 `sha256` |
| 核心项目 README / LICENSE | 许可证、兼容平台、运行说明 | 许可证文本建议同步到 `licenses/` |

### 推荐采集流程

1. 从 yikm 或维护者清单取得候选游戏、分类和站内别名。
2. 按分类选择权威身份源：主机类走 No-Intro / libretro-database，街机走 MAME，非主机类走各自运行时来源。
3. 用哈希优先匹配标准身份；没有文件时只生成候选条目并标记为 `metadata_only`。
4. 按标准名或运行时身份补封面、截图和简介。
5. 判断授权状态，只有授权明确时才把文件标为 `public`。
6. 下载或导入文件后计算 `sha256`，必要时补 `crc32`、`md5`、`sha1`、`headerlessCrc32`。
7. 生成每个游戏目录内的 `game.json`，再自动生成分类列表、全部列表和搜索索引。

## 第三方维护者要求

第三方要维护自己的资源仓库，至少必须提供：

- `catalog/index.v2.json`
- 至少一个分类的 `manifest.list.v2.json`
- 每个列表项指向可访问的 `game.json`
- 每个 `game.json` 使用 UTF-8 编码
- 所有相对路径都必须遵守本文档的 URL 解析规则
- 可下载文件必须包含 `sha256`
- 可下载文件必须声明 `availability`
- 游戏和媒体的授权状态必须声明
- GitHub Pages 或静态站点必须能直接访问 JSON 和资源文件

推荐提供：

- `catalog/all/manifest.list.v2.json`
- `catalog/search-index.v2.json`
- `README.md` 说明来源和版权边界
- `licenses/` 保存许可文本
- 生成脚本，避免手工维护汇总清单

禁止事项：

- 不要把权利不明确的商业 ROM 标成开源或公共领域。
- 不要在公共清单中提供未授权二进制直链。
- 不要把私有本地路径写入公开清单。
- 不要省略核心或游戏本体的哈希。
- 不要让 `all` 清单变成第二份手写数据源。

## 验收标准

一个资源仓库可以被 Retro Hall 正确消费，需要满足：

- App 能读取 `catalog/index.v2.json`。
- App 能展示“全部”和各分类。
- App 能按分类加载轻量列表。
- App 能进入详情页并加载游戏目录内的 `game.json`。
- 封面和截图能按相对路径解析。
- 可下载游戏本体能完成下载和哈希校验。
- 不可下载条目不会显示直接下载入口。
- 核心仓库能列出核心、下载核心、选择核心和删除下载核心。
- 无网络时能使用已缓存列表、详情、封面和已下载核心。
