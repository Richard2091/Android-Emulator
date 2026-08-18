package com.richard.retrohall.domain.game

interface CoverImageLoader {
    suspend fun prepareCover(gameId: String, coverUrl: String): String
}
