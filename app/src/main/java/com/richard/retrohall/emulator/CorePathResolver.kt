package com.richard.retrohall.emulator

import java.io.File

data class CoreResolution(
    val descriptor: CoreDescriptor,
    val file: File,
)

class CorePathResolver(
    private val filesRoot: File,
    private val supportedAbis: List<String>,
) {
    fun resolve(platform: String): CoreResolution? {
        val descriptors = CoreDescriptors.forPlatform(platform)
        if (descriptors.isEmpty()) return null

        // ABI 优先 → 核心回退顺序（FCEUmm → Mesen）。
        for (abi in supportedAbis) {
            for (descriptor in descriptors) {
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
}
