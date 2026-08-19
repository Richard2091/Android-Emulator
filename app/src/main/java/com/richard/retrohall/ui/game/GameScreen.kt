package com.richard.retrohall.ui.game

import android.graphics.Bitmap
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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.input.GameAction
import com.richard.retrohall.domain.save.SaveSlot
import com.richard.retrohall.domain.settings.AspectRatio
import com.richard.retrohall.domain.settings.ControlMode
import com.richard.retrohall.domain.settings.UserSettings
import com.richard.retrohall.domain.settings.VirtualPadVisibility
import com.richard.retrohall.emulator.EmulatorSession
import com.richard.retrohall.emulator.EmulatorState
import com.richard.retrohall.ui.AppBackground
import com.richard.retrohall.ui.GameSpeedOptions
import com.richard.retrohall.ui.UiBlue
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiGold
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiText
import com.richard.retrohall.ui.aspectRatioLabel
import com.richard.retrohall.ui.components.GamePanelButton
import com.richard.retrohall.ui.components.GameSegButton
import com.richard.retrohall.ui.components.HallConfirmDialog
import com.richard.retrohall.ui.components.PillButton
import com.richard.retrohall.ui.components.SegmentedChoice
import com.richard.retrohall.ui.components.TopToast
import com.richard.retrohall.ui.gameSpeedLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun GameScreen(
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
                            val padVisible by rememberUpdatedState(autoHidePadVisible)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            if (padVisible) {
                                                hideVirtualPad()
                                            } else {
                                                revealVirtualPad()
                                            }
                                        })
                                    },
                            )
                        }

                        if (autoHidePadVisible) {
                            VirtualPadOverlay(
                                padOpacity = padOpacity,
                                padScale = padScale,
                                paused = paused,
                                onRegisterInteraction = ::registerVirtualPadInteraction,
                                onOpenSettings = {
                                    pauseGame()
                                    settingsVisible = true
                                },
                                onTogglePause = {
                                    if (paused) {
                                        resumeGame()
                                        message = "已继续游戏"
                                    } else {
                                        pauseGame()
                                    }
                                },
                                onSelect = { tapKey(GameAction.Select) },
                                onStart = { tapKey(GameAction.Start) },
                                onDirs = { u, d, l, r ->
                                    registerVirtualPadInteraction()
                                    session.sendInput(GameAction.Up, u)
                                    session.sendInput(GameAction.Down, d)
                                    session.sendInput(GameAction.Left, l)
                                    session.sendInput(GameAction.Right, r)
                                },
                                onAction = { a, p ->
                                    registerVirtualPadInteraction()
                                    session.sendInput(a, p)
                                },
                            )
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
private fun VirtualPadOverlay(
    padOpacity: Float,
    padScale: Float,
    paused: Boolean,
    onRegisterInteraction: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePause: () -> Unit,
    onSelect: () -> Unit,
    onStart: () -> Unit,
    onDirs: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit,
    onAction: (GameAction, Boolean) -> Unit,
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
            PadSlideGroup(
                modifier = Modifier.offset(y = 46.dp),
                enterOffsetX = { -it },
                exitOffsetX = { -it },
            ) {
                JoyStickPad(
                    opacity = padOpacity,
                    onInteraction = onRegisterInteraction,
                    onDirs = onDirs,
                    modifier = Modifier
                        .size(156.dp * padScale)
                        .testTag("virtual_joy")
                        .semantics { contentDescription = "虚拟方向键" },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PadSlideGroup(
                enterOffsetY = { it / 2 },
                exitOffsetY = { it / 2 },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton("设置", padOpacity, danger = false, onInteraction = onRegisterInteraction, onClick = onOpenSettings)
                    PillButton(if (paused) "继续" else "暂停", padOpacity, danger = false, onInteraction = onRegisterInteraction, onClick = onTogglePause)
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
            PadSlideGroup(
                modifier = Modifier.offset(y = 44.dp),
                enterOffsetX = { it },
                exitOffsetX = { it },
            ) {
                AbxyPad(
                    opacity = padOpacity,
                    onInteraction = onRegisterInteraction,
                    onAction = onAction,
                    modifier = Modifier
                        .size(150.dp * padScale)
                        .testTag("virtual_abxy")
                        .semantics { contentDescription = "虚拟按键ABXY" },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PadSlideGroup(
                enterOffsetY = { it / 2 },
                exitOffsetY = { it / 2 },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton("选择", padOpacity, danger = false, onInteraction = onRegisterInteraction, onClick = onSelect)
                    PillButton("开始", padOpacity, danger = false, onInteraction = onRegisterInteraction, onClick = onStart)
                }
            }
        }
    }
}

@Composable
private fun PadSlideGroup(
    modifier: Modifier = Modifier,
    enterOffsetX: (Int) -> Int = { 0 },
    exitOffsetX: (Int) -> Int = { 0 },
    enterOffsetY: (Int) -> Int = { 0 },
    exitOffsetY: (Int) -> Int = { 0 },
    content: @Composable () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    AnimatedVisibility(
        visible = entered,
        enter = fadeIn(tween(180)) +
            slideInHorizontally(tween(240), initialOffsetX = enterOffsetX) +
            slideInVertically(tween(240), initialOffsetY = enterOffsetY),
        exit = fadeOut(tween(140)) +
            slideOutHorizontally(tween(160), targetOffsetX = exitOffsetX) +
            slideOutVertically(tween(160), targetOffsetY = exitOffsetY),
        modifier = modifier,
    ) {
        content()
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