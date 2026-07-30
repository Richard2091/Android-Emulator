# Save System

## 目标

第一版支持：

- SRAM 自动存档
- 1 个自动即时存档槽
- 3 个手动即时存档槽

所有存档写入 App 私有目录，不依赖公开外部存储。

## 私有目录结构

```text
files/
└─ saves/
   ├─ sram/
   │  └─ {gameId}.srm
   └─ states/
      └─ {gameId}/
         ├─ auto.state
         ├─ manual-1.state
         ├─ manual-2.state
         └─ manual-3.state
```

## 路径规则

SRAM：

```text
files/saves/sram/{gameId}.srm
```

自动即时存档：

```text
files/saves/states/{gameId}/auto.state
```

手动即时存档：

```text
files/saves/states/{gameId}/manual-{index}.state
```

`index` 只能是 1、2、3。

## 保存时机

SRAM 保存：

- 用户退出游戏返回大厅时保存。
- App 进入后台时尽量保存。
- 模拟器停止前保存。
- 真实 libretro 接入后必须调用 core 暴露的 SRAM memory 或 host 保存机制，不允许只更新数据库索引。

自动即时存档：

- 退出游戏时如果 `autoSaveStateEnabled = true`，保存到自动槽。
- 进入游戏时可读取自动槽作为恢复选项，但第一版不强制自动恢复。

手动即时存档：

- 用户在暂停菜单中选择保存槽位时保存。
- 真实 libretro 接入后必须调用 `serializeState`。
- 保存成功后更新 `SaveStateEntity`。
- 保存失败时显示提示。
- 保存失败时不更新 `SaveStateEntity`。

## 读取时机

即时读档：

- 用户在暂停菜单中选择读取槽位。
- 文件不存在时提示 `没有可读取的存档`。
- unserialize 失败时提示 `读档失败`。
- 读档失败不改变当前游戏状态。
- 真实 libretro 接入后必须调用 `unserializeState`。

## 错误处理

SRAM 保存失败：

- 显示短提示：`进度保存失败`
- 写入日志：gameId、path、异常类型
- 不阻止用户返回大厅

即时存档失败：

- 显示短提示：`保存失败`
- 不更新 `SaveStateEntity`

即时读档失败：

- 显示短提示：`读档失败`
- 保持当前游戏继续暂停或返回暂停菜单

## 测试要求

`SavePathResolverTest` 至少覆盖：

- SRAM 路径生成
- 自动槽路径生成
- 手动槽 1/2/3 路径生成
- 手动槽 0 抛出错误
- 手动槽 4 抛出错误

真实模拟器存档验收至少覆盖：

- 保存手动槽 1 后生成 state 文件。
- 读取手动槽 1 后游戏恢复到保存点。
- 保存失败时不写入索引。
- 读档失败时当前游戏仍可继续或返回暂停菜单。
