package com.richard.retrohall.data.game

import android.content.Context
import com.richard.retrohall.domain.game.GameFileInfo
import com.richard.retrohall.domain.game.LocalGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 游戏内容下载：支持 files[] 多文件下载、下载后校验 sha256、按 availability 控制。
 *
 * 下载目录：files/content-cache/<gameId>/<fileId>/<fileName>
 */
class ContentDownloadManager(context: Context) {
    private val filesRoot = context.applicationContext.filesDir
    private val contentRoot = File(filesRoot, "content-cache")

    fun isDownloaded(gameId: String): Boolean {
        val dir = File(contentRoot, gameId)
        return dir.listFiles()
            ?.flatMap { it.listFiles()?.toList() ?: emptyList() }
            ?.any { it.isFile && it.length() > 0L } == true
    }

    fun isFileDownloaded(gameId: String, fileInfo: GameFileInfo): Boolean {
        val file = localFile(gameId, fileInfo)
        return file.isFile && file.length() > 0L && (fileInfo.sha256.isBlank() || verifySha256(file, fileInfo.sha256))
    }

    fun localFile(gameId: String, fileInfo: GameFileInfo): File {
        val fileId = fileInfo.id.ifBlank { "main" }
        return File(File(contentRoot, gameId), "$fileId/${fileNameOf(fileInfo)}")
    }

    fun localSize(gameId: String): Long? {
        val dir = File(contentRoot, gameId)
        val files = dir.listFiles()?.flatMap { it.listFiles()?.toList() ?: emptyList() }
            ?.filter { it.isFile && it.length() > 0L } ?: return null
        return files.sumOf { it.length() }.takeIf { it > 0L }
    }

    suspend fun deleteLocal(gameId: String) = withContext(Dispatchers.IO) {
        File(contentRoot, gameId).deleteRecursively()
    }

    /**
     * 下载全部可下载文件，主文件路径回写到 [LocalGame.romPath]。
     *
     * @param files 游戏详情中的文件列表。
     */
    suspend fun prepare(game: LocalGame, files: List<GameFileInfo>): LocalGame = withContext(Dispatchers.IO) {
        val downloadable = files.filter { it.isDownloadable }
        if (downloadable.isEmpty()) return@withContext game

        var primaryPath: String? = null
        for (fileInfo in downloadable) {
            val target = localFile(game.id, fileInfo)
            if (isFileDownloaded(game.id, fileInfo)) {
                if (fileInfo.role == "primary" || fileInfo.id == "main") primaryPath = target.absolutePath
                continue
            }
            target.parentFile?.mkdirs()
            download(fileInfo.url, target, fileInfo.sha256)
            if (fileInfo.role == "primary" || fileInfo.id == "main") primaryPath = target.absolutePath
        }
        val primary = primaryPath ?: game.romPath
        game.copy(romPath = primary)
    }

    private fun download(sourceUrl: String, target: File, expectedSha256: String) {
        val candidates = FcRomsSourceResolver.expand(sourceUrl)
        var lastError: Exception? = null
        for (candidate in candidates) {
            try {
                downloadFrom(candidate, target)
                if (expectedSha256.isNotBlank() && !verifySha256(target, expectedSha256)) {
                    target.delete()
                    throw IllegalStateException("SHA-256 校验失败（$sourceUrl）")
                }
                return
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("内容下载失败：无可用的下载源")
    }

    private fun downloadFrom(sourceUrl: String, target: File) {
        val temp = File(target.parentFile, "${target.name}.download")
        temp.delete()
        val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 120_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "RetroHall")
        }
        connection.use {
            val code = responseCode
            if (code !in 200..299) {
                throw IllegalStateException("内容下载失败：HTTP $code（$sourceUrl）")
            }
            target.parentFile?.mkdirs()
            inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (temp.length() == 0L) {
                temp.delete()
                throw IllegalStateException("内容下载结果为空")
            }
            if (target.exists()) target.delete()
            temp.renameTo(target)
        }
    }

    private fun verifySha256(file: File, expected: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.equals(expected, ignoreCase = true)
    }

    private fun fileNameOf(fileInfo: GameFileInfo): String {
        val raw = fileInfo.path.substringAfterLast('/').ifBlank { fileInfo.url.substringAfterLast('/').substringBefore('?') }
        return raw.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "content.bin" }
    }
}

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}
