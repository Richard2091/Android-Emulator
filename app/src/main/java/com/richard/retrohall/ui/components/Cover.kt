package com.richard.retrohall.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.richard.retrohall.domain.game.CoverImageLoader
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.gamePalette
import java.io.File

@Composable
internal fun CoverArt(
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
internal sealed interface CoverState {
    data object Loading : CoverState
    data class Loaded(val bitmap: ImageBitmap) : CoverState
    data object Empty : CoverState
}

@Composable
internal fun rememberCoverBitmap(game: LocalGame, reloadTick: Long, coverImageLoader: CoverImageLoader): State<CoverState> {
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