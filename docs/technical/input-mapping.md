# Input Mapping

## 统一动作

系统内部只传递以下动作：

```kotlin
enum class GameAction {
    Up,
    Down,
    Left,
    Right,
    Confirm,
    Back,
    Menu,
    NesA,
    NesB,
    Start,
    Select,
}
```

## KeyEvent 映射

Android 键值映射：

| Android KeyEvent | GameAction |
| --- | --- |
| `KEYCODE_DPAD_UP` | `Up` |
| `KEYCODE_DPAD_DOWN` | `Down` |
| `KEYCODE_DPAD_LEFT` | `Left` |
| `KEYCODE_DPAD_RIGHT` | `Right` |
| `KEYCODE_DPAD_CENTER` | `Confirm` |
| `KEYCODE_ENTER` | `Confirm` |
| `KEYCODE_BACK` | `Back` |
| `KEYCODE_MENU` | `Menu` |
| `KEYCODE_BUTTON_A` | `NesA` |
| `KEYCODE_BUTTON_B` | `NesB` |
| `KEYCODE_BUTTON_START` | `Start` |
| `KEYCODE_BUTTON_SELECT` | `Select` |

## 场景消费规则

大厅：

- 方向动作移动焦点。
- `Confirm` 打开当前游戏或当前入口。
- `Back` 返回上一级；大厅根页面不退出 App，除非用户再次确认。
- `Menu` 打开设置或上下文菜单。

游戏中：

- `NesA`、`NesB`、`Start`、`Select` 传给模拟器。
- 方向动作传给模拟器。
- `Menu` 打开暂停菜单。
- `Back` 打开暂停菜单，不直接退出。

暂停菜单：

- 方向动作移动焦点。
- `Confirm` 执行当前菜单项。
- `Back` 或 `Menu` 关闭暂停菜单并继续游戏。

设置页：

- 方向动作移动焦点或调整控件。
- `Confirm` 切换开关或进入选项。
- `Back` 返回上一页。

## 触摸输入

触摸输入不直接操作业务状态，必须转换为动作或 UI 事件：

- 点击游戏封面：等价于选中并确认。
- 点击分类：切换分类。
- 点击虚拟方向键：发出方向动作。
- 点击虚拟 A/B：发出 `NesA` / `NesB`。
- 点击 Start/Select：发出 `Start` / `Select`。
- 点击菜单按钮：发出 `Menu`。

## 按下和松开

游戏内输入必须区分按下和松开：

```kotlin
fun sendInput(action: GameAction, pressed: Boolean)
```

大厅和菜单可以只消费按下事件，忽略松开事件。

## 测试要求

`KeyEventMapperTest` 至少覆盖：

- DPAD 四方向
- Confirm
- Back
- Menu
- NES A/B
- Start/Select
- 未识别 key 返回 null
