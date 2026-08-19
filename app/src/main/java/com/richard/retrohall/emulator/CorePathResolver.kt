package com.richard.retrohall.emulator

import com.richard.retrohall.data.core.CoreCatalog
import com.richard.retrohall.data.core.CoreSelectionStore
import java.io.File

data class CoreResolution(
    val descriptor: CoreDescriptor,
    val file: File,
)

/**
 * 核心路径解析：按「用户选择 -> 默认核心 -> 内置核心」顺序回退。
 *
 * 已下载核心位于 files/cores/<abi>/<fileName>。
 */
class CorePathResolver(
    private val filesRoot: File,
    private val supportedAbis: List<String>,
    private val coreSelectionStore: CoreSelectionStore? = null,
) {
    /**
     * 解析平台核心。catalog 为 null 时回退硬编码内置核心。
     */
    suspend fun resolve(platform: String, catalog: CoreCatalog? = null): CoreResolution? {
        val descriptors = catalogDescriptors(platform, catalog)
            .ifEmpty { CoreDescriptors.forPlatform(platform) }
        if (descriptors.isEmpty()) return null

        val selectedId = coreSelectionStore?.selectedCoreFor(platform)
        val ordered = descriptors.sortedByDescending { it.id == selectedId }

        for (abi in supportedAbis) {
            for (descriptor in ordered) {
                for (name in descriptor.candidateSoNames) {
                    val file = File(filesRoot, "cores/$abi/$name")
                    if (file.isFile && file.length() > 0L) {
                        return CoreResolution(descriptor, file)
                    }
                }
            }
        }
        return null
    }

    private fun catalogDescriptors(platform: String, catalog: CoreCatalog?): List<CoreDescriptor> {
        if (catalog == null) return emptyList()
        val normalized = platform.uppercase()
        val platformId = when {
            "FC" in normalized || "NES" in normalized || normalized.contains("FAMICOM") -> "nes"
            else -> return emptyList()
        }
        return catalog.forPlatform(platformId).map { core ->
            val soNames = core.files.mapNotNull { it.fileName }.ifEmpty { listOf("${core.id}_libretro_android.so") }
            CoreDescriptor(
                id = core.id,
                displayName = core.displayName,
                platforms = setOf("FC", "NES"),
                candidateSoNames = soNames,
            )
        }
    }
}
