package com.richard.retrohall.data.game

import android.content.Context
import com.richard.retrohall.domain.game.LocalGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class RomDownloadManager(context: Context) {
    private val filesRoot = context.applicationContext.filesDir

    fun isDownloaded(gameId: String): Boolean {
        val dir = File(filesRoot, "rom-cache/$gameId")
        return dir.listFiles()?.any { it.isFile && it.length() > 0L } == true
    }

    /**
     * 本地注入的 ROM（如 files/roms 下的私有资源）也视为已下载。
     */
    fun isDownloaded(game: LocalGame): Boolean {
        if (!game.romPath.startsWith("http://") && !game.romPath.startsWith("https://")) {
            val file = File(game.romPath)
            return file.isFile && file.length() > 0L
        }
        return isDownloaded(game.id)
    }

    fun localSize(gameId: String): Long? {
        val dir = File(filesRoot, "rom-cache/$gameId")
        val files = dir.listFiles()?.filter { it.isFile && it.length() > 0L } ?: return null
        return files.sumOf { it.length() }.takeIf { it > 0L }
    }

    suspend fun deleteLocal(game: LocalGame) = withContext(Dispatchers.IO) {
        File(filesRoot, "rom-cache/${game.id}").deleteRecursively()
    }

    suspend fun prepare(game: LocalGame): LocalGame = withContext(Dispatchers.IO) {
        if (!game.romPath.startsWith("http://") && !game.romPath.startsWith("https://")) {
            return@withContext game
        }

        val target = File(File(filesRoot, "rom-cache/${game.id}"), fileNameFromUrl(game.romPath))
        if (!target.exists() || target.length() == 0L) {
            download(game.romPath, target)
        }
        game.copy(romPath = target.absolutePath)
    }

    private fun download(sourceUrl: String, target: File) {
        val candidates = FcRomsSourceResolver.expand(sourceUrl)
        var lastError: Exception? = null
        for (candidate in candidates) {
            try {
                downloadFrom(candidate, target)
                return
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("ROM 下载失败：无可用的下载源")
    }

    private fun downloadFrom(sourceUrl: String, target: File) {
        val temp = File(target.parentFile, "${target.name}.download")
        temp.delete()
        val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 60_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "RetroHall")
        }
        connection.use {
            val code = responseCode
            if (code !in 200..299) {
                throw IllegalStateException("ROM 下载失败：HTTP $code（$sourceUrl）")
            }
            target.parentFile?.mkdirs()
            inputStream.use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (temp.length() == 0L) {
                temp.delete()
                throw IllegalStateException("ROM 下载结果为空")
            }
            if (target.exists()) target.delete()
            temp.renameTo(target)
        }
    }

    private fun fileNameFromUrl(sourceUrl: String): String {
        val rawName = sourceUrl.substringAfterLast('/').substringBefore('?').ifBlank { "game.nes" }
        return rawName.replace(Regex("""[\\/:*?"<>|]"""), "_")
    }
}

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}
