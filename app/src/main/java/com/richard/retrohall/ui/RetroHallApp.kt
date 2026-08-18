package com.richard.retrohall.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.State
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import com.richard.retrohall.RetroHallDependencies
import com.richard.retrohall.data.cache.CacheManager
import com.richard.retrohall.data.game.CoverDownloader
import com.richard.retrohall.data.game.GameRepository
import com.richard.retrohall.data.game.RomDownloadManager
import com.richard.retrohall.data.assets.PrivateAssetInitializer
import com.richard.retrohall.data.db.SaveStateEntity
import com.richard.retrohall.data.save.SaveStateRepository
import com.richard.retrohall.data.settings.UserSettingsStore
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.settings.AspectRatio
import com.richard.retrohall.domain.settings.ControlMode
import com.richard.retrohall.domain.settings.UserSettings
import com.richard.retrohall.emulator.EmulatorSession
import com.richard.retrohall.emulator.EmulatorSessionFactory
import com.richard.retrohall.emulator.EmulatorState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val UiBg = Color(0xFF071013)
private val UiPanel = Color(0xFF0B171B)
private val UiPanelSoft = Color(0xFF101D22)
private val UiText = Color(0xFFF2FBFC)
private val UiMuted = Color(0xFF8DA5AA)
private val UiCyan = Color(0xFF35F1DD)
private val UiBlue = Color(0xFF1AAEF0)
private val UiGold = Color(0xFFFFE36E)
private val UiLine = Color(0x3ADAF1F4)

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
        emulatorSessionFactory = dependencies.emulatorSessionFactory,
        privateAssetInitializer = dependencies.privateAssetInitializer,
        userSettingsStore = dependencies.userSettingsStore,
        saveStateRepository = dependencies.saveStateRepository,
        cacheManager = dependencies.cacheManager,
    )
}

@Composable
private fun RetroHallAppContent(
    gameRepository: GameRepository,
    romDownloadManager: RomDownloadManager,
    emulatorSessionFactory: EmulatorSessionFactory,
    privateAssetInitializer: PrivateAssetInitializer?,
    userSettingsStore: UserSettingsStore,
    saveStateRepository: SaveStateRepository,
    cacheManager: CacheManager,
) {
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf<AppRoute>(AppRoute.Hall) }
    var launchMessage by remember { mutableStateOf<String?>(null) }
    var selectedSaveIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedHallSection by rememberSaveable { mutableStateOf("游戏库") }
    var downloadVersion by remember { mutableStateOf(0) }
    var coverReloadTick by remember { mutableStateOf(0L) }
    val libraryGridState = rememberLazyGridState()
    val recentGridState = rememberLazyGridState()
    val favoritesGridState = rememberLazyGridState()
    val games by gameRepository.games.collectAsState(initial = emptyList())
    val settings by userSettingsStore.settings.collectAsState(initial = UserSettings())

    fun openHall(section: String = "游戏库") {
        selectedHallSection = section
        route = AppRoute.Hall
    }

    LaunchedEffect(gameRepository) {
        privateAssetInitializer?.initialize()
        gameRepository.seedIfEmpty()
        runCatching { gameRepository.syncRemoteCatalog() }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F172A)) {
            when (val current = route) {
                AppRoute.Hall -> HallScreen(
                    games = games,
                    selectedSection = selectedHallSection,
                    libraryGridState = libraryGridState,
                    recentGridState = recentGridState,
                    favoritesGridState = favoritesGridState,
                    coverReloadTick = coverReloadTick,
                    onRefreshCovers = {
                        val changed = gameRepository.refreshCovers()
                        if (changed) coverReloadTick++
                    },
                    onSelectSection = { selectedHallSection = it },
                    onOpenGame = {
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
                    DetailScreen(
                        game = latestGame,
                        message = launchMessage,
                        saveStateRepository = saveStateRepository,
                        selectedSaveId = selectedSaveIds[latestGame.id],
                        coverReloadTick = coverReloadTick,
                        onOpenSaveManager = { route = AppRoute.SaveManager(latestGame) },
                        onSelectLibrary = { openHall("游戏库") },
                        onSelectRecent = { openHall("最近") },
                        onSelectFavorites = { openHall("收藏") },
                        onOpenSettings = { route = AppRoute.Settings },
                        onToggleFavorite = {
                            scope.launch { gameRepository.toggleFavorite(latestGame) }
                        },
                        isDownloaded = remember(latestGame.id, downloadVersion) { romDownloadManager.isDownloaded(latestGame.id) },
                        downloadedSizeText = remember(latestGame.id, downloadVersion) {
                            val size = romDownloadManager.localSize(latestGame.id)
                            if (size == null) "未下载" else formatFileSize(size)
                        },
                        onDelete = { includeSaves ->
                            scope.launch {
                                if (includeSaves) saveStateRepository.deleteForGame(latestGame.id)
                                romDownloadManager.deleteLocal(latestGame)
                                downloadVersion++
                            }
                        },
                        onDownload = { onComplete ->
                            scope.launch {
                                try {
                                    romDownloadManager.prepare(latestGame)
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
                                    val playableGame = romDownloadManager.prepare(latestGame)
                                    val startedAt = System.currentTimeMillis()
                                    val launch = emulatorSessionFactory.createStartedSession(playableGame)
                                    launchMessage = launch.message
                                    gameRepository.markPlayed(playableGame.id, startedAt)
                                    route = AppRoute.Game(playableGame, launch.session, startedAt, launch.message)
                                } catch (e: Exception) {
                                    launchMessage = "启动失败：${e.message ?: "未知错误"}"
                                }
                            }
                        },
                    )
                }

                is AppRoute.SaveManager -> SaveManagerScreen(
                    game = current.game,
                    saveStateRepository = saveStateRepository,
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
                    onExit = {
                        current.session.saveSram()
                        current.session.stop()
                        scope.launch {
                            gameRepository.recordPlaySession(current.game.id, current.startedAt)
                        }
                        route = AppRoute.Hall
                    },
                    onOpenSettings = { route = AppRoute.Settings },
                )

                AppRoute.Settings -> SettingsScreen(
                    settings = settings,
                    cacheManager = cacheManager,
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
    }
}

private enum class NavIcon {
    Library,
    Search,
    Recent,
    Star,
    Gear,
}

private val NavIcon.vector: ImageVector
    get() = when (this) {
        NavIcon.Library -> Icons.Outlined.Apps
        NavIcon.Search -> Icons.Outlined.Search
        NavIcon.Recent -> Icons.Outlined.AccessTime
        NavIcon.Star -> Icons.Outlined.StarBorder
        NavIcon.Gear -> Icons.Outlined.Settings
    }

@Composable
private fun AppShell(
    selectedNav: String,
    edgeToEdgeMain: Boolean = false,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    AppBackground(contentPadding = PaddingValues(0.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp || maxWidth < maxHeight
            val sidebarWidth = when {
                maxWidth >= 1500.dp -> 300.dp
                maxWidth >= 1100.dp -> 210.dp
                maxWidth >= 900.dp -> 160.dp
                else -> 148.dp
            }
            val mainHorizontalPadding = when {
                maxWidth >= 1500.dp -> 66.dp
                maxWidth >= 1100.dp -> 44.dp
                else -> 24.dp
            }
            val mainVerticalPadding = when {
                maxHeight >= 900.dp -> 48.dp
                maxHeight >= 700.dp -> 34.dp
                else -> 24.dp
            }

            if (compact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CompactTopNav(
                        selectedNav = selectedNav,
                        onSelectLibrary = onSelectLibrary,
                        onSelectRecent = onSelectRecent,
                        onSelectFavorites = onSelectFavorites,
                        onOpenSettings = onOpenSettings,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                    ) {
                        content()
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    HallSidebar(
                        selectedSection = selectedNav,
                        sidebarWidth = sidebarWidth,
                        onSelectLibrary = onSelectLibrary,
                        onSelectRecent = onSelectRecent,
                        onSelectFavorites = onSelectFavorites,
                        onOpenSettings = onOpenSettings,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = mainHorizontalPadding,
                                top = if (edgeToEdgeMain) 0.dp else mainVerticalPadding,
                                end = mainHorizontalPadding,
                                bottom = if (edgeToEdgeMain) 0.dp else mainVerticalPadding - 8.dp,
                            ),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTopNav(
    selectedNav: String,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .background(Color(0xF2071115))
            .drawBehind {
                drawLine(
                    color = UiLine,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactNavItem(NavIcon.Library, "游戏库", selectedNav == "游戏库", onSelectLibrary)
        CompactNavItem(NavIcon.Recent, "最近", selectedNav == "最近", onSelectRecent)
        CompactNavItem(NavIcon.Star, "收藏", selectedNav == "收藏", onSelectFavorites)
        CompactNavItem(NavIcon.Gear, "设置", selectedNav == "设置", onOpenSettings)
    }
}

@Composable
private fun CompactNavItem(icon: NavIcon, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .height(62.dp)
            .width(70.dp)
            .background(if (selected) Color(0x3335F1DD) else Color.Transparent, RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) UiCyan else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HallIcon(icon = icon, color = if (selected) UiText else UiMuted, modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) UiText else UiMuted, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PageTitle(title: String, sub: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, color = UiText, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
        if (sub.isNotBlank()) {
            Text(sub, color = UiMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyPanel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Brush.verticalGradient(listOf(Color(0xEB0F1E24), Color(0xEB081115))), RoundedCornerShape(16.dp))
            .border(1.dp, UiLine, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = UiMuted, fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EmptyPanelFrame(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPanel(text = text)
    }
}

@Composable
private fun HallIcon(icon: NavIcon, color: Color, modifier: Modifier = Modifier) {
    Icon(imageVector = icon.vector, contentDescription = null, tint = color, modifier = modifier)
}

@Composable
private fun StarIcon(filled: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = starPath(size.width, size.height)
        if (filled) {
            drawPath(path, color = color)
        }
        drawPath(path, color = color, style = Stroke(width = size.minDimension * 0.08f, join = StrokeJoin.Round))
    }
}

private fun starPath(width: Float, height: Float): Path {
    val path = Path()
    val centerX = width / 2f
    val centerY = height / 2f
    val outer = min(width, height) * 0.44f
    val inner = outer * 0.48f
    repeat(10) { index ->
        val radius = if (index % 2 == 0) outer else inner
        val angle = (-PI / 2.0 + index * PI / 5.0).toFloat()
        val x = centerX + cos(angle) * radius
        val y = centerY + sin(angle) * radius
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private enum class GameSort(val label: String) {
    Hotness("热度"),
    Name("名称"),
    Recent("最近游玩"),
    PlayTime("游戏时长"),
}

@Composable
private fun HallScreen(
    games: List<LocalGame>,
    selectedSection: String,
    libraryGridState: LazyGridState,
    recentGridState: LazyGridState,
    favoritesGridState: LazyGridState,
    coverReloadTick: Long,
    onRefreshCovers: suspend () -> Unit,
    onSelectSection: (String) -> Unit,
    onOpenGame: (LocalGame) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: (LocalGame) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var focusSearchOnReveal by remember { mutableStateOf(false) }
    var selectedPlatform by rememberSaveable { mutableStateOf("FC") }
    var selectedSort by rememberSaveable { mutableStateOf(GameSort.Hotness) }
    var toolbarInteractionTick by remember { mutableStateOf(0L) }
    val searchFocusRequester = remember { FocusRequester() }
    val pageFocusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val searchPullThreshold = with(density) { 52.dp.toPx() }
    val gridTopPadding = 28.dp
    val platformOptions = remember(games) {
        val all = games
            .mapNotNull { it.platform }
            .flatMap { it.split("/", "、", "，", ",", ";").map { p -> p.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("全部", "FC") + all.filter { it != "FC" && it != "全部" }
    }

    val filtered = games.filter { game ->
        val sectionMatches = when (selectedSection) {
            "收藏" -> game.favorite
            "最近" -> game.lastPlayedAt != null
            else -> true
        }
        val platformMatches = selectedPlatform == "全部" ||
            game.platform.contains(selectedPlatform, ignoreCase = true) ||
            (selectedPlatform == "FC" && game.platform.contains("NES", ignoreCase = true))
        val queryMatches = query.isBlank() ||
            game.title.contains(query, ignoreCase = true) ||
            game.category.contains(query, ignoreCase = true) ||
            game.platform.contains(query, ignoreCase = true)
        sectionMatches && platformMatches && queryMatches
    }
    val gamesShown = when (selectedSort) {
        GameSort.Hotness -> filtered.sortedWith(compareByDescending<LocalGame> { it.hotness ?: Double.NEGATIVE_INFINITY })
        GameSort.Name -> filtered.sortedBy { it.title.lowercase() }
        GameSort.Recent -> filtered.sortedWith(compareByDescending<LocalGame> { it.lastPlayedAt ?: Long.MIN_VALUE })
        GameSort.PlayTime -> filtered.sortedByDescending { it.totalPlayTimeMillis }
    }
    val showSearch = searchVisible

    LaunchedEffect(searchVisible, toolbarInteractionTick) {
        if (searchVisible) {
            delay(5000)
            if (searchVisible) {
                searchVisible = false
                focusSearchOnReveal = false
            }
        }
    }

    LaunchedEffect(searchVisible) {
        if (searchVisible && focusSearchOnReveal) {
            searchFocusRequester.requestFocus()
            focusSearchOnReveal = false
        }
    }

    LaunchedEffect(Unit) {
        pageFocusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        onRefreshCovers()
    }

    AppShell(
        selectedNav = selectedSection,
        edgeToEdgeMain = true,
        onSelectLibrary = {
            onSelectSection("游戏库")
            searchVisible = false
        },
        onSelectRecent = {
            onSelectSection("最近")
            searchVisible = false
        },
        onSelectFavorites = {
            onSelectSection("收藏")
            searchVisible = false
        },
        onOpenSettings = onOpenSettings,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(pageFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && isSearchRevealKey(event.key)) {
                        focusSearchOnReveal = true
                        searchVisible = true
                        true
                    } else {
                        false
                    }
                },
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (filtered.isEmpty()) {
                    EmptyPanelFrame(
                        text = if (query.isBlank()) "没有游戏" else "没有匹配的游戏",
                    )
                } else {
                    val gridState = when (selectedSection) {
                        "最近" -> recentGridState
                        "收藏" -> favoritesGridState
                        else -> libraryGridState
                    }
                    var pullDistance by remember(selectedSection) { mutableStateOf(0f) }
                    val searchRevealScroll = remember(gridState, selectedSection, searchPullThreshold, query) {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                if (source != NestedScrollSource.UserInput) return Offset.Zero
                                if (available.y > 0f) {
                                    pullDistance = min(pullDistance + available.y, searchPullThreshold * 2f)
                                    if (pullDistance >= searchPullThreshold) {
                                        focusSearchOnReveal = false
                                        if (!searchVisible) searchVisible = true
                                        toolbarInteractionTick++
                                    }
                                } else if (available.y < 0f) {
                                    pullDistance = 0f
                                    focusSearchOnReveal = false
                                    if (searchVisible) {
                                        searchVisible = false
                                        toolbarInteractionTick++
                                    }
                                }
                                return Offset.Zero
                            }
                        }
                    }
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = when {
                            maxWidth >= 600.dp -> 4
                            maxWidth >= 430.dp -> 3
                            else -> 2
                        }
                        val gap = when {
                            maxWidth >= 600.dp -> 18.dp
                            maxWidth >= 430.dp -> 14.dp
                            else -> 12.dp
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(searchRevealScroll),
                            contentPadding = PaddingValues(top = gridTopPadding, bottom = 28.dp),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            items(gamesShown, key = { it.id }) { game ->
                                GameTile(
                                    game = game,
                                    coverReloadTick = coverReloadTick,
                                    onClick = { onOpenGame(game) },
                                    onToggleFavorite = { onToggleFavorite(game) },
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = gridTopPadding)
                        .zIndex(2f),
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSearch,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    ) {
                        HallFilterBar(
                            query = query,
                            onQueryChange = {
                                query = it
                                toolbarInteractionTick++
                            },
                            selectedPlatform = selectedPlatform,
                            platformOptions = platformOptions,
                            onSelectPlatform = { selectedPlatform = it },
                            selectedSort = selectedSort,
                            sortOptions = GameSort.entries,
                            onSelectSort = { selectedSort = it },
                            focusRequester = searchFocusRequester,
                            onInteraction = { toolbarInteractionTick++ },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HallSidebar(
    selectedSection: String,
    sidebarWidth: androidx.compose.ui.unit.Dp,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF2071115), Color(0xFA040A0D)),
                ),
            )
            .drawBehind {
                val gap = 8.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    var y = 0f
                    while (y <= size.height) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            radius = 1.05.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y),
                        )
                        y += gap
                    }
                    x += gap
                }
                drawLine(
                    color = UiLine,
                    start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            .padding(start = 22.dp, top = 34.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        SidebarLogo()
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                SidebarItem(icon = NavIcon.Library, label = "游戏库", selected = selectedSection == "游戏库", onClick = onSelectLibrary)
                SidebarItem(icon = NavIcon.Recent, label = "最近", selected = selectedSection == "最近", onClick = onSelectRecent)
                SidebarItem(icon = NavIcon.Star, label = "收藏", selected = selectedSection == "收藏", onClick = onSelectFavorites)
                SidebarItem(icon = NavIcon.Gear, label = "设置", selected = selectedSection == "设置", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
private fun SidebarLogo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        RetroConsoleLogo(modifier = Modifier.width(106.dp).height(50.dp))
    }
}

@Composable
private fun RetroConsoleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = h * 0.055f
        val radius = h * 0.22f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1469F4), UiCyan, Color(0xFFFF6B49)),
                start = Offset(0f, h),
                end = Offset(w, 0f),
            ),
            size = Size(w, h),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = Color(0xFF11171A),
            topLeft = Offset(stroke, stroke),
            size = Size(w - stroke * 2f, h - stroke * 2f),
            cornerRadius = CornerRadius(radius * 0.78f, radius * 0.78f),
        )
        drawRoundRect(
            color = Color(0xFF070A0C),
            topLeft = Offset(w * 0.26f, h * 0.12f),
            size = Size(w * 0.47f, h * 0.76f),
            cornerRadius = CornerRadius(h * 0.06f, h * 0.06f),
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D3F36), Color(0xFF020907)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = h * 0.62f,
            ),
            topLeft = Offset(w * 0.29f, h * 0.17f),
            size = Size(w * 0.41f, h * 0.66f),
            cornerRadius = CornerRadius(h * 0.045f, h * 0.045f),
        )

        val dpadX = w * 0.12f
        val dpadY = h * 0.34f
        drawRoundRect(
            color = Color(0xFF070A0C),
            topLeft = Offset(dpadX - w * 0.018f, dpadY - h * 0.13f),
            size = Size(w * 0.12f, h * 0.42f),
            cornerRadius = CornerRadius(h * 0.035f, h * 0.035f),
        )
        drawRoundRect(
            color = Color(0xFF070A0C),
            topLeft = Offset(dpadX - w * 0.06f, dpadY - h * 0.02f),
            size = Size(w * 0.20f, h * 0.16f),
            cornerRadius = CornerRadius(h * 0.035f, h * 0.035f),
        )
        drawRoundRect(
            color = UiCyan,
            topLeft = Offset(dpadX - w * 0.012f, dpadY - h * 0.19f),
            size = Size(w * 0.024f, h * 0.10f),
            cornerRadius = CornerRadius(h * 0.014f, h * 0.014f),
        )
        drawRoundRect(
            color = UiCyan,
            topLeft = Offset(dpadX - w * 0.095f, dpadY - h * 0.005f),
            size = Size(w * 0.07f, h * 0.045f),
            cornerRadius = CornerRadius(h * 0.014f, h * 0.014f),
        )

        drawRect(color = UiCyan, topLeft = Offset(w * 0.43f, h * 0.37f), size = Size(w * 0.048f, h * 0.12f))
        drawRect(color = UiCyan, topLeft = Offset(w * 0.56f, h * 0.37f), size = Size(w * 0.048f, h * 0.12f))
        drawLine(
            color = UiCyan,
            start = Offset(w * 0.44f, h * 0.59f),
            end = Offset(w * 0.59f, h * 0.59f),
            strokeWidth = stroke * 1.05f,
            cap = StrokeCap.Round,
        )

        listOf(
            Offset(w * 0.85f, h * 0.24f) to UiGold,
            Offset(w * 0.80f, h * 0.39f) to UiBlue,
            Offset(w * 0.90f, h * 0.39f) to UiCyan,
            Offset(w * 0.85f, h * 0.54f) to Color(0xFFFF6B49),
        ).forEach { (center, color) ->
            drawCircle(color = Color(0xFF040708), radius = h * 0.076f, center = center)
            drawCircle(color = color, radius = h * 0.045f, center = center)
        }
        drawRoundRect(
            color = UiGold,
            topLeft = Offset(w * 0.80f, h * 0.70f),
            size = Size(w * 0.12f, h * 0.035f),
            cornerRadius = CornerRadius(h * 0.018f, h * 0.018f),
        )
    }
}

@Composable
private fun SidebarItem(icon: NavIcon, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                if (selected) Brush.linearGradient(listOf(Color(0x3A35F1DD), Color(0x1035F1DD)))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                RoundedCornerShape(14.dp),
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) UiCyan else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HallIcon(icon = icon, color = if (selected) UiText else Color(0xD1F2FBFC), modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) UiText else Color(0xD1F2FBFC), fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun HallFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedPlatform: String,
    platformOptions: List<String>,
    onSelectPlatform: (String) -> Unit,
    selectedSort: GameSort,
    sortOptions: List<GameSort>,
    onSelectSort: (GameSort) -> Unit,
    focusRequester: FocusRequester,
    onInteraction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xEC091216), Color(0xE2040A0D))),
                RoundedCornerShape(16.dp),
            )
            .border(1.dp, UiLine, RoundedCornerShape(16.dp))
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.16f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarSelect(
                label = "类型",
                value = selectedPlatform,
                options = platformOptions,
                onSelect = {
                    onSelectPlatform(it)
                    onInteraction()
                },
                onInteraction = onInteraction,
            )
            ToolbarSelect(
                label = "排序",
                value = selectedSort.label,
                options = sortOptions.map { it.label },
                onSelect = { label ->
                    sortOptions.firstOrNull { it.label == label }?.let {
                        onSelectSort(it)
                        onInteraction()
                    }
                },
                onInteraction = onInteraction,
            )
        }
        ToolbarDivider()
        SearchBox(
            query = query,
            onQueryChange = onQueryChange,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0x4DDAF1F4), Color.Transparent)),
            ),
    )
}

@Composable
private fun ToolbarSelect(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onInteraction: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (expanded) Color(0x2835F1DD) else Color(0x14FFFFFF), RoundedCornerShape(10.dp))
                .border(1.dp, if (expanded) UiCyan else Color(0x1FDAF1F4), RoundedCornerShape(10.dp))
                .clickable {
                    expanded = !expanded
                    onInteraction()
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = UiMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(value, color = UiText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Canvas(modifier = Modifier.size(10.dp)) {
                val triangle = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(triangle, color = UiCyan.copy(alpha = 0.9f))
            }
        }
        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(LocalDensity.current) { 48.dp.roundToPx() }),
            ) {
                Column(
                    modifier = Modifier
                        .width(156.dp)
                        .shadow(16.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.28f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xF30F1E24), Color(0xF3060C10))), RoundedCornerShape(14.dp))
                        .border(1.dp, UiLine, RoundedCornerShape(14.dp))
                        .padding(5.dp),
                ) {
                    options.forEach { option ->
                        val selected = option == value
                        Text(
                            option,
                            color = if (selected) Color(0xFF031112) else UiText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) UiCyan else Color.Transparent)
                                .clickable {
                                    expanded = false
                                    onSelect(option)
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0x2835F1DD) else Color(0x16FFFFFF), RoundedCornerShape(12.dp))
            .border(1.dp, if (focused) UiCyan else Color(0x1FDAF1F4), RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HallIcon(icon = NavIcon.Search, color = if (focused) UiCyan else UiMuted, modifier = Modifier.size(22.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = UiText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                ),
                cursorBrush = Brush.verticalGradient(listOf(UiCyan, UiCyan)),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    if (query.isBlank()) {
                        Text("搜索游戏", color = UiText.copy(alpha = 0.55f), fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                    innerTextField()
                },
            )
        }
    }
}

@Composable
private fun GameTile(game: LocalGame, coverReloadTick: Long, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val palette = remember(game.id) { gamePalette(game) }
    val coverBitmap = rememberCoverBitmap(game, coverReloadTick).value
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val highlighted = isFocused || isHovered || isPressed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(if (highlighted) 20.dp else 8.dp, RoundedCornerShape(8.dp), ambientColor = if (highlighted) UiCyan.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.28f), spotColor = if (highlighted) UiCyan.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.28f))
            .background(Color(0xFF101820), RoundedCornerShape(8.dp))
            .border(if (highlighted) 3.dp else 1.5.dp, if (highlighted) UiCyan else Color(0x884A565C), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(palette.first.copy(alpha = 0.9f), palette.second.copy(alpha = 0.82f))))
                drawCircle(Color.White.copy(alpha = 0.18f), radius = size.minDimension * 0.18f, center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.24f))
                drawCircle(Color.Black.copy(alpha = 0.18f), radius = size.minDimension * 0.42f, center = androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.42f))
                rotate(-8f, pivot = androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.42f)) {
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.05f))),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.10f, size.height * 0.12f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.40f, size.height * 0.56f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    )
                }
                rotate(-18f, pivot = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.70f)) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.16f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.50f, size.height * 0.68f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.42f, size.height * 0.14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(999.dp.toPx(), 999.dp.toPx()),
                    )
                    drawCircle(Color.White.copy(alpha = 0.13f), radius = 18.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.54f))
                    drawCircle(Color.White.copy(alpha = 0.10f), radius = 15.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.23f, size.height * 0.67f))
                }
            },
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .align(Alignment.BottomStart)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)))),
        )
        if (game.favorite) {
            StarIcon(
                filled = true,
                color = UiGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(22.dp)
                    .clickable(onClick = onToggleFavorite),
            )
        }
        Text(
            game.title,
            color = UiText,
            fontSize = 15.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 42.dp, bottom = 10.dp),
        )
    }
}

private fun gamePalette(game: LocalGame): Pair<Color, Color> {
    val colors = listOf(
        Color(0xFF122F66) to Color(0xFFFF7C45),
        Color(0xFF123A3A) to Color(0xFF2AD6BC),
        Color(0xFF513064) to Color(0xFFF6B35A),
        Color(0xFF071C3B) to Color(0xFF5A8EE6),
        Color(0xFF125069) to Color(0xFFF9C443),
        Color(0xFF0B6B83) to Color(0xFF7BD66F),
        Color(0xFF214D76) to Color(0xFFE9F2FF),
        Color(0xFF412269) to Color(0xFFF0757C),
    )
    return colors[(game.id.hashCode() and Int.MAX_VALUE) % colors.size]
}

@Composable
private fun DetailScreen(
    game: LocalGame,
    message: String?,
    saveStateRepository: SaveStateRepository,
    selectedSaveId: String?,
    coverReloadTick: Long,
    onOpenSaveManager: () -> Unit,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: () -> Unit,
    isDownloaded: Boolean,
    downloadedSizeText: String,
    onDelete: (includeSaves: Boolean) -> Unit,
    onDownload: (onComplete: () -> Unit) -> Unit,
    onStart: () -> Unit,
) {
    var detailMessage by remember(message) { mutableStateOf(message) }
    var showDeleteDialog by remember(game.id) { mutableStateOf(false) }
    var busy by remember(game.id) { mutableStateOf(false) }
    val saveStates by saveStateRepository.observeForGame(game.id).collectAsState(initial = emptyList())
    val selectedSaveState = saveStates.firstOrNull { it.id == selectedSaveId } ?: saveStates.firstOrNull()
    val selectedSaveName = selectedSaveState?.displayName() ?: "新存档"

    AppShell(
        selectedNav = "",
        onSelectLibrary = onSelectLibrary,
        onSelectRecent = onSelectRecent,
        onSelectFavorites = onSelectFavorites,
        onOpenSettings = onOpenSettings,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val dense = maxHeight < 520.dp
            val dividerColor = Color(0x12FFFFFF)
            val coverSize = when {
                maxHeight < 470.dp -> 176.dp
                maxHeight < 620.dp -> 248.dp
                maxHeight < 760.dp -> 328.dp
                else -> (maxHeight * 0.56f).coerceAtMost(560.dp)
            }
            val topGap = if (dense) 14.dp else 22.dp
            val titleSize = if (dense) 32.sp else 46.sp
            val titleLineHeight = if (dense) 35.sp else 50.sp
            val descSize = if (dense) 16.sp else 20.sp
            val descLineHeight = if (dense) 23.sp else 30.sp
            val detailActionCompact = dense

            Column(modifier = Modifier.fillMaxSize()) {
                Row(horizontalArrangement = Arrangement.spacedBy(topGap), modifier = Modifier.height(coverSize)) {
                    Box(
                        modifier = Modifier
                            .width(coverSize)
                            .fillMaxSize()
                            .shadow(26.dp, RoundedCornerShape(18.dp), ambientColor = UiCyan.copy(alpha = 0.32f), spotColor = UiCyan.copy(alpha = 0.32f))
                            .background(UiPanelSoft, RoundedCornerShape(18.dp))
                            .border(3.dp, UiCyan, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp)),
                    ) {
                        CoverArt(game = game, focused = false, coverReloadTick = coverReloadTick, modifier = Modifier.fillMaxSize())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(game.title, color = UiText, fontSize = titleSize, lineHeight = titleLineHeight, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(if (dense) 10.dp else 16.dp))
                            HorizontalDivider(color = dividerColor)
                            Spacer(Modifier.height(if (dense) 10.dp else 16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(if (dense) 10.dp else 18.dp)) {
                                DetailInfoItem("游戏格式", game.platform, valueSize = if (dense) 14.sp else 15.sp, modifier = Modifier.weight(1f))
                                DetailInfoItem("最近游玩", formatTimestamp(game.lastPlayedAt), valueSize = if (dense) 14.sp else 15.sp, modifier = Modifier.weight(1f))
                                DetailInfoItem("游戏时长", formatPlayTime(game.totalPlayTimeMillis), valueSize = if (dense) 14.sp else 15.sp, modifier = Modifier.weight(1f))
                                DetailInfoItem("ROM 大小", if (busy) "正在下载" else downloadedSizeText, valueSize = if (dense) 14.sp else 15.sp, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(if (dense) 10.dp else 16.dp))
                        HorizontalDivider(color = dividerColor)
                        Spacer(Modifier.weight(1f))
                        if (detailMessage != null) {
                            Text(detailMessage.orEmpty(), color = UiCyan, fontSize = if (dense) 16.sp else 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(if (dense) 8.dp else 12.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(if (dense) 12.dp else 18.dp)) {
                            HallActionButton(
                                when {
                                    isDownloaded -> "开始游戏"
                                    busy -> "下载中"
                                    else -> "下载"
                                },
                                focused = true,
                                compact = detailActionCompact,
                                icon = if (isDownloaded) Icons.Outlined.PlayArrow else Icons.Outlined.Download,
                                iconSize = if (dense) 26.dp else 30.dp,
                                enabled = !busy,
                                onClick = {
                                    if (isDownloaded) {
                                        onStart()
                                    } else {
                                        if (busy) return@HallActionButton
                                        busy = true
                                        onDownload { busy = false }
                                    }
                                },
                            )
                            HallActionButton(selectedSaveName, focused = false, compact = detailActionCompact, icon = Icons.Outlined.Description, onClick = onOpenSaveManager)
                            HallActionButton(
                                if (game.favorite) "已收藏" else "收藏",
                                focused = false,
                                compact = detailActionCompact,
                                icon = if (game.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                iconSize = if (dense) 26.dp else 30.dp,
                                onClick = onToggleFavorite,
                            )
                            if (isDownloaded) {
                                HallActionButton("删除", focused = false, danger = true, compact = detailActionCompact, icon = Icons.Outlined.Delete, onClick = { showDeleteDialog = true })
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = if (dense) 14.dp else 22.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(26.dp, RoundedCornerShape(18.dp), ambientColor = UiCyan.copy(alpha = 0.18f), spotColor = UiCyan.copy(alpha = 0.18f))
                            .background(UiPanelSoft, RoundedCornerShape(18.dp))
                            .border(1.dp, UiLine, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .padding(if (dense) 14.dp else 22.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text(
                                game.description.ifBlank { "探索经典 FC / NES 世界，支持手柄、触摸和横屏大屏浏览。游戏资源来自在线游戏库索引，可下载后本地缓存运行。" },
                                color = UiText.copy(alpha = 0.72f),
                                fontSize = descSize,
                                lineHeight = descLineHeight,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.98f),
                shape = RoundedCornerShape(22.dp),
                color = UiPanel,
                border = BorderStroke(1.dp, UiLine),
                shadowElevation = 24.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HallActionButton("仅删除游戏", icon = Icons.Outlined.Delete, fillWidth = true, onClick = {
                        showDeleteDialog = false
                        onDelete(false)
                    })
                    HallActionButton("删除游戏和存档", icon = Icons.Outlined.Delete, fillWidth = true, onClick = {
                        showDeleteDialog = false
                        onDelete(true)
                    })
                    HallActionButton("取消", icon = Icons.Outlined.Close, fillWidth = true, onClick = { showDeleteDialog = false })
                }
            }
        }
    }
}

@Composable
private fun DetailInfoItem(label: String, value: String, valueSize: TextUnit = 15.sp, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = UiMuted, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(value, color = UiText, fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatPlayTime(totalPlayTimeMillis: Long): String {
    val totalMinutes = totalPlayTimeMillis / 60000
    if (totalMinutes <= 0) return "未游玩"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}小时${minutes}分钟"
        hours > 0 -> "${hours}小时"
        else -> "${minutes}分钟"
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024.0) "%.0f KB".format(kb, Locale.CHINA)
    else "%.1f MB".format(kb / 1024.0, Locale.CHINA)
}

@Composable
private fun SaveManagerScreen(
    game: LocalGame,
    saveStateRepository: SaveStateRepository,
    selectedSaveId: String?,
    onSelectSave: (SaveStateEntity) -> Unit,
    onBackToDetail: () -> Unit,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val saveStates by saveStateRepository.observeForGame(game.id).collectAsState(initial = emptyList())

    AppShell(
        selectedNav = "",
        onSelectLibrary = onSelectLibrary,
        onSelectRecent = onSelectRecent,
        onSelectFavorites = onSelectFavorites,
        onOpenSettings = onOpenSettings,
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("存档管理", color = UiText, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
                    Text(game.title, color = UiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HallActionButton("新增槽位", focused = true, compact = true) {
                        scope.launch { saveStateRepository.addSlot(game.id) }
                    }
                    HallActionButton("返回详情", focused = false, compact = true, onClick = onBackToDetail)
                }
            }

            if (saveStates.isEmpty()) {
                EmptyPanel("暂无存档")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    lazyItems(saveStates, key = { it.id }) { saveState ->
                        SaveStateRow(
                            saveState = saveState,
                            selected = saveState.id == selectedSaveId,
                            onSelect = { onSelectSave(saveState) },
                            onCopy = { scope.launch { saveStateRepository.copy(saveState) } },
                            onDelete = { scope.launch { saveStateRepository.delete(saveState) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveStateRow(
    saveState: SaveStateEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Brush.verticalGradient(listOf(Color(0xEB0F1E24), Color(0xEB081115))), RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) UiCyan else UiLine, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                saveState.displayName(),
                color = UiText,
                fontSize = 21.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "最后游玩 ${formatTimestamp(saveState.updatedAt)}",
                color = UiMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HallActionButton("复制", focused = false, compact = true, onClick = onCopy)
            HallActionButton("删除", focused = false, compact = true, onClick = onDelete)
        }
    }
}

private fun SaveStateEntity.displayName(): String {
    return when (slotType) {
        "auto" -> "自动存档"
        else -> "手动槽 ${slotIndex ?: 1}"
    }
}

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "未游玩"
    return SimpleDateFormat("M/d HH:mm", Locale.CHINA).format(Date(timestamp))
}

@Composable
private fun CoverArt(game: LocalGame, focused: Boolean, coverReloadTick: Long, modifier: Modifier = Modifier) {
    val palette = remember(game.id) { gamePalette(game) }
    val coverBitmap = rememberCoverBitmap(game, coverReloadTick).value
    Box(
        modifier = modifier
            .background(Color(0xFF15262B), RoundedCornerShape(14.dp))
            .border(if (focused) 3.dp else 0.dp, if (focused) UiCyan else Color.Transparent, RoundedCornerShape(14.dp))
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(palette.first, palette.second)))
                drawCircle(Color.White.copy(alpha = 0.34f), radius = size.minDimension * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width * 0.76f, size.height * 0.22f))
                drawCircle(Color.White.copy(alpha = 0.14f), radius = size.minDimension * 0.22f, center = androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.42f))
                rotate(-8f, pivot = androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.42f)) {
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.08f))),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.11f, size.height * 0.16f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.36f, size.height * 0.50f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    )
                }
                rotate(-18f, pivot = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.70f)) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.25f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.50f, size.height * 0.68f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.42f, size.height * 0.14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(999.dp.toPx(), 999.dp.toPx()),
                    )
                    drawCircle(Color.White.copy(alpha = 0.13f), radius = 18.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.54f))
                    drawCircle(Color.White.copy(alpha = 0.10f), radius = 15.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.23f, size.height * 0.67f))
                }
            },
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun rememberCoverBitmap(game: LocalGame, reloadTick: Long): State<ImageBitmap?> {
    val context = LocalContext.current
    val downloader = remember { RetroHallDependencies.get(context).coverDownloader }
    val path = game.coverPath
    return produceState<ImageBitmap?>(initialValue = null, path, reloadTick) {
        value = if (path.isBlank()) {
            null
        } else if (!path.startsWith("http://", ignoreCase = true) && !path.startsWith("https://", ignoreCase = true)) {
            runCatching {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        } else {
            val localPath = downloader.prepareCover(game.id, path)
            if (localPath != path) {
                runCatching {
                    val file = File(localPath)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                }.getOrNull()
            } else {
                null
            }
        }
    }
}

@Composable
private fun HallActionButton(
    label: String,
    focused: Boolean = false,
    danger: Boolean = false,
    accent: Color? = null,
    icon: ImageVector? = null,
    iconSize: Dp? = null,
    compact: Boolean = false,
    fillWidth: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val foreground = when {
        danger -> Color(0xFFFFE3E3)
        focused -> Color(0xFF031112)
        accent != null -> Color(0xFF031112)
        else -> UiText
    }
    Box(
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(if (compact) 124.dp else 150.dp))
            .height(if (compact) 48.dp else 62.dp)
            .background(
                when {
                    danger -> Brush.linearGradient(listOf(Color(0xFFE5484D), Color(0xFFB91C1C)))
                    focused -> Brush.linearGradient(listOf(UiCyan, Color(0xFF60FFE8)))
                    accent != null -> Brush.linearGradient(listOf(accent, lerp(accent, Color.White, 0.35f)))
                    else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.03f)))
                },
                RoundedCornerShape(14.dp),
            )
            .border(
                if (focused) 3.dp else 1.dp,
                when {
                    danger -> Color(0xFFE5484D)
                    focused -> UiCyan
                    accent != null -> accent
                    else -> Color(0x33DAF1F4)
                },
                RoundedCornerShape(14.dp),
            )
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(iconSize ?: (if (compact) 22.dp else 26.dp)),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                color = foreground,
                fontSize = if (compact) 16.sp else 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String, showDivider: Boolean = true, compact: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (showDivider) {
                    drawLine(
                        color = Color(0x1FDAF1F4),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .padding(top = if (showDivider) if (compact) 9.dp else 16.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = UiMuted, fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.Bold)
        Text(value, color = UiText, fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SegmentedChoice(
    values: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    compact: Boolean = false,
    showSelected: Boolean = true,
    showContainer: Boolean = true,
) {
    Row(
        modifier = Modifier
            .background(if (showContainer) Color.White.copy(alpha = 0.08f) else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(if (compact) 4.dp else 5.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEachIndexed { index, value ->
            Box(
                modifier = Modifier
                    .height(if (compact) 30.dp else 38.dp)
                    .background(if (showSelected && index == selectedIndex) UiCyan else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onSelected(index) }
                    .padding(horizontal = if (compact) 10.dp else 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value,
                    color = if (showSelected && index == selectedIndex) Color(0xFF031112) else UiMuted,
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HallToggle(checked: Boolean, compact: Boolean = false, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .width(if (compact) 50.dp else 58.dp)
            .height(if (compact) 26.dp else 30.dp)
            .background(if (checked) UiCyan.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 18.dp else 22.dp)
                .background(Color.White, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun GameScreen(
    game: LocalGame,
    session: EmulatorSession,
    settings: UserSettings,
    launchNotice: String?,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pauseVisible by remember { mutableStateOf(false) }
    var message by remember(launchNotice) { mutableStateOf(launchNotice ?: "游戏运行中") }
    val virtualPadActive = settings.controlMode == ControlMode.VirtualPad && settings.virtualPadVisible

    AppBackground {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(2.dp, UiCyan, RoundedCornerShape(8.dp))
                    .background(Color.Black, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(game.title, color = UiText, fontSize = 40.sp, fontWeight = FontWeight.Black)
                    Text("模拟器状态：${session.state}", color = UiCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("画面比例：${aspectRatioLabel(settings.aspectRatio)}", color = Color(0xD9F2FBFC), fontSize = 20.sp)
                    Text("操作模式：${controlModeLabel(settings.controlMode)}", color = Color(0xD9F2FBFC), fontSize = 20.sp)
                    Text("虚拟按键：${if (virtualPadActive) "显示" else "隐藏"}", color = Color(0xD9F2FBFC), fontSize = 20.sp)
                    Text(message, color = Color(0xD9F2FBFC), fontSize = 20.sp)
                }

                if (virtualPadActive) {
                    Text(
                        text = "方向键    选择    开始    乙    甲",
                        color = Color.White.copy(alpha = settings.virtualPadOpacity),
                        fontSize = (16.sp.value * settings.virtualPadScale).sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                    )
                } else if (settings.controlMode == ControlMode.Gamepad) {
                    Text(
                        text = "手柄模式",
                        color = UiCyan.copy(alpha = 0.78f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                    )
                }

                if (pauseVisible) {
                    PauseMenu(
                        onResume = {
                            session.resume()
                            pauseVisible = false
                            message = "已继续游戏"
                        },
                        onSave = {
                            message = if (session.saveState(com.richard.retrohall.domain.save.SaveSlot.Manual(1))) {
                                "已保存到手动槽 1"
                            } else {
                                "保存失败"
                            }
                        },
                        onLoad = {
                            message = if (session.loadState(com.richard.retrohall.domain.save.SaveSlot.Manual(1))) {
                                "已读取手动槽 1"
                            } else {
                                "读档失败"
                            }
                        },
                        onReset = {
                            session.reset()
                            pauseVisible = false
                            message = "游戏已重置"
                        },
                        onOpenSettings = onOpenSettings,
                        onExit = onExit,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("菜单 暂停    返回 大厅", color = UiCyan, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        session.pause()
                        pauseVisible = true
                    }, shape = RoundedCornerShape(8.dp)) { Text("暂停菜单") }
                    OutlinedButton(onClick = onExit, shape = RoundedCornerShape(8.dp)) { Text("退出到大厅") }
                }
            }
        }
    }
}

@Composable
private fun PauseMenu(
    onResume: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onReset: () -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(320.dp)
            .background(Color(0xF20B171B), RoundedCornerShape(8.dp))
            .border(2.dp, UiCyan, RoundedCornerShape(8.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("暂停", color = UiText, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Button(onClick = onResume) { Text("继续游戏") }
            OutlinedButton(onClick = onSave) { Text("保存即时存档") }
            OutlinedButton(onClick = onLoad) { Text("读取即时存档") }
            OutlinedButton(onClick = onReset) { Text("重置游戏") }
            OutlinedButton(onClick = onOpenSettings) { Text("设置") }
            OutlinedButton(onClick = onExit) { Text("退出到大厅") }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: UserSettings,
    cacheManager: CacheManager,
    onCacheCleared: () -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AppShell(
        selectedNav = "设置",
        onSelectLibrary = onSelectLibrary,
        onSelectRecent = onSelectRecent,
        onSelectFavorites = onSelectFavorites,
        onOpenSettings = onOpenSettings,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item { SettingsVisualSection(settings, onUpdateSettings, Modifier.fillMaxWidth(), dense = false) }
            item { SettingsAudioSection(settings, onUpdateSettings, Modifier.fillMaxWidth(), dense = false) }
            item { SettingsControlSection(settings, onUpdateSettings, Modifier.fillMaxWidth(), dense = false) }
            item { SettingsGameSection(settings, onUpdateSettings, Modifier.fillMaxWidth(), dense = false) }
            item { SettingsSystemSection(cacheManager, onCacheCleared, Modifier.fillMaxWidth(), dense = false) }
        }
    }
}

@Composable
private fun SettingsVisualSection(
    settings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
) {
    SettingsSection(title = "画面", modifier = modifier, dense = dense) {
        SettingRow("画面比例", first = true, dense = dense) {
            SegmentedChoice(
                values = AspectRatio.entries.map { aspectRatioLabel(it) },
                selectedIndex = AspectRatio.entries.indexOf(settings.aspectRatio),
                onSelected = { index -> onUpdateSettings(settings.copy(aspectRatio = AspectRatio.entries[index])) },
                compact = dense,
                showSelected = false,
                showContainer = false,
            )
        }
        SettingRow("滤镜", dense = dense) {
            SegmentedChoice(
                values = listOf("像素", "平滑"),
                selectedIndex = if (settings.filterEnabled) 0 else 1,
                onSelected = { index -> onUpdateSettings(settings.copy(filterEnabled = index == 0)) },
                compact = dense,
            )
        }
    }
}

@Composable
private fun SettingsAudioSection(
    settings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
) {
    SettingsSection(title = "声音", modifier = modifier, dense = dense) {
        SettingRow("声音", first = true, dense = dense) {
            HallToggle(checked = settings.audioEnabled, compact = dense, onCheckedChange = { onUpdateSettings(settings.copy(audioEnabled = it)) })
        }
        SettingRow("音量", dense = dense) {
            HallSlider(value = settings.volume, compact = dense, onValueChange = { onUpdateSettings(settings.copy(volume = it)) })
        }
    }
}

@Composable
private fun SettingsControlSection(
    settings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
) {
    SettingsSection(title = "控制", modifier = modifier, dense = dense) {
        SettingRow("操作模式", first = true, dense = dense) {
            SegmentedChoice(
                values = ControlMode.entries.map { controlModeLabel(it) },
                selectedIndex = ControlMode.entries.indexOf(settings.controlMode),
                onSelected = { index ->
                    val mode = ControlMode.entries[index]
                    onUpdateSettings(settings.copy(controlMode = mode, virtualPadVisible = mode == ControlMode.VirtualPad))
                },
                compact = dense,
            )
        }
        SettingRow("虚拟按键", dense = dense) {
            HallToggle(checked = settings.virtualPadVisible, compact = dense, onCheckedChange = { onUpdateSettings(settings.copy(virtualPadVisible = it)) })
        }
        SettingRow("按键透明度", dense = dense) {
            HallSlider(value = settings.virtualPadOpacity, compact = dense, onValueChange = { onUpdateSettings(settings.copy(virtualPadOpacity = it)) })
        }
        SettingRow("按键大小", dense = dense) {
            HallSlider(value = settings.virtualPadScale, valueRange = 0.5f..2f, compact = dense, onValueChange = { onUpdateSettings(settings.copy(virtualPadScale = it)) })
        }
        SettingRow("手柄自动隐藏", dense = dense) {
            SegmentedChoice(
                values = listOf("隐藏虚拟键", "保留"),
                selectedIndex = if (settings.hideVirtualPadWhenGamepadConnected) 0 else 1,
                onSelected = { onUpdateSettings(settings.copy(hideVirtualPadWhenGamepadConnected = it == 0)) },
                compact = dense,
            )
        }
    }
}

@Composable
private fun SettingsGameSection(
    settings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
) {
    SettingsSection(title = "游戏", modifier = modifier, dense = dense) {
        SettingRow("自动存档", first = true, dense = dense) {
            HallToggle(checked = settings.autoSaveStateEnabled, compact = dense, onCheckedChange = { onUpdateSettings(settings.copy(autoSaveStateEnabled = it)) })
        }
    }
}

@Composable
private fun SettingsSystemSection(
    cacheManager: CacheManager,
    onCacheCleared: () -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
) {
    SettingsSection(title = "系统", modifier = modifier, dense = dense) {
        SettingRow("缓存", first = true, dense = dense) {
            var cacheSize by remember { mutableStateOf(0L) }
            var clearing by remember { mutableStateOf(false) }
            var showClearConfirm by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(cacheManager) {
                cacheSize = cacheManager.totalSize()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (clearing) "清理中..." else CacheManager.formatBytes(cacheSize),
                    color = if (clearing) UiMuted else UiText,
                    fontSize = if (dense) 14.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                )
                Box(
                    modifier = Modifier
                        .size(if (dense) 26.dp else 32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, UiLine, RoundedCornerShape(10.dp))
                        .clickable(enabled = !clearing && cacheSize > 0L) { showClearConfirm = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "清理缓存",
                        tint = if (cacheSize > 0L && !clearing) UiCyan else UiMuted,
                        modifier = Modifier.size(if (dense) 15.dp else 18.dp),
                    )
                }
            }

            if (showClearConfirm) {
                HallConfirmDialog(
                    title = "清理缓存",
                    message = "将删除封面和元数据缓存，已下载的游戏和存档不会受影响，重新打开游戏库会自动重新加载封面。确定清理吗？",
                    confirmLabel = "清理",
                    dismissLabel = "取消",
                    onConfirm = {
                        showClearConfirm = false
                        clearing = true
                        scope.launch {
                            cacheManager.clear()
                            cacheSize = cacheManager.totalSize()
                            clearing = false
                            onCacheCleared()
                        }
                    },
                    onDismiss = { showClearConfirm = false },
                )
            }
        }
    }
}

@Composable
private fun HallConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .shadow(24.dp, RoundedCornerShape(18.dp), ambientColor = UiCyan.copy(alpha = 0.30f), spotColor = UiCyan.copy(alpha = 0.30f))
                .background(Brush.verticalGradient(listOf(Color(0xEB0F1E24), Color(0xEB081115))), RoundedCornerShape(18.dp))
                .border(2.dp, UiCyan, RoundedCornerShape(18.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = UiText, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Text(
                message,
                color = UiText.copy(alpha = 0.72f),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HallDialogButton(label = dismissLabel, primary = false, onClick = onDismiss)
                HallDialogButton(label = confirmLabel, primary = true, onClick = onConfirm)
            }
        }
    }
}

@Composable
private fun HallDialogButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) UiCyan else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
    ) {
        Text(label, color = if (primary) Color(0xFF071013) else UiText, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SettingsSection(title: String, modifier: Modifier = Modifier, dense: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .shadow(20.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .background(Brush.verticalGradient(listOf(Color(0xEB0F1E24), Color(0xEB081115))), RoundedCornerShape(16.dp))
            .border(1.dp, UiLine, RoundedCornerShape(16.dp))
            .padding(if (dense) 16.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(title, color = UiText, fontSize = if (dense) 18.sp else 20.sp, lineHeight = if (dense) 18.sp else 20.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(if (dense) 10.dp else 14.dp))
        content()
    }
}

@Composable
private fun SettingRow(label: String, focused: Boolean = false, first: Boolean = false, dense: Boolean = false, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (dense) 38.dp else 54.dp)
            .background(if (focused) Color(0x1C35F1DD) else Color.Transparent, RoundedCornerShape(12.dp))
            .border(if (focused) 2.dp else 0.dp, if (focused) UiCyan else Color.Transparent, RoundedCornerShape(12.dp))
            .drawBehind {
                if (!first && !focused) {
                    drawLine(
                        color = Color(0x1FDAF1F4),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .padding(horizontal = if (focused) 12.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = UiText.copy(alpha = 0.84f), fontSize = if (dense) 15.sp else 16.sp, fontWeight = FontWeight.ExtraBold)
        content()
    }
}

@Composable
private fun HallSlider(value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float> = 0f..1f, compact: Boolean = false) {
    val sliderWidth = if (compact) 154.dp else 210.dp
    val thumbSize = if (compact) 18.dp else 24.dp
    val rawFraction = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val fraction = rawFraction.coerceIn(0f, 1f)

    fun updateFromX(x: Float, width: Float) {
        val nextFraction = (x / width).coerceIn(0f, 1f)
        val nextValue = valueRange.start + (valueRange.endInclusive - valueRange.start) * nextFraction
        onValueChange(nextValue)
    }

    BoxWithConstraints(
        modifier = Modifier
            .width(sliderWidth)
            .height(if (compact) 26.dp else 32.dp)
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = { offset -> updateFromX(offset.x, size.width.toFloat()) },
                    onDrag = { change, _ -> updateFromX(change.position.x, size.width.toFloat()) },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp)),
        )
        Box(
            modifier = Modifier
                .width(maxWidth * fraction)
                .height(8.dp)
                .background(UiCyan, RoundedCornerShape(999.dp)),
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - thumbSize) * fraction)
                .size(thumbSize)
                .background(Color.White, RoundedCornerShape(50)),
        )
    }
}

private fun aspectRatioLabel(ratio: AspectRatio): String = when (ratio) {
    AspectRatio.Original -> "原始"
    AspectRatio.FourThree -> "4:3"
    AspectRatio.Fullscreen -> "铺满"
}

private fun controlModeLabel(mode: ControlMode): String = when (mode) {
    ControlMode.VirtualPad -> "虚拟按键"
    ControlMode.Gamepad -> "手柄"
}

private fun isSearchRevealKey(key: Key): Boolean = key == Key.Menu ||
    key == Key.Search ||
    key == Key.ButtonSelect ||
    key == Key.ButtonMode

@Composable
private fun AppBackground(
    contentPadding: PaddingValues = PaddingValues(horizontal = 38.dp, vertical = 32.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(UiBg, Color(0xFF0A1216), Color(0xFF05090C))
                )
            )
            .drawBehind {
                val gap = 8.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    var y = 0f
                    while (y <= size.height) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.018f),
                            radius = 1.1.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y),
                        )
                        y += gap
                    }
                    x += gap
                }
            }
            .padding(contentPadding)
    ) {
        content()
    }
}
