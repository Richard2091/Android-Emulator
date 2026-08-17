package com.richard.retrohall.emulator

import java.io.File

class CorePathResolver(
    private val filesRoot: File,
    private val supportedAbis: List<String>,
) {
    fun resolve(platform: String): File? {
        if (!isNesPlatform(platform)) return null

        val candidateNames = listOf(
            "fceumm_libretro_android.so",
            "nestopia_libretro_android.so",
            "quicknes_libretro_android.so",
        )
        return supportedAbis.asSequence()
            .flatMap { abi -> candidateNames.asSequence().map { name -> File(filesRoot, "cores/$abi/$name") } }
            .firstOrNull { it.isFile && it.length() > 0L }
    }

    private fun isNesPlatform(platform: String): Boolean {
        val normalized = platform.uppercase()
        val tokens = normalized.split(Regex("[^A-Z0-9]+")).filter { it.isNotBlank() }
        return "FC" in tokens ||
            "NES" in tokens ||
            normalized.contains("FAMICOM")
    }
}
