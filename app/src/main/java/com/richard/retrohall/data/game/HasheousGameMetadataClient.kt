package com.richard.retrohall.data.game

import android.content.Context
import com.richard.retrohall.domain.game.LocalGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

interface GameMetadataClient {
    suspend fun enrich(game: LocalGame): LocalGame
}

/**
 * 使用 Hasheous 按 ROM hash 补全游戏元数据，并把封面下载到应用私有缓存。
 */
class HasheousGameMetadataClient(
    context: Context,
    private val baseUrl: String = "https://hasheous.org",
) : GameMetadataClient {
    private val filesRoot = context.applicationContext.filesDir
    private val metadataRoot = File(filesRoot, "metadata-cache/hasheous")
    private val coverRoot = File(filesRoot, "metadata-cache/covers")

    override suspend fun enrich(game: LocalGame): LocalGame = withContext(Dispatchers.IO) {
        var enriched = game
        val lookupJson = loadOrFetchLookup(game)
        if (lookupJson != null) {
            enriched = applyHasheousMetadata(game, lookupJson)
        }
        enriched.copy(coverPath = cacheCover(enriched.id, enriched.coverPath))
    }

    private fun loadOrFetchLookup(game: LocalGame): JSONObject? {
        if (!game.hasAnyHash()) return null
        val cacheFile = File(metadataRoot, "${safeFileName(game.id)}-${game.hashCacheKey()}.json")
        val cached = cacheFile.takeIf { it.isFile && it.length() > 0L }?.readText(Charsets.UTF_8)
        if (!cached.isNullOrBlank()) {
            return parseJsonObject(cached)
        }

        val fetched = runCatching { fetchHasheousLookup(game) }.getOrNull() ?: return null
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText(fetched, Charsets.UTF_8)
        return parseJsonObject(fetched)
    }

    private fun fetchHasheousLookup(game: LocalGame): String {
        val url = URL("$baseUrl/api/v1/Lookup/ByHash?returnAllSources=true&returnFields=Signatures,Metadata,Attributes")
        val body = JSONObject().apply {
            if (game.romMd5.isNotBlank()) put("md5", game.romMd5)
            if (game.romSha1.isNotBlank()) put("sha1", game.romSha1)
            if (game.romCrc32.isNotBlank()) put("crc", game.romCrc32)
        }.toString().toByteArray(Charsets.UTF_8)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "RetroHall")
        }
        return connection.use {
            outputStream.use { it.write(body) }
            val code = responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Hasheous 元数据请求失败：HTTP $code")
            }
            inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }

    private fun applyHasheousMetadata(game: LocalGame, root: JSONObject): LocalGame {
        val hasheousTitle = root.optString("name").ifBlank {
            findString(root, setOf("displaytitle", "title", "name")).orEmpty()
        }
        val rawDescription = findAttribute(root, "AIDescription").ifBlank {
            findString(root, setOf("description", "summary", "overview", "storyline")).orEmpty()
        }
        val coverUrl = findAttributeLink(root, "Logo").ifBlank {
            findCoverUrl(root).orEmpty()
        }
        val title = game.title.ifBlank { hasheousTitle }
        // 已有简介（含预生成中文简介）则保留，否则用 Hasheous 英文原文兜底，不做机翻。
        val description = game.description.ifBlank { rawDescription }
        return game.copy(
            title = title,
            description = description,
            coverPath = coverUrl.ifBlank { game.coverPath },
        )
    }

    private fun cacheCover(gameId: String, source: String): String {
        if (!source.startsWith("http://") && !source.startsWith("https://")) return source
        val target = File(coverRoot, "${safeFileName(gameId)}-${sha1(source).take(12)}.jpg")
        if (target.isFile && target.length() > 0L) return target.absolutePath

        runCatching {
            val connection = (URL(source).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "RetroHall")
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
        return target.takeIf { it.isFile && it.length() > 0L }?.absolutePath ?: source
    }

    private fun parseJsonObject(text: String): JSONObject? {
        return when (val parsed = JSONTokener(text).nextValue()) {
            is JSONObject -> parsed
            is JSONArray -> parsed.optJSONObject(0)
            else -> null
        }
    }

    private fun findString(value: Any?, keys: Set<String>): String? {
        return when (value) {
            is JSONObject -> value.keys().asSequence().firstNotNullOfOrNull { key ->
                val child = value.opt(key)
                if (keys.contains(key.lowercase(Locale.US)) && child is String && child.isNotBlank()) {
                    child
                } else {
                    findString(child, keys)
                }
            }
            is JSONArray -> (0 until value.length()).asSequence().firstNotNullOfOrNull { findString(value.opt(it), keys) }
            else -> null
        }
    }

    private fun findCoverUrl(value: Any?): String? {
        return when (value) {
            is JSONObject -> value.keys().asSequence().firstNotNullOfOrNull { key ->
                val child = value.opt(key)
                val lowerKey = key.lowercase(Locale.US)
                if (lowerKey.contains("cover") || lowerKey.contains("boxfront") || lowerKey.contains("image")) {
                    imageUrlFrom(child) ?: findCoverUrl(child)
                } else {
                    findCoverUrl(child)
                }
            }
            is JSONArray -> (0 until value.length()).asSequence().firstNotNullOfOrNull { findCoverUrl(value.opt(it)) }
            is String -> value.takeIf { it.startsWith("http") && (it.contains("/image") || it.contains("/cover") || it.endsWith(".jpg") || it.endsWith(".png")) }
            else -> null
        }
    }

    private fun findAttribute(root: JSONObject, name: String): String {
        val attributes = root.optJSONArray("attributes") ?: root.optJSONArray("Attributes") ?: return ""
        for (index in 0 until attributes.length()) {
            val item = attributes.optJSONObject(index) ?: continue
            if (item.optString("attributeName").equals(name, ignoreCase = true)) {
                return item.optString("value").trim()
            }
        }
        return ""
    }

    private fun findAttributeLink(root: JSONObject, name: String): String {
        val attributes = root.optJSONArray("attributes") ?: root.optJSONArray("Attributes") ?: return ""
        for (index in 0 until attributes.length()) {
            val item = attributes.optJSONObject(index) ?: continue
            if (item.optString("attributeName").equals(name, ignoreCase = true)) {
                val link = item.optString("link").trim()
                if (link.startsWith("http")) return link
                if (link.startsWith("/")) return "$baseUrl$link"
                val value = item.optString("value").trim()
                if (value.isNotBlank()) return "$baseUrl/api/v1/images/$value"
            }
        }
        return ""
    }

    private fun imageUrlFrom(value: Any?): String? {
        return when (value) {
            is String -> {
                when {
                    value.startsWith("http") -> value
                    value.isNotBlank() -> "$baseUrl/api/v1/images/$value"
                    else -> null
                }
            }
            is JSONObject -> {
                val direct = listOf("url", "Url", "URL").firstNotNullOfOrNull { value.optString(it).takeIf(String::isNotBlank) }
                if (!direct.isNullOrBlank()) return direct
                val id = listOf("id", "Id", "ID", "imageId", "ImageId").firstNotNullOfOrNull { value.optString(it).takeIf(String::isNotBlank) }
                id?.let { "$baseUrl/api/v1/images/$it" }
            }
            else -> null
        }
    }

    private fun LocalGame.hasAnyHash(): Boolean = romMd5.isNotBlank() || romSha1.isNotBlank() || romCrc32.isNotBlank()

    private fun LocalGame.hashCacheKey(): String = listOf(romMd5, romSha1, romCrc32)
        .filter { it.isNotBlank() }
        .joinToString("-") { it.lowercase(Locale.US) }

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
