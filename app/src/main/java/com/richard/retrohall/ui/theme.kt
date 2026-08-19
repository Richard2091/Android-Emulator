package com.richard.retrohall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.settings.AspectRatio
import com.richard.retrohall.domain.settings.ControlMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal val UiBg = Color(0xFF071013)
internal val UiPanel = Color(0xFF0B171B)
internal val UiPanelSoft = Color(0xFF101D22)
internal val UiText = Color(0xFFF2FBFC)
internal val UiMuted = Color(0xFF8DA5AA)
internal val UiCyan = Color(0xFF35F1DD)
internal val UiBlue = Color(0xFF1AAEF0)
internal val UiGold = Color(0xFFFFE36E)
internal val UiLine = Color(0x3ADAF1F4)

internal enum class NavIcon {
    Library,
    Search,
    Recent,
    Star,
    Gear,
}

internal val NavIcon.vector: ImageVector
    get() = when (this) {
        NavIcon.Library -> Icons.Outlined.Apps
        NavIcon.Search -> Icons.Outlined.Search
        NavIcon.Recent -> Icons.Outlined.AccessTime
        NavIcon.Star -> Icons.Outlined.StarBorder
        NavIcon.Gear -> Icons.Outlined.Settings
    }

@Composable
internal fun HallIcon(icon: NavIcon, color: Color, modifier: Modifier = Modifier) {
    Icon(imageVector = icon.vector, contentDescription = null, tint = color, modifier = modifier)
}

@Composable
internal fun StarIcon(filled: Boolean, color: Color, modifier: Modifier = Modifier) {
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

internal fun gamePalette(game: LocalGame): Pair<Color, Color> {
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

internal fun gamePaletteForIndex(index: Int): Pair<Color, Color> {
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

internal fun formatPlayTime(totalPlayTimeMillis: Long): String {
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

internal fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024.0) "%.0f KB".format(kb, Locale.CHINA)
    else "%.1f MB".format(kb / 1024.0, Locale.CHINA)
}

internal fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "未游玩"
    return SimpleDateFormat("M/d HH:mm", Locale.CHINA).format(Date(timestamp))
}

internal val GameSpeedOptions = listOf(0.5f, 1f, 1.5f, 2f)

internal fun gameSpeedLabel(speed: Float): String = when (speed) {
    0.5f -> "0.5x"
    1f -> "1x"
    1.5f -> "1.5x"
    2f -> "2x"
    else -> "${speed}x"
}

internal fun aspectRatioLabel(ratio: AspectRatio): String = when (ratio) {
    AspectRatio.Original -> "原始"
    AspectRatio.FourThree -> "4:3"
    AspectRatio.SixteenNine -> "16:9"
    AspectRatio.Fullscreen -> "全屏"
}

internal fun controlModeLabel(mode: ControlMode): String = when (mode) {
    ControlMode.VirtualPad -> "虚拟按键"
    ControlMode.Gamepad -> "手柄"
}

internal fun isSearchRevealKey(key: Key): Boolean = key == Key.Menu ||
    key == Key.Search ||
    key == Key.ButtonSelect ||
    key == Key.ButtonMode

@Composable
internal fun AppBackground(
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