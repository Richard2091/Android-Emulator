package com.richard.retrohall.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "save_states")
data class SaveStateEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val slotType: String,
    val slotIndex: Int?,
    val filePath: String,
    val createdAt: Long,
    val updatedAt: Long,
)
