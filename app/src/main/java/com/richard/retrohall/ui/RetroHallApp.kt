package com.richard.retrohall.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.richard.retrohall.RetroHallDependencies
import com.richard.retrohall.data.bootstrap.AppBootstrapper
import com.richard.retrohall.data.core.CoreCatalogClient
import com.richard.retrohall.data.core.CoreDownloadManager
import com.richard.retrohall.data.core.CoreSelectionStore
import com.richard.retrohall.data.game.ContentDownloadManager
import com.richard.retrohall.data.game.GameRepository
import com.richard.retrohall.data.game.ResourceCatalogClient
import com.richard.retrohall.data.game.RomDownloadManager
import com.richard.retrohall.data.settings.UserSettingsStore
import com.richard.retrohall.data.settings.ResourceSourceStore
import com.richard.retrohall.domain.game.CoverImageLoader
import com.richard.retrohall.domain.game.GameDetail
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.save.SaveSlot
import com.richard.retrohall.domain.save.SaveStateStore
import com.richard.retrohall.domain.save.toSaveSlot
import com.richard.retrohall.domain.settings.CacheMaintenance
import com.richard.retrohall.domain.settings.UserSettings
import com.richard.retrohall.emulator.EmulatorSession
import com.richard.retrohall.emulator.EmulatorSessionFactory
import com.richard.retrohall.ui.components.TopToast
import com.richard.retrohall.ui.detail.DetailScreen
import com.richard.retrohall.ui.game.GameScreen
import com.richard.retrohall.ui.hall.HallFilters
import com.richard.retrohall.ui.hall.HallScreen
import com.richard.retrohall.ui.save.SaveManagerScreen
import com.richard.retrohall.ui.save.displayName
import com.richard.retrohall.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface AppRoute {
    data object Hall : AppRoute
    data class Detail(val game: LocalGame) : AppRoute
    data class SaveManager(val game: LocalGame) : AppRoute
    data class Game(
        val game: LocalGame,
        val session: EmulatorSession,
        val startedAt: Long,
        val launchNotice: String?,
    ) : AppRoute
    data object Settings : AppRoute
}

@Composable
fun RetroHallApp() {
    val context = LocalContext.current
    val dependencies = remember { RetroHallDependencies.get(context) }

    RetroHallAppContent(
        gameRepository = dependencies.gameRepository,
        romDownloadManager = dependencies.romDownloadManager,
        contentDownloadManager = dependencies.contentDownloadManager,
        resourceCatalogClient = dependencies.resourceCatalogClient,
        emulatorSessionFactory = dependencies.emulatorSessionFactory,
        appBootstrapper = dependencies.appBootstrapper,
        userSettingsStore = dependencies.userSettingsStore,
        saveStateStore = dependencies.saveStateStore,
        cacheMaintenance = dependencies.cacheMaintenance,
        coverImageLoader = dependencies.coverImageLoader,
        coreCatalogClient = dependencies.coreCatalogClient,
        coreDownloadManager = dependencies.coreDownloadManager,
        coreSelectionStore = dependencies.coreSelectionStore,
        resourceSourceStore = dependencies.resourceSourceStore,
    )
}

@Composable
private fun RetroHallAppContent(
    gameRepository: GameRepository,
    romDownloadManager: RomDownloadManager,
    contentDownloadManager: ContentDownloadManager,
    resourceCatalogClient: ResourceCatalogClient,
    emulatorSessionFactory: EmulatorSessionFactory,
    appBootstrapper: AppBootstrapper,
    userSettingsStore: UserSettingsStore,
    saveStateStore: SaveStateStore,
    cacheMaintenance: CacheMaintenance,
    coverImageLoader: CoverImageLoader,
    coreCatalogClient: CoreCatalogClient,
    coreDownloadManager: CoreDownloadManager,
    coreSelectionStore: CoreSelectionStore,
    resourceSourceStore: ResourceSourceStore,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var route by remember { mutableStateOf<AppRoute>(AppRoute.Hall) }
    var launchMessage by remember { mutableStateOf<String?>(null) }
    var selectedSaveIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedHallSection by rememberSaveable { mutableStateOf("游戏库") }
    var detailSourceSection by rememberSaveable { mutableStateOf("游戏库") }
    var hallFilters by rememberSaveable { mutableStateOf(HallFilters()) }
    var recentFilters by rememberSaveable { mutableStateOf(HallFilters()) }
    var favoritesFilters by rememberSaveable { mutableStateOf(HallFilters()) }
    var downloadVersion by remember { mutableStateOf(0) }
    var coverReloadTick by remember { mutableStateOf(0L) }
    var exitConfirmTick by remember { mutableStateOf(0L) }
    var hallExitToast by remember { mutableStateOf<String?>(null) }
    val libraryGridState = rememberLazyGridState()
    val recentGridState = rememberLazyGridState()
    val favoritesGridState = rememberLazyGridState()
    val games by gameRepository.games.collectAsState(initial = emptyList())
    val settings by userSettingsStore.settings.collectAsState(initial = UserSettings())

    fun openHall(section: String = "游戏库") {
        exitConfirmTick = 0L
        selectedHallSection = section
        route = AppRoute.Hall
    }

    fun handleHallBack() {
        val now = System.currentTimeMillis()
        if (exitConfirmTick == 0L || now - exitConfirmTick > 3000L) {
            exitConfirmTick = now
            hallExitToast = "再按一次返回键退出软件"
        } else {
            (context as? Activity)?.finish()
        }
    }

    BackHandler(enabled = route !is AppRoute.Game) {
        when (val current = route) {
            is AppRoute.Hall -> handleHallBack()
            is AppRoute.Detail -> openHall(detailSourceSection)
            is AppRoute.SaveManager -> route = AppRoute.Detail(current.game)
            AppRoute.Settings -> openHall("游戏库")
            is AppRoute.Game -> Unit
        }
    }

    LaunchedEffect(appBootstrapper) {
        appBootstrapper.bootstrap()
    }

    LaunchedEffect(route) {
        if (route !is AppRoute.Hall) hallExitToast = null
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F172A)) {
            when (val current = route) {
                AppRoute.Hall -> HallScreen(
                    games = games,
                    romDownloadManager = romDownloadManager,
                    contentDownloadManager = contentDownloadManager,
                    downloadVersion = downloadVersion,
                    selectedSection = selectedHallSection,
                    filters = when (selectedHallSection) {
                        "最近" -> recentFilters
                        "收藏" -> favoritesFilters
                        else -> hallFilters
                    },
                    onFilterChange = { next ->
                        when (selectedHallSection) {
                            "最近" -> recentFilters = next
                            "收藏" -> favoritesFilters = next
                            else -> hallFilters = next
                        }
                    },
                    libraryGridState = libraryGridState,
                    recentGridState = recentGridState,
                    favoritesGridState = favoritesGridState,
                    coverReloadTick = coverReloadTick,
                    coverImageLoader = coverImageLoader,
                    onRefreshCovers = {
                        val changed = gameRepository.refreshCovers()
                        if (changed) coverReloadTick++
                    },
                    onSelectSection = {
                        exitConfirmTick = 0L
                        selectedHallSection = it
                    },
                    onOpenGame = {
                        detailSourceSection = selectedHallSection
                        launchMessage = null
                        route = AppRoute.Detail(it)
                    },
                    onOpenSettings = { route = AppRoute.Settings },
                    onToggleFavorite = { game ->
                        scope.launch { gameRepository.toggleFavorite(game) }
                    },
                )

                is AppRoute.Detail -> {
                    val latestGame = games.firstOrNull { it.id == current.game.id } ?: current.game
                    var detail by remember(latestGame.id) { mutableStateOf<GameDetail?>(null) }
                    LaunchedEffect(latestGame.id, latestGame.detailUrl) {
                        if (latestGame.detailUrl.isNotBlank()) {
                            detail = runCatching<GameDetail> {
                                gameRepository.fetchGameDetail(resourceCatalogClient, latestGame, detail)
                            }.getOrNull()
                        }
                    }
                    val displayGame = latestGame.let { base ->
                        val d = detail
                        if (d == null) {
                            base
                        } else {
                            base.copy(
                                description = d.description["zh"].orEmpty().ifBlank { base.description },
                                screenshots = if (base.screenshots.isEmpty()) d.media.screenshotUrls else base.screenshots,
                            )
                        }
                    }
                    DetailScreen(
                        game = displayGame,
                        message = launchMessage,
                        saveStateStore = saveStateStore,
                        selectedSaveId = selectedSaveIds[latestGame.id],
                        coverReloadTick = coverReloadTick,
                        coverImageLoader = coverImageLoader,
                        onOpenSaveManager = { route = AppRoute.SaveManager(latestGame) },
                        onSelectLibrary = { openHall("游戏库") },
                        onSelectRecent = { openHall("最近") },
                        onSelectFavorites = { openHall("收藏") },
                        onOpenSettings = { route = AppRoute.Settings },
                        onToggleFavorite = {
                            scope.launch { gameRepository.toggleFavorite(latestGame) }
                        },
                        isDownloaded = remember(latestGame, downloadVersion) {
                            contentDownloadManager.isDownloaded(latestGame.id) || romDownloadManager.isDownloaded(latestGame)
                        },
                        downloadedSizeText = remember(latestGame.id, downloadVersion) {
                            val size = contentDownloadManager.localSize(latestGame.id) ?: romDownloadManager.localSize(latestGame.id)
                            if (size == null) "未下载" else formatFileSize(size)
                        },
                        onDelete = { includeSaves ->
                            scope.launch {
                                if (includeSaves) saveStateStore.deleteForGame(latestGame.id)
                                contentDownloadManager.deleteLocal(latestGame.id)
                                romDownloadManager.deleteLocal(latestGame)
                                downloadVersion++
                            }
                        },
                        onDownload = { onComplete ->
                            scope.launch {
                                try {
                                    val files = detail?.files.orEmpty()
                                    if (files.isNotEmpty()) {
                                        contentDownloadManager.prepare(latestGame, files)
                                    } else {
                                        romDownloadManager.prepare(latestGame)
                                    }
                                    downloadVersion++
                                } catch (e: Exception) {
                                    launchMessage = "下载失败：${e.message ?: "未知错误"}"
                                } finally {
                                    onComplete()
                                }
                            }
                        },
                        onStart = {
                            scope.launch {
                                try {
                                    val (playableGame, startedAt, launch) = withContext(Dispatchers.IO) {
                                        val files = detail?.files.orEmpty()
                                        val prepared = if (files.isNotEmpty()) {
                                            contentDownloadManager.prepare(latestGame, files)
                                        } else {
                                            romDownloadManager.prepare(latestGame)
                                        }
                                        val startedAt = System.currentTimeMillis()
                                        val launch = emulatorSessionFactory.createStartedSession(prepared)
                                        Triple(prepared, startedAt, launch)
                                    }
                                    android.util.Log.i("RetroHallApp", "onStart ok, session.state=${launch.session.state}, msg=${launch.message}")
                                    launchMessage = launch.message
                                    gameRepository.markPlayed(playableGame.id, startedAt)
                                    route = AppRoute.Game(playableGame, launch.session, startedAt, launch.message)
                                    android.util.Log.i("RetroHallApp", "route switched to Game")
                                } catch (e: Exception) {
                                    android.util.Log.e("RetroHallApp", "onStart failed", e)
                                    launchMessage = "启动失败：${e.message ?: "未知错误"}"
                                }
                            }
                        },
                    )
                }

                is AppRoute.SaveManager -> SaveManagerScreen(
                    game = current.game,
                    saveStateStore = saveStateStore,
                    selectedSaveId = selectedSaveIds[current.game.id],
                    onSelectSave = { saveState ->
                        selectedSaveIds = selectedSaveIds + (current.game.id to saveState.id)
                        route = AppRoute.Detail(current.game)
                    },
                    onBackToDetail = { route = AppRoute.Detail(current.game) },
                    onSelectLibrary = { openHall("游戏库") },
                    onSelectRecent = { openHall("最近") },
                    onSelectFavorites = { openHall("收藏") },
                    onOpenSettings = { route = AppRoute.Settings },
                )

                is AppRoute.Game -> GameScreen(
                    game = current.game,
                    session = current.session,
                    settings = settings,
                    launchNotice = current.launchNotice,
                    selectedSaveStates = saveStateStore.observeForGame(current.game.id).collectAsState(initial = emptyList()).value,
                    selectedSaveId = selectedSaveIds[current.game.id],
                    onPersistSaveState = { slot ->
                        scope.launch { saveStateStore.upsert(current.game.id, slot) }
                    },
                    onExit = {
                        current.session.stop()
                        scope.launch {
                            gameRepository.recordPlaySession(current.game.id, current.startedAt)
                        }
                        route = AppRoute.Detail(current.game)
                    },
                    onOpenSettings = { route = AppRoute.Settings },
                    onUpdateSettings = { next ->
                        scope.launch { userSettingsStore.update(next) }
                    },
                )

                AppRoute.Settings -> SettingsScreen(
                    settings = settings,
                    cacheMaintenance = cacheMaintenance,
                    coreCatalogClient = coreCatalogClient,
                    coreDownloadManager = coreDownloadManager,
                    coreSelectionStore = coreSelectionStore,
                    resourceSourceStore = resourceSourceStore,
                    onCacheCleared = { coverReloadTick++ },
                    onUpdateSettings = { next ->
                        scope.launch { userSettingsStore.update(next) }
                    },
                    onSelectLibrary = { openHall("游戏库") },
                    onSelectRecent = { openHall("最近") },
                    onSelectFavorites = { openHall("收藏") },
                    onOpenSettings = { route = AppRoute.Settings },
                )
            }
        }
        TopToast(hallExitToast, onDismiss = { hallExitToast = null })
    }
}
