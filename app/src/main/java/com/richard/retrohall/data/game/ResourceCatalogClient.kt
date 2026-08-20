package com.richard.retrohall.data.game

import android.content.Context
import com.richard.retrohall.data.settings.ResourceSourceStore
import com.richard.retrohall.domain.game.CategoryCatalog
import com.richard.retrohall.domain.game.CategoryDescriptor
import com.richard.retrohall.domain.game.GameDetail
import com.richard.retrohall.domain.game.GameFileInfo
import com.richard.retrohall.domain.game.GameListItem
import com.richard.retrohall.domain.game.GameMedia
import com.richard.retrohall.domain.game.SearchIndex
import com.richard.retrohall.domain.game.SearchIndexEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI

/**
 * 读取资源仓库 v2 目录（catalog/index.v2.json、manifest.list.v2.json、game.json）。
 *
 * 相对路径按《资源仓库规范》解析：相对当前 JSON 文件所在目录。
 * 数据源根地址从 [ResourceSourceStore] 读取，留空时使用内置默认。
 */
class ResourceCatalogClient(
    private val sourceStore: ResourceSourceStore,
    private val fetcher: HttpTextFetcher = HttpTextFetcher(),
    context: Context? = null,
) {
    private var lastDirUrl: String = ResourceRepositoryConfig.GAME_CATALOG_BASE_URL

    /** 详情磁盘缓存目录（metadata-cache/details/<gameId>.json）。无 Context 时不启用。 */
    private val detailCacheRoot: File? = context?.applicationContext?.filesDir?.let { File(it, "metadata-cache/details") }

    /** 搜索索引磁盘缓存目录（metadata-cache/details/search-index.v2.json）。无 Context 时不启用。 */
    private val searchCacheRoot: File? = detailCacheRoot

    suspend fun fetchIndex(): CategoryCatalog = withContext(Dispatchers.IO) {
        val baseUrl = ResourceRepositoryConfig.gameBaseUrl(sourceStore.gameSourceUrl())
        val url = baseUrl + "catalog/index.v2.json"
        val json = JSONObject(fetcher.fetch(url))
        lastDirUrl = url.substringBeforeLast('/') + "/"
        parseIndex(json)
    }

    suspend fun fetchCategoryList(listUrl: String): List<GameListItem> = withContext(Dispatchers.IO) {
        val url = resolve(lastDirUrl, listUrl)
        val json = JSONObject(fetcher.fetch(url))
        lastDirUrl = url.substringBeforeLast('/') + "/"
        parseList(json)
    }

    suspend fun fetchDetail(detailUrl: String): GameDetail = withContext(Dispatchers.IO) {
        val url = resolve(lastDirUrl, detailUrl)
        val json = JSONObject(fetcher.fetch(url))
        lastDirUrl = url.substringBeforeLast('/') + "/"
        parseDetail(json)
    }

    /**
     * 拉取并缓存游戏详情：在线成功时写入磁盘缓存；离线失败时回退最近一次磁盘缓存。
     *
     * @param gameId 游戏 ID，用于磁盘缓存命名。
     */
    suspend fun fetchDetailCached(gameId: String, detailUrl: String): GameDetail = withContext(Dispatchers.IO) {
        val cacheFile = detailCacheRoot?.let { File(it, "${safeFileName(gameId)}.json") }
        val url = resolve(lastDirUrl, detailUrl)
        try {
            val json = JSONObject(fetcher.fetch(url))
            lastDirUrl = url.substringBeforeLast('/') + "/"
            val detail = parseDetail(json)
            if (cacheFile != null) {
                runCatching {
                    cacheFile.parentFile?.mkdirs()
                    cacheFile.writeText(json.toString(), Charsets.UTF_8)
                }
            }
            detail
        } catch (error: Exception) {
            val cached = cacheFile?.takeIf { it.isFile && it.length() > 0L }?.readText(Charsets.UTF_8)
            if (cached != null) {
                parseDetail(JSONObject(cached))
            } else {
                throw error
            }
        }
    }

    /**
     * 拉取并缓存全局搜索索引：在线成功时写入磁盘；离线失败时回退最近一次缓存。
     */
    suspend fun fetchSearchIndex(): SearchIndex = withContext(Dispatchers.IO) {
        val baseUrl = ResourceRepositoryConfig.gameBaseUrl(sourceStore.gameSourceUrl())
        val cacheFile = searchCacheRoot?.let { File(it, "search-index.v2.json") }
        val url = baseUrl + "catalog/search-index.v2.json"
        try {
            val json = JSONObject(fetcher.fetch(url))
            if (cacheFile != null) {
                runCatching {
                    cacheFile.parentFile?.mkdirs()
                    cacheFile.writeText(json.toString(), Charsets.UTF_8)
                }
            }
            parseSearchIndex(json)
        } catch (error: Exception) {
            val cached = cacheFile?.takeIf { it.isFile && it.length() > 0L }?.readText(Charsets.UTF_8)
            if (cached != null) {
                parseSearchIndex(JSONObject(cached))
            } else {
                throw error
            }
        }
    }

    private fun parseSearchIndex(json: JSONObject): SearchIndex {
        val entriesArray = json.optJSONArray("entries") ?: JSONArray()
        val entries = buildList {
            for (i in 0 until entriesArray.length()) {
                val e = entriesArray.getJSONObject(i)
                add(
                    SearchIndexEntry(
                        id = e.optString("id"),
                        slug = e.optString("slug"),
                        categoryId = e.optString("categoryId"),
                        title = e.optStringMap("title"),
                        primaryPlatformId = e.optString("primaryPlatformId"),
                        detailUrl = resolve(lastDirUrl, e.optString("detailUrl")),
                        releaseYear = e.optInt("releaseYear", 0),
                    ),
                )
            }
        }
        return SearchIndex(
            schemaVersion = json.optInt("schemaVersion", 0),
            gameCount = json.optInt("gameCount", 0),
            entries = entries,
        )
    }

    private fun resolve(baseDirUrl: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative
        val trimmed = relative.removePrefix("/")
        return URI(baseDirUrl).resolve(trimmed).toString()
    }

    private fun parseIndex(json: JSONObject): CategoryCatalog {
        val categoriesArray = json.optJSONArray("categories") ?: JSONArray()
        val categories = buildList {
            for (i in 0 until categoriesArray.length()) {
                val cat = categoriesArray.getJSONObject(i)
                add(
                    CategoryDescriptor(
                        id = cat.optString("id"),
                        displayName = cat.optString("displayName"),
                        runtimeFamily = cat.optString("runtimeFamily"),
                        platformIds = cat.optStringList("platformIds"),
                        listUrl = cat.optString("listUrl"),
                        gameCount = cat.optInt("gameCount", 0),
                    ),
                )
            }
        }
        return CategoryCatalog(
            schemaVersion = json.optInt("schemaVersion", 0),
            catalogId = json.optString("catalogId"),
            catalogName = json.optString("catalogName"),
            defaultCategoryId = json.optString("defaultCategoryId", "all"),
            categories = categories,
            searchIndexUrl = json.optString("searchIndexUrl"),
        )
    }

    private fun parseList(json: JSONObject): List<GameListItem> {
        val gamesArray = json.optJSONArray("games") ?: JSONArray()
        return buildList {
            for (i in 0 until gamesArray.length()) {
                val item = gamesArray.getJSONObject(i)
                val availability = item.optJSONObject("availability")
                add(
                    GameListItem(
                        id = item.optString("id"),
                        slug = item.optString("slug"),
                        categoryId = item.optString("categoryId"),
                        primaryPlatformId = item.optString("primaryPlatformId"),
                        platformName = item.optString("platformName"),
                        runtimeFamily = item.optString("runtimeFamily"),
                        title = item.optStringMap("title"),
                        coverUrl = resolve(lastDirUrl, item.optString("coverUrl")),
                        detailUrl = resolve(lastDirUrl, item.optString("detailUrl")),
                        tags = item.optStringList("tags"),
                        releaseYear = item.optInt("releaseYear", 0),
                        availabilityBinary = availability?.optString("binary").orEmpty(),
                    ),
                )
            }
        }
    }

    private fun parseDetail(json: JSONObject): GameDetail {
        val media = json.optJSONObject("media")
        val cover = media?.optJSONObject("cover")
        val screenshots = media?.optJSONArray("screenshots") ?: JSONArray()
        val screenshotUrls = buildList {
            for (i in 0 until screenshots.length()) {
                screenshots.getJSONObject(i).optString("url").takeIf { it.isNotBlank() }
                    ?.let { resolve(lastDirUrl, it) }?.let { add(it) }
            }
        }
        val filesArray = json.optJSONArray("files") ?: JSONArray()
        val files = buildList {
            for (i in 0 until filesArray.length()) {
                val f = filesArray.getJSONObject(i)
                add(
                    GameFileInfo(
                        id = f.optString("id"),
                        kind = f.optString("kind"),
                        role = f.optString("role"),
                        path = f.optString("path"),
                        url = if (f.optString("url").isNotBlank()) {
                            resolve(lastDirUrl, f.optString("url"))
                        } else {
                            resolve(lastDirUrl, f.optString("path"))
                        },
                        mime = f.optString("mime"),
                        size = f.optLong("size", 0L),
                        hashes = f.optJSONObject("hashes")?.toStringMap().orEmpty(),
                        availability = f.optString("availability"),
                    ),
                )
            }
        }
        val runtime = json.optJSONObject("runtime")
        val recommendedCores = runtime?.optStringList("recommendedCoreIds").orEmpty()
        return GameDetail(
            id = json.optString("id"),
            slug = json.optString("slug"),
            categoryId = json.optString("categoryId"),
            platformIds = json.optStringList("platformIds"),
            primaryPlatformId = json.optString("primaryPlatformId"),
            runtimeFamily = json.optString("runtimeFamily"),
            title = json.optStringMap("title"),
            alternateTitles = json.optStringList("alternateTitles"),
            description = json.optStringMap("description"),
            media = GameMedia(
                coverUrl = cover?.optString("url").orEmpty().let { if (it.isBlank()) "" else resolve(lastDirUrl, it) },
                screenshotUrls = screenshotUrls,
            ),
            files = files,
            recommendedCoreIds = recommendedCores,
            releaseYear = json.optInt("releaseYear", 0),
        )
    }
}

private fun JSONObject.optStringList(name: String): List<String> {
    val array = optJSONArray(name) ?: return emptyList()
    return buildList {
        for (i in 0 until array.length()) {
            array.optString(i).trim().takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}

private fun JSONObject.optStringMap(name: String): Map<String, String> {
    val obj = optJSONObject(name) ?: return emptyMap()
    val result = linkedMapOf<String, String>()
    obj.keys().forEach { key -> result[key] = obj.optString(key) }
    return result
}

private fun JSONObject.toStringMap(): Map<String, String> {
    val result = linkedMapOf<String, String>()
    keys().forEach { key -> result[key] = optString(key) }
    return result
}

private fun safeFileName(value: String): String = value.replace(Regex("""[\\/:*?"<>|]"""), "_")
