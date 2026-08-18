package com.richard.retrohall.data.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CacheManagerTest {
    @Test
    fun formatBytesHandlesZero() {
        assertEquals("0 B", CacheManager.formatBytes(0L))
    }

    @Test
    fun formatBytesHandlesByteSteps() {
        assertEquals("512.0 B", CacheManager.formatBytes(512L))
        assertEquals("1.0 KB", CacheManager.formatBytes(1024L))
        assertEquals("1.5 MB", CacheManager.formatBytes(1024L * 1024L + 512L * 1024L))
        assertEquals("1.0 GB", CacheManager.formatBytes(1024L * 1024L * 1024L))
    }

    @Test
    fun formatBytesNeverExceedsGigabyteUnit() {
        assertEquals("1024.0 GB", CacheManager.formatBytes(1024L * 1024L * 1024L * 1024L))
    }

    @Test
    fun clearKeepsRomsAndSavesButRemovesMetadataAndSystemCache() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filesDir = context.filesDir
        val cacheDir = context.cacheDir

        val romDir = File(filesDir, "rom-cache/game-1")
        romDir.mkdirs()
        val romFile = File(romDir, "game.nes")
        romFile.writeText("rom")

        val saveDir = File(filesDir, "saves/states/game-1")
        saveDir.mkdirs()
        val saveFile = File(saveDir, "manual-1.state")
        saveFile.writeText("save")

        val coverDir = File(filesDir, "metadata-cache/covers")
        coverDir.mkdirs()
        val coverFile = File(coverDir, "cover.jpg")
        coverFile.writeText("cover")
        val lookupDir = File(filesDir, "metadata-cache/hasheous")
        lookupDir.mkdirs()
        File(lookupDir, "lookup.json").writeText("lookup")

        val tmpFile = File(cacheDir, "temp.bin")
        tmpFile.writeText("tmp")

        CacheManager(context).clear()

        assertTrue("已下载 ROM 必须保留", romFile.isFile)
        assertTrue("存档必须保留", saveFile.isFile)
        assertFalse("封面缓存必须删除", coverFile.exists())
        assertFalse("元数据缓存目录必须删除", File(filesDir, "metadata-cache").exists())
        assertEquals("系统缓存必须清空", 0, cacheDir.listFiles()?.size ?: 0)
    }
}
