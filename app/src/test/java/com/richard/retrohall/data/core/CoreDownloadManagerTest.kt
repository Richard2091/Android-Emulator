package com.richard.retrohall.data.core

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 核心下载 ABI 自动匹配：在设备支持列表里找清单提供的第一个 ABI。
 */
@RunWith(RobolectricTestRunner::class)
class CoreDownloadManagerTest {

    private fun coreWithAbis(vararg abis: String): CoreInfo = CoreInfo(
        id = "fceumm",
        displayName = "FCEUmm",
        platformIds = listOf("nes"),
        runtimeFamily = "libretro",
        version = "nightly",
        license = "GPL-2.0-or-later",
        licenseUrl = "",
        sourceUrl = "",
        defaultForPlatform = true,
        files = abis.map { abi ->
            CoreFileInfo(
                abi = abi,
                url = "https://example.test/$abi/fceumm_libretro_android.so",
                fileName = "fceumm_libretro_android.so",
                size = 100L,
                sha256 = "0".repeat(64),
                minSdk = 23,
            )
        },
    )

    private fun manager() = CoreDownloadManager(ApplicationProvider.getApplicationContext())

    @Test
    fun matchingAbiPicksFirstDeviceSupportedAbi() {
        val core = coreWithAbis("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        val manager = manager()
        val abi = manager.matchingAbi(core)
        // 应为「设备支持 ABI ∩ 清单 ABI」的首个（Robolectric 设备 ABI 与测试机相关）。
        val expected = manager.supportedAbis.firstOrNull { a -> core.files.any { it.abi == a } }
        assertEquals(expected, abi)
    }

    @Test
    fun matchingAbiSkipsAbisMissingFromManifest() {
        // 清单只提供 arm64-v8a；若设备支持则匹配到它，否则 null。
        val core = coreWithAbis("arm64-v8a")
        val manager = manager()
        assertEquals(
            if ("arm64-v8a" in manager.supportedAbis) "arm64-v8a" else null,
            manager.matchingAbi(core),
        )
    }

    @Test
    fun matchingAbiReturnsNullWhenNoIntersection() {
        val core = coreWithAbis("mips64")
        assertNull(manager().matchingAbi(core))
    }

    @Test
    fun matchingAbiHandlesEmptyFiles() {
        val core = coreWithAbis()
        assertNull(manager().matchingAbi(core))
    }
}
