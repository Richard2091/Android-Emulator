package com.richard.retrohall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richard.retrohall.data.core.CoreCatalogClient
import com.richard.retrohall.data.core.CoreDownloadManager
import com.richard.retrohall.data.core.CoreSelectionStore
import com.richard.retrohall.data.settings.ResourceSourceStore
import com.richard.retrohall.data.settings.ResourceSources
import com.richard.retrohall.domain.settings.AspectRatio
import com.richard.retrohall.domain.settings.CacheMaintenance
import com.richard.retrohall.domain.settings.ControlMode
import com.richard.retrohall.domain.settings.UserSettings
import com.richard.retrohall.domain.settings.VirtualPadVisibility
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiPanel
import com.richard.retrohall.ui.UiText
import com.richard.retrohall.ui.aspectRatioLabel
import com.richard.retrohall.ui.components.AppShell
import com.richard.retrohall.ui.components.HallActionButton
import com.richard.retrohall.ui.components.HallConfirmDialog
import com.richard.retrohall.ui.components.HallSlider
import com.richard.retrohall.ui.components.HallToggle
import com.richard.retrohall.ui.components.SegmentedChoice
import com.richard.retrohall.ui.controlModeLabel
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import kotlinx.coroutines.launch

@Composable
internal fun SettingsScreen(
    settings: UserSettings,
    cacheMaintenance: CacheMaintenance,
    coreCatalogClient: CoreCatalogClient,
    coreDownloadManager: CoreDownloadManager,
    coreSelectionStore: CoreSelectionStore,
    resourceSourceStore: ResourceSourceStore,
    onCacheCleared: () -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showCoreManager by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<SourceField?>(null) }
    val sources by resourceSourceStore.sources.collectAsState(initial = ResourceSources())
    val scope = rememberCoroutineScope()

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
            item {
                SettingsSourceSection(
                    sources = sources,
                    onEditSource = { editingSource = it },
                    modifier = Modifier.fillMaxWidth(),
                    dense = false,
                )
            }
            item {
                SettingsCoreSection(
                    coreSelectionStore = coreSelectionStore,
                    onOpenCoreManager = { showCoreManager = true },
                    modifier = Modifier.fillMaxWidth(),
                    dense = false,
                )
            }
            item { SettingsSystemSection(cacheMaintenance, onCacheCleared, Modifier.fillMaxWidth(), dense = false) }
        }
    }

    if (showCoreManager) {
        CoreManagerDialog(
            coreCatalogClient = coreCatalogClient,
            coreDownloadManager = coreDownloadManager,
            coreSelectionStore = coreSelectionStore,
            onDismiss = { showCoreManager = false },
        )
    }

    editingSource?.let { field ->
        SourceUrlDialog(
            title = if (field == SourceField.Game) "游戏源" else "核心源",
            initialValue = if (field == SourceField.Game) sources.gameSourceUrl else sources.coreSourceUrl,
            onConfirm = { value ->
                val next = when (field) {
                    SourceField.Game -> sources.copy(gameSourceUrl = value)
                    SourceField.Core -> sources.copy(coreSourceUrl = value)
                }
                scope.launch { resourceSourceStore.update(next) }
                editingSource = null
            },
            onDismiss = { editingSource = null },
        )
    }
}

private enum class SourceField { Game, Core }

@Composable
private fun SettingsSourceSection(
    sources: ResourceSources,
    onEditSource: (SourceField) -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
) {
    SettingsSection(title = "数据源", modifier = modifier, dense = dense) {
        SettingRow("游戏源", first = true, dense = dense) {
            SourceValueRow(value = sources.gameSourceUrl, onClick = { onEditSource(SourceField.Game) }, dense = dense)
        }
        SettingRow("核心源", dense = dense) {
            SourceValueRow(value = sources.coreSourceUrl, onClick = { onEditSource(SourceField.Core) }, dense = dense)
        }
    }
}

@Composable
private fun SourceValueRow(value: String, onClick: () -> Unit, dense: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            value.ifBlank { "未设置" },
            color = if (value.isBlank()) UiMuted else UiText,
            fontSize = if (dense) 13.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, UiCyan, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("修改", color = UiCyan, fontSize = if (dense) 13.sp else 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SourceUrlDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(22.dp),
            color = UiPanel,
            border = androidx.compose.foundation.BorderStroke(1.dp, UiLine),
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, color = UiText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "请输入资源仓库根地址，留空则使用内置默认地址。",
                    color = UiMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(if (title == "游戏源") "https://example.com/RetroGame/" else "https://example.com/RetroGame-Cores/", color = UiMuted)
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = UiText, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HallActionButton("保存", focused = true, fillWidth = true, onClick = { onConfirm(value) })
                }
                HallActionButton("取消", focused = false, fillWidth = true, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun SettingsCoreSection(
    coreSelectionStore: CoreSelectionStore,
    onOpenCoreManager: () -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
) {
    val selections by coreSelectionStore.selections.collectAsState(initial = emptyMap())
    val currentCore = remember(selections) {
        selections["nes"]?.takeIf { it.isNotBlank() } ?: "默认"
    }
    SettingsSection(title = "核心", modifier = modifier, dense = dense) {
        SettingRow("核心管理", first = true, dense = dense) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "当前：$currentCore",
                    color = UiText,
                    fontSize = if (dense) 14.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, UiCyan, RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenCoreManager)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("管理", color = UiCyan, fontSize = if (dense) 13.sp else 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
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