package com.richard.retrohall.data.game

import com.richard.retrohall.domain.game.LocalGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

interface RemoteGameCatalogClient {
    suspend fun fetchGames(): List<LocalGame>
}

class GitHubGameCatalogClient(
    private val manifestUrls: List<String> = DEFAULT_MANIFEST_URLS,
) : RemoteGameCatalogClient {
    override suspend fun fetchGames(): List<LocalGame> = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        for (manifestUrl in manifestUrls) {
            val json = runCatching { fetchText(manifestUrl) }
                .onFailure { error -> errors += "${manifestUrl}: ${error.message}" }
                .getOrNull()
            if (!json.isNullOrBlank()) {
                return@withContext parseManifest(JSONObject(json))
            }
        }
        throw IllegalStateException("远程游戏库同步失败：${errors.joinToString("；")}")
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
                throw IllegalStateException("GitHub 游戏库请求失败：HTTP $code")
            }
            inputStream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
        }
    }

    private fun parseManifest(root: JSONObject): List<LocalGame> {
        val games = root.getJSONArray("games")
        return buildList {
            for (index in 0 until games.length()) {
                val item = games.getJSONObject(index)
                val roms = item.optJSONArray("roms")
                if (roms == null || roms.length() == 0) continue
                val firstRom = (0 until roms.length())
                    .asSequence()
                    .map { roms.getJSONObject(it) }
                    .firstOrNull { rom -> rom.optString("url").isNotBlank() || rom.optString("path").isNotBlank() }
                    ?: continue
                val sourceId = item.optString("id")
                val title = item.optString("displayTitle").ifBlank {
                    val titleObject = item.optJSONObject("title")
                    titleObject?.optString("zh")?.ifBlank { titleObject.optString("en") } ?: sourceId
                }
                val assets = item.optJSONObject("assets")
                val hashes = firstRom.optJSONObject("hash") ?: firstRom.optJSONObject("hashes")
                val description = item.optString("description").ifBlank {
                    val descriptionObject = item.optJSONObject("description")
                    descriptionObject?.optString("zh")?.ifBlank { descriptionObject.optString("en") }.orEmpty()
                }
                val hotness = if (item.has("hotness")) item.optDouble("hotness", Double.NaN) else Double.NaN
                val screenshotUrls = assets?.optStringList("screenshotUrls").orEmpty()
                val logoUrls = assets?.optStringList("logoUrls").orEmpty()

                add(
                    LocalGame(
                        id = "github-$sourceId",
                        title = title,
                        platform = item.optString("platform").ifBlank { "FC/NES" },
                        category = item.optString("category").ifBlank { "在线游戏库" },
                        coverPath = assets?.optString("coverUrl").orEmpty().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty(),
                        romPath = firstRom.optString("url").ifBlank { firstRom.optString("path") },
                        description = description,
                        romMd5 = firstRom.optHash("md5").ifBlank { hashes?.optHash("md5").orEmpty() },
                        romSha1 = firstRom.optHash("sha1").ifBlank { hashes?.optHash("sha1").orEmpty() },
                        romCrc32 = firstRom.optHash("crc32").ifBlank { hashes?.optHash("crc32").orEmpty() },
                        hotness = hotness.takeIf { it.isFinite() && it > 0 },
                        screenshots = screenshotUrls,
                        logos = logoUrls,
                    ),
                )
            }
        }
    }

    companion object {
        const val DEFAULT_MANIFEST_URL = "https://richard2091.github.io/RetroGame/legacy/manifest.v1.json"
        val DEFAULT_MANIFEST_URLS = ResourceRepositoryConfig.LEGACY_MANIFEST_URLS
            .flatMap { FcRomsSourceResolver.expand(it) }
    }
}

private fun JSONObject.optHash(name: String): String {
    return optString(name).ifBlank { optString(name.uppercase()) }.trim()
}

private fun JSONObject.optStringList(name: String): List<String> {
    val array = optJSONArray(name) ?: return emptyList()
    val seen = linkedSetOf<String>()
    return buildList {
        for (index in 0 until array.length()) {
            array.optString(index).trim().takeIf { it.isNotBlank() && seen.add(it) }?.let { add(it) }
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
