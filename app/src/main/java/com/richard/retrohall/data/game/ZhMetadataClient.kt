package com.richard.retrohall.data.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 单个游戏的中文元数据（来自 GameWiki + zdg-kinlon 索引，预生成后随游戏库发布）。
 */
data class ZhMetadataEntry(
    val zhTitle: String = "",
    val titleCn: String = "",
    val developer: String = "",
    val publisher: String = "",
    val japanDate: String = "",
    val intro: String = "",
)

/**
 * 从 FC_ROMS Pages 拉取预生成的中文元数据清单（zh-metadata.v1.json）。
 *
 * 与游戏索引 manifest 同源发布，按游戏 sourceId（如 "0001"）索引。
 */
class ZhMetadataClient(
    private val metadataUrls: List<String> = DEFAULT_ZH_METADATA_URLS,
) {
    @Volatile
    private var cached: Map<String, ZhMetadataEntry>? = null

    suspend fun load(): Map<String, ZhMetadataEntry> = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        for (url in metadataUrls) {
            val json = runCatching { fetchText(url) }.getOrNull()
            if (!json.isNullOrBlank()) {
                val map = parse(json)
                cached = map
                return@withContext map
            }
        }
        emptyMap()
    }

    private fun fetchText(url: String): String {
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
                throw IllegalStateException("中文元数据请求失败：HTTP $code")
            }
            inputStream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
        }
    }

    private fun parse(json: String): Map<String, ZhMetadataEntry> {
        val root = JSONObject(json)
        val games = root.optJSONObject("games") ?: return emptyMap()
        return buildMap {
            for (key in games.keys()) {
                val item = games.optJSONObject(key) ?: continue
                put(
                    key,
                    ZhMetadataEntry(
                        zhTitle = item.optString("zh"),
                        titleCn = item.optString("titleCn"),
                        developer = item.optString("dev"),
                        publisher = item.optString("pub"),
                        japanDate = item.optString("japan"),
                        intro = item.optString("intro"),
                    ),
                )
            }
        }
    }

    companion object {
        const val DEFAULT_ZH_METADATA_URL =
            "https://richard2091.github.io/FC_ROMS/zh-metadata.v1.json"
        val DEFAULT_ZH_METADATA_URLS = FcRomsSourceResolver.expand(
            "https://raw.githubusercontent.com/Richard2091/FC_ROMS/main/zh-metadata.v1.json",
        )
    }
}

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}