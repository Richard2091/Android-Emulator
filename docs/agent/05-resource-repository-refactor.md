# 资源仓库重构验收跟踪

本文件跟踪《资源仓库规范》（`docs/technical/resource-repository-spec.md`）的 Android 端改造完成度，供后续维护者对照验收。

## 总览

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 一 | 文档与兼容配置 | ✅ |
| 二 | 游戏目录 v2 读取 | ✅ |
| 三 | Room 与缓存升级 | ✅（含详情离线磁盘缓存） |
| 四 | 资源下载升级 | ✅ |
| 五 | 核心仓库接入 | ✅ |
| 六 | 设置页核心管理 | ✅ |
| 七 | 分类差异化 UI | ✅ |

## 阶段核对明细

### 阶段一：文档与兼容配置 ✅

- [x] 新增 `resource-repository-spec.md`
- [x] 更新文档索引
- [x] `FC_ROMS` 命名改为可配置资源仓库名
- [x] 保留旧 URL 兼容回退（`ResourceRepositoryConfig.LEGACY_MANIFEST_URLS`）

### 阶段二：游戏目录 v2 读取 ✅

- [x] `ResourceCatalogClient`：读取 `catalog/index.v2.json`、`manifest.list.v2.json`、`game.json`
- [x] `CategoryCatalog` / `GameListItem` / `GameDetail` 领域模型
- [x] `detailUrl` 按需读取
- [x] 相对路径按当前 JSON 所在目录解析

### 阶段三：Room 与缓存升级 ✅

- [x] `LocalGameEntity` 增加 `categoryId`、`platformId`、`runtimeFamily`、`detailUrl`
- [x] 封面缓存支持相对 URL 解析（`CoverDownloader`）
- [x] 详情离线磁盘缓存：`metadata-cache/details/<gameId>.json`（`ResourceCatalogClient.fetchDetailCached`）
- [x] 搜索索引离线缓存：`metadata-cache/details/search-index.v2.json`（`fetchSearchIndex`）

### 阶段四：资源下载升级 ✅

- [x] `ContentDownloadManager`：多文件下载 + sha256 校验 + 按 availability 控制
- [x] 下载后主文件路径回写 `LocalGame.romPath`
- [x] `isDownloaded` 递归检测（`content-cache/<gameId>/<fileId>/<file>`）

### 阶段五：核心仓库接入 ✅

- [x] `CoreCatalogClient` 读取 `core-manifest.v1.json`，相对 URL 按 manifest 目录解析
- [x] `CoreDownloadManager`：下载 .so + sha256 校验
- [x] `CoreSelectionStore`：平台选择写入 DataStore
- [x] `CorePathResolver`：用户选择 → 默认核心 → 内置核心回退
- [x] ABI 自动匹配：`matchingAbi` = 设备支持 ABI ∩ 清单 ABI（`CoreManagerDialog` 两处调用均已接入）
- [x] 多架构核心发布：fceumm 提供 `arm64-v8a` / `armeabi-v7a` / `x86` / `x86_64`

### 阶段六：设置页核心管理 ✅

- [x] `SettingsScreen` 核心卡片，按游戏类型分行
- [x] `CoreManagerDialog`：平台筛选、下载、选择、删除
- [x] 未配置时按默认核心回退展示选中态
- [x] 全部文案中文化

### 阶段七：分类差异化 UI ✅

- [x] 详情页 `unsupportedRuntime` 提示（非 libretro 运行时禁用开始）
- [x] 大厅平台筛选项由在线 `index.v2.json` 的 `categories` 驱动，失败回退本地推断
- [x] 全局搜索接入 `search-index.v2.json`（中/英/日标题与 slug 匹配，离线可用缓存）

## 端到端验证

`ResourceLifecycleVerificationTest`（androidTest，真实设备/模拟器 + 在线仓库）：

- 游戏资源：在线拉取 → 下载 ROM（sha256 校验）→ 核心加载运行 → 删除
- 核心资源：在线拉取清单 → 自动匹配 ABI 下载（x86_64 模拟器命中 x86_64 核心）→ dlopen 加载 → 删除

JVM 单测：

- `CoreCatalogClientTest`：相对 URL 解析、默认核心回退
- `CoreDownloadManagerTest`：ABI 自动匹配

## 历史修复记录

| 问题 | 修复 |
| --- | --- |
| `ContentDownloadManager.isDownloaded` 只查第一层目录恒为 false | 递归检测 `<gameId>/<fileId>/<file>` |
| `CoreCatalogClient` 相对 URL 用仓库根解析，`../cores` 丢失仓库段 | 改为按 manifest 所在目录（`catalog/`）解析 |
| 在线核心仓库缺 x86/x86_64，x86 设备无法下载 | 仓库补多架构；软件端自动匹配 ABI |

## 已知边界

- `search-index.v2.json` 的命中依赖游戏已同步进本地库（索引仅用于放宽标题匹配，不做未同步游戏注入）。
- 多平台分类仍以 FC/NES 为事实主平台；新增平台需在仓库 `index.v2.json` 声明 `platformIds` 与 `displayName`。
- **只消费数据源资源**：启动不再注入任何私有资源（游戏与核心均来自数据源）。游戏目录由 `index.v2` 同步；核心由核心管理弹窗按需从在线核心仓库下载。同步会清理不在数据源中的本地残留游戏。

## 数据源优先策略（2026-08-20）

| 改动 | 说明 |
| --- | --- |
| `AppBootstrapper` | 移除 `PrivateAssetInitializer` 注入与 `seedIfEmpty` 假数据，只做 v2/v1 数据源同步 |
| `RetroHallDependencies` | 不再创建 `PrivateAssetInitializer`，`AppBootstrapper` 签名简化 |
| `GameRepository.syncFromResourceCatalog` | 同步后 `deleteAllExcept(remoteIds)` 清理私有/遗留游戏 |
| `LocalGameDao` | 新增 `deleteAllExcept(keepIds)` |
| `DatabaseSourceOnlyTest`（androidTest） | 断言设备库只含数据源游戏、无私有魂斗罗 |
| `GameplayFlowTest` | 改为数据源真实游戏（Donkey Kong）全链路：同步→搜索→下载 ROM+核心→出帧 |

> 私有资源目录与 `PrivateAssetInitializer` 仍保留，仅供本地验收测试（`LibretroCoreInstrumentedTest` 等）自备 ROM/core 使用，不参与 App 运行时。
