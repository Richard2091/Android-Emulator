package com.richard.retrohall.data.game

import com.richard.retrohall.data.assets.FakeGameCatalog
import com.richard.retrohall.data.db.LocalGameDao
import com.richard.retrohall.data.db.toEntity
import com.richard.retrohall.domain.game.LocalGame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 管理本地游戏库、收藏状态和最近游玩统计。
 *
 * @param localGameDao 本地游戏 DAO。
 */
class GameRepository(private val localGameDao: LocalGameDao) {
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
}
