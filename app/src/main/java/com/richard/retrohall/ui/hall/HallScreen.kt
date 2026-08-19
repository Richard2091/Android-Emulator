package com.richard.retrohall.ui.hall

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import com.richard.retrohall.data.game.RomDownloadManager
import com.richard.retrohall.data.game.ContentDownloadManager
import com.richard.retrohall.domain.game.CoverImageLoader
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.ui.HallIcon
import com.richard.retrohall.ui.NavIcon
import com.richard.retrohall.ui.StarIcon
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiGold
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiText
import com.richard.retrohall.ui.components.AppShell
import com.richard.retrohall.ui.components.CoverLoadingIndicator
import com.richard.retrohall.ui.components.CoverState
import com.richard.retrohall.ui.components.EmptyPanelFrame
import com.richard.retrohall.ui.components.rememberCoverBitmap
import com.richard.retrohall.ui.gamePalette
import com.richard.retrohall.ui.isSearchRevealKey
import kotlinx.coroutines.delay
import kotlin.math.min

internal enum class GameSort(val label: String) {
    Hotness("热度"),
    Name("名称"),
    Recent("最近游玩"),
    PlayTime("游戏时长"),
}

internal data class HallFilters(
    val query: String = "",
    val platform: String = "FC",
    val sort: GameSort = GameSort.Hotness,
    val downloadStatus: String = "全部",
) : java.io.Serializable

@Composable
internal fun HallScreen(
    games: List<LocalGame>,
    romDownloadManager: RomDownloadManager,
    contentDownloadManager: ContentDownloadManager,
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

    val downloadedIds = remember(romDownloadManager, contentDownloadManager, games, downloadVersion) {
        games.filter {
            contentDownloadManager.isDownloaded(it.id) || romDownloadManager.isDownloaded(it)
        }.map { it.id }.toSet()
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