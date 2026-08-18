package com.richard.retrohall.data.cache

import android.content.Context
import com.richard.retrohall.domain.settings.CacheMaintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * 负责统计与清理应用缓存（封面、元数据等）。
 *
 * 清理时只删除可再生的元数据缓存：远程封面、Hasheous 查询缓存与系统临时缓存。
 * 已下载的 ROM（rom-cache）、存档（saves）以及打包私有资源（roms/covers/cores）一律保留。
 */
class CacheManager(private val context: Context) : CacheMaintenance {
    private val filesRoot get() = context.applicationContext.filesDir
    private val cacheRoot get() = context.applicationContext.cacheDir

    private fun cacheTargets(): List<File> = listOf(
        File(filesRoot, "metadata-cache"),
        cacheRoot,
    )

    override suspend fun totalSize(): Long = withContext(Dispatchers.IO) {
        cacheTargets().sumOf { it.dirSize() }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            // 删除封面与元数据缓存目录本身。
            File(filesRoot, "metadata-cache").deleteRecursively()
            // 系统缓存目录只清空内容，保留目录本身。
            cacheRoot.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    private fun File.dirSize(): Long {
        return when {
            isDirectory -> listFiles()?.sumOf { it.dirSize() } ?: 0L
            isFile -> length()
            else -> 0L
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val exp = minOf((63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10, units.size - 1)
            val value = bytes.toDouble() / (1L shl (exp * 10)).toDouble()
            return String.format(Locale.US, "%.1f %s", value, units[exp])
        }
    }

    override fun formatBytes(bytes: Long): String = CacheManager.formatBytes(bytes)
}
