package com.richard.retrohall.data.core

import android.content.Context
import android.os.Build
import com.richard.retrohall.data.game.FcRomsSourceResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 核心下载与本地管理：下载 .so 到 files/cores/<abi>/<fileName>，校验 sha256。
 */
class CoreDownloadManager(context: Context) {
    private val filesRoot = context.applicationContext.filesDir
    val supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()

    fun isDownloaded(core: CoreInfo): Boolean {
        return supportedAbis.any { abi ->
            val file = core.fileFor(abi) ?: return@any false
            localFile(file).let { it.isFile && it.length() > 0L && (file.sha256.isBlank() || verifySha256(it, file.sha256)) }
        }
    }

    fun localFile(fileInfo: CoreFileInfo): File = File(filesRoot, "cores/${fileInfo.abi}/${fileInfo.fileName}")

    fun downloadedAbis(core: CoreInfo): List<String> =
        supportedAbis.filter { abi ->
            val info = core.fileFor(abi) ?: return@filter false
            val file = localFile(info)
            file.isFile && file.length() > 0L && (info.sha256.isBlank() || verifySha256(file, info.sha256))
        }

    /**
     * 选择「设备支持 ABI ∩ 清单提供 ABI」的第一个，用于下载时自动匹配架构。
     * 模拟器 x86_64 有 native bridge 但应用自加载 arm so 会 dlopen 失败，因此优先本机真实 ABI。
     */
    fun matchingAbi(core: CoreInfo): String? =
        supportedAbis.firstOrNull { abi -> core.fileFor(abi) != null }

    /** 下载核心到自动匹配的 ABI；找不到匹配时抛错，由调用方提示用户。 */
    suspend fun download(core: CoreInfo) {
        val abi = matchingAbi(core)
            ?: throw IllegalStateException("核心 ${core.displayName} 无本机支持的架构（${supportedAbis.joinToString("、")}）")
        download(core, abi)
    }

    suspend fun download(core: CoreInfo, abi: String) = withContext(Dispatchers.IO) {
        val info = core.fileFor(abi) ?: throw IllegalStateException("核心 ${core.id} 不支持 ABI $abi")
        val target = localFile(info)
        if (isFileValid(target, info)) return@withContext
        target.parentFile?.mkdirs()
        downloadFile(info, target)
        if (!isFileValid(target, info)) {
            target.delete()
            throw IllegalStateException("核心 SHA-256 校验失败（${core.displayName}）")
        }
    }

    suspend fun delete(core: CoreInfo) = withContext(Dispatchers.IO) {
        for (info in core.files) {
            localFile(info).delete()
        }
    }

    private fun isFileValid(file: File, info: CoreFileInfo): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        if (file.length() != info.size) return false
        return info.sha256.isBlank() || verifySha256(file, info.sha256)
    }

    private fun downloadFile(info: CoreFileInfo, target: File) {
        val candidates = FcRomsSourceResolver.expand(info.url)
        var lastError: Exception? = null
        for (candidate in candidates) {
            try {
                downloadFrom(candidate, target)
                return
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("核心下载失败：无可用的下载源")
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
                throw IllegalStateException("核心下载失败：HTTP $code（$sourceUrl）")
            }
            inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (temp.length() == 0L) {
                temp.delete()
                throw IllegalStateException("核心下载结果为空")
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
}

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}
