package com.richard.retrohall.data.game

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.richard.retrohall.data.db.RetroHallDatabase
import com.richard.retrohall.data.db.toEntity
import com.richard.retrohall.domain.game.LocalGame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

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

    @Test
    fun syncRemoteCatalogWritesGamesAndPreservesLocalStats() = runTest {
        val remoteGame = LocalGame(
            id = "github-0001",
            title = "大金刚",
            platform = "NES",
            category = "在线游戏库",
            coverPath = "",
            romPath = "https://example.test/rom.nes",
        )
        repository = GameRepository(
            database.localGameDao(),
            object : RemoteGameCatalogClient {
                override suspend fun fetchGames(): List<LocalGame> = listOf(remoteGame)
            },
        )

        assertEquals(1, repository.syncRemoteCatalog())
        repository.toggleFavorite(repository.games.first().first())
        repository.markPlayed("github-0001", playedAt = 2_000L)
        repository.recordPlaySession("github-0001", startedAt = 2_000L, endedAt = 5_000L)

        assertEquals(1, repository.syncRemoteCatalog())

        val synced = repository.games.first().first { it.id == "github-0001" }
        assertTrue(synced.favorite)
        assertEquals(5_000L, synced.lastPlayedAt)
        assertEquals(3_000L, synced.totalPlayTimeMillis)
    }

    @Test
    fun syncRemoteCatalogKeepsStatsChangedDuringFetch() = runTest {
        val remoteGame = LocalGame(
            id = "github-0001",
            title = "大金刚",
            platform = "NES",
            category = "在线游戏库",
            coverPath = "",
            romPath = "https://example.test/rom.nes",
        )
        database.localGameDao().upsertAll(listOf(remoteGame.toEntity()))
        repository = GameRepository(
            database.localGameDao(),
            object : RemoteGameCatalogClient {
                override suspend fun fetchGames(): List<LocalGame> {
                    database.localGameDao().updateFavorite("github-0001", true)
                    database.localGameDao().updatePlayStats("github-0001", 8_000L, 4_000L)
                    return listOf(remoteGame)
                }
            },
        )

        repository.syncRemoteCatalog()

        val synced = repository.games.first().first { it.id == "github-0001" }
        assertTrue(synced.favorite)
        assertEquals(8_000L, synced.lastPlayedAt)
        assertEquals(4_000L, synced.totalPlayTimeMillis)
    }

    @Test
    fun refreshCoversReloadsMissingLocalCover() = runTest {
        val missingCover = File(System.getProperty("java.io.tmpdir"), "retrohall-missing-cover-${System.nanoTime()}.jpg").absolutePath
        val game = LocalGame(
            id = "github-0002",
            title = "测试游戏",
            platform = "NES",
            category = "在线游戏库",
            coverPath = missingCover,
            romPath = "https://example.test/rom.nes",
        )
        database.localGameDao().upsertAll(listOf(game.toEntity()))
        repository = GameRepository(
            database.localGameDao(),
            metadataClient = object : GameMetadataClient {
                override suspend fun enrich(game: LocalGame): LocalGame =
                    game.copy(coverPath = "/tmp/reloaded-cover.jpg")
            },
        )

        val changed = repository.refreshCovers()

        assertTrue(changed)
        val stored = repository.games.first().first { it.id == game.id }
        assertEquals("/tmp/reloaded-cover.jpg", stored.coverPath)
    }

    @Test
    fun refreshCoversDownloadsRemoteCoverUrls() = runTest {
        val game = LocalGame(
            id = "github-0003",
            title = "远程封面游戏",
            platform = "NES",
            category = "在线游戏库",
            coverPath = "https://example.test/cover.jpg",
            romPath = "https://example.test/rom.nes",
        )
        database.localGameDao().upsertAll(listOf(game.toEntity()))
        repository = GameRepository(
            database.localGameDao(),
            metadataClient = object : GameMetadataClient {
                override suspend fun enrich(game: LocalGame): LocalGame =
                    game.copy(coverPath = "/data/covers/remote.jpg")
            },
        )

        val changed = repository.refreshCovers()

        assertTrue(changed)
        val stored = repository.games.first().first { it.id == game.id }
        assertEquals("/data/covers/remote.jpg", stored.coverPath)
    }

    @Test
    fun refreshCoversSkipsExistingCoverFiles() = runTest {
        val existingCover = File(System.getProperty("java.io.tmpdir"), "retrohall-existing-cover-${System.nanoTime()}.jpg")
        existingCover.writeText("cover")
        val game = LocalGame(
            id = "github-0004",
            title = "已有封面游戏",
            platform = "NES",
            category = "在线游戏库",
            coverPath = existingCover.absolutePath,
            romPath = "https://example.test/rom.nes",
        )
        database.localGameDao().upsertAll(listOf(game.toEntity()))
        var enrichCalled = false
        repository = GameRepository(
            database.localGameDao(),
            metadataClient = object : GameMetadataClient {
                override suspend fun enrich(game: LocalGame): LocalGame {
                    enrichCalled = true
                    return game.copy(coverPath = "/tmp/never-used.jpg")
                }
            },
        )

        val changed = repository.refreshCovers()

        assertFalse(changed)
        assertFalse(enrichCalled)
        assertEquals(existingCover.absolutePath, repository.games.first().first().coverPath)
        existingCover.delete()
    }

    @Test
    fun syncFromResourceCatalogRemovesPrivateResidue() = runTest {
        // 预置一条私有资源残留（如旧的魂斗罗）。
        database.localGameDao().upsertAll(
            listOf(
                LocalGame(
                    id = "contra-j",
                    title = "魂斗罗",
                    platform = "NES",
                    category = "Action",
                    coverPath = "",
                    romPath = "roms/contra.nes",
                ).toEntity(),
            ),
        )

        val indexJson = """
            {
              "schemaVersion": 2,
              "catalogId": "retrogame",
              "catalogName": "RetroGame",
              "defaultCategoryId": "all",
              "searchIndexUrl": "search-index.v2.json",
              "categories": [
                {"id": "all", "displayName": "全部", "runtimeFamily": "mixed",
                 "listUrl": "all/manifest.list.v2.json", "gameCount": 1}
              ]
            }
        """.trimIndent()
        val listJson = """
            {
              "schemaVersion": 2,
              "categoryId": "all",
              "games": [
                {
                  "id": "fc-0001", "slug": "0001", "categoryId": "fc",
                  "primaryPlatformId": "nes", "platformName": "FC",
                  "runtimeFamily": "libretro",
                  "title": {"zh": "大金刚", "en": "Donkey Kong"},
                  "coverUrl": "cover.png", "detailUrl": "game.json",
                  "tags": [], "releaseYear": 1983,
                  "availability": {"binary": "public"}
                }
              ]
            }
        """.trimIndent()
        val fetcher = object : HttpTextFetcher() {
            override suspend fun fetch(url: String): String =
                if (url.endsWith("index.v2.json")) indexJson else listJson
        }
        val client = ResourceCatalogClient(
            com.richard.retrohall.data.settings.ResourceSourceStore(
                ApplicationProvider.getApplicationContext(),
            ),
            fetcher,
        )

        val synced = repository.syncFromResourceCatalog(client)

        assertEquals(1, synced)
        val ids = repository.games.first().map { it.id }
        assertTrue("应包含数据源游戏", "fc-0001" in ids)
        assertFalse("私有残留应被清除", "contra-j" in ids)
    }
}
