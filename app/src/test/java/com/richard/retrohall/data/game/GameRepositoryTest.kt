package com.richard.retrohall.data.game

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.richard.retrohall.data.db.RetroHallDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameRepositoryTest {
    private lateinit var database: RetroHallDatabase
    private lateinit var repository: GameRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RetroHallDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = GameRepository(database.localGameDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedIfEmptyWritesFakeCatalogOnce() = runTest {
        assertEquals(6, repository.seedIfEmpty())
        assertEquals(6, repository.games.first().size)

        assertEquals(6, repository.seedIfEmpty())
        assertEquals(6, repository.games.first().size)
    }

    @Test
    fun toggleFavoriteUpdatesFavorites() = runTest {
        repository.seedIfEmpty()
        val game = repository.games.first().first { !it.favorite }

        repository.toggleFavorite(game)

        assertTrue(repository.favorites.first().any { it.id == game.id })
    }

    @Test
    fun playStatsUpdateRecentAndTotalTime() = runTest {
        repository.seedIfEmpty()
        val game = repository.games.first().first()

        repository.markPlayed(game.id, playedAt = 1_000L)
        repository.recordPlaySession(game.id, startedAt = 1_000L, endedAt = 4_000L)

        val recentGame = repository.recent.first().first { it.id == game.id }
        assertEquals(4_000L, recentGame.lastPlayedAt)
        assertEquals(3_000L, recentGame.totalPlayTimeMillis)
    }
}
