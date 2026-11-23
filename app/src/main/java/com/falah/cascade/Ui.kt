package com.falah.cascade

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun glassFill(): Color = if (isDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.65f)

@Composable
fun glassBorder(): Color = if (isDarkTheme()) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.07f)

/** Click with a springy press-down scale. */
fun Modifier.pressClick(scaleTo: Float = 0.92f, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) scaleTo else 1f,
        spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Round translucent button with press animation. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    fill: Color = glassFill(),
    border: Color = glassBorder(),
    content: @Composable () -> Unit,
) {
    Box(
        modifier.pressClick(0.88f, onClick).size(size).clip(CircleShape).background(fill).border(1.dp, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Selectable pill used on dark panels. */
@Composable
fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) Brand else Color.White.copy(alpha = 0.10f), tween(220), label = "chip")
    Box(
        modifier.pressClick(0.94f, onClick).clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
}

/** Fade + slide-up entrance, staggered by [index]. */
@Composable
fun Appear(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay((index % 8) * 45L); shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(450), label = "appear")
    Box(Modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 36.dp.toPx() }) { content() }
}
