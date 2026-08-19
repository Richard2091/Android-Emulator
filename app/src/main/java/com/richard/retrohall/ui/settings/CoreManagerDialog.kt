package com.richard.retrohall.ui.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
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
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiPanel
import com.richard.retrohall.ui.UiText
import com.richard.retrohall.ui.components.HallActionButton
import kotlinx.coroutines.launch

/**
 * 核心管理弹窗：按平台查看核心，支持在线下载、选择、删除已下载核心。
 */
@Composable
internal fun CoreManagerDialog(
    coreCatalogClient: CoreCatalogClient,
    coreDownloadManager: CoreDownloadManager,
    coreSelectionStore: CoreSelectionStore,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<CoreCatalog?>(null) }
    var platformId by remember { mutableStateOf<String?>(null) }
    var busyCoreId by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val selections by coreSelectionStore.selections.collectAsState(initial = emptyMap())

    LaunchedEffect(coreCatalogClient) {
        catalog = runCatching { coreCatalogClient.fetchCatalog() }.getOrNull()
        platformId = catalog?.cores?.flatMap { it.platformIds }?.firstOrNull()
    }

    val platforms = remember(catalog) {
        catalog?.cores?.flatMap { it.platformIds }?.distinct()?.sorted().orEmpty()
    }
    val activePlatform = platformId ?: platforms.firstOrNull()
    val platformCores = catalog?.forPlatform(activePlatform.orEmpty()).orEmpty()
    val selectedCoreId = selections[activePlatform.orEmpty()]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).height(560.dp),
            shape = RoundedCornerShape(22.dp),
            color = UiPanel,
            border = androidx.compose.foundation.BorderStroke(1.dp, UiLine),
            shadowElevation = 24.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("核心管理", color = UiText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, UiLine, RoundedCornerShape(10.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = UiText, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (platforms.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        platforms.forEach { platform ->
                            val selected = platform == activePlatform
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) UiCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
                                    .border(if (selected) 1.dp else 0.dp, UiCyan, RoundedCornerShape(10.dp))
                                    .clickable { platformId = platform }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    platformLabel(platform),
                                    color = if (selected) UiCyan else UiText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                notice?.let { message ->
                    Text(message, color = UiMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }

                if (catalog == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("核心清单加载失败，请检查网络后重试。", color = UiMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (platformCores.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("该平台暂无可用核心。", color = UiMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                        coreSelectionStore.select(activePlatform.orEmpty(), core.id)
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
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) UiCyan.copy(alpha = 0.14f) else Color(0x0DFFFFFF))
            .border(1.dp, if (isSelected) UiCyan else UiLine, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    core.displayName,
                    color = UiText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isSelected) {
                    Spacer(Modifier.size(6.dp))
                    Icon(Icons.Outlined.Check, contentDescription = "当前核心", tint = UiCyan, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                "${core.version} · ${core.license}",
                color = UiMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (downloadedAbis.isEmpty()) "未下载" else "已下载：${downloadedAbis.joinToString("、")}",
                color = if (downloadedAbis.isEmpty()) UiMuted else UiCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(10.dp))
        if (downloadedAbis.isEmpty()) {
            HallActionButton(if (busy) "下载中" else "下载", focused = true, compact = true, icon = Icons.Outlined.Download, enabled = !busy, onClick = onDownload)
        } else {
            HallActionButton(
                if (isSelected) "已选择" else "选择",
                focused = false,
                compact = true,
                icon = if (isSelected) Icons.Outlined.Check else null,
                enabled = !busy && !isSelected,
                onClick = onSelect,
            )
            Spacer(Modifier.size(6.dp))
            HallActionButton("删除", focused = false, compact = true, danger = true, icon = Icons.Outlined.Delete, enabled = !busy, onClick = onDelete)
        }
    }
}

private fun platformLabel(platformId: String): String = when (platformId.lowercase()) {
    "nes" -> "FC / NES"
    "snes" -> "SFC / SNES"
    "gba" -> "GBA"
    "nds" -> "NDS"
    "md" -> "MD / Genesis"
    else -> platformId.uppercase()
}
