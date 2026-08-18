# Project Structure

## 根 package

所有 Kotlin 代码放在：

```text
app/src/main/java/com/richard/retrohall/
```

根 package：

```text
com.richard.retrohall
```

## 目录结构

```text
app/src/main/java/com/richard/retrohall/
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
└─ ui/
   └─ RetroHallApp.kt
```

Native 代码放在：

```text
app/src/main/cpp/
├─ CMakeLists.txt
└─ libretro_host.cpp
```

## 依赖方向

允许：

```text
ui -> domain
ui -> emulator
ui -> input
domain -> 无 Android UI 依赖
data -> domain
emulator -> domain
input -> domain
```

禁止：

```text
ui -> data.db
ui -> DataStore
ui -> JNI
emulator -> Compose
data -> ui
input -> ui
```

## 文件职责

- `ui`：只负责界面、焦点、用户操作分发；当前实现里 `RetroHallApp.kt` 仍偏大，后续应继续拆到 `ui/hall`、`ui/detail`、`ui/game`、`ui/settings`、`ui/components`。
- `domain`：只负责业务模型和纯逻辑。
- `data`：负责 Room、DataStore、内置资源索引、文件路径与应用启动编排。
- `emulator`：负责模拟器生命周期、libretro Kotlin 门面、JNI 桥接。
- `input`：负责把设备输入转换为 `GameAction`。

## 命名规则

- Compose 页面以 `Screen` 结尾，例如 `HallScreen`。
- Compose 可复用控件以具体 UI 名称命名，例如 `GameCoverTile`。
- ViewModel 以 `ViewModel` 结尾。
- Room Entity 以 `Entity` 结尾。
- DAO 以 `Dao` 结尾。
- Repository 以 `Repository` 结尾。
- 输入映射器以 `Mapper` 结尾。
