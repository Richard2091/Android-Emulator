package com.richard.retrohall.ui.save

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.save.SaveStateSlot
import com.richard.retrohall.domain.save.SaveStateStore
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiText
import com.richard.retrohall.ui.components.AppShell
import com.richard.retrohall.ui.components.EmptyPanel
import com.richard.retrohall.ui.components.HallActionButton
import com.richard.retrohall.ui.formatTimestamp
import kotlinx.coroutines.launch

@Composable
internal fun SaveManagerScreen(
    game: LocalGame,
    saveStateStore: SaveStateStore,
    selectedSaveId: String?,
    onSelectSave: (SaveStateSlot) -> Unit,
    onBackToDetail: () -> Unit,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val saveStates by saveStateStore.observeForGame(game.id).collectAsState(initial = emptyList())

    AppShell(
        selectedNav = "",
        onSelectLibrary = onSelectLibrary,
        onSelectRecent = onSelectRecent,
        onSelectFavorites = onSelectFavorites,
        onOpenSettings = onOpenSettings,
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("存档管理", color = UiText, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
                    Text(game.title, color = UiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HallActionButton("新增槽位", focused = true, compact = true) {
                        scope.launch { saveStateStore.addSlot(game.id) }
                    }
                    HallActionButton("返回详情", focused = false, compact = true, onClick = onBackToDetail)
                }
            }

            if (saveStates.isEmpty()) {
                EmptyPanel("暂无存档")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    lazyItems(saveStates, key = { it.id }) { saveState ->
                        SaveStateRow(
                            saveState = saveState,
                            selected = saveState.id == selectedSaveId,
                            onSelect = { onSelectSave(saveState) },
                            onCopy = { scope.launch { saveStateStore.copy(saveState.id) } },
                            onDelete = { scope.launch { saveStateStore.delete(saveState.id) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveStateRow(
    saveState: SaveStateSlot,
    selected: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Brush.verticalGradient(listOf(Color(0xEB0F1E24), Color(0xEB081115))), RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) UiCyan else UiLine, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                saveState.displayName(),
                color = UiText,
                fontSize = 21.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "最后游玩 ${formatTimestamp(saveState.updatedAt)}",
                color = UiMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HallActionButton("复制", focused = false, compact = true, onClick = onCopy)
            HallActionButton("删除", focused = false, compact = true, onClick = onDelete)
        }
    }
}

internal fun SaveStateSlot.displayName(): String {
    return when (slotType) {
        "auto" -> "自动存档"
        else -> "手动槽 ${slotIndex ?: 1}"
    }
}