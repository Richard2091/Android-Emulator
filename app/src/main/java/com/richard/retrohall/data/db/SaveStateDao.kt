package com.richard.retrohall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SaveStateDao {
    @Query("SELECT * FROM save_states WHERE gameId = :gameId ORDER BY slotType, slotIndex")
    suspend fun getForGame(gameId: String): List<SaveStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(saveState: SaveStateEntity)
}
