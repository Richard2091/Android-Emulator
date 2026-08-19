package com.richard.retrohall.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiText

@Composable
internal fun HallActionButton(
    label: String,
    focused: Boolean = false,
    danger: Boolean = false,
    accent: Color? = null,
    icon: ImageVector? = null,
    iconSize: Dp? = null,
    compact: Boolean = false,
    fillWidth: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val foreground = when {
        danger -> Color(0xFFFFE3E3)
        focused -> Color(0xFF031112)
        accent != null -> Color(0xFF031112)
        else -> UiText
    }
    Box(
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(if (compact) 124.dp else 150.dp))
            .height(if (compact) 48.dp else 62.dp)
            .background(
                when {
                    danger -> Brush.linearGradient(listOf(Color(0xFFE5484D), Color(0xFFB91C1C)))
                    focused -> Brush.linearGradient(listOf(UiCyan, Color(0xFF60FFE8)))
                    accent != null -> Brush.linearGradient(listOf(accent, lerp(accent, Color.White, 0.35f)))
                    else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.03f)))
                },
                RoundedCornerShape(14.dp),
            )
            .border(
                if (focused) 3.dp else 1.dp,
                when {
                    danger -> Color(0xFFE5484D)
                    focused -> UiCyan
                    accent != null -> accent
                    else -> Color(0x33DAF1F4)
                },
                RoundedCornerShape(14.dp),
            )
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(iconSize ?: (if (compact) 22.dp else 26.dp)),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                color = foreground,
                fontSize = if (compact) 16.sp else 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun MetaLine(label: String, value: String, showDivider: Boolean = true, compact: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (showDivider) {
                    drawLine(
                        color = Color(0x1FDAF1F4),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .padding(top = if (showDivider) if (compact) 9.dp else 16.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = UiMuted, fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.Bold)
        Text(value, color = UiText, fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun SegmentedChoice(
    values: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    compact: Boolean = false,
    showSelected: Boolean = true,
    showContainer: Boolean = true,
) {
    Row(
        modifier = Modifier
            .background(if (showContainer) Color.White.copy(alpha = 0.08f) else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(if (compact) 4.dp else 5.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEachIndexed { index, value ->
            Box(
                modifier = Modifier
                    .height(if (compact) 30.dp else 38.dp)
                    .background(if (showSelected && index == selectedIndex) UiCyan else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onSelected(index) }
                    .padding(horizontal = if (compact) 10.dp else 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value,
                    color = if (showSelected && index == selectedIndex) Color(0xFF031112) else UiMuted,
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun HallToggle(checked: Boolean, compact: Boolean = false, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .width(if (compact) 50.dp else 58.dp)
            .height(if (compact) 26.dp else 30.dp)
            .background(if (checked) UiCyan.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 18.dp else 22.dp)
                .background(Color.White, RoundedCornerShape(50)),
        )
    }
}

@Composable
internal fun PillButton(
    text: String,
    opacity: Float,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onInteraction: () -> Unit = {},
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .alpha(opacity)
            .clip(RoundedCornerShape(999.dp))
            .background(if (pressed) UiCyan.copy(alpha = 0.25f) else Color(0xE6101D22))
            .border(1.5.dp, if (pressed) UiCyan else UiLine, RoundedCornerShape(999.dp))
            .clickable(interactionSource = interaction, indication = null) {
                onInteraction()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (danger) Color(0xFFFF8A6B) else UiText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun GameSegButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, UiLine),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
internal fun GamePanelButton(text: String, primary: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    pressed -> UiCyan.copy(alpha = 0.5f)
                    primary -> UiCyan
                    else -> Color(0xE6101D22)
                },
            )
            .border(1.dp, if (primary || pressed) UiCyan else UiLine, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = when {
                primary -> Color(0xFF031112)
                danger -> Color(0xFFFF8A6B)
                else -> UiText
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun HallSlider(value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float> = 0f..1f, compact: Boolean = false) {
    val sliderWidth = if (compact) 154.dp else 210.dp
    val thumbSize = if (compact) 18.dp else 24.dp
    val rawFraction = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val fraction = rawFraction.coerceIn(0f, 1f)

    fun updateFromX(x: Float, width: Float) {
        val nextFraction = (x / width).coerceIn(0f, 1f)
        val nextValue = valueRange.start + (valueRange.endInclusive - valueRange.start) * nextFraction
        onValueChange(nextValue)
    }

    BoxWithConstraints(
        modifier = Modifier
            .width(sliderWidth)
            .height(if (compact) 26.dp else 32.dp)
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = { offset -> updateFromX(offset.x, size.width.toFloat()) },
                    onDrag = { change, _ -> updateFromX(change.position.x, size.width.toFloat()) },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp)),
        )
        Box(
            modifier = Modifier
                .width(maxWidth * fraction)
                .height(8.dp)
                .background(UiCyan, RoundedCornerShape(999.dp)),
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - thumbSize) * fraction)
                .size(thumbSize)
                .background(Color.White, RoundedCornerShape(50)),
        )
    }
}