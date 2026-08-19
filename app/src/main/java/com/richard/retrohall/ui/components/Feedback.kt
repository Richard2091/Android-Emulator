package com.richard.retrohall.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.richard.retrohall.ui.UiCyan
import com.richard.retrohall.ui.UiPanel
import com.richard.retrohall.ui.UiText
import kotlinx.coroutines.delay

@Composable
internal fun TopToast(
    message: String?,
    onDismiss: () -> Unit,
) {
    if (message == null) return
    val visible = remember(message) { Animatable(0f) }
    LaunchedEffect(message) {
        visible.snapTo(0f)
        visible.animateTo(1f, animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
        delay(3000)
        onDismiss()
    }
    val slidePx = with(LocalDensity.current) { 72.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
            .graphicsLayer {
                translationY = -slidePx * (1f - visible.value)
                alpha = visible.value
            }
            .padding(top = 18.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = UiPanel,
            border = BorderStroke(1.dp, UiCyan),
            shadowElevation = 12.dp,
        ) {
            Text(
                message,
                color = UiText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
internal fun CoverLoadingIndicator(size: Dp) {
    val transition = rememberInfiniteTransition(label = "coverLoading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coverAlpha",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = UiCyan.copy(alpha = alpha),
            strokeWidth = 3.dp,
        )
    }
}