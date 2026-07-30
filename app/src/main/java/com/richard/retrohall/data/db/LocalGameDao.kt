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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<LocalGameEntity>)
}
