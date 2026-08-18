package com.richard.retrohall.emulator

import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.richard.retrohall.data.assets.PrivateAssetManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 在真实 Android 设备/模拟器上验证 libretro 核心链路：
 * dlopen FCEUmm → 加载 ROM → 逐帧运行 → 产出画面帧与音频。
 */
class LibretroCoreInstrumentedTest {
    @Test
    fun loadsFceummAndRunsContra() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ensurePrivateAssets(context)
        val filesDir = context.filesDir
        val abi = Build.SUPPORTED_ABIS.first()
        val core = File(filesDir, "cores/$abi/fceumm_libretro_android.so")
        val rom = File(filesDir, "roms/contra.nes")

        Log.i("RetroHallTest", "ABI=$abi")
        Log.i("RetroHallTest", "core=$core exists=${core.isFile} size=${if (core.isFile) core.length() else -1}")
        Log.i("RetroHallTest", "rom=$rom exists=${rom.isFile} size=${if (rom.isFile) rom.length() else -1}")

        assertTrue("FCEUmm 核心不存在: $core", core.isFile)
        assertTrue("ROM 不存在: $rom", rom.isFile)

        val host = LibretroHost()
        Log.i("RetroHallTest", "nativeVersion=${host.nativeVersion()}")

        assertTrue("loadCore 失败", host.loadCore(core.absolutePath))

        val av = host.getAvInfo()
        Log.i("RetroHallTest", "avInfo=$av")

        assertTrue("loadGame 失败", host.loadGame(rom.absolutePath))

        var framesOk = 0
        repeat(180) {
            if (host.runFrame()) framesOk++
        }
        Log.i("RetroHallTest", "runFrame ok count=$framesOk/180")
        assertTrue("runFrame 多次失败", framesOk > 150)

        val info = host.getFrameInfo()
        Log.i("RetroHallTest", "frameInfo=$info")
        assertNotNull("getFrameInfo 返回空", info)
        assertTrue("帧未就绪", info!!.ready)
        assertTrue("帧尺寸异常 w=${info.width} h=${info.height}", info.width > 0 && info.height > 0)

        val buffer = ByteArray(info.width * info.height * 4)
        val written = host.pollFrame(buffer)
        assertEquals("pollFrame 字节数不匹配", info.width * info.height * 4, written)

        var nonzero = 0
        for (i in buffer.indices) {
            if (buffer[i] != 0.toByte()) {
                nonzero++
                if (nonzero > 500) break
            }
        }
        Log.i("RetroHallTest", "frame nonzero samples=$nonzero")
        assertTrue("画面帧全黑", nonzero > 500)

        val audio = ByteArray(8192)
        var totalAudio = 0
        repeat(30) {
            totalAudio += host.drainAudio(audio)
        }
        Log.i("RetroHallTest", "audio drained bytes=$totalAudio")
        assertTrue("音频输出异常", totalAudio > 0)

        host.unloadCore()
    }

    private fun ensurePrivateAssets(context: android.content.Context) {
        val manifestText = context.assets
            .open("retrohall_private/manifest.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val manifest = PrivateAssetManifest.parse(manifestText)
        val filesRoot = context.filesDir

        manifest.games.forEach { game ->
            copyAsset(context, "retrohall_private/${game.rom}", File(filesRoot, game.rom))
            if (game.cover.isNotBlank()) {
                copyAsset(context, "retrohall_private/${game.cover}", File(filesRoot, game.cover))
            }
        }
        manifest.cores.values.forEach { abiMap ->
            abiMap.values.forEach { corePath ->
                copyAsset(context, "retrohall_private/$corePath", File(filesRoot, corePath))
            }
        }
    }

    private fun copyAsset(context: android.content.Context, assetPath: String, target: File) {
        if (target.isFile) return
        target.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        Log.i("RetroHallTest", "copied $assetPath -> $target (${target.length()} bytes)")
    }
}
