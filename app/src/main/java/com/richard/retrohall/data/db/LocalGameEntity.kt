package com.richard.retrohall.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.richard.retrohall.domain.game.LocalGame

@Entity(tableName = "local_games")
data class LocalGameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val platform: String,
    val category: String,
    val coverPath: String,
    val romPath: String,
    val favorite: Boolean,
    val lastPlayedAt: Long?,
    val totalPlayTimeMillis: Long,
) {
    fun toDomain(): LocalGame = LocalGame(
        id = id,
        title = title,
        platform = platform,
        category = category,
        coverPath = coverPath,
        romPath = romPath,
        favorite = favorite,
        lastPlayedAt = lastPlayedAt,
        totalPlayTimeMillis = totalPlayTimeMillis,
    )
}

fun LocalGame.toEntity(): LocalGameEntity = LocalGameEntity(
    id = id,
    title = title,
    platform = platform,
    category = category,
    coverPath = coverPath,
    romPath = romPath,
    favorite = favorite,
    lastPlayedAt = lastPlayedAt,
    totalPlayTimeMillis = totalPlayTimeMillis,
)
