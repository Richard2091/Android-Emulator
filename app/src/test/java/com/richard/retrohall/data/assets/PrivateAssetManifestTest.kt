package com.richard.retrohall.data.assets

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrivateAssetManifestTest {
    @Test
    fun parsesGamesAndCores() {
        val manifest = PrivateAssetManifest.parse(
            """
            {
              "games": [
                {
                  "id": "sample-nes",
                  "title": "Sample NES",
                  "platform": "NES",
                  "category": "Action",
                  "rom": "roms/sample.nes",
                  "cover": "covers/sample.png"
                }
              ],
              "cores": {
                "NES": {
                  "arm64-v8a": "cores/arm64-v8a/fceumm_libretro_android.so"
                }
              }
            }
            """.trimIndent()
        )

        assertEquals(1, manifest.games.size)
        assertEquals("sample-nes", manifest.games.first().id)
        assertEquals("cores/arm64-v8a/fceumm_libretro_android.so", manifest.cores["NES"]?.get("arm64-v8a"))
    }
}
