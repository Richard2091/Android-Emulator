package com.richard.retrohall.domain.save

import kotlinx.coroutines.flow.Flow

interface SaveStateStore {
    fun observeForGame(gameId: String): Flow<List<SaveStateSlot>>

    suspend fun upsert(gameId: String, slot: SaveSlot): SaveStateSlot

    suspend fun addSlot(gameId: String): SaveStateSlot

    suspend fun copy(slotId: String): SaveStateSlot?

    suspend fun delete(slotId: String)

    suspend fun deleteForGame(gameId: String)
}
