# Data Model

## LocalGame

领域模型：

```kotlin
data class LocalGame(
    val id: String,
    val title: String,
    val platform: String,
    val category: String,
    val coverPath: String,
    val romPath: String,
    val favorite: Boolean,
    val lastPlayedAt: Long?,
    val totalPlayTimeMillis: Long,
)
```

Room Entity：

```kotlin
@Entity(tableName = "local_games")
data class LocalGameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val platform: String,
    val category: String,
    val coverPath: String,
    val romPath: String,
    val favorite: Boolean,
    val lastPlayedAt: Long?,
    val totalPlayTimeMillis: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
```

DAO 必须支持：

- 查询全部游戏
- 按分类查询
- 查询收藏游戏
- 查询最近游玩游戏
- 更新收藏状态
- 更新最近游玩时间
- 更新累计游玩时长

## SaveState

领域模型：

```kotlin
sealed class SaveSlot {
    data object Auto : SaveSlot()
    data class Manual(val index: Int) : SaveSlot()
}

data class SaveState(
    val gameId: String,
    val slot: SaveSlot,
    val filePath: String,
    val createdAt: Long,
    val updatedAt: Long,
)
```

Room Entity：

```kotlin
@Entity(
    tableName = "save_states",
    primaryKeys = ["gameId", "slotType", "slotIndex"]
)
data class SaveStateEntity(
    val gameId: String,
    val slotType: String,
    val slotIndex: Int,
    val filePath: String,
    val createdAt: Long,
    val updatedAt: Long,
)
```

槽位规则：

- `slotType = "auto"` 时 `slotIndex = 0`
- `slotType = "manual"` 时 `slotIndex` 只能是 1、2、3

## UserSettings

领域模型：

```kotlin
data class UserSettings(
    val aspectRatio: AspectRatioMode = AspectRatioMode.Original,
    val filterEnabled: Boolean = false,
    val audioEnabled: Boolean = true,
    val volume: Float = 1.0f,
    val virtualPadVisible: Boolean = true,
    val virtualPadOpacity: Float = 0.55f,
    val virtualPadScale: Float = 1.0f,
    val virtualPadLayout: VirtualPadLayout = VirtualPadLayout.Default,
    val hideVirtualPadWhenGamepadConnected: Boolean = true,
    val autoSaveStateEnabled: Boolean = true,
)

enum class AspectRatioMode {
    Original,
    FourByThree,
    Fullscreen
}

data class VirtualPadLayout(
    val dpadX: Float,
    val dpadY: Float,
    val buttonsX: Float,
    val buttonsY: Float,
    val startSelectX: Float,
    val startSelectY: Float,
) {
    companion object {
        val Default = VirtualPadLayout(
            dpadX = 0.12f,
            dpadY = 0.68f,
            buttonsX = 0.78f,
            buttonsY = 0.66f,
            startSelectX = 0.44f,
            startSelectY = 0.82f,
        )
    }
}
```

DataStore key：

- `aspect_ratio`
- `filter_enabled`
- `audio_enabled`
- `volume`
- `virtual_pad_visible`
- `virtual_pad_opacity`
- `virtual_pad_scale`
- `virtual_pad_layout`
- `hide_virtual_pad_when_gamepad_connected`
- `auto_save_state_enabled`

## GameOverrideSettings

第一版可以实现数据模型，但 UI 可放在高级设置中。

```kotlin
data class GameOverrideSettings(
    val gameId: String,
    val aspectRatio: AspectRatioMode?,
    val filterEnabled: Boolean?,
    val audioEnabled: Boolean?,
    val updatedAt: Long,
)
```

## 数据初始化

首次启动流程：

```text
读取 DataStore 默认设置
-> 初始化 Room
-> 读取内置游戏索引
-> 写入缺失的 LocalGame
-> 复制缺失的私有资源
-> 进入大厅
```
