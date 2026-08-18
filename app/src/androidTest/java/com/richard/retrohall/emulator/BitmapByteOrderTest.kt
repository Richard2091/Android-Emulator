package com.richard.retrohall.emulator

import android.graphics.Bitmap
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

/**
 * 确认 Bitmap.copyPixelsFromBuffer 对 ARGB_8888 期望的字节序（RGB 还是 BGR）。
 */
class BitmapByteOrderTest {

    @Test
    fun determineByteOrder() {
        // buffer A: 字节序 R,G,B,A -> R=255,G=0,B=0
        val rgba = byteArrayOf(255.toByte(), 0, 0, 255.toByte())
        // buffer B: 字节序 B,G,R,A -> B=255
        val bgra = byteArrayOf(0, 0, 255.toByte(), 255.toByte())

        val bmpRgba = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bmpRgba.copyPixelsFromBuffer(ByteBuffer.wrap(rgba))
        val cRgba = bmpRgba.getPixel(0, 0)

        val bmpBgra = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bmpBgra.copyPixelsFromBuffer(ByteBuffer.wrap(bgra))
        val cBgra = bmpBgra.getPixel(0, 0)

        Log.i("RetroHallColor", "RGBA buffer -> 0x${Integer.toHexString(cRgba)}")
        Log.i("RetroHallColor", "BGRA buffer -> 0x${Integer.toHexString(cBgra)}")

        if (cRgba == 0xFFFF0000.toInt()) {
            Log.i("RetroHallColor", "copyPixelsFromBuffer 期望 RGB 字节序")
        } else if (cRgba == 0xFF0000FF.toInt()) {
            Log.i("RetroHallColor", "copyPixelsFromBuffer 期望 BGR 字节序")
        } else {
            Log.i("RetroHallColor", "其他: $cRgba")
        }
        assertEquals("颜色转换应可预期", 0xFFFF0000.toInt(), cRgba)
    }
}
