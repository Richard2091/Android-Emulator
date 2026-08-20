package com.richard.retrohall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalGameDao {
    @Query("SELECT * FROM local_games ORDER BY title")
    suspend fun getAll(): List<LocalGameEntity>

    @Query("SELECT COUNT(*) FROM local_games")
    suspend fun count(): Int

    @Query("SELECT * FROM local_games ORDER BY title")
    fun observeAll(): Flow<List<LocalGameEntity>>

    @Query("SELECT * FROM local_games WHERE category = :category ORDER BY title")
    suspend fun getByCategory(category: String): List<LocalGameEntity>

    @Query("SELECT * FROM local_games WHERE favorite = 1 ORDER BY title")
    suspend fun getFavorites(): List<LocalGameEntity>

    @Query("SELECT * FROM local_games WHERE favorite = 1 ORDER BY title")
    fun observeFavorites(): Flow<List<LocalGameEntity>>

    @Query("SELECT * FROM local_games WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    suspend fun getRecent(): List<LocalGameEntity>

    @Query("SELECT * FROM local_games WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun observeRecent(): Flow<List<LocalGameEntity>>

    @Query("UPDATE local_games SET favorite = :favorite WHERE id = :gameId")
    suspend fun updateFavorite(gameId: String, favorite: Boolean)

    @Query("UPDATE local_games SET lastPlayedAt = :lastPlayedAt, totalPlayTimeMillis = totalPlayTimeMillis + :additionalMillis WHERE id = :gameId")
    suspend fun updatePlayStats(gameId: String, lastPlayedAt: Long, additionalMillis: Long)

    @Query(
        """
        UPDATE local_games
        SET title = :title,
            platform = :platform,
            category = :category,
            coverPath = :coverPath,
            description = :description,
            romMd5 = :romMd5,
            romSha1 = :romSha1,
            romCrc32 = :romCrc32
        WHERE id = :gameId
        """,
    )
    suspend fun updateMetadata(
        gameId: String,
        title: String,
        platform: String,
        category: String,
        coverPath: String,
        description: String,
        romMd5: String,
        romSha1: String,
        romCrc32: String,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<LocalGameEntity>)

    @Query("DELETE FROM local_games WHERE categoryId = ''")
    suspend fun deleteLegacyWithoutCategory()

    @Query("DELETE FROM local_games WHERE id LIKE 'github-%'")
    suspend fun deleteLegacyGithubSource()

    @Query("DELETE FROM local_games WHERE id = :gameId")
    suspend fun deleteById(gameId: String)

    /** 删除 id 不在保留集合中的游戏，用于同步后清理私有/遗留条目。 */
    @Query("DELETE FROM local_games WHERE id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(keepIds: List<String>)
}
