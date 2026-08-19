package com.richard.retrohall.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import com.richard.retrohall.data.bootstrap.AppBootstrapper
import com.richard.retrohall.data.game.GameRepository
import com.richard.retrohall.data.game.RomDownloadManager
import com.richard.retrohall.data.settings.UserSettingsStore
import com.richard.retrohall.RetroHallDependencies
import com.richard.retrohall.domain.game.CoverImageLoader
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.input.GameAction
import com.richard.retrohall.domain.save.SaveStateSlot
import com.richard.retrohall.domain.save.SaveStateStore
import com.richard.retrohall.domain.settings.AspectRatio
import com.richard.retrohall.domain.settings.CacheMaintenance
import com.richard.retrohall.domain.settings.ControlMode
import com.richard.retrohall.domain.settings.VirtualPadVisibility
import com.richard.retrohall.domain.settings.UserSettings
import com.richard.retrohall.emulator.EmulatorSession
import com.richard.retrohall.emulator.EmulatorSessionFactory
import com.richard.retrohall.emulator.EmulatorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
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
        appBootstrapper = dependencies.appBootstrapper,
        userSettingsStore = dependencies.userSettingsStore,
        saveStateStore = dependencies.saveStateStore,
        cacheMaintenance = dependencies.cacheMaintenance,
        coverImageLoader = dependencies.coverImageLoader,
    )
}

@Composable
private fun RetroHallAppContent(
    gameRepository: GameRepository,
    romDownloadManager: RomDownloadManager,
    emulatorSessionFactory: EmulatorSessionFactory,
    appBootstrapper: AppBootstrapper,
    userSettingsStore: UserSettingsStore,
    saveStateStore: SaveStateStore,
    cacheMaintenance: CacheMaintenance,
    coverImageLoader: CoverImageLoader,
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
                    DetailScreen(
                        game = latestGame,
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
                        isDownloaded = remember(latestGame, downloadVersion) { romDownloadManager.isDownloaded(latestGame) },
                        downloadedSizeText = remember(latestGame.id, downloadVersion) {
                            val size = romDownloadManager.localSize(latestGame.id)
                            if (size == null) "未下载" else formatFileSize(size)
                        },
                        onDelete = { includeSaves ->
                            scope.launch {
                                if (includeSaves) saveStateStore.deleteForGame(latestGame.id)
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
                                    val (playableGame, startedAt, launch) = withContext(Dispatchers.IO) {
                                        val playableGame = romDownloadManager.prepare(latestGame)
                                        val startedAt = System.currentTimeMillis()
                                        val launch = emulatorSessionFactory.createStartedSession(playableGame)
                                        Triple(playableGame, startedAt, launch)
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
                    onExit = {
                        current.session.saveSram()
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

private data class HallFilters(
    val query: String = "",
    val platform: String = "FC",
    val sort: GameSort = GameSort.Hotness,
    val downloadStatus: String = "全部",
) : java.io.Serializable

@Composable
private fun HallScreen(
    games: List<LocalGame>,
    romDownloadManager: RomDownloadManager,
    downloadVersion: Int,
    selectedSection: String,
    filters: HallFilters,
    onFilterChange: (HallFilters) -> Unit,
    libraryGridState: LazyGridState,
    recentGridState: LazyGridState,
    favoritesGridState: LazyGridState,
    coverReloadTick: Long,
    coverImageLoader: CoverImageLoader,
    onRefreshCovers: suspend () -> Unit,
    onSelectSection: (String) -> Unit,
    onOpenGame: (LocalGame) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: (LocalGame) -> Unit,
) {
    var searchVisible by remember { mutableStateOf(false) }
    var focusSearchOnReveal by remember { mutableStateOf(false) }
    val downloadStatusOptions = listOf("全部", "已下载", "未下载")
    var toolbarInteractionTick by remember { mutableStateOf(0L) }
    var searchFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val pageFocusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val searchPullThreshold = with(density) { 52.dp.toPx() }
    val gridTopPadding = 28.dp
    val toolbarHeight = 56.dp
    val toolbarGap = 12.dp
    val platformOptions = remember(games) {
        val all = games
            .mapNotNull { it.platform }
            .flatMap { it.split("/", "、", "，", ",", ";").map { p -> p.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("全部", "FC") + all.filter { it != "FC" && it != "全部" }
    }

    val downloadedIds = remember(romDownloadManager, games, downloadVersion) {
        games.filter { romDownloadManager.isDownloaded(it) }.map { it.id }.toSet()
    }
    val filtered = games.filter { game ->
        val sectionMatches = when (selectedSection) {
            "收藏" -> game.favorite
            "最近" -> game.lastPlayedAt != null
            else -> true
        }
        val platformMatches = filters.platform == "全部" ||
            game.platform.contains(filters.platform, ignoreCase = true) ||
            (filters.platform == "FC" && game.platform.contains("NES", ignoreCase = true))
        val statusMatches = when (filters.downloadStatus) {
            "已下载" -> game.id in downloadedIds
            "未下载" -> game.id !in downloadedIds
            else -> true
        }
        val queryMatches = filters.query.isBlank() ||
            game.title.contains(filters.query, ignoreCase = true) ||
            game.category.contains(filters.query, ignoreCase = true) ||
            game.platform.contains(filters.query, ignoreCase = true)
        sectionMatches && platformMatches && statusMatches && queryMatches
    }
    val gamesShown = when (filters.sort) {
        GameSort.Hotness -> filtered.sortedWith(compareByDescending<LocalGame> { it.hotness ?: Double.NEGATIVE_INFINITY })
        GameSort.Name -> filtered.sortedBy { it.title.lowercase() }
        GameSort.Recent -> filtered.sortedWith(compareByDescending<LocalGame> { it.lastPlayedAt ?: Long.MIN_VALUE })
        GameSort.PlayTime -> filtered.sortedByDescending { it.totalPlayTimeMillis }
    }
    val showSearch = searchVisible

    BackHandler(enabled = searchVisible || selectedSection != "游戏库") {
        when {
            searchVisible -> searchVisible = false
            selectedSection != "游戏库" -> onSelectSection("游戏库")
        }
    }

    LaunchedEffect(searchVisible, toolbarInteractionTick, searchFocused) {
        if (searchVisible && !searchFocused) {
            delay(5000)
            if (searchVisible && !searchFocused) {
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
                val toolbarProgress by animateFloatAsState(
                    targetValue = if (showSearch) 1f else 0f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    label = "toolbarProgress",
                )
                if (filtered.isEmpty()) {
                    var emptyPullDistance by remember { mutableStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(searchPullThreshold) {
                                detectVerticalDragGestures(
                                    onDragEnd = { emptyPullDistance = 0f },
                                    onDragCancel = { emptyPullDistance = 0f },
                                ) { _, dragAmount ->
                                    if (dragAmount > 0f) {
                                        emptyPullDistance += dragAmount
                                        if (emptyPullDistance >= searchPullThreshold) {
                                            focusSearchOnReveal = false
                                            if (!searchVisible) searchVisible = true
                                            toolbarInteractionTick++
                                        }
                                    } else {
                                        emptyPullDistance = 0f
                                        focusSearchOnReveal = false
                                        if (searchVisible) {
                                            searchVisible = false
                                            toolbarInteractionTick++
                                        }
                                    }
                                }
                            },
                    ) {
                        EmptyPanelFrame(
                            text = if (filters.query.isBlank()) "没有游戏" else "没有匹配的游戏",
                        )
                    }
                } else {
                    val gridState = when (selectedSection) {
                        "最近" -> recentGridState
                        "收藏" -> favoritesGridState
                        else -> libraryGridState
                    }
                    var listAtTop by remember(gridState) { mutableStateOf(true) }
                    val spaceProgress by animateFloatAsState(
                        targetValue = if (showSearch || !listAtTop) 1f else 0f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                        label = "toolbarSpaceProgress",
                    )
                    LaunchedEffect(gridState) {
                        snapshotFlow {
                            gridState.firstVisibleItemIndex == 0 &&
                                gridState.firstVisibleItemScrollOffset <= 0
                        }.collect { isTop -> listAtTop = isTop }
                    }
                    var pullDistance by remember(selectedSection) { mutableStateOf(0f) }
                    val searchRevealScroll = remember(gridState, selectedSection, searchPullThreshold, filters) {
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
                                    val atTop = gridState.firstVisibleItemIndex == 0 &&
                                        gridState.firstVisibleItemScrollOffset <= 0
                                    if (atTop) {
                                        return Offset(0f, available.y)
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
                            contentPadding = PaddingValues(
                                top = gridTopPadding + (toolbarHeight + toolbarGap) * spaceProgress,
                                bottom = 28.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            items(gamesShown, key = { it.id }) { game ->
                                GameTile(
                                    game = game,
                                    coverReloadTick = coverReloadTick,
                                    coverImageLoader = coverImageLoader,
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
                        .offset(y = gridTopPadding - toolbarHeight * (1f - toolbarProgress))
                        .alpha(toolbarProgress)
                        .zIndex(2f),
                ) {
                    HallFilterBar(
                        query = filters.query,
                        onQueryChange = {
                            onFilterChange(filters.copy(query = it))
                            toolbarInteractionTick++
                        },
                        selectedStatus = filters.downloadStatus,
                        statusOptions = downloadStatusOptions,
                        onSelectStatus = {
                            onFilterChange(filters.copy(downloadStatus = it))
                            toolbarInteractionTick++
                        },
                        selectedPlatform = filters.platform,
                        platformOptions = platformOptions,
                        onSelectPlatform = { onFilterChange(filters.copy(platform = it)) },
                        selectedSort = filters.sort,
                        sortOptions = GameSort.entries,
                        onSelectSort = { onFilterChange(filters.copy(sort = it)) },
                        focusRequester = searchFocusRequester,
                        onInteraction = { toolbarInteractionTick++ },
                        onSearchFocusChange = { searchFocused = it },
                        toolbarVisible = showSearch,
                    )
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
    selectedStatus: String,
    statusOptions: List<String>,
    onSelectStatus: (String) -> Unit,
    selectedPlatform: String,
    platformOptions: List<String>,
    onSelectPlatform: (String) -> Unit,
    selectedSort: GameSort,
    sortOptions: List<GameSort>,
    onSelectSort: (GameSort) -> Unit,
    focusRequester: FocusRequester,
    onInteraction: () -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    toolbarVisible: Boolean,
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
                label = "状态",
                value = selectedStatus,
                options = statusOptions,
                onSelect = {
                    onSelectStatus(it)
                    onInteraction()
                },
                onInteraction = onInteraction,
                toolbarVisible = toolbarVisible,
            )
            ToolbarSelect(
                label = "类型",
                value = selectedPlatform,
                options = platformOptions,
                onSelect = {
                    onSelectPlatform(it)
                    onInteraction()
                },
                onInteraction = onInteraction,
                toolbarVisible = toolbarVisible,
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
                toolbarVisible = toolbarVisible,
            )
        }
        ToolbarDivider()
        SearchBox(
            query = query,
            onQueryChange = onQueryChange,
            focusRequester = focusRequester,
            onFocusChange = onSearchFocusChange,
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
    toolbarVisible: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(toolbarVisible) {
        if (!toolbarVisible) expanded = false
    }
    BackHandler(enabled = expanded) { expanded = false }
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
    onFocusChange: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0x2835F1DD) else Color(0x16FFFFFF), RoundedCornerShape(12.dp))
            .border(1.dp, if (focused) UiCyan else Color(0x1FDAF1F4), RoundedCornerShape(12.dp))
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
            },
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
private fun GameTile(
    game: LocalGame,
    coverReloadTick: Long,
    coverImageLoader: CoverImageLoader,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val palette = remember(game.id) { gamePalette(game) }
    val coverState = rememberCoverBitmap(game, coverReloadTick, coverImageLoader).value
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
        when (coverState) {
            is CoverState.Loaded -> Image(
                bitmap = coverState.bitmap,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            is CoverState.Loading -> CoverLoadingIndicator(size = 40.dp)
            is CoverState.Empty -> Unit
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
    saveStateStore: SaveStateStore,
    selectedSaveId: String?,
    coverReloadTick: Long,
    coverImageLoader: CoverImageLoader,
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
    var screenshotViewerIndex by remember(game.id) { mutableStateOf(-1) }
    val saveStates by saveStateStore.observeForGame(game.id).collectAsState(initial = emptyList())
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
            val lowerHalfWidth = maxWidth / 2

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
                        CoverArt(
                            game = game,
                            focused = false,
                            coverReloadTick = coverReloadTick,
                            coverImageLoader = coverImageLoader,
                            modifier = Modifier.fillMaxSize(),
                        )
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
                    DetailLowerSection(
                        game = game,
                        dense = dense,
                        descSize = descSize,
                        descLineHeight = descLineHeight,
                        coverReloadTick = coverReloadTick,
                        coverImageLoader = coverImageLoader,
                        halfWidth = lowerHalfWidth,
                        onOpenScreenshot = { screenshotViewerIndex = it },
                    )
                }
            }

            TopToast(detailMessage, onDismiss = { detailMessage = null })
        }
    }

    val viewerUrls = game.screenshots.ifEmpty { game.logos }
    if (screenshotViewerIndex in viewerUrls.indices) {
        ScreenshotViewer(
            urls = viewerUrls,
            index = screenshotViewerIndex,
            onIndexChange = { screenshotViewerIndex = it },
            coverImageLoader = coverImageLoader,
            onDismiss = { screenshotViewerIndex = -1 },
        )
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
private fun DetailLowerSection(
    game: LocalGame,
    dense: Boolean,
    descSize: TextUnit,
    descLineHeight: TextUnit,
    coverReloadTick: Long,
    coverImageLoader: CoverImageLoader,
    halfWidth: Dp,
    onOpenScreenshot: (Int) -> Unit,
) {
    val screenshots = game.screenshots.ifEmpty { game.logos }
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(if (dense) 12.dp else 18.dp),
    ) {
        Box(
            modifier = Modifier
                .width(halfWidth)
                .fillMaxHeight()
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

        if (screenshots.isEmpty()) {
            ScreenshotPlaceholder(dense = dense)
        } else {
            screenshots.forEachIndexed { index, url ->
                ScreenshotCard(
                    url = url,
                    index = index,
                    dense = dense,
                    coverReloadTick = coverReloadTick,
                    coverImageLoader = coverImageLoader,
                    onClick = { onOpenScreenshot(index) },
                )
            }
        }
    }
}

@Composable
private fun ScreenshotPlaceholder(dense: Boolean) {
    val palette = Color(0xFF122F66) to Color(0xFFFF7C45)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxHeight()
            .shadow(if (dense) 12.dp else 20.dp, RoundedCornerShape(16.dp), ambientColor = palette.first.copy(alpha = 0.4f), spotColor = palette.second.copy(alpha = 0.4f))
            .background(Brush.linearGradient(listOf(palette.first.copy(alpha = 0.9f), palette.second.copy(alpha = 0.82f))), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Photo,
                contentDescription = null,
                tint = UiText.copy(alpha = 0.6f),
                modifier = Modifier.size(if (dense) 36.dp else 56.dp),
            )
            Spacer(Modifier.height(if (dense) 8.dp else 14.dp))
            Text(
                "暂无截图",
                color = UiText.copy(alpha = 0.6f),
                fontSize = if (dense) 14.sp else 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ScreenshotCard(
    url: String,
    index: Int,
    dense: Boolean,
    coverReloadTick: Long,
    coverImageLoader: CoverImageLoader,
    onClick: () -> Unit,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url, coverReloadTick) {
        value = if (url.isBlank()) {
            null
        } else if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            runCatching {
                val file = File(url)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        } else {
            val localPath = coverImageLoader.prepareCover("shot-${index}-${url.hashCode().toUInt()}", url)
            if (localPath != url) {
                runCatching {
                    val file = File(localPath)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                }.getOrNull()
            } else {
                null
            }
        }
    }
    val palette = remember(index) { gamePaletteForIndex(index) }

    val heightRatio = bitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f
    Box(
        modifier = Modifier
            .aspectRatio(heightRatio)
            .fillMaxHeight()
            .shadow(if (dense) 12.dp else 20.dp, RoundedCornerShape(16.dp), ambientColor = palette.first.copy(alpha = 0.4f), spotColor = palette.second.copy(alpha = 0.4f))
            .background(Brush.linearGradient(listOf(palette.first.copy(alpha = 0.9f), palette.second.copy(alpha = 0.82f))), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (index + 1).toString(),
                    color = UiText.copy(alpha = 0.6f),
                    fontSize = if (dense) 28.sp else 44.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private fun gamePaletteForIndex(index: Int): Pair<Color, Color> {
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
    return colors[Math.floorMod(index, colors.size)]
}

@Composable
private fun ScreenshotViewer(
    urls: List<String>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    coverImageLoader: CoverImageLoader,
    onDismiss: () -> Unit,
) {
    val url = urls.getOrNull(index) ?: run { onDismiss(); return }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url, index) {
        value = runCatching {
            val localPath = coverImageLoader.prepareCover("viewer-$index-${url.hashCode().toUInt()}", url)
            if (localPath != url) {
                val file = File(localPath)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            } else {
                null
            }
        }.getOrNull()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 96.dp, vertical = 24.dp),
                )
            } else {
                Text("加载中…", color = UiMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (urls.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 18.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(enabled = index > 0) { if (index > 0) onIndexChange(index - 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = if (index > 0) UiText else UiMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(30.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(enabled = index < urls.size - 1) { onIndexChange(index + 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = if (index < urls.size - 1) UiText else UiMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(30.dp),
                    )
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
    saveStateStore: SaveStateStore,
    selectedSaveId: String?,
    onSelectSave: (SaveStateSlot) -> Unit,
    onBackToDetail: () -> Unit,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val saveStates by saveStateStore.observeForGame(game.id).collectAsState(initial = emptyList())

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
                        scope.launch { saveStateStore.addSlot(game.id) }
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
                            onCopy = { scope.launch { saveStateStore.copy(saveState.id) } },
                            onDelete = { scope.launch { saveStateStore.delete(saveState.id) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveStateRow(
    saveState: SaveStateSlot,
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

private fun SaveStateSlot.displayName(): String {
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
private fun CoverArt(
    game: LocalGame,
    focused: Boolean,
    coverReloadTick: Long,
    coverImageLoader: CoverImageLoader,
    modifier: Modifier = Modifier,
) {
    val palette = remember(game.id) { gamePalette(game) }
    val coverState = rememberCoverBitmap(game, coverReloadTick, coverImageLoader).value
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
        when (coverState) {
            is CoverState.Loaded -> Image(
                bitmap = coverState.bitmap,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            is CoverState.Loading -> CoverLoadingIndicator(size = 44.dp)
            is CoverState.Empty -> Unit
        }
    }
}

/** 封面加载状态：下载中 / 已加载 / 确认无封面。 */
private sealed interface CoverState {
    data object Loading : CoverState
    data class Loaded(val bitmap: ImageBitmap) : CoverState
    data object Empty : CoverState
}

@Composable
private fun rememberCoverBitmap(game: LocalGame, reloadTick: Long, coverImageLoader: CoverImageLoader): State<CoverState> {
    val path = game.coverPath
    return produceState<CoverState>(initialValue = CoverState.Loading, path, reloadTick) {
        value = decodeCoverState(path, coverImageLoader, game.id)
    }
}

/** 根据封面路径解码为加载状态。 */
private suspend fun decodeCoverState(path: String, coverImageLoader: CoverImageLoader, gameId: String): CoverState {
    if (path.isBlank()) return CoverState.Empty
    return if (!path.startsWith("http://", ignoreCase = true) && !path.startsWith("https://", ignoreCase = true)) {
        val file = File(path)
        if (file.exists()) {
            val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
            bitmap?.let { CoverState.Loaded(it) } ?: CoverState.Empty
        } else {
            CoverState.Empty
        }
    } else {
        val localPath = coverImageLoader.prepareCover(gameId, path)
        if (localPath != path) {
            val file = File(localPath)
            if (file.exists()) {
                val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
                bitmap?.let { CoverState.Loaded(it) } ?: CoverState.Empty
            } else {
                CoverState.Empty
            }
        } else {
            // 远程封面下载失败且无本地缓存。
            CoverState.Empty
        }
    }
}
@Composable
private fun CoverLoadingIndicator(size: Dp) {
    val transition = rememberInfiniteTransition(label = "coverLoading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coverAlpha",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = UiCyan.copy(alpha = alpha),
            strokeWidth = 3.dp,
        )
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
    onUpdateSettings: (UserSettings) -> Unit,
) {
    var paused by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var message by remember(launchNotice) { mutableStateOf(launchNotice) }
    val autoHideMode = settings.controlMode == ControlMode.VirtualPad &&
        settings.virtualPadVisibility == VirtualPadVisibility.AutoHide
    val showPadAlways = settings.controlMode == ControlMode.VirtualPad &&
        settings.virtualPadVisibility == VirtualPadVisibility.Visible
    var autoHidePadVisible by rememberSaveable { mutableStateOf(false) }
    var autoHideTicket by remember { mutableStateOf(0) }
    val padOpacity = settings.virtualPadOpacity.coerceIn(0.2f, 1f)
    val padScale = minOf(settings.virtualPadScale.coerceIn(0.6f, 1.6f), 1.15f)
    val scope = rememberCoroutineScope()

    val frame by session.frames.collectAsState()
    val frameAspectRatio = frame?.let {
        if (it.height > 0) {
            it.width.toFloat() / it.height.toFloat()
        } else {
            DefaultGameFrameAspectRatio
        }
    } ?: DefaultGameFrameAspectRatio

    LaunchedEffect(settings.gameSpeed) {
        session.setGameSpeed(settings.gameSpeed)
    }

    LaunchedEffect(settings.controlMode, settings.virtualPadVisibility) {
        autoHidePadVisible = showPadAlways
        autoHideTicket += 1
    }

    LaunchedEffect(autoHideTicket, settings.controlMode, settings.virtualPadVisibility, paused, settingsVisible) {
        if (!autoHideMode || !autoHidePadVisible || paused || settingsVisible) return@LaunchedEffect
        delay(3000)
        if (settings.controlMode == ControlMode.VirtualPad &&
            settings.virtualPadVisibility == VirtualPadVisibility.AutoHide &&
            !paused &&
            !settingsVisible
        ) {
            autoHidePadVisible = false
        }
    }

    // 瞬时按键（开始/选择）需要保持按下至少一帧，否则帧循环采样不到按下状态。
    fun tapKey(action: GameAction) {
        scope.launch {
            session.sendInput(action, true)
            delay(120)
            session.sendInput(action, false)
        }
    }

    fun revealVirtualPad() {
        if (!autoHideMode) return
        autoHidePadVisible = true
        autoHideTicket += 1
    }

    fun hideVirtualPad() {
        if (!autoHideMode) return
        autoHidePadVisible = false
        autoHideTicket += 1
    }

    fun registerVirtualPadInteraction() {
        if (!autoHideMode) return
        autoHidePadVisible = true
        autoHideTicket += 1
    }

    fun resumeGame() {
        session.resume()
        paused = false
        settingsVisible = false
    }

    fun pauseGame() {
        session.pause()
        paused = true
    }

    BackHandler {
        when {
            settingsVisible -> settingsVisible = false
            paused -> onExit()
            else -> pauseGame()
        }
    }

    AppBackground(contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val viewportWidth = gameViewportWidth(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    aspectRatio = settings.aspectRatio,
                    originalFrameRatio = frameAspectRatio,
                )
                Box(
                    modifier = Modifier
                        .width(viewportWidth)
                        .fillMaxHeight()
                        .background(Color.Black)
                        .border(1.dp, UiLine, RoundedCornerShape(6.dp))
                        .zIndex(0f),
                    contentAlignment = Alignment.Center,
                ) {
                    GameFrame(
                        title = game.title,
                        state = session.state,
                        frame = frame,
                        modifier = Modifier.fillMaxSize().testTag("game_frame"),
                    )
                    if (paused) {
                        PauseWatermark()
                    }
                }

                if (settings.controlMode == ControlMode.VirtualPad) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f),
                    ) {
                        if (autoHideMode && !paused && !settingsVisible) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(autoHidePadVisible) {
                                        detectTapGestures(onTap = {
                                            if (autoHidePadVisible) {
                                                hideVirtualPad()
                                            } else {
                                                revealVirtualPad()
                                            }
                                        })
                                    },
                            )
                        }

                        AnimatedVisibility(
                            visible = autoHidePadVisible,
                            enter = fadeIn() + slideInHorizontally { fullWidth -> fullWidth / 4 },
                            exit = fadeOut() + slideOutHorizontally { fullWidth -> fullWidth / 4 },
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(168.dp * padScale)
                                        .fillMaxHeight()
                                        .padding(start = 10.dp, top = 8.dp, bottom = 8.dp)
                                        .zIndex(2f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    AnimatedVisibility(
                                        visible = autoHidePadVisible,
                                        enter = fadeIn() + slideInHorizontally { fullWidth -> -fullWidth },
                                        exit = fadeOut() + slideOutHorizontally { fullWidth -> -fullWidth },
                                    ) {
                                        Box(modifier = Modifier.offset(y = 46.dp)) {
                                            JoyStickPad(
                                                opacity = padOpacity,
                                                onInteraction = ::registerVirtualPadInteraction,
                                                onDirs = { u, d, l, r ->
                                                    registerVirtualPadInteraction()
                                                    session.sendInput(GameAction.Up, u)
                                                    session.sendInput(GameAction.Down, d)
                                                    session.sendInput(GameAction.Left, l)
                                                    session.sendInput(GameAction.Right, r)
                                                },
                                                modifier = Modifier.size(156.dp * padScale),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    AnimatedVisibility(
                                        visible = autoHidePadVisible,
                                        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / 2 },
                                        exit = fadeOut() + slideOutVertically { fullHeight -> fullHeight / 2 },
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            PillButton("设置", padOpacity, danger = false, onInteraction = ::registerVirtualPadInteraction) {
                                                pauseGame()
                                                settingsVisible = true
                                            }
                                            PillButton(if (paused) "继续" else "暂停", padOpacity, danger = false, onInteraction = ::registerVirtualPadInteraction) {
                                                if (paused) {
                                                    resumeGame()
                                                } else {
                                                    pauseGame()
                                                }
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(158.dp * padScale)
                                        .fillMaxHeight()
                                        .padding(end = 10.dp, top = 8.dp, bottom = 8.dp)
                                        .zIndex(2f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    AnimatedVisibility(
                                        visible = autoHidePadVisible,
                                        enter = fadeIn() + slideInHorizontally { fullWidth -> fullWidth },
                                        exit = fadeOut() + slideOutHorizontally { fullWidth -> fullWidth },
                                    ) {
                                        Box(modifier = Modifier.offset(y = 44.dp)) {
                                            AbxyPad(
                                                opacity = padOpacity,
                                                onInteraction = ::registerVirtualPadInteraction,
                                                onAction = { a, p ->
                                                    registerVirtualPadInteraction()
                                                    session.sendInput(a, p)
                                                },
                                                modifier = Modifier.size(150.dp * padScale),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    AnimatedVisibility(
                                        visible = autoHidePadVisible,
                                        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / 2 },
                                        exit = fadeOut() + slideOutVertically { fullHeight -> fullHeight / 2 },
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            PillButton("选择", padOpacity, danger = false, onInteraction = ::registerVirtualPadInteraction) {
                                                tapKey(GameAction.Select)
                                            }
                                            PillButton("开始", padOpacity, danger = false, onInteraction = ::registerVirtualPadInteraction) {
                                                tapKey(GameAction.Start)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            TopToast(message, onDismiss = { message = null })
        }
    }

    if (settingsVisible) {
        GameSettingsOverlay(
            settings = settings,
            onContinue = { resumeGame() },
            onVirtualPadVisibilityChange = { next ->
                onUpdateSettings(settings.copy(virtualPadVisibility = next))
            },
            onAspectRatioChange = { next ->
                onUpdateSettings(settings.copy(aspectRatio = next))
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
                resumeGame()
                message = "游戏已重置"
            },
            onGameSpeedChange = { speed ->
                onUpdateSettings(settings.copy(gameSpeed = speed))
            },
            onExit = onExit,
            onDismiss = { settingsVisible = false },
        )
    }
}

@Composable
private fun GameFrame(title: String, state: EmulatorState, frame: Bitmap?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.testTag("game_frame")) {
        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "游戏画面",
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                Text(
                    title, color = UiText.copy(alpha = 0.92f), fontSize = 30.sp,
                    fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(stateLabel(state), color = UiCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun stateLabel(state: EmulatorState): String = when (state) {
    EmulatorState.Idle -> "空闲"
    EmulatorState.Loaded -> "已载入"
    EmulatorState.Running -> "运行中"
    EmulatorState.Paused -> "已暂停"
    EmulatorState.Stopped -> "已停止"
    EmulatorState.Error -> "出错"
}

@Composable
private fun PauseWatermark() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "已暂停",
            color = Color(0x52F2FBFC),
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 12.sp,
            modifier = Modifier.rotate(-8f),
        )
    }
}

@Composable
private fun JoyStickPad(
    onDirs: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit,
    onInteraction: () -> Unit = {},
    modifier: Modifier = Modifier,
    opacity: Float = 0.7f,
) {
    var up by remember { mutableStateOf(false) }
    var down by remember { mutableStateOf(false) }
    var left by remember { mutableStateOf(false) }
    var right by remember { mutableStateOf(false) }
    var stickX by remember { mutableStateOf(0f) }
    var stickY by remember { mutableStateOf(0f) }

    fun setDirs(u: Boolean, d: Boolean, l: Boolean, r: Boolean) {
        if (u != up) { up = u; onDirs(u, down, left, right) }
        if (d != down) { down = d; onDirs(up, d, left, right) }
        if (l != left) { left = l; onDirs(up, down, l, right) }
        if (r != right) { right = r; onDirs(up, down, left, r) }
    }

    fun apply(px: Float, py: Float, sizePx: Float) {
        val c = sizePx / 2f
        var nx = px - c
        var ny = py - c
        val max = c - sizePx * 0.1667f
        val len = hypot(nx, ny)
        if (len > max) {
            nx = nx / len * max
            ny = ny / len * max
        }
        stickX = nx
        stickY = ny
        val dirs = BooleanArray(4)
        if (len >= 6f) {
            val oct = Math.round(atan2(ny.toDouble(), nx.toDouble()) * 180.0 / PI / 45.0).toInt()
            when (oct) {
                0 -> dirs[3] = true
                1 -> { dirs[3] = true; dirs[1] = true }
                2 -> dirs[1] = true
                3 -> { dirs[1] = true; dirs[2] = true }
                4, -4 -> dirs[2] = true
                -3 -> { dirs[2] = true; dirs[0] = true }
                -2 -> dirs[0] = true
                -1 -> { dirs[0] = true; dirs[3] = true }
            }
        }
        setDirs(dirs[0], dirs[1], dirs[2], dirs[3])
    }

    Box(
        modifier = modifier
            .alpha(opacity)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onInteraction()
                    apply(down.position.x, down.position.y, size.width.toFloat())
                    while (true) {
                        val event = awaitPointerEvent()
                        val p = event.changes.firstOrNull { it.pressed } ?: break
                        apply(p.position.x, p.position.y, size.width.toFloat())
                        if (event.changes.none { it.pressed }) break
                        event.changes.forEach { if (!it.pressed) it.consume() }
                    }
                    stickX = 0f
                    stickY = 0f
                    setDirs(false, false, false, false)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = size.minDimension / 2f
            val center = Offset(c, c)
            // 底座投影 drop-shadow(0 6px 14px rgba(0,0,0,0.5))
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = Color.Black.copy(alpha = 0.5f).toArgb()
                    maskFilter = BlurMaskFilter(7.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawCircle(center.x, center.y + 6.dp.toPx(), c, paint)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF22343B), Color(0xFF0B1215)),
                    center = Offset(c, c * 0.84f),
                    radius = c * 1.5f,
                ),
                radius = c,
                center = center,
            )
            drawCircle(color = Color(0x40DAF1F4), radius = c, center = center, style = Stroke(1.5f))
            val zoneR = c * 0.94f
            drawZone(up, center, zoneR, -135f, 90f)
            drawZone(right, center, zoneR, -45f, 90f)
            drawZone(down, center, zoneR, 45f, 90f)
            drawZone(left, center, zoneR, 135f, 90f)
            drawCircle(
                color = UiCyan.copy(alpha = 0.16f),
                radius = c * 0.625f,
                center = center,
                style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.7f, 4.7f))),
            )
            val headR = c * 0.359f
            val headCenter = center + Offset(stickX, stickY)
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = headR,
                center = headCenter + Offset(0f, headR * 0.21f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF2E454D),
                        0.74f to Color(0xFF0F191D),
                        1f to Color(0xFF0F191D),
                    ),
                    center = headCenter - Offset(headR * 0.32f, headR * 0.40f),
                    radius = headR * 1.41f,
                ),
                radius = headR,
                center = headCenter,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.38f),
                    ),
                    center = headCenter - Offset(0f, headR * 0.5f),
                    radius = headR * 1.2f,
                ),
                radius = headR,
                center = headCenter,
            )
            drawCircle(color = UiCyan, radius = headR, center = headCenter, style = Stroke(2f))
        }
    }
}

private fun DrawScope.drawZone(active: Boolean, center: Offset, radius: Float, startDeg: Float, sweep: Float) {
    if (!active) return
    val innerRadius = radius * 0.46f
    val outerRadius = radius
    val startRad = Math.toRadians(startDeg.toDouble())
    val endRad = Math.toRadians((startDeg + sweep).toDouble())
    val path = Path()
    path.moveTo(
        center.x + outerRadius * cos(startRad).toFloat(),
        center.y + outerRadius * sin(startRad).toFloat(),
    )
    path.arcTo(
        Rect(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius),
        startDeg,
        sweep,
        false,
    )
    path.lineTo(
        center.x + innerRadius * cos(endRad).toFloat(),
        center.y + innerRadius * sin(endRad).toFloat(),
    )
    path.arcTo(
        Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius),
        startDeg + sweep,
        -sweep,
        false,
    )
    path.close()
    val paint = Paint().apply {
        shader = RadialGradient(
            center.x, center.y,
            outerRadius * 1.05f,
            intArrayOf(
                UiCyan.copy(alpha = 0f).toArgb(),
                UiCyan.copy(alpha = 0f).toArgb(),
                UiCyan.copy(alpha = 0.36f).toArgb(),
                UiCyan.copy(alpha = 0.58f).toArgb(),
                UiCyan.copy(alpha = 0.70f).toArgb(),
            ),
            floatArrayOf(0f, 0.34f, 0.58f, 0.82f, 1f),
            Shader.TileMode.CLAMP,
        )
        maskFilter = BlurMaskFilter(2.4.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

@Composable
private fun AbxyPad(
    onAction: (GameAction, Boolean) -> Unit,
    onInteraction: () -> Unit = {},
    modifier: Modifier = Modifier,
    opacity: Float = 0.7f,
) {
    Box(modifier = modifier.alpha(opacity)) {
        RoundKey(
            label = "X", action = GameAction.NesA, color = UiBlue,
            onAction = onAction,
            onInteraction = onInteraction,
            modifier = Modifier.align(Alignment.TopCenter).size(56.dp),
        )
        RoundKey(
            label = "Y", action = GameAction.NesB, color = Color(0xFFC8B6FF),
            onAction = onAction,
            onInteraction = onInteraction,
            modifier = Modifier.align(Alignment.CenterStart).size(56.dp),
        )
        RoundKey(
            label = "B", action = GameAction.NesB, color = UiGold,
            onAction = onAction,
            onInteraction = onInteraction,
            modifier = Modifier.align(Alignment.CenterEnd).size(56.dp),
        )
        RoundKey(
            label = "A", action = GameAction.NesA, color = UiCyan,
            onAction = onAction,
            onInteraction = onInteraction,
            modifier = Modifier.align(Alignment.BottomCenter).size(56.dp),
        )
    }
}

@Composable
private fun RoundKey(
    label: String,
    action: GameAction,
    color: Color,
    onAction: (GameAction, Boolean) -> Unit,
    onInteraction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (pressed) 0.90f else 1f
                scaleY = if (pressed) 0.90f else 1f
            }
            .shadow(
                elevation = if (pressed) 2.dp else 10.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = if (pressed) 0.30f else 0.55f),
                spotColor = Color.Black.copy(alpha = if (pressed) 0.30f else 0.50f),
            )
            .clip(CircleShape)
            .drawBehind {
                val r = size.minDimension / 2f
                val c = Offset(r, r)
                if (pressed) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(color.copy(alpha = 0.50f), Color(0xFF0A1215)),
                            center = c,
                            radius = r * 1.2f,
                        ),
                        radius = r,
                        center = c,
                    )
                    drawCircle(color = Color.Black.copy(alpha = 0.32f), radius = r * 0.72f, center = c)
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.42f),
                                color.copy(alpha = 0.55f),
                                color.copy(alpha = 0.20f),
                                Color(0xFF0C1417),
                            ),
                            center = Offset(r * 0.62f, r * 0.60f),
                            radius = r * 1.7f,
                        ),
                        radius = r,
                        center = c,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)),
                            center = c - Offset(0f, r * 0.4f),
                            radius = r * 1.3f,
                        ),
                        radius = r,
                        center = c,
                    )
                }
            }
            .border(2.dp, color, CircleShape)
            .pointerInput(action) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onInteraction()
                    pressed = true
                    onAction(action, true)
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                        event.changes.forEach { if (!it.pressed) it.consume() }
                    }
                    pressed = false
                    onAction(action, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (pressed) Color(0xFF0A1215) else UiText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = if (pressed) 0.9f else 1f
                scaleY = if (pressed) 0.9f else 1f
            },
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    opacity: Float,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onInteraction: () -> Unit = {},
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .alpha(opacity)
            .clip(RoundedCornerShape(999.dp))
            .background(if (pressed) UiCyan.copy(alpha = 0.25f) else Color(0xE6101D22))
            .border(1.5.dp, if (pressed) UiCyan else UiLine, RoundedCornerShape(999.dp))
            .clickable(interactionSource = interaction, indication = null) {
                onInteraction()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (danger) Color(0xFFFF8A6B) else UiText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GameSettingsOverlay(
    settings: UserSettings,
    onContinue: () -> Unit,
    onVirtualPadVisibilityChange: (VirtualPadVisibility) -> Unit,
    onAspectRatioChange: (AspectRatio) -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onReset: () -> Unit,
    onGameSpeedChange: (Float) -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x8C031012))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
            .fillMaxWidth(0.41f)
            .widthIn(min = 300.dp)
            .fillMaxHeight(0.9f)
                .background(Color(0xF70B171B), RoundedCornerShape(16.dp))
                .border(2.dp, UiCyan, RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { }
                },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("设置", color = UiText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                GamePanelButton("继续游戏", primary = true) { onContinue() }
                HorizontalDivider(color = UiLine)
                GameSettingRow("画面比例") {
                    SegmentedChoice(
                        values = AspectRatio.entries.map { aspectRatioLabel(it) },
                        selectedIndex = AspectRatio.entries.indexOf(settings.aspectRatio),
                        onSelected = { index -> onAspectRatioChange(AspectRatio.entries[index]) },
                    )
                }
                GameSettingRow("游戏速度") {
                    SegmentedChoice(
                        values = GameSpeedOptions.map { gameSpeedLabel(it) },
                        selectedIndex = GameSpeedOptions.indexOfFirst { it == settings.gameSpeed }.coerceAtLeast(0),
                        onSelected = { index -> onGameSpeedChange(GameSpeedOptions[index]) },
                    )
                }
                GameSettingRow("虚拟按键") {
                    SegmentedChoice(
                        values = listOf("显示", "自动隐藏"),
                        selectedIndex = if (settings.virtualPadVisibility == VirtualPadVisibility.Visible) 0 else 1,
                        onSelected = { index ->
                            onVirtualPadVisibilityChange(
                                if (index == 0) VirtualPadVisibility.Visible else VirtualPadVisibility.AutoHide,
                            )
                        },
                    )
                }
                GameSettingRow("即时存档") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameSegButton("保存") { onSave() }
                        GameSegButton("读取") { onLoad() }
                    }
                }
                GameSettingRow("重置游戏") {
                    GameSegButton("重置") { showResetConfirm = true }
                }
                HorizontalDivider(color = UiLine)
                GamePanelButton("退出游戏", primary = false, danger = true) { onExit() }
            }
        }
    }

    if (showResetConfirm) {
        HallConfirmDialog(
            title = "重置游戏",
            message = "将清除当前游戏进度并重新开始，确定重置吗？",
            confirmLabel = "重置",
            dismissLabel = "取消",
            onConfirm = {
                showResetConfirm = false
                onReset()
            },
            onDismiss = { showResetConfirm = false },
        )
    }
}

private fun gameViewportWidth(
    maxWidth: Dp,
    maxHeight: Dp,
    aspectRatio: AspectRatio,
    originalFrameRatio: Float,
): Dp = when (aspectRatio) {
    AspectRatio.Original -> minOf(maxWidth, maxHeight * originalFrameRatio)
    AspectRatio.FourThree -> minOf(maxWidth, maxHeight * (4f / 3f))
    AspectRatio.SixteenNine -> minOf(maxWidth, maxHeight * (16f / 9f))
    AspectRatio.Fullscreen -> maxWidth
}

private const val DefaultGameFrameAspectRatio = 4f / 3f

@Composable
private fun GameSettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = UiText.copy(alpha = 0.84f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun GameSegButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, UiLine),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
private fun GamePanelButton(text: String, primary: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    pressed -> UiCyan.copy(alpha = 0.5f)
                    primary -> UiCyan
                    else -> Color(0xE6101D22)
                },
            )
            .border(1.dp, if (primary || pressed) UiCyan else UiLine, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = when {
                primary -> Color(0xFF031112)
                danger -> Color(0xFFFF8A6B)
                else -> UiText
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingsScreen(
    settings: UserSettings,
    cacheMaintenance: CacheMaintenance,
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
            item { SettingsSystemSection(cacheMaintenance, onCacheCleared, Modifier.fillMaxWidth(), dense = false) }
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
                        onUpdateSettings(settings.copy(controlMode = mode))
                    },
                    compact = dense,
                )
            }
        SettingRow("虚拟按键", dense = dense) {
            SegmentedChoice(
                values = listOf("显示", "自动隐藏"),
                selectedIndex = if (settings.virtualPadVisibility == VirtualPadVisibility.Visible) 0 else 1,
                onSelected = { index ->
                    onUpdateSettings(
                        settings.copy(
                            virtualPadVisibility = if (index == 0) VirtualPadVisibility.Visible else VirtualPadVisibility.AutoHide,
                        ),
                    )
                },
                compact = dense,
            )
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
    cacheMaintenance: CacheMaintenance,
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

            LaunchedEffect(cacheMaintenance) {
                cacheSize = cacheMaintenance.totalSize()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (clearing) "清理中..." else cacheMaintenance.formatBytes(cacheSize),
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
                            cacheMaintenance.clear()
                            cacheSize = cacheMaintenance.totalSize()
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

private val GameSpeedOptions = listOf(0.5f, 1f, 1.5f, 2f)

private fun gameSpeedLabel(speed: Float): String = when (speed) {
    0.5f -> "0.5x"
    1f -> "1x"
    1.5f -> "1.5x"
    2f -> "2x"
    else -> "${speed}x"
}

private fun aspectRatioLabel(ratio: AspectRatio): String = when (ratio) {
    AspectRatio.Original -> "原始"
    AspectRatio.FourThree -> "4:3"
    AspectRatio.SixteenNine -> "16:9"
    AspectRatio.Fullscreen -> "全屏"
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
private fun TopToast(
    message: String?,
    onDismiss: () -> Unit,
) {
    if (message == null) return
    val visible = remember(message) { Animatable(0f) }
    LaunchedEffect(message) {
        visible.snapTo(0f)
        visible.animateTo(1f, animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
        delay(3000)
        onDismiss()
    }
    val slidePx = with(LocalDensity.current) { 72.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
            .graphicsLayer {
                translationY = -slidePx * (1f - visible.value)
                alpha = visible.value
            }
            .padding(top = 18.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = UiPanel,
            border = BorderStroke(1.dp, UiCyan),
            shadowElevation = 12.dp,
        ) {
            Text(
                message,
                color = UiText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

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
