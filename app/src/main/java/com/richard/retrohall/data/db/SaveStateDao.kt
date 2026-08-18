package com.richard.retrohall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveStateDao {
    @Query("SELECT * FROM save_states WHERE gameId = :gameId ORDER BY updatedAt DESC")
    suspend fun getForGame(gameId: String): List<SaveStateEntity>

    @Query("SELECT * FROM save_states WHERE gameId = :gameId ORDER BY updatedAt DESC")
    fun observeForGame(gameId: String): Flow<List<SaveStateEntity>>

    @Query("DELETE FROM save_states WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM save_states WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(saveState: SaveStateEntity)
}
