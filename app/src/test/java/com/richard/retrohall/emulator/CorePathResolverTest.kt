package com.richard.retrohall.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CorePathResolverTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun resolvesNesCoreForFirstAvailableAbi() {
        val filesRoot = temp.newFolder("files")
        val core = filesRoot.resolve("cores/arm64-v8a/fceumm_libretro_android.so")
        core.parentFile?.mkdirs()
        core.writeBytes(byteArrayOf(1))

        val resolver = CorePathResolver(filesRoot, listOf("x86", "arm64-v8a"))

        assertEquals(core.absolutePath, resolver.resolve("FC/NES")?.file?.absolutePath)
    }

    @Test
    fun returnsNullWhenPlatformIsUnsupported() {
        val filesRoot = temp.newFolder("files")
        val core = filesRoot.resolve("cores/arm64-v8a/fceumm_libretro_android.so")
        core.parentFile?.mkdirs()
        core.writeBytes(byteArrayOf(1))

        val resolver = CorePathResolver(filesRoot, listOf("arm64-v8a"))

        assertNull(resolver.resolve("SFC"))
    }
}
