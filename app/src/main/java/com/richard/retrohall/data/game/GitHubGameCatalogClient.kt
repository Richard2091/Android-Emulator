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
    private val manifestUrl: String = DEFAULT_MANIFEST_URL,
) : RemoteGameCatalogClient {
    override suspend fun fetchGames(): List<LocalGame> = withContext(Dispatchers.IO) {
        val json = fetchText(manifestUrl)
        parseManifest(JSONObject(json))
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

                add(
                    LocalGame(
                        id = "github-$sourceId",
                        title = title,
                        platform = item.optString("platform").ifBlank { "FC/NES" },
                        category = item.optString("category").ifBlank { "在线游戏库" },
                        coverPath = assets?.optString("coverUrl").orEmpty(),
                        romPath = firstRom.optString("url").ifBlank { firstRom.optString("path") },
                    ),
                )
            }
        }
    }

    companion object {
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/Richard2091/FC_ROMS/main/manifest.v1.json"
    }
}

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}
