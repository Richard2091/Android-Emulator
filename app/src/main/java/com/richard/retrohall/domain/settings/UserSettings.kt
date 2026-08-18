package com.richard.retrohall.domain.settings

data class UserSettings(
    val aspectRatio: AspectRatio = AspectRatio.Original,
    val filterEnabled: Boolean = false,
    val audioEnabled: Boolean = true,
    val volume: Float = 0.8f,
    val virtualPadVisibility: VirtualPadVisibility = VirtualPadVisibility.Visible,
    val virtualPadOpacity: Float = 0.7f,
    val virtualPadScale: Float = 1.0f,
    val virtualPadLayout: VirtualPadLayout = VirtualPadLayout.Default,
    val controlMode: ControlMode = ControlMode.VirtualPad,
    val hideVirtualPadWhenGamepadConnected: Boolean = true,
    val autoSaveStateEnabled: Boolean = true,
    val gameSpeed: Float = 1f,
) {
    val virtualPadVisible: Boolean
        get() = virtualPadVisibility == VirtualPadVisibility.Visible
}

enum class VirtualPadVisibility {
    Visible,
    AutoHide,
}

enum class AspectRatio {
    Original,
    FourThree,
    SixteenNine,
    Fullscreen,
}

enum class VirtualPadLayout {
    Default,
}

enum class ControlMode {
    VirtualPad,
    Gamepad,
}
