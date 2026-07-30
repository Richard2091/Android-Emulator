# Test Strategy

## 单元测试

必须覆盖：

- `KeyEventMapper`
- `SavePathResolver`
- UserSettings 默认值
- 收藏状态切换
- 最近游玩更新时间
- 即时存档槽位规则

运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Room 测试

覆盖：

- 插入游戏
- 查询全部游戏
- 按分类查询
- 查询收藏
- 更新最近游玩
- 保存存档索引

可以使用 AndroidX Room testing 或 Robolectric。

## DataStore 测试

覆盖：

- 首次读取返回默认值
- 写入后读取新值
- 音量边界限制在 0.0 到 1.0
- 虚拟按键透明度限制在 0.1 到 1.0

## Compose UI 测试

覆盖：

- 大厅首屏显示分类和游戏网格
- 焦点可以移动
- 点击游戏进入游戏详情页
- 详情页开始游戏进入游戏页
- 从游戏库、最近、搜索和收藏进入详情页的行为一致
- 暂停菜单显示所有菜单项
- 设置页可以切换开关

运行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

没有设备时，记录跳过原因，不把该项说成已通过。

## Native 冒烟测试

libretro 接入后至少验证：

- native library 能加载
- `nativeVersion()` 返回非空字符串
- core 加载失败时返回 false
- ROM 加载失败时返回 false
- runFrame 失败不会崩溃 App

## 最终验证命令

```powershell
.\gradlew.bat clean :app:testDebugUnitTest :app:assembleDebug
```

有设备时再执行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```
