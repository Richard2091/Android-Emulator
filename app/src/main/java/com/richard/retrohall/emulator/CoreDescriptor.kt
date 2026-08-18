package com.richard.retrohall.emulator

/**
 * 模拟器核心元数据：让核心成为一等公民，便于后续扩展多核心/多平台。
 */
data class CoreDescriptor(
    val id: String,
    val displayName: String,
    val platforms: Set<String>,
    val candidateSoNames: List<String>,
)

object CoreDescriptors {
    val NES = listOf(
        CoreDescriptor(
            id = "fceumm",
            displayName = "FCEUmm",
            platforms = setOf("FC", "NES"),
            candidateSoNames = listOf("fceumm_libretro_android.so"),
        ),
        CoreDescriptor(
            id = "mesen",
            displayName = "Mesen",
            platforms = setOf("FC", "NES"),
            candidateSoNames = listOf("mesen_libretro_android.so"),
        ),
    )

    val all: List<CoreDescriptor> = listOf(
        *NES.toTypedArray(),
    )

    fun forPlatform(platform: String): List<CoreDescriptor> {
        val normalized = platform.uppercase()
        val tokens = normalized.split(Regex("[^A-Z0-9]+")).filter { it.isNotBlank() }
        val isNes = "FC" in tokens || "NES" in tokens || normalized.contains("FAMICOM")
        return if (isNes) NES else emptyList()
    }
}
