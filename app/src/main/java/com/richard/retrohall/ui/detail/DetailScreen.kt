package com.richard.retrohall.ui.detail

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.richard.retrohall.domain.game.CoverImageLoader
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.save.SaveStateStore
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiPanel
import com.richard.retrohall.ui.UiPanelSoft
import com.richard.retrohall.ui.UiText
import com.richard.retrohall.ui.components.AppShell
import com.richard.retrohall.ui.components.CoverArt
import com.richard.retrohall.ui.components.CoverLoadingIndicator
import com.richard.retrohall.ui.components.HallActionButton
import com.richard.retrohall.ui.components.TopToast
import com.richard.retrohall.ui.formatPlayTime
import com.richard.retrohall.ui.formatTimestamp
import com.richard.retrohall.ui.gamePaletteForIndex
import com.richard.retrohall.ui.save.displayName
import java.io.File

@Composable
internal fun DetailScreen(
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
    val unsupportedRuntime = game.runtimeFamily.isNotBlank() && game.runtimeFamily != "libretro"
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
                                DetailInfoItem(
                                    "游戏格式",
                                    if (game.runtimeFamily.isBlank()) game.platform else game.runtimeFamily,
                                    valueSize = if (dense) 14.sp else 15.sp,
                                    modifier = Modifier.weight(1f),
                                )
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
                                    unsupportedRuntime -> "暂不支持"
                                    isDownloaded -> "开始游戏"
                                    busy -> "下载中"
                                    else -> "下载"
                                },
                                focused = true,
                                compact = detailActionCompact,
                                icon = when {
                                    unsupportedRuntime -> null
                                    isDownloaded -> Icons.Outlined.PlayArrow
                                    else -> Icons.Outlined.Download
                                },
                                iconSize = if (dense) 26.dp else 30.dp,
                                enabled = !busy && !unsupportedRuntime,
                                onClick = {
                                    if (unsupportedRuntime) return@HallActionButton
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
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        DetailLowerSection(
                            game = game,
                            dense = dense,
                            descSize = descSize,
                            descLineHeight = descLineHeight,
                            coverReloadTick = coverReloadTick,
                            coverImageLoader = coverImageLoader,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            onOpenScreenshot = { screenshotViewerIndex = it },
                        )
                    }
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
    maxWidth: Dp,
    maxHeight: Dp,
    onOpenScreenshot: (Int) -> Unit,
) {
    val screenshots = game.screenshots.ifEmpty { game.logos }
    val scrollState = rememberScrollState()
    val spacing = if (dense) 12.dp else 18.dp
    val singleShot = screenshots.size == 1
    var shotRatio by remember(game.id, screenshots.size, coverReloadTick) { mutableFloatStateOf(1f) }
    val minIntroWidth = maxWidth * 0.38f
    val shotWidth = if (singleShot) {
        (maxHeight * shotRatio).coerceAtMost(maxWidth - minIntroWidth - spacing)
    } else {
        Dp.Unspecified
    }
    val introWidth = if (singleShot) {
        (maxWidth - spacing - shotWidth).coerceAtLeast(minIntroWidth)
    } else {
        maxWidth / 2
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Box(
            modifier = Modifier
                .width(introWidth)
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
                    widthOverride = if (singleShot) shotWidth else null,
                    coverReloadTick = coverReloadTick,
                    coverImageLoader = coverImageLoader,
                    onAspectRatio = { ratio -> if (singleShot) shotRatio = ratio },
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
    widthOverride: Dp?,
    coverReloadTick: Long,
    coverImageLoader: CoverImageLoader,
    onAspectRatio: (Float) -> Unit,
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

    val loadedRatio = bitmap?.let { it.width.toFloat() / it.height.toFloat() }
    LaunchedEffect(bitmap) {
        if (loadedRatio != null) onAspectRatio(loadedRatio)
    }
    val heightRatio = loadedRatio ?: 1f
    val sizeModifier = if (widthOverride != null) Modifier.width(widthOverride) else Modifier.aspectRatio(heightRatio)

    Box(
        modifier = Modifier
            .then(sizeModifier)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CoverLoadingIndicator(size = 40.dp)
                    Spacer(Modifier.height(if (dense) 6.dp else 10.dp))
                    Text(
                        (index + 1).toString(),
                        color = UiText.copy(alpha = 0.6f),
                        fontSize = if (dense) 16.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
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
                CoverLoadingIndicator(size = 56.dp)
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