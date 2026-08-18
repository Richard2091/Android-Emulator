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
 * 封面 URL 默认指向 raw.githubusercontent.com，单 IP 高频请求可能触发 429；
 * 因此下载前会把域名重写为 GitHub Pages 对应地址（无此限流）。
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

        runCatching {
            val connection = (URL(rewriteForPages(coverUrl)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "RetroHall")
                setRequestProperty("Accept", "image/webp,image/*,*/*;q=0.8")
            }
            connection.use {
                if (responseCode !in 200..299) return@runCatching
                target.parentFile?.mkdirs()
                val temp = File(target.parentFile, "${target.name}.download")
                inputStream.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
                if (temp.length() > 0L) {
                    if (target.exists()) target.delete()
                    temp.renameTo(target)
                } else {
                    temp.delete()
                }
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

    private fun rewriteForPages(url: String): String {
        if (!url.contains("raw.githubusercontent.com")) return url
        val path = url.substringAfter("raw.githubusercontent.com").substringAfter("/", "")
        // 形如 Richard2091/FC_ROMS/<ref>/ROM/0001/cover.webp -> 跳过仓库名与 ref
        val romIndex = path.indexOf("/ROM/")
        val romPath = if (romIndex >= 0) path.substring(romIndex) else "/$path"
        return "https://richard2091.github.io/FC_ROMS$romPath"
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