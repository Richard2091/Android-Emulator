package com.richard.retrohall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richard.retrohall.RetroHallDependencies
import com.richard.retrohall.data.game.GameRepository
import com.richard.retrohall.data.game.RomDownloadManager
import com.richard.retrohall.data.assets.PrivateAssetInitializer
import com.richard.retrohall.data.settings.UserSettingsStore
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.settings.AspectRatio
import com.richard.retrohall.domain.settings.UserSettings
import com.richard.retrohall.emulator.EmulatorSession
import com.richard.retrohall.emulator.EmulatorSessionFactory
import com.richard.retrohall.emulator.EmulatorState
import kotlinx.coroutines.launch

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
    )
}

@Composable
private fun RetroHallAppContent(
    gameRepository: GameRepository,
    romDownloadManager: RomDownloadManager,
    emulatorSessionFactory: EmulatorSessionFactory,
    privateAssetInitializer: PrivateAssetInitializer?,
    userSettingsStore: UserSettingsStore,
) {
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf<AppRoute>(AppRoute.Hall) }
    var launchMessage by remember { mutableStateOf<String?>(null) }
    val games by gameRepository.games.collectAsState(initial = emptyList())
    val settings by userSettingsStore.settings.collectAsState(initial = UserSettings())

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
                    onOpenGame = {
                        launchMessage = null
                        route = AppRoute.Detail(it)
                    },
                    onOpenSettings = { route = AppRoute.Settings },
                    onToggleFavorite = { game ->
                        scope.launch { gameRepository.toggleFavorite(game) }
                    },
                )

                is AppRoute.Detail -> DetailScreen(
                    game = current.game,
                    message = launchMessage,
                    onBack = { route = AppRoute.Hall },
                    onToggleFavorite = {
                        scope.launch { gameRepository.toggleFavorite(current.game) }
                    },
                    onStart = {
                        launchMessage = "正在准备游戏..."
                        scope.launch {
                            val playableGame = runCatching { romDownloadManager.prepare(current.game) }
                                .getOrElse { error ->
                                    launchMessage = "准备游戏失败：${error.message ?: "未知错误"}"
                                    return@launch
                                }
                            val startedAt = System.currentTimeMillis()
                            val launch = emulatorSessionFactory.createStartedSession(playableGame)
                            launchMessage = launch.message
                            gameRepository.markPlayed(playableGame.id, startedAt)
                            route = AppRoute.Game(playableGame, launch.session, startedAt, launch.message)
                        }
                    },
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
                    onUpdateSettings = { next ->
                        scope.launch { userSettingsStore.update(next) }
                    },
                    onBack = { route = AppRoute.Hall },
                )
            }
        }
    }
}

@Composable
private fun HallScreen(
    games: List<LocalGame>,
    onOpenGame: (LocalGame) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: (LocalGame) -> Unit,
) {
    val sections = listOf("游戏库", "最近", "收藏")
    var selectedSection by remember { mutableStateOf("游戏库") }
    var query by remember { mutableStateOf("") }
    val filtered = games.filter { game ->
        val sectionMatches = when (selectedSection) {
            "收藏" -> game.favorite
            "最近" -> game.lastPlayedAt != null
            else -> true
        }
        val queryMatches = query.isBlank() ||
            game.title.contains(query, ignoreCase = true) ||
            game.category.contains(query, ignoreCase = true) ||
            game.platform.contains(query, ignoreCase = true)
        sectionMatches && queryMatches
    }

    AppBackground {
        Row(modifier = Modifier.fillMaxSize()) {
            HallSidebar(
                selectedSection = selectedSection,
                sections = sections,
                onSelectSection = { selectedSection = it },
                onOpenSettings = onOpenSettings,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 36.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(selectedSection, color = UiText, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text("${filtered.size} 款游戏", color = UiMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))

                SearchBox(query = query, onQueryChange = { query = it })

                Spacer(Modifier.height(18.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("没有游戏", color = UiMuted, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(filtered, key = { it.id }) { game ->
                            GameTile(
                                game = game,
                                onClick = { onOpenGame(game) },
                                onToggleFavorite = { onToggleFavorite(game) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("确认 详情    返回 大厅    菜单 设置", color = UiCyan, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun HallSidebar(
    selectedSection: String,
    sections: List<String>,
    onSelectSection: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(230.dp)
            .fillMaxSize()
            .border(1.dp, UiLine, RoundedCornerShape(0.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF2071115), Color(0xFA040A0D)),
                ),
            )
            .padding(horizontal = 22.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("游戏大厅", color = UiText, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("FC / NES 经典游戏", color = UiCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        sections.forEach { section ->
            SidebarItem(
                icon = when (section) {
                    "最近" -> "◷"
                    "收藏" -> "☆"
                    else -> "▦"
                },
                label = section,
                selected = selectedSection == section,
                onClick = { onSelectSection(section) },
            )
        }
        SidebarItem(icon = "⚙", label = "设置", selected = false, onClick = onOpenSettings)
    }
}

@Composable
private fun SidebarItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                if (selected) Color(0x3323F1DD) else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) UiCyan else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(icon, color = if (selected) UiText else UiMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, color = if (selected) UiText else Color(0xD1F2FBFC), fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        leadingIcon = { Text("⌕", color = UiText, fontSize = 26.sp, fontWeight = FontWeight.Bold) },
        placeholder = { Text("搜索游戏", color = UiMuted, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = UiText,
            unfocusedTextColor = UiText,
            cursorColor = UiCyan,
            focusedBorderColor = UiCyan,
            unfocusedBorderColor = UiLine,
            focusedContainerColor = Color(0x660B171B),
            unfocusedContainerColor = Color(0x660B171B),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
    )
}

@Composable
private fun GameTile(game: LocalGame, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val palette = remember(game.id) {
        val colors = listOf(
            Color(0xFF253D7A) to Color(0xFFE16A45),
            Color(0xFF105B55) to Color(0xFF26D1B8),
            Color(0xFF6A376A) to Color(0xFFE4A052),
            Color(0xFF0D315E) to Color(0xFF5A8EE6),
            Color(0xFF215D62) to Color(0xFFF0C844),
            Color(0xFF205D35) to Color(0xFFCBEF63),
            Color(0xFF0C4E69) to Color(0xFF2BC2EA),
            Color(0xFF5F2D67) to Color(0xFFD86282),
        )
        colors[(game.id.hashCode() and Int.MAX_VALUE) % colors.size]
    }
    Box(
        modifier = Modifier
            .height(112.dp)
            .border(2.dp, UiLine, RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(palette.first, palette.second)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(
            game.platform,
            color = Color(0x55F2FBFC),
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            text = if (game.favorite) "★" else "☆",
            color = if (game.favorite) UiGold else UiText,
            fontSize = 26.sp,
            modifier = Modifier.align(Alignment.TopEnd).clickable(onClick = onToggleFavorite),
        )
        Text(
            game.title,
            color = UiText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun DetailScreen(
    game: LocalGame,
    message: String?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStart: () -> Unit,
) {
    AppBackground {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(42.dp)) {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(420.dp)
                        .border(3.dp, UiCyan, RoundedCornerShape(8.dp))
                        .background(UiPanelSoft, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(game.platform, color = UiText, fontSize = 56.sp, fontWeight = FontWeight.Black)
                }
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(game.title, color = UiText, fontSize = 52.sp, fontWeight = FontWeight.Black)
                    Text("平台：${game.platform}", color = Color(0xD9F2FBFC), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("分类：${game.category}", color = Color(0xD9F2FBFC), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("收藏：${if (game.favorite) "是" else "否"}", color = Color(0xD9F2FBFC), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("最近游玩：${game.lastPlayedAt?.toString() ?: "暂无"}", color = Color(0xD9F2FBFC), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("累计游玩：${game.totalPlayTimeMillis / 60000} 分钟", color = Color(0xD9F2FBFC), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    if (message != null) {
                        Text(message, color = UiCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(containerColor = UiCyan, contentColor = UiBg),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text("开始游戏", fontWeight = FontWeight.Black) }
                        OutlinedButton(onClick = onToggleFavorite, shape = RoundedCornerShape(8.dp)) {
                            Text(if (game.favorite) "取消收藏" else "收藏")
                        }
                        OutlinedButton(onClick = onBack, shape = RoundedCornerShape(8.dp)) { Text("返回") }
                    }
                }
            }
            Text("确认 开始    返回 大厅    菜单 设置", color = UiCyan, fontSize = 16.sp)
        }
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
                    Text("虚拟按键：${if (settings.virtualPadVisible) "显示" else "隐藏"}", color = Color(0xD9F2FBFC), fontSize = 20.sp)
                    Text(message, color = Color(0xD9F2FBFC), fontSize = 20.sp)
                }

                if (settings.virtualPadVisible) {
                    Text(
                        text = "方向键    选择    开始    乙    甲",
                        color = Color.White.copy(alpha = settings.virtualPadOpacity),
                        fontSize = (16.sp.value * settings.virtualPadScale).sp,
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
    onUpdateSettings: (UserSettings) -> Unit,
    onBack: () -> Unit,
) {
    AppBackground {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("设置", color = UiText, fontSize = 42.sp, fontWeight = FontWeight.Black)
                SettingRow("滤镜", settings.filterEnabled) {
                    onUpdateSettings(settings.copy(filterEnabled = it))
                }
                SettingRow("声音", settings.audioEnabled) {
                    onUpdateSettings(settings.copy(audioEnabled = it))
                }
                SettingRow("虚拟按键", settings.virtualPadVisible) {
                    onUpdateSettings(settings.copy(virtualPadVisible = it))
                }
                SettingRow("连接手柄隐藏虚拟按键", settings.hideVirtualPadWhenGamepadConnected) {
                    onUpdateSettings(settings.copy(hideVirtualPadWhenGamepadConnected = it))
                }
                Text("音量：${(settings.volume * 100).toInt()}%", color = Color(0xD9F2FBFC), fontSize = 20.sp)
                Slider(value = settings.volume, onValueChange = { onUpdateSettings(settings.copy(volume = it)) })
                Text("虚拟按键透明度：${(settings.virtualPadOpacity * 100).toInt()}%", color = Color(0xD9F2FBFC), fontSize = 20.sp)
                Slider(value = settings.virtualPadOpacity, onValueChange = { onUpdateSettings(settings.copy(virtualPadOpacity = it)) })
                Text("虚拟按键大小：${"%.1f".format(settings.virtualPadScale)} 倍", color = Color(0xD9F2FBFC), fontSize = 20.sp)
                Slider(
                    value = settings.virtualPadScale,
                    valueRange = 0.5f..2f,
                    onValueChange = { onUpdateSettings(settings.copy(virtualPadScale = it)) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AspectRatio.entries.forEach { ratio ->
                        OutlinedButton(onClick = { onUpdateSettings(settings.copy(aspectRatio = ratio)) }, shape = RoundedCornerShape(8.dp)) {
                            Text(if (settings.aspectRatio == ratio) "已选 ${aspectRatioLabel(ratio)}" else aspectRatioLabel(ratio))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(8.dp)) { Text("返回大厅") }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color(0xD9F2FBFC), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun aspectRatioLabel(ratio: AspectRatio): String = when (ratio) {
    AspectRatio.Original -> "原始"
    AspectRatio.FourThree -> "四比三"
    AspectRatio.Fullscreen -> "铺满"
}

@Composable
private fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(UiBg, Color(0xFF0A1216), Color(0xFF05090C))
                )
            )
            .padding(horizontal = 38.dp, vertical = 32.dp)
    ) {
        content()
    }
}
