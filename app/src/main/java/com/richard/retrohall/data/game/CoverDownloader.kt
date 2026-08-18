package com.richard.retrohall.data.game

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 懒加载下载远程封面到本地缓存。
 *
 * 封面 URL 默认指向 raw.githubusercontent.com，单 IP 高频请求可能触发 429 且大陆连通性差；
 * 因此下载前会按 FcRomsSourceResolver 展开为 GitHub Pages / jsDelivr 等候选源逐一尝试。
 */
class CoverDownloader(context: Context) {
    private val filesRoot = context.applicationContext.filesDir
    private val coverRoot = File(filesRoot, "metadata-cache/covers")

    /**
     * 将远程封面下载到本地缓存。
     *
     * @param gameId 游戏 ID，用于缓存文件命名。
     * @param coverUrl 远程封面地址。
     * @return 本地缓存文件路径；下载失败时返回原始 URL。
     */
    suspend fun prepareCover(gameId: String, coverUrl: String): String = withContext(Dispatchers.IO) {
        if (!coverUrl.startsWith("http://") && !coverUrl.startsWith("https://")) return@withContext coverUrl
        val target = cacheTarget(gameId, coverUrl)
        if (target.isFile && target.length() > 0L) return@withContext target.absolutePath

        val candidates = FcRomsSourceResolver.expand(coverUrl)
        runCatching {
            for (candidate in candidates) {
                if (downloadTo(candidate, target)) break
            }
        }
        if (target.isFile && target.length() > 0L) target.absolutePath else coverUrl
    }

    /**
     * 清理指定游戏的封面缓存。
     */
    fun deleteCover(gameId: String) {
        coverRoot.listFiles()?.filter { it.name.startsWith("${safeFileName(gameId)}-") }?.forEach { it.delete() }
    }

    private fun cacheTarget(gameId: String, coverUrl: String): File {
        val ext = when {
            coverUrl.substringAfter('?').substringBefore('?').lowercase().endsWith(".webp") -> ".webp"
            coverUrl.lowercase().contains(".webp") -> ".webp"
            else -> ".jpg"
        }
        return File(coverRoot, "${safeFileName(gameId)}-${sha1(coverUrl).take(12)}$ext")
    }

    private fun downloadTo(sourceUrl: String, target: File): Boolean {
        val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "RetroHall")
            setRequestProperty("Accept", "image/webp,image/*,*/*;q=0.8")
        }
        connection.use {
            if (responseCode !in 200..299) return false
            target.parentFile?.mkdirs()
            val temp = File(target.parentFile, "${target.name}.download")
            temp.delete()
            inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (temp.length() > 0L) {
                if (target.exists()) target.delete()
                temp.renameTo(target)
                return true
            }
            temp.delete()
        }
        return false
    }

    private fun safeFileName(value: String): String = value.replace(Regex("""[\\/:*?"<>|]"""), "_")

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}