package com.richard.retrohall.domain.settings

data class UserSettings(
    val aspectRatio: AspectRatio = AspectRatio.Original,
    val filterEnabled: Boolean = false,
    val audioEnabled: Boolean = true,
    val volume: Float = 0.8f,
    val virtualPadVisible: Boolean = true,
    val virtualPadOpacity: Float = 0.7f,
    val virtualPadScale: Float = 1.0f,
    val virtualPadLayout: VirtualPadLayout = VirtualPadLayout.Default,
    val hideVirtualPadWhenGamepadConnected: Boolean = true,
    val autoSaveStateEnabled: Boolean = true,
)

enum class AspectRatio {
    Original,
    FourThree,
    Fullscreen,
}

enum class VirtualPadLayout {
    Default,
}
