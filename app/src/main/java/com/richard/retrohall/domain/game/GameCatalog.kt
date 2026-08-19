package com.richard.retrohall.domain.game

/**
 * 资源仓库 v2 领域模型：分类、列表项、详情。
 *
 * 对应《资源仓库规范》中的 catalog/index.v2.json、manifest.list.v2.json、game.json。
 */
data class CategoryDescriptor(
    val id: String,
    val displayName: String,
    val runtimeFamily: String,
    val platformIds: List<String>,
    val listUrl: String,
    val gameCount: Int,
)

data class CategoryCatalog(
    val schemaVersion: Int,
    val catalogId: String,
    val catalogName: String,
    val defaultCategoryId: String,
    val categories: List<CategoryDescriptor>,
    val searchIndexUrl: String,
) {
    fun category(id: String): CategoryDescriptor? = categories.firstOrNull { it.id == id }
}

data class GameListItem(
    val id: String,
    val slug: String,
    val categoryId: String,
    val primaryPlatformId: String,
    val platformName: String,
    val runtimeFamily: String,
    val title: Map<String, String>,
    val coverUrl: String,
    val detailUrl: String,
    val tags: List<String>,
    val releaseYear: Int,
    val availabilityBinary: String,
) {
    fun displayTitle(): String =
        title["zh"]?.ifBlank { null } ?: title["en"] ?: title["ja"] ?: title.values.firstOrNull() ?: slug
}

data class GameFileInfo(
    val id: String,
    val kind: String,
    val role: String,
    val path: String,
    val url: String,
    val mime: String,
    val size: Long,
    val hashes: Map<String, String>,
    val availability: String,
) {
    val sha256: String get() = hashes["sha256"].orEmpty()
    val isDownloadable: Boolean get() = availability == "public" && url.isNotBlank()
}

data class GameMedia(
    val coverUrl: String,
    val screenshotUrls: List<String>,
)

data class GameDetail(
    val id: String,
    val slug: String,
    val categoryId: String,
    val platformIds: List<String>,
    val primaryPlatformId: String,
    val runtimeFamily: String,
    val title: Map<String, String>,
    val alternateTitles: List<String>,
    val description: Map<String, String>,
    val media: GameMedia,
    val files: List<GameFileInfo>,
    val recommendedCoreIds: List<String>,
    val releaseYear: Int,
) {
    fun displayTitle(): String =
        title["zh"]?.ifBlank { null } ?: title["en"] ?: title["ja"] ?: title.values.firstOrNull() ?: slug

    fun primaryFile(): GameFileInfo? = files.firstOrNull { it.role == "primary" || it.id == "main" } ?: files.firstOrNull()

    fun downloadableFiles(): List<GameFileInfo> = files.filter { it.isDownloadable }
}
