package com.richard.retrohall.data.core

import androidx.test.core.app.ApplicationProvider
import com.richard.retrohall.data.game.HttpTextFetcher
import com.richard.retrohall.data.settings.ResourceSourceStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 核心清单 URL 解析：相对路径按 manifest 所在目录解析（../cores/... 上溯一级到仓库根）。
 */
@RunWith(RobolectricTestRunner::class)
class CoreCatalogClientTest {

    private val manifestJson = """
        {
          "schemaVersion": 1,
          "catalogId": "retrogame-cores",
          "catalogName": "RetroGame 核心仓库",
          "generatedAt": "2026-08-20T00:00:00Z",
          "cores": [
            {
              "id": "fceumm",
              "displayName": "FCEUmm",
              "platformIds": ["nes"],
              "runtimeFamily": "libretro",
              "version": "nightly-20260819",
              "license": "GPL-2.0-or-later",
              "licenseUrl": "../licenses/fceumm.txt",
              "sourceUrl": "https://github.com/libretro/libretro-fceumm",
              "defaultForPlatform": true,
              "files": [
                {
                  "abi": "x86_64",
                  "url": "../cores/nes/fceumm/x86_64/fceumm_libretro_android.so",
                  "fileName": "fceumm_libretro_android.so",
                  "size": 2840216,
                  "sha256": "f5526f457d4b707037d038f5d2c472d511b989886007e36a7d4e3436d3617799",
                  "minSdk": 23
                },
                {
                  "abi": "arm64-v8a",
                  "url": "../cores/nes/fceumm/arm64-v8a/fceumm_libretro_android.so",
                  "fileName": "fceumm_libretro_android.so",
                  "size": 2878032,
                  "sha256": "3a383500cca96e793ca30f74c5737e8eb7e797691aa376a4d18fe146c982c66e",
                  "minSdk": 23
                }
              ]
            }
          ]
        }
    """.trimIndent()

    private fun clientWith(): CoreCatalogClient {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fetcher = object : HttpTextFetcher() {
            override suspend fun fetch(url: String): String = manifestJson
        }
        return CoreCatalogClient(ResourceSourceStore(context), fetcher)
    }

    @Test
    fun resolvesRelativeCoreUrlsAgainstManifestDirectory() = runBlocking {
        val catalog = clientWith().fetchCatalog()

        val core = catalog.forPlatform("nes").first()
        val x86_64 = core.files.first { it.abi == "x86_64" }
        val arm64 = core.files.first { it.abi == "arm64-v8a" }

        // ../cores/... 相对 catalog/ 上溯到仓库根，不丢失 RetroGame-Cores 段。
        assertEquals(
            "https://richard2091.github.io/RetroGame-Cores/cores/nes/fceumm/x86_64/fceumm_libretro_android.so",
            x86_64.url,
        )
        assertEquals(
            "https://richard2091.github.io/RetroGame-Cores/cores/nes/fceumm/arm64-v8a/fceumm_libretro_android.so",
            arm64.url,
        )
    }

    @Test
    fun defaultForPrefersDefaultPlatformCore() = runBlocking {
        val catalog = clientWith().fetchCatalog()
        assertEquals("fceumm", catalog.defaultFor("nes")?.id)
    }
}
