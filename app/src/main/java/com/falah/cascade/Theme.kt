package com.falah.cascade

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp

val Brand = Color(0xFFF08440)
val BrandLight = Color(0xFFF69A5C)

private val DarkScheme = darkColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    background = Color(0xFF0E0F13),
    onBackground = Color(0xFFF2F2F5),
    surface = Color(0xFF1B1C22),
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF262831),
    onSurfaceVariant = Color(0xFFA9ABB5),
)

private val LightScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    background = Color(0xFFF6F3F0),
    onBackground = Color(0xFF16171B),
    surface = Color.White,
    onSurface = Color(0xFF16171B),
    surfaceVariant = Color(0xFFE8E8EC),
    onSurfaceVariant = Color(0xFF6B6D78),
)

/** mode: 0 = follow system, 1 = light, 2 = dark */
@Composable
fun CascadeTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}

private val BarHeights = floatArrayOf(0.16f, 0.30f, 0.42f, 0.26f, 0.36f, 0.18f)

/** The Cascade logo: orange rounded square with a white waveform. [animated] makes the bars dance. */
@Composable
fun LogoMark(dp: Dp, modifier: Modifier = Modifier, animated: Boolean = false) {
    val scales = if (animated) {
        val t = rememberInfiniteTransition(label = "eq")
        List(6) { i ->
            t.animateFloat(
                0.55f, 1.25f,
                infiniteRepeatable(tween(520 + i * 110, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bar$i",
            )
        }
    } else null

    Box(
        modifier
            .size(dp)
            .clip(RoundedCornerShape(dp * 0.26f))
            .background(Brush.linearGradient(listOf(BrandLight, Color(0xFFEE7E3E)))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val gap = w * 0.085f
            val bw = w * 0.058f
            BarHeights.forEachIndexed { i, f ->
                val sc = scales?.get(i)?.value ?: 1f
                val x = w / 2 + (i - 2.5f) * gap
                val half = h * f * sc / 2
                drawLine(Color.White, Offset(x, h / 2 - half), Offset(x, h / 2 + half), strokeWidth = bw, cap = StrokeCap.Round)
            }
        }
    }
}
