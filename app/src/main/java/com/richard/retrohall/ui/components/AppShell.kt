package com.richard.retrohall.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richard.retrohall.ui.AppBackground
import com.richard.retrohall.ui.HallIcon
import com.richard.retrohall.ui.NavIcon
import com.richard.retrohall.ui.UiBlue
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiGold
import com.richard.retrohall.ui.UiLine
import com.richard.retrohall.ui.UiMuted
import com.richard.retrohall.ui.UiText

@Composable
internal fun AppShell(
    selectedNav: String,
    edgeToEdgeMain: Boolean = false,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    AppBackground(contentPadding = PaddingValues(0.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp || maxWidth < maxHeight
            val sidebarWidth = when {
                maxWidth >= 1500.dp -> 300.dp
                maxWidth >= 1100.dp -> 210.dp
                maxWidth >= 900.dp -> 160.dp
                else -> 148.dp
            }
            val mainHorizontalPadding = when {
                maxWidth >= 1500.dp -> 66.dp
                maxWidth >= 1100.dp -> 44.dp
                else -> 24.dp
            }
            val mainVerticalPadding = when {
                maxHeight >= 900.dp -> 48.dp
                maxHeight >= 700.dp -> 34.dp
                else -> 24.dp
            }

            if (compact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CompactTopNav(
                        selectedNav = selectedNav,
                        onSelectLibrary = onSelectLibrary,
                        onSelectRecent = onSelectRecent,
                        onSelectFavorites = onSelectFavorites,
                        onOpenSettings = onOpenSettings,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                    ) {
                        content()
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    HallSidebar(
                        selectedSection = selectedNav,
                        sidebarWidth = sidebarWidth,
                        onSelectLibrary = onSelectLibrary,
                        onSelectRecent = onSelectRecent,
                        onSelectFavorites = onSelectFavorites,
                        onOpenSettings = onOpenSettings,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = mainHorizontalPadding,
                                top = if (edgeToEdgeMain) 0.dp else mainVerticalPadding,
                                end = mainHorizontalPadding,
                                bottom = if (edgeToEdgeMain) 0.dp else mainVerticalPadding - 8.dp,
                            ),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompactTopNav(
    selectedNav: String,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .background(Color(0xF2071115))
            .drawBehind {
                drawLine(
                    color = UiLine,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactNavItem(NavIcon.Library, "游戏库", selectedNav == "游戏库", onSelectLibrary)
        CompactNavItem(NavIcon.Recent, "最近", selectedNav == "最近", onSelectRecent)
        CompactNavItem(NavIcon.Star, "收藏", selectedNav == "收藏", onSelectFavorites)
        CompactNavItem(NavIcon.Gear, "设置", selectedNav == "设置", onOpenSettings)
    }
}

@Composable
internal fun CompactNavItem(icon: NavIcon, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .height(62.dp)
            .width(70.dp)
            .background(if (selected) Color(0x3335F1DD) else Color.Transparent, RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) UiCyan else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HallIcon(icon = icon, color = if (selected) UiText else UiMuted, modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) UiText else UiMuted, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun PageTitle(title: String, sub: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, color = UiText, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
        if (sub.isNotBlank()) {
            Text(sub, color = UiMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun EmptyPanel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Brush.verticalGradient(listOf(Color(0xEB0F1E24), Color(0xEB081115))), RoundedCornerShape(16.dp))
            .border(1.dp, UiLine, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = UiMuted, fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun EmptyPanelFrame(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPanel(text = text)
    }
}

@Composable
internal fun HallSidebar(
    selectedSection: String,
    sidebarWidth: androidx.compose.ui.unit.Dp,
    onSelectLibrary: () -> Unit,
    onSelectRecent: () -> Unit,
    onSelectFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF2071115), Color(0xFA040A0D)),
                ),
            )
            .drawBehind {
                val gap = 8.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    var y = 0f
                    while (y <= size.height) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            radius = 1.05.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y),
                        )
                        y += gap
                    }
                    x += gap
                }
                drawLine(
                    color = UiLine,
                    start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            .padding(start = 22.dp, top = 34.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        SidebarLogo()
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                SidebarItem(icon = NavIcon.Library, label = "游戏库", selected = selectedSection == "游戏库", onClick = onSelectLibrary)
                SidebarItem(icon = NavIcon.Recent, label = "最近", selected = selectedSection == "最近", onClick = onSelectRecent)
                SidebarItem(icon = NavIcon.Star, label = "收藏", selected = selectedSection == "收藏", onClick = onSelectFavorites)
                SidebarItem(icon = NavIcon.Gear, label = "设置", selected = selectedSection == "设置", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
internal fun SidebarLogo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        RetroConsoleLogo(modifier = Modifier.width(106.dp).height(50.dp))
    }
}

@Composable
internal fun RetroConsoleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = h * 0.055f
        val radius = h * 0.22f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1469F4), UiCyan, Color(0xFFFF6B49)),
                start = Offset(0f, h),
                end = Offset(w, 0f),
            ),
            size = Size(w, h),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = Color(0xFF11171A),
            topLeft = Offset(stroke, stroke),
            size = Size(w - stroke * 2f, h - stroke * 2f),
            cornerRadius = CornerRadius(radius * 0.78f, radius * 0.78f),
        )
        drawRoundRect(
            color = Color(0xFF070A0C),
            topLeft = Offset(w * 0.26f, h * 0.12f),
            size = Size(w * 0.47f, h * 0.76f),
            cornerRadius = CornerRadius(h * 0.06f, h * 0.06f),
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D3F36), Color(0xFF020907)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = h * 0.62f,
            ),
            topLeft = Offset(w * 0.29f, h * 0.17f),
            size = Size(w * 0.41f, h * 0.66f),
            cornerRadius = CornerRadius(h * 0.045f, h * 0.045f),
        )

        val dpadX = w * 0.12f
        val dpadY = h * 0.34f
        drawRoundRect(
            color = Color(0xFF070A0C),
            topLeft = Offset(dpadX - w * 0.018f, dpadY - h * 0.13f),
            size = Size(w * 0.12f, h * 0.42f),
            cornerRadius = CornerRadius(h * 0.035f, h * 0.035f),
        )
        drawRoundRect(
            color = Color(0xFF070A0C),
            topLeft = Offset(dpadX - w * 0.06f, dpadY - h * 0.02f),
            size = Size(w * 0.20f, h * 0.16f),
            cornerRadius = CornerRadius(h * 0.035f, h * 0.035f),
        )
        drawRoundRect(
            color = UiCyan,
            topLeft = Offset(dpadX - w * 0.012f, dpadY - h * 0.19f),
            size = Size(w * 0.024f, h * 0.10f),
            cornerRadius = CornerRadius(h * 0.014f, h * 0.014f),
        )
        drawRoundRect(
            color = UiCyan,
            topLeft = Offset(dpadX - w * 0.095f, dpadY - h * 0.005f),
            size = Size(w * 0.07f, h * 0.045f),
            cornerRadius = CornerRadius(h * 0.014f, h * 0.014f),
        )

        drawRect(color = UiCyan, topLeft = Offset(w * 0.43f, h * 0.37f), size = Size(w * 0.048f, h * 0.12f))
        drawRect(color = UiCyan, topLeft = Offset(w * 0.56f, h * 0.37f), size = Size(w * 0.048f, h * 0.12f))
        drawLine(
            color = UiCyan,
            start = Offset(w * 0.44f, h * 0.59f),
            end = Offset(w * 0.59f, h * 0.59f),
            strokeWidth = stroke * 1.05f,
            cap = StrokeCap.Round,
        )

        listOf(
            Offset(w * 0.85f, h * 0.24f) to UiGold,
            Offset(w * 0.80f, h * 0.39f) to UiBlue,
            Offset(w * 0.90f, h * 0.39f) to UiCyan,
            Offset(w * 0.85f, h * 0.54f) to Color(0xFFFF6B49),
        ).forEach { (center, color) ->
            drawCircle(color = Color(0xFF040708), radius = h * 0.076f, center = center)
            drawCircle(color = color, radius = h * 0.045f, center = center)
        }
        drawRoundRect(
            color = UiGold,
            topLeft = Offset(w * 0.80f, h * 0.70f),
            size = Size(w * 0.12f, h * 0.035f),
            cornerRadius = CornerRadius(h * 0.018f, h * 0.018f),
        )
    }
}

@Composable
internal fun SidebarItem(icon: NavIcon, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                if (selected) Brush.linearGradient(listOf(Color(0x3A35F1DD), Color(0x1035F1DD)))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                RoundedCornerShape(14.dp),
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) UiCyan else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HallIcon(icon = icon, color = if (selected) UiText else Color(0xD1F2FBFC), modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) UiText else Color(0xD1F2FBFC), fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}