package com.richard.retrohall.data.db

import com.richard.retrohall.domain.game.LocalGame
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalGameEntityTest {
    @Test
    fun mapsLocalGameToEntityAndBack() {
        val game = LocalGame(
            id = "game-1",
            title = "Game One",
            platform = "NES",
            category = "动作",
            coverPath = "covers/game-one.png",
            romPath = "files/roms/game-one.nes",
            favorite = true,
            lastPlayedAt = 1000L,
            totalPlayTimeMillis = 2000L,
        )

        assertEquals(game, game.toEntity().toDomain())
    }
}
