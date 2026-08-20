package com.richard.retrohall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.richard.retrohall.data.core.CoreCatalog
import com.richard.retrohall.data.core.CoreCatalogClient
import com.richard.retrohall.data.core.CoreDownloadManager
import com.richard.retrohall.data.core.CoreInfo
import com.richard.retrohall.data.core.CoreSelectionStore
import com.richard.retrohall.data.game.ResourceCatalogClient
import com.richard.retrohall.domain.game.CategoryCatalog
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiPanel
import com.richard.retrohall.ui.UiText
import kotlinx.coroutines.launch

private data class PlatformOption(
    val categoryId: String,
    val displayName: String,
    val platformId: String,
)

/**
 * 核心管理弹窗：平台选项读取游戏目录（catalog/index.v2.json）分类，
 * 每个平台的核心列表读取核心源（core-manifest.v1.json）。支持下载、选择、删除。
 */
@Composable
internal fun CoreManagerDialog(
    coreCatalogClient: CoreCatalogClient,
    coreDownloadManager: CoreDownloadManager,
    coreSelectionStore: CoreSelectionStore,
    resourceCatalogClient: ResourceCatalogClient,
    initialCategoryId: String? = null,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var coreCatalog by remember { mutableStateOf<CoreCatalog?>(null) }
    var gameIndex by remember { mutableStateOf<CategoryCatalog?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var busyCoreId by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val selections by coreSelectionStore.selections.collectAsState(initial = emptyMap())

    LaunchedEffect(coreCatalogClient, resourceCatalogClient) {
        coreCatalog = runCatching { coreCatalogClient.fetchCatalog() }.getOrNull()
        gameIndex = runCatching { resourceCatalogClient.fetchIndex() }.getOrNull()
        selectedCategoryId = initialCategoryId
            ?: buildPlatformOptions(gameIndex, coreCatalog).firstOrNull()?.categoryId
    }

    val platformOptions = remember(gameIndex, coreCatalog) { buildPlatformOptions(gameIndex, coreCatalog) }
    val activeOption = platformOptions.firstOrNull { it.categoryId == selectedCategoryId }
    val activePlatformId = activeOption?.platformId ?: platformOptions.firstOrNull()?.platformId.orEmpty()
    val platformCores = coreCatalog?.forPlatform(activePlatformId).orEmpty()
    val selectedCoreId = selections[activePlatformId]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.70f),
            shape = RoundedCornerShape(20.dp),
            color = UiPanel,
            border = androidx.compose.foundation.BorderStroke(1.dp, UiLine),
            shadowElevation = 24.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("核心管理", color = UiText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, UiLine, RoundedCornerShape(9.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = UiText, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (platformOptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        platformOptions.forEach { option ->
                            val selected = option.categoryId == selectedCategoryId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (selected) UiCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
                                    .border(if (selected) 1.dp else 0.dp, UiCyan, RoundedCornerShape(9.dp))
                                    .clickable { selectedCategoryId = option.categoryId }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    option.displayName,
                                    color = if (selected) UiCyan else UiText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                notice?.let { message ->
                    Text(message, color = UiMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                }

                when {
                    coreCatalog == null && gameIndex == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("清单加载失败，请检查数据源后重试。", color = UiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    platformCores.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("该平台暂无可用核心。", color = UiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(platformCores.size) { index ->
                                val core = platformCores[index]
                                CoreRow(
                                    core = core,
                                    downloadedAbis = coreDownloadManager.downloadedAbis(core),
                                    isSelected = core.id == selectedCoreId,
                                    busy = busyCoreId == core.id,
                                    onSelect = {
                                        scope.launch {
                                            coreSelectionStore.select(activePlatformId, core.id)
                                            notice = "已选择 ${core.displayName}"
                                        }
                                    },
                                    onDownload = {
                                        busyCoreId = core.id
                                        scope.launch {
                                            try {
                                                coreDownloadManager.download(core, coreDownloadManager.supportedAbis.first())
                                                notice = "${core.displayName} 下载完成"
                                            } catch (e: Exception) {
                                                notice = "下载失败：${e.message ?: "未知错误"}"
                                            } finally {
                                                busyCoreId = null
                                            }
                                        }
                                    },
                                    onDelete = {
                                        busyCoreId = core.id
                                        scope.launch {
                                            coreDownloadManager.delete(core)
                                            notice = "${core.displayName} 已删除"
                                            busyCoreId = null
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreRow(
    core: CoreInfo,
    downloadedAbis: List<String>,
    isSelected: Boolean,
    busy: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) UiCyan.copy(alpha = 0.14f) else Color(0x0DFFFFFF))
            .border(1.dp, if (isSelected) UiCyan else UiLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    core.displayName,
                    color = UiText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isSelected) {
                    Spacer(Modifier.size(5.dp))
                    Icon(Icons.Outlined.Check, contentDescription = "当前核心", tint = UiCyan, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "${core.version} · ${core.license}",
                color = UiMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (downloadedAbis.isEmpty()) "未下载" else "已下载：${downloadedAbis.joinToString("、")}",
                color = if (downloadedAbis.isEmpty()) UiMuted else UiCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (downloadedAbis.isEmpty()) {
                MiniActionButton(
                    if (busy) "下载中" else "下载",
                    focused = true,
                    icon = Icons.Outlined.Download,
                    enabled = !busy,
                    onClick = onDownload,
                )
            } else {
                MiniActionButton(
                    if (isSelected) "已选" else "选择",
                    focused = isSelected,
                    icon = if (isSelected) Icons.Outlined.Check else null,
                    enabled = !busy && !isSelected,
                    onClick = onSelect,
                )
                MiniActionButton("删除", danger = true, enabled = !busy, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun MiniActionButton(
    label: String,
    focused: Boolean = false,
    danger: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val foreground = when {
        danger -> Color(0xFFFFE3E3)
        focused -> Color(0xFF031112)
        else -> UiText
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    danger -> Color(0xFFE5484D)
                    focused -> UiCyan
                    else -> Color.White.copy(alpha = 0.08f)
                },
            )
            .border(
                1.dp,
                when {
                    danger -> Color(0xFFE5484D)
                    focused -> UiCyan
                    else -> UiLine
                },
                RoundedCornerShape(8.dp),
            )
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(13.dp))
                Spacer(Modifier.size(3.dp))
            }
            Text(
                label,
                color = foreground,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

private fun buildPlatformOptions(gameIndex: CategoryCatalog?, coreCatalog: CoreCatalog?): List<PlatformOption> {
    val fromIndex = gameIndex?.categories
        ?.filter { it.id != "all" && it.platformIds.isNotEmpty() }
        ?.map { PlatformOption(it.id, it.displayName, it.platformIds.first()) }
        .orEmpty()
    if (fromIndex.isNotEmpty()) return fromIndex

    return coreCatalog?.cores
        ?.flatMap { it.platformIds }
        ?.distinct()
        ?.map { PlatformOption(it, platformLabel(it), it) }
        .orEmpty()
}

private fun platformLabel(platformId: String): String = when (platformId.lowercase()) {
    "nes" -> "FC / NES"
    "snes" -> "SFC / SNES"
    "gba" -> "GBA"
    "nds" -> "NDS"
    "md" -> "MD / Genesis"
    else -> platformId.uppercase()
}
