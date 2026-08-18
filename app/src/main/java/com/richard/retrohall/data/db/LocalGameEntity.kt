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
    val description: String = "",
    val romMd5: String = "",
    val romSha1: String = "",
    val romCrc32: String = "",
    val hotness: Double? = null,
    val screenshots: List<String> = emptyList(),
    val logos: List<String> = emptyList(),
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
        description = description,
        romMd5 = romMd5,
        romSha1 = romSha1,
        romCrc32 = romCrc32,
        hotness = hotness,
        screenshots = screenshots,
        logos = logos,
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
    description = description,
    romMd5 = romMd5,
    romSha1 = romSha1,
    romCrc32 = romCrc32,
    hotness = hotness,
    screenshots = screenshots,
    logos = logos,
)
