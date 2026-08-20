package com.richard.retrohall.data.core

import com.richard.retrohall.data.game.HttpTextFetcher
import com.richard.retrohall.data.game.ResourceRepositoryConfig
import com.richard.retrohall.data.settings.ResourceSourceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CoreFileInfo(
    val abi: String,
    val url: String,
    val fileName: String,
    val size: Long,
    val sha256: String,
    val minSdk: Int,
)

data class CoreInfo(
    val id: String,
    val displayName: String,
    val platformIds: List<String>,
    val runtimeFamily: String,
    val version: String,
    val license: String,
    val licenseUrl: String,
    val sourceUrl: String,
    val defaultForPlatform: Boolean,
    val files: List<CoreFileInfo>,
) {
    fun fileFor(abi: String): CoreFileInfo? = files.firstOrNull { it.abi == abi }
}

data class CoreCatalog(
    val schemaVersion: Int,
    val catalogId: String,
    val catalogName: String,
    val cores: List<CoreInfo>,
) {
    fun forPlatform(platformId: String): List<CoreInfo> =
        cores.filter { core -> core.platformIds.any { it.equals(platformId, ignoreCase = true) } }

    fun defaultFor(platformId: String): CoreInfo? =
        forPlatform(platformId).firstOrNull { it.defaultForPlatform } ?: forPlatform(platformId).firstOrNull()
}

/**
 * 读取核心仓库清单（catalog/core-manifest.v1.json）。
 * 数据源根地址从 [ResourceSourceStore] 读取，留空时使用内置默认。
 */
class CoreCatalogClient(
    private val sourceStore: ResourceSourceStore,
    private val fetcher: HttpTextFetcher = HttpTextFetcher(),
) {
    suspend fun fetchCatalog(): CoreCatalog = withContext(Dispatchers.IO) {
        val baseUrl = ResourceRepositoryConfig.coreBaseUrl(sourceStore.coreSourceUrl())
        val url = baseUrl + "catalog/core-manifest.v1.json"
        val json = JSONObject(fetcher.fetch(url))
        parse(json, baseUrl)
    }

    private fun parse(json: JSONObject, baseUrl: String): CoreCatalog {
        val coresArray = json.optJSONArray("cores") ?: org.json.JSONArray()
        val cores = buildList {
            for (i in 0 until coresArray.length()) {
                val core = coresArray.getJSONObject(i)
                val filesArray = core.optJSONArray("files") ?: org.json.JSONArray()
                val files = buildList {
                    for (j in 0 until filesArray.length()) {
                        val f = filesArray.getJSONObject(j)
                        add(
                            CoreFileInfo(
                                abi = f.optString("abi"),
                                url = resolveUrl(baseUrl, f.optString("url")),
                                fileName = f.optString("fileName"),
                                size = f.optLong("size", 0L),
                                sha256 = f.optString("sha256"),
                                minSdk = f.optInt("minSdk", 23),
                            ),
                        )
                    }
                }
                add(
                    CoreInfo(
                        id = core.optString("id"),
                        displayName = core.optString("displayName"),
                        platformIds = core.optJSONArray("platformIds")?.toStringList().orEmpty(),
                        runtimeFamily = core.optString("runtimeFamily"),
                        version = core.optString("version"),
                        license = core.optString("license"),
                        licenseUrl = core.optString("licenseUrl"),
                        sourceUrl = core.optString("sourceUrl"),
                        defaultForPlatform = core.optBoolean("defaultForPlatform", false),
                        files = files,
                    ),
                )
            }
        }
        return CoreCatalog(
            schemaVersion = json.optInt("schemaVersion", 0),
            catalogId = json.optString("catalogId"),
            catalogName = json.optString("catalogName"),
            cores = cores,
        )
    }

    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative
        return java.net.URI(base.trimEnd('/') + "/").resolve(relative.removePrefix("/")).toString()
    }
}

private fun org.json.JSONArray.toStringList(): List<String> {
    return buildList {
        for (i in 0 until length()) {
            optString(i).trim().takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}
