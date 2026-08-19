package com.richard.retrohall.data.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 轻量 HTTP 文本获取器：按候选源逐个尝试，首个成功即返回。
 *
 * 候选源由 [FcRomsSourceResolver] 展开（支持 Pages / CDN / raw 兜底）。
 */
class HttpTextFetcher {
    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val candidates = FcRomsSourceResolver.expand(url)
        var lastError: Exception? = null
        for (candidate in candidates) {
            try {
                return@withContext fetchFrom(candidate)
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("请求失败：$url")
    }

    private fun fetchFrom(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "RetroHall")
        }
        return connection.use {
            val code = responseCode
            if (code !in 200..299) {
                throw IllegalStateException("请求失败：HTTP $code（$url）")
            }
            inputStream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
        }
    }
}

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}
