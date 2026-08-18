# Android 复古游戏大厅设计文档

## 1. 设计目标

本设计文档描述 Android 复古游戏大厅第一版的目标架构和当前收口方向。

当前仓库已经初始化 Android 工程，`app` 模块、Compose UI、Room、DataStore、JNI/CMake、测试和 libretro 宿主都已存在；文档中的部分分包仍是目标态，需要继续向代码收敛。

第一版目标：

- 普通用户开箱即用。
- 定制玩家可以主动进入设置页调整体验。
- 手机和电视使用同一套核心体验。
- 横屏优先。
- 焦点、手柄、遥控器操作优先。
- 第一阶段只支持 FC / NES。
- 使用 libretro 方案集成成熟 FC / NES 模拟核心。
- 游戏以内置方式提供，数据来源于开源社区 `tomiaa12/nesRoms`。
- 仅供个人研究和本地验证，不面向公开分发。
- 不支持本地 ROM 导入。
- 不支持游戏源。
- 不做账号和云同步。
- 公开仓库不提交 ROM；本地验证通过私有资源注入准备 ROM、封面和 libretro core。

## 2. 工程结构

第一版采用单 module Android 工程，通过 package 分层维护边界。当前实现里 UI 入口仍偏集中，后续应继续拆分为更细的文件级结构。

```text
app/
├─ MainActivity.kt
├─ RetroHallDependencies.kt
├─ data/
│  ├─ assets/
│  ├─ bootstrap/
│  ├─ cache/
│  ├─ db/
│  ├─ game/
│  ├─ save/
│  └─ settings/
├─ domain/
│  ├─ game/
│  ├─ input/
│  ├─ save/
│  └─ settings/
├─ emulator/
│  ├─ CoreDescriptor.kt
│  ├─ CorePathResolver.kt
│  ├─ EmulatorSession.kt
│  ├─ EmulatorSessionFactory.kt
│  ├─ EmulatorState.kt
│  ├─ FakeEmulatorSession.kt
│  ├─ LibretroEmulatorSession.kt
│  └─ LibretroHost.kt
├─ input/
│  └─ KeyEventMapper.kt
├─ ui/
│  └─ RetroHallApp.kt
```

依赖方向：

```text
ui -> domain
ui -> emulator
ui -> input
data -> domain
emulator -> domain
input -> domain
```

约束：

- UI 层不直接访问 Room、DataStore 或 JNI。
- 模拟器层不依赖 Compose。
- 输入层只输出统一动作，不直接控制 UI。
- 数据层不依赖 UI。
- libretro 细节封装在 `emulator` 包内。
- domain 不依赖 data；Repository 实现放在 data 层，向 UI 或用例层暴露 domain 模型。

## 3. 总体架构

```text
Android App
├─ UI 层
│  ├─ 游戏大厅
│  ├─ 收藏
│  ├─ 最近游玩
│  ├─ 游戏详情
│  ├─ 游戏界面
│  ├─ 暂停菜单
│  └─ 设置
├─ 应用层
│  ├─ 游戏库用例
│  ├─ 启动游戏用例
│  ├─ 存档用例
│  ├─ 输入映射用例
│  └─ 设置用例
├─ 数据层
│  ├─ Room 数据库
│  ├─ DataStore 设置
│  ├─ 内置资源索引
│  ├─ 封面缓存
│  └─ 本地文件管理
├─ 模拟器层
│  ├─ Kotlin 模拟器门面
│  ├─ JNI / NDK 桥接
│  ├─ libretro host
│  ├─ FC / NES libretro core
│  ├─ 视频帧输出
│  ├─ 音频输出
│  └─ 输入注入
└─ 设备输入层
   ├─ 触摸输入
   ├─ 虚拟按键
   ├─ Android KeyEvent
   ├─ 手柄
   └─ 电视遥控器
```

## 4. libretro 集成设计

第一版不直接改造 RetroArch 前端，而是在自研 Android 游戏大厅中实现一个最小 libretro host，加载成熟 FC / NES libretro core。

libretro 集成边界：

- Android / Kotlin 侧负责大厅、设置、输入、文件、存档、生命周期。
- JNI / C++ 侧负责承载 libretro core。
- libretro core 负责 NES 模拟执行。
- 视频帧、音频采样、输入状态、存档数据通过 host 回调桥接到 Android。

libretro host 需要承担：

- 加载 libretro core 动态库。
- 调用 core 初始化和释放。
- 设置 environment、video、audio、input 回调。
- 加载 `.nes` ROM。
- 驱动 `retro_run()`。
- 转发输入状态。
- 接收视频帧并交给渲染层。
- 接收音频采样并交给音频输出层。
- 管理 SRAM。
- 管理即时存档序列化和反序列化。

Kotlin 侧统一接口：

```kotlin
interface EmulatorSession {
    fun load(game: LocalGame)
    fun start()
    fun pause()
    fun resume()
    fun reset()
    fun stop()
    fun sendInput(action: GameAction, pressed: Boolean)
    fun saveSram()
    fun saveState(slot: SaveSlot)
    fun loadState(slot: SaveSlot)
}
```

`EmulatorSession` 对 UI 隐藏 libretro 细节。UI 只关心启动、暂停、输入、存档和退出。

## 5. UI 设计

### 5.1 游戏大厅

职责：

- 展示左侧导航。
- 展示本地游戏封面网格。
- 展示收藏。
- 展示最近游玩。
- 在游戏库顶部提供搜索输入框。
- 处理焦点移动。
- 处理确认、返回、菜单动作。
- 跳转设置。
- 进入游戏详情页。

设计要求：

- 使用 Jetpack Compose。
- 全横屏布局。
- 大屏优先。
- 所有主要交互元素都必须可获得焦点。
- 焦点高亮清晰。
- 支持触摸点击。
- 支持遥控器和手柄上下左右移动。
- 游戏库、最近游玩和收藏中的游戏卡片确认或点击后，都进入同一个游戏详情页。

### 5.2 游戏详情页

职责：

- 显示游戏封面、标题、平台、分类、收藏状态、最近游玩时间和累计游玩时长。
- 提供开始游戏入口。
- 提供收藏 / 取消收藏入口。
- 处理确认、返回、菜单动作。
- 从游戏库、最近游玩和收藏进入时保持一致交互。

设计要求：

- 默认焦点放在开始游戏。
- Confirm 在开始游戏上启动游戏。
- Back 返回进入详情页前的列表和焦点位置。
- 收藏状态变更后同步影响收藏页和游戏卡片显示。
- 详情页是启动模拟器的唯一前置页面，游戏列表不直接启动游戏。

### 5.3 游戏界面

职责：

- 显示模拟器画面。
- 接收虚拟按键输入。
- 接收手柄和遥控器输入。
- 打开暂停菜单。
- 根据设备类型显示或隐藏虚拟按键。

手机默认显示：

- 虚拟方向键
- A / B
- Start / Select
- 菜单按钮

电视默认显示：

- 模拟器画面
- 不显示虚拟按键

### 5.4 暂停菜单

暂停菜单包含：

- 继续游戏
- 保存即时存档
- 读取即时存档
- 重置游戏
- 画面设置
- 声音设置
- 退出到大厅

暂停菜单必须支持焦点操作。

### 5.5 设置

设置分为基础设置和高级设置。

基础设置：

- 画面比例
- 滤镜开关
- 声音开关
- 音量
- 虚拟按键显示方式
- 虚拟按键透明度
- 虚拟按键大小
- 虚拟按键位置
- 清理缓存

高级设置：

- 单个游戏画面比例覆盖
- 单个游戏滤镜覆盖
- 单个游戏声音覆盖
- 手柄连接时是否自动隐藏虚拟按键
- 自动即时存档策略

## 6. 输入设计

内部统一动作：

```text
Up
Down
Left
Right
Confirm
Back
Menu
NesA
NesB
Start
Select
```

输入来源：

- 触摸输入
- 虚拟按键
- Android KeyEvent
- 手机手柄
- 电视遥控器
- 电视手柄

输入处理流程：

```text
设备输入
-> 输入 Mapper
-> GameAction
-> 当前场景分发
-> 大厅 / 游戏 / 暂停菜单
```

场景规则：

- 大厅消费方向、确认、返回、菜单。
- 游戏内消费 NES 控制动作和菜单动作。
- 暂停菜单消费方向、确认、返回、菜单。
- 同一物理按键允许在不同场景下映射为不同语义。

## 7. 数据设计

### 7.1 LocalGame

```text
id
title
platform
category
coverPath
romPath
favorite
lastPlayedAt
totalPlayTime
createdAt
updatedAt
```

### 7.2 SaveState

```text
gameId
slotType
slotIndex
filePath
createdAt
updatedAt
```

槽位规则：

- `slotType = auto` 表示自动槽。
- `slotType = manual` 表示手动槽。
- 自动槽固定 1 个。
- 手动槽固定 3 个。

### 7.3 UserSettings

```text
aspectRatio
filterEnabled
audioEnabled
volume
virtualPadVisibility
virtualPadOpacity
virtualPadScale
virtualPadLayout
hideVirtualPadWhenGamepadConnected
autoSaveStateEnabled
```

### 7.4 GameOverrideSettings

```text
gameId
aspectRatio
filterEnabled
audioEnabled
updatedAt
```

## 8. 文件设计

App 私有目录结构：

```text
files/
├─ roms/
├─ covers/
├─ saves/
│  ├─ sram/
│  └─ states/
└─ logs/
```

要求：

- 内置 ROM 首次启动时复制到 App 私有目录。公开仓库不保存 ROM，Debug / private build 通过私有资源注入机制提供 ROM。
- SRAM 按游戏 ID 存放。
- 即时存档按游戏 ID 和槽位存放。
- 封面缓存与游戏 ID 关联。
- 日志不记录用户敏感信息。

## 9. 主要流程

### 9.1 启动 App

```text
启动 App
-> 强制横屏
-> 初始化 DataStore 设置
-> 初始化 Room 数据库
-> 初始化内置游戏索引
-> 检查私有资源注入结果
-> 复制缺失的内置 ROM、封面和 core 到 App 私有目录
-> 进入大厅
-> 默认聚焦最近游玩游戏或第一个游戏
```

### 9.2 进入游戏详情页

```text
游戏库 / 最近游玩 / 搜索 / 收藏中选中游戏
-> 确认
-> 查询 LocalGame
-> 进入游戏详情页
-> 默认聚焦开始游戏
```

### 9.3 启动游戏

```text
游戏详情页
-> 选择开始游戏
-> 创建 EmulatorSession
-> 加载 libretro core
-> 加载 ROM
-> 进入游戏全屏界面
-> 开始驱动模拟循环
-> 输出视频和音频
```

### 9.4 游戏输入

```text
设备输入
-> 输入 Mapper
-> GameAction
-> EmulatorSession.sendInput()
-> libretro input callback 返回当前按键状态
-> core 在 retro_run() 中读取输入
```

### 9.5 暂停和退出

```text
游戏中按菜单键
-> 暂停模拟器
-> 打开暂停菜单
-> 继续 / 保存 / 读档 / 重置 / 设置 / 退出
-> 退出时保存 SRAM
-> 更新最近游玩和累计时长
-> 释放 EmulatorSession
-> 返回大厅
```

### 9.6 即时存档

```text
选择保存槽位
-> 暂停模拟器
-> 调用 libretro serialize
-> 写入 states 文件
-> 更新 SaveState 记录
-> 提示保存完成
```

### 9.7 即时读档

```text
选择读取槽位
-> 暂停模拟器
-> 读取 states 文件
-> 调用 libretro unserialize
-> 恢复游戏
-> 提示读取完成
```

## 10. 错误处理

第一版必须处理：

- 内置 ROM 复制失败。
- 封面加载失败。
- 本地 ROM 文件不存在。
- libretro core 加载失败。
- ROM 加载失败。
- 视频帧输出失败。
- 音频输出失败。
- SRAM 保存失败。
- 即时存档保存失败。
- 即时存档读取失败。
- 手柄断开连接。

处理原则：

- 普通错误用短提示说明。
- 技术细节写入日志。
- 游戏无法继续时提供返回大厅的退路。
- 存档失败必须提示。
- 退出游戏时尽量保存 SRAM。

## 11. 测试策略

第一版测试覆盖：

- Room 数据访问测试。
- DataStore 设置读写测试。
- 内置游戏索引初始化测试。
- 内置 ROM 复制测试。
- 输入映射单元测试。
- 存档路径生成测试。
- SRAM 保存路径测试。
- 即时存档槽位测试。
- 大厅焦点导航 UI 测试。
- 暂停菜单焦点导航 UI 测试。
- libretro core 加载冒烟测试。
- NES ROM 启动冒烟测试。
- 手机横屏触摸冒烟测试。
- Android TV / 遥控器焦点操作冒烟测试。
- 手柄输入冒烟测试。

## 12. 验收标准

设计实现完成后需要满足：

- App 使用单 module 工程结构。
- UI、domain、data、emulator、input 分层清晰。
- UI 不直接调用 JNI。
- libretro 细节封装在模拟器层。
- 大厅、游戏界面、暂停菜单都支持焦点操作。
- 至少一个通过私有资源注入准备好的 NES 游戏可以启动。
- libretro core 可以完成加载、运行、暂停、释放。
- 视频和音频输出可用。
- 输入可以从触摸、手柄、遥控器映射到统一动作。
- SRAM 自动保存可用。
- 1 个自动即时存档槽可用。
- 3 个手动即时存档槽可用。
- 设置可以保存并生效。
- 第一版不包含游戏源入口。
- 第一版不包含 ROM 导入入口。
- 构建产物仅用于个人研究和本地验证。
