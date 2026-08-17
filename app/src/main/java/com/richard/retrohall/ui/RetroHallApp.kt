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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richard.retrohall.RetroHallDependencies
import com.richard.retrohall.data.assets.FakeGameCatalog
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
    val categories = listOf("全部", "收藏", "最近游玩") + games.map { it.category }.distinct()
    var selectedCategory by remember { mutableStateOf("全部") }
    var query by remember { mutableStateOf("") }
    val filtered = games.filter { game ->
        val categoryMatches = when (selectedCategory) {
            "全部" -> true
            "收藏" -> game.favorite
            "最近游玩" -> game.lastPlayedAt != null
            else -> game.category == selectedCategory
        }
        categoryMatches && game.title.contains(query, ignoreCase = true)
    }

    AppBackground {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(184.dp)
                    .fillMaxSize()
                    .padding(end = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Retro Hall", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                categories.forEach { category ->
                    val selected = category == selectedCategory
                    Text(
                        text = category,
                        color = if (selected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                        fontSize = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) Color(0xFFBAE6FD) else Color.Transparent,
                                RoundedCornerShape(6.dp),
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text("搜索游戏") },
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                    )
                    OutlinedButton(onClick = onOpenSettings) {
                        Text("设置")
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("没有游戏", color = Color(0xFFCBD5E1), fontSize = 20.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
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

                Text("A 详情    B 返回    菜单 设置", color = Color(0xFFBAE6FD), fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun GameTile(game: LocalGame, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Column(
        modifier = Modifier
            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
            .background(Color(0xCC111827), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFF334155), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(game.platform, color = Color(0xFFE0F2FE), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(game.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(game.category, color = Color(0xFFCBD5E1), fontSize = 13.sp)
            Text(
                text = if (game.favorite) "★" else "☆",
                color = Color(0xFFFACC15),
                fontSize = 18.sp,
                modifier = Modifier.clickable(onClick = onToggleFavorite),
            )
        }
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
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(320.dp)
                        .background(Color(0xFF334155), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(game.platform, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(game.title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text("平台：${game.platform}", color = Color(0xFFCBD5E1), fontSize = 18.sp)
                    Text("分类：${game.category}", color = Color(0xFFCBD5E1), fontSize = 18.sp)
                    Text("收藏：${if (game.favorite) "是" else "否"}", color = Color(0xFFCBD5E1), fontSize = 18.sp)
                    Text("最近游玩：${game.lastPlayedAt?.toString() ?: "暂无"}", color = Color(0xFFCBD5E1), fontSize = 18.sp)
                    Text("累计游玩：${game.totalPlayTimeMillis / 60000} 分钟", color = Color(0xFFCBD5E1), fontSize = 18.sp)
                    if (message != null) {
                        Text(message, color = Color(0xFFBAE6FD), fontSize = 16.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onStart) { Text("开始游戏") }
                        OutlinedButton(onClick = onToggleFavorite) { Text(if (game.favorite) "取消收藏" else "收藏") }
                        OutlinedButton(onClick = onBack) { Text("返回") }
                    }
                }
            }
            Text("A 开始    B 返回    菜单 设置", color = Color(0xFFBAE6FD), fontSize = 16.sp)
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
                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                    .background(Color.Black, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(game.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("模拟器状态：${session.state}", color = Color(0xFFBAE6FD), fontSize = 18.sp)
                    Text("画面比例：${settings.aspectRatio}", color = Color(0xFFCBD5E1), fontSize = 16.sp)
                    Text("虚拟按键：${if (settings.virtualPadVisible) "显示" else "隐藏"}", color = Color(0xFFCBD5E1), fontSize = 16.sp)
                    Text(message, color = Color(0xFFCBD5E1), fontSize = 16.sp)
                }

                if (settings.virtualPadVisible) {
                    Text(
                        text = "方向键    Select    Start    B    A",
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
                Text("菜单 暂停    B 返回大厅", color = Color(0xFFBAE6FD), fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        session.pause()
                        pauseVisible = true
                    }) { Text("暂停菜单") }
                    OutlinedButton(onClick = onExit) { Text("退出到大厅") }
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
            .background(Color(0xEE0F172A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("暂停", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
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
                Text("设置", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
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
                Text("音量：${(settings.volume * 100).toInt()}%", color = Color(0xFFCBD5E1), fontSize = 18.sp)
                Slider(value = settings.volume, onValueChange = { onUpdateSettings(settings.copy(volume = it)) })
                Text("虚拟按键透明度：${(settings.virtualPadOpacity * 100).toInt()}%", color = Color(0xFFCBD5E1), fontSize = 18.sp)
                Slider(value = settings.virtualPadOpacity, onValueChange = { onUpdateSettings(settings.copy(virtualPadOpacity = it)) })
                Text("虚拟按键大小：${"%.1f".format(settings.virtualPadScale)}x", color = Color(0xFFCBD5E1), fontSize = 18.sp)
                Slider(
                    value = settings.virtualPadScale,
                    valueRange = 0.5f..2f,
                    onValueChange = { onUpdateSettings(settings.copy(virtualPadScale = it)) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AspectRatio.entries.forEach { ratio ->
                        OutlinedButton(onClick = { onUpdateSettings(settings.copy(aspectRatio = ratio)) }) {
                            Text(if (settings.aspectRatio == ratio) "✓ $ratio" else ratio.name)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("返回大厅") }
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
        Text(label, color = Color(0xFFCBD5E1), fontSize = 18.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF172554), Color(0xFF111827))
                )
            )
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        content()
    }
}
