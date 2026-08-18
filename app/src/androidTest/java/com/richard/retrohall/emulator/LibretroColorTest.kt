package com.richard.retrohall.emulator

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.richard.retrohall.data.assets.PrivateAssetManifest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/**
 * 直接分析 native 帧缓冲（XRGB8888）的颜色分布，判断 RGB565 转换是否正确、
 * 是否发生 R/B 通道交换，并把一帧保存为 PNG 供人工查看。
 */
class LibretroColorTest {

    @Test
    fun frameColorsAreSane() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ensurePrivateAssets(context)
        val filesDir = context.filesDir
        val abi = Build.SUPPORTED_ABIS.first()
        val core = File(filesDir, "cores/$abi/fceumm_libretro_android.so")
        val rom = File(filesDir, "roms/contra.nes")

        val host = LibretroHost()
        assertTrue("loadCore", host.loadCore(core.absolutePath))
        assertTrue("loadGame", host.loadGame(rom.absolutePath))

        // 跑 180 帧覆盖标题画面进入游戏
        repeat(180) { host.runFrame() }

        val info = host.getFrameInfo()
        requireNotNull(info) { "frame not ready" }
        assertTrue("frame not ready", info.ready)
        val buffer = ByteArray(info.width * info.height * 4)
        val written = host.pollFrame(buffer)
        assertTrue("pollFrame failed", written > 0)

        // 统计通道分布
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0L
        var bluePx = 0L
        var greenPx = 0L
        var redPx = 0L
        var brightPx = 0L
        val colorCount = HashMap<Int, Int>()

        for (i in 0 until written step 4) {
            val r = buffer[i].toInt() and 0xFF
            val g = buffer[i + 1].toInt() and 0xFF
            val b = buffer[i + 2].toInt() and 0xFF
            rSum += r; gSum += g; bSum += b; count++
            if (b > 100 && b > r && b > g) bluePx++
            if (g > 90 && g > r && g > b) greenPx++
            if (r > 100 && r > g && r > b) redPx++
            if (r + g + b > 300) brightPx++
            val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            colorCount[key] = (colorCount[key] ?: 0) + 1
        }

        val avgR = rSum * 100 / count
        val avgG = gSum * 100 / count
        val avgB = bSum * 100 / count
        Log.i("RetroHallColor", "size=${info.width}x${info.height} count=$count")
        Log.i("RetroHallColor", "avgR=$avgR avgG=$avgG avgB=$avgB")
        Log.i("RetroHallColor", "blue=$bluePx(${bluePx * 100 / count}%) green=$greenPx(${greenPx * 100 / count}%) red=$redPx(${redPx * 100 / count}%) bright=$brightPx(${brightPx * 100 / count}%)")
        Log.i("RetroHallColor", "top colors: ${colorCount.entries.sortedByDescending { it.value }.take(10)}")

        // 保存 PNG 到应用私有目录供查看
        val bmp = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(buffer, 0, written))
        val out = File(context.filesDir, "frame.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Log.i("RetroHallColor", "saved frame to ${out.absolutePath} size=${out.length()}")

        // 写标志文件，供外部 pull 帧文件
        File(context.filesDir, "frame-ready").writeText("done")
        Log.i("RetroHallColor", "frame-ready marker written, keeping alive 30s")
        Thread.sleep(30_000)

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
    }
}
