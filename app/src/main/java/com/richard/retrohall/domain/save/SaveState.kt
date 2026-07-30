package com.richard.retrohall.domain.save

data class SaveState(
    val gameId: String,
    val slot: SaveSlot,
    val filePath: String,
    val createdAt: Long,
    val updatedAt: Long,
)
