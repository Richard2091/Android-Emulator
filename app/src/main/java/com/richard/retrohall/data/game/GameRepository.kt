package com.richard.retrohall.data.game

import com.richard.retrohall.data.assets.FakeGameCatalog
import com.richard.retrohall.data.db.LocalGameDao
import com.richard.retrohall.data.db.toEntity
import com.richard.retrohall.domain.game.GameDetail
import com.richard.retrohall.domain.game.GameListItem
import com.richard.retrohall.domain.game.LocalGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 管理本地游戏库、收藏状态和最近游玩统计。
 *
 * @param localGameDao 本地游戏 DAO。
 */
class GameRepository(
    private val localGameDao: LocalGameDao,
    private val remoteClient: RemoteGameCatalogClient? = null,
    private val metadataClient: GameMetadataClient? = null,
    private val zhMetadataClient: ZhMetadataClient? = null,
) {
    val games: Flow<List<LocalGame>> = localGameDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    val favorites: Flow<List<LocalGame>> = localGameDao.observeFavorites().map { entities ->
        entities.map { it.toDomain() }
    }

    val recent: Flow<List<LocalGame>> = localGameDao.observeRecent().map { entities ->
        entities.map { it.toDomain() }
    }

    /**
     * 首次启动时向数据库写入开发样例游戏。
     *
     * @return 写入后的游戏数量，已有数据时返回当前数量。
     */
    suspend fun seedIfEmpty(): Int {
        // 查询当前游戏库，避免覆盖真实私有资源初始化结果。
        val existing = localGameDao.getAll()
        if (existing.isNotEmpty()) return existing.size

        // 写入公开仓库允许保留的假数据，供无私有资源时开发和演示。
        localGameDao.upsertAll(FakeGameCatalog.games.map { it.toEntity() })
        return FakeGameCatalog.games.size
    }

    /** 本地游戏库是否为空。 */
    suspend fun isEmpty(): Boolean = localGameDao.count() == 0

    /**
     * 从资源仓库 v2 目录同步游戏索引（优先走 v2，失败返回 0 由调用方回退 v1）。
     *
     * @return 本次写入的游戏数量。
     */
    suspend fun syncFromResourceCatalog(client: ResourceCatalogClient): Int {
        return try {
            val index = client.fetchIndex()
            val items = mutableListOf<GameListItem>()
            val allCategory = index.category(index.defaultCategoryId)
            if (allCategory != null) {
                items += client.fetchCategoryList(allCategory.listUrl)
            } else {
                for (category in index.categories) {
                    if (category.id == index.defaultCategoryId) continue
                    items += client.fetchCategoryList(category.listUrl)
                }
            }
            val existingById = localGameDao.getAll().associateBy { it.id }
            val remoteGames = items.mapNotNull { item ->
                val existing = existingById[item.id]
                if (existing != null) {
                    item.toLocalGame(
                        favorite = existing.favorite,
                        lastPlayedAt = existing.lastPlayedAt,
                        totalPlayTimeMillis = existing.totalPlayTimeMillis,
                    )
                } else {
                    item.toLocalGame()
                }
            }
            localGameDao.upsertAll(remoteGames.map { it.toEntity() })
            if (remoteGames.isNotEmpty()) {
                localGameDao.deleteLegacyGithubSource()
            }
            // 只保留数据源条目：清理不在本次同步结果中的本地游戏（私有资源残留等）。
            val remoteIds = remoteGames.map { it.id }
            localGameDao.deleteAllExcept(remoteIds)
            remoteGames.size
        } catch (error: Exception) {
            0
        }
    }

    /**
     * 从远程游戏库同步游戏索引。
     *
     * @return 本次同步写入的游戏数量。
     */
    suspend fun syncRemoteCatalog(): Int {
        val client = remoteClient ?: return 0
        val fetchedGames = client.fetchGames()
        val zhMap = zhMetadataClient?.load().orEmpty()
        val existingById = localGameDao.getAll().associateBy { it.id }
        val remoteGames = fetchedGames.map { game ->
            val existing = existingById[game.id]
            val base = if (existing == null) {
                game
            } else {
                game.copy(
                    favorite = existing.favorite,
                    lastPlayedAt = existing.lastPlayedAt,
                    totalPlayTimeMillis = existing.totalPlayTimeMillis,
                    // 保留已下载到本地的封面；否则优先远程 coverUrl，确保新上线的封面可被懒加载。
                    coverPath = if (existing.coverPath.isNotBlank() && !existing.coverPath.startsWith("http", ignoreCase = true)) {
                        existing.coverPath
                    } else {
                        game.coverPath.ifBlank { existing.coverPath }
                    },
                    description = game.description.ifBlank { existing.description },
                    hotness = game.hotness ?: existing.hotness,
                )
            }
            // 预生成中文元数据优先提供简介，无简介时留给 Hasheous 英文兜底。
            val zh = zhMap[game.id.removePrefix("github-")]
            if (zh != null && zh.intro.isNotBlank()) {
                base.copy(description = zh.intro)
            } else {
                base
            }
        }
        localGameDao.upsertAll(remoteGames.map { it.toEntity() })
        metadataClient?.let { client ->
            remoteGames.forEach { game ->
                val enriched = client.enrich(game)
                localGameDao.updateMetadata(
                    gameId = enriched.id,
                    title = enriched.title,
                    platform = enriched.platform,
                    category = enriched.category,
                    coverPath = enriched.coverPath,
                    description = enriched.description,
                    romMd5 = enriched.romMd5,
                    romSha1 = enriched.romSha1,
                    romCrc32 = enriched.romCrc32,
                )
            }
        }
        return remoteGames.size
    }

    /**
     * 切换指定游戏的收藏状态。
     *
     * @param game 当前游戏快照。
     */
    suspend fun toggleFavorite(game: LocalGame) {
        // 收藏状态来自当前 UI 快照，写入相反值。
        localGameDao.updateFavorite(game.id, !game.favorite)
    }

    /**
     * 记录游戏启动时间，用于最近游玩列表。
     *
     * @param gameId 游戏 ID。
     * @param playedAt 最近游玩时间戳。
     */
    suspend fun markPlayed(gameId: String, playedAt: Long = System.currentTimeMillis()) {
        // 启动时只刷新最近游玩，不增加时长。
        localGameDao.updatePlayStats(gameId, playedAt, 0L)
    }

    /**
     * 懒加载封面：仅对封面文件缺失或仍是远程地址的游戏重新获取元数据并下载封面。
     *
     * 清理缓存后数据库中的本地封面路径会失效，打开游戏列表时调用本方法即可按需恢复，
     * 已存在的封面（含打包私有资源）会被跳过。
     *
     * @return 是否有封面被重新加载（调用方可据此刷新封面渲染缓存）。
     */
    suspend fun refreshCovers(): Boolean = withContext(Dispatchers.IO) {
        var changed = false
        localGameDao.getAll().map { it.toDomain() }.forEach { game ->
            if (!coverMissing(game)) return@forEach
            val enriched = metadataClient?.enrich(game) ?: return@forEach
            if (enriched.coverPath != game.coverPath) {
                localGameDao.updateMetadata(
                    gameId = enriched.id,
                    title = enriched.title,
                    platform = enriched.platform,
                    category = enriched.category,
                    coverPath = enriched.coverPath,
                    description = enriched.description,
                    romMd5 = enriched.romMd5,
                    romSha1 = enriched.romSha1,
                    romCrc32 = enriched.romCrc32,
                )
            }
            changed = true
        }
        changed
    }

    private fun coverMissing(game: LocalGame): Boolean {
        val path = game.coverPath
        if (path.isBlank()) return false
        if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
            return true
        }
        val file = File(path)
        // 相对路径属于打包资源引用，不属于可清理的缓存，跳过。
        if (!file.isAbsolute) return false
        return !file.isFile
    }

    /**
     * 从本地游戏库移除指定游戏记录。
     *
     * @param gameId 游戏 ID。
     */
    suspend fun deleteGame(gameId: String) {
        localGameDao.deleteById(gameId)
    }

    /**
     * 累加本次游戏时长。
     *
     * @param gameId 游戏 ID。
     * @param startedAt 本次启动时间。
     * @param endedAt 本次结束时间。
     */
    suspend fun recordPlaySession(gameId: String, startedAt: Long, endedAt: Long = System.currentTimeMillis()) {
        // 防止异常时间戳造成负数时长。
        val additionalMillis = (endedAt - startedAt).coerceAtLeast(0L)
        localGameDao.updatePlayStats(gameId, endedAt, additionalMillis)
    }

    /**
     * 拉取并缓存游戏详情，返回 [GameDetail]。
     *
     * 详情页按需调用；离线时回退最近一次磁盘缓存，无缓存才抛异常。
     */
    suspend fun fetchGameDetail(
        client: ResourceCatalogClient,
        game: LocalGame,
        cachedDetail: GameDetail?,
    ): GameDetail {
        return if (cachedDetail != null && cachedDetail.id == game.id) {
            cachedDetail
        } else {
            client.fetchDetailCached(game.id, game.detailUrl)
        }
    }

    /**
     * 用全局搜索索引匹配查询（支持中/英/日文标题与 slug）。
     *
     * 返回命中的游戏 id 集合；索引加载失败时返回 null，由调用方回退本地字段过滤。
     */
    suspend fun searchByIndex(client: ResourceCatalogClient, query: String): Set<String>? = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext null
        val index = runCatching { client.fetchSearchIndex() }.getOrNull() ?: return@withContext null
        index.entries
            .filter { entry ->
                entry.title.values.any { it.contains(q, ignoreCase = true) } ||
                    entry.slug.contains(q, ignoreCase = true)
            }
            .mapTo(mutableSetOf()) { it.id }
    }
}

private fun GameListItem.toLocalGame(
    favorite: Boolean = false,
    lastPlayedAt: Long? = null,
    totalPlayTimeMillis: Long = 0L,
): LocalGame = LocalGame(
    id = id,
    title = displayTitle(),
    platform = platformName.ifBlank { primaryPlatformId },
    category = categoryId,
    categoryId = categoryId,
    platformId = primaryPlatformId,
    runtimeFamily = runtimeFamily,
    detailUrl = detailUrl,
    coverPath = coverUrl,
    romPath = "",
    favorite = favorite,
    lastPlayedAt = lastPlayedAt,
    totalPlayTimeMillis = totalPlayTimeMillis,
)
