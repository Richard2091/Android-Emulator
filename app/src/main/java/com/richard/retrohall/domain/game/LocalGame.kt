package com.richard.retrohall.domain.game

data class LocalGame(
    val id: String,
    val title: String,
    val platform: String,
    val category: String,
    val coverPath: String,
    val romPath: String,
    val favorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val totalPlayTimeMillis: Long = 0L,
    val description: String = "",
    val romMd5: String = "",
    val romSha1: String = "",
    val romCrc32: String = "",
    val hotness: Double? = null,
    val screenshots: List<String> = emptyList(),
    val logos: List<String> = emptyList(),
)
