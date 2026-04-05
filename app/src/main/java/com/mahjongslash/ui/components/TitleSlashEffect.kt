package com.mahjongslash.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mahjongslash.ui.theme.AccentRed
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated red calligraphy slash that draws across the title area.
 * Triggers on first composition with a smooth brush-stroke reveal.
 */
@Composable
fun TitleSlashEffect(
    modifier: Modifier = Modifier,
) {
    val slashProgress = remember { Animatable(0f) }
    val splatterAlpha = remember { Animatable(0f) }

    // Pre-generate splatter positions (no allocation during draw)
    val splatters = remember {
        val rng = Random(42)
        List(8) {
            SplatterDot(
                offsetRatio = rng.nextFloat() * 0.3f + 0.7f, // cluster near end of slash
                angle = rng.nextFloat() * 360f,
                distance = rng.nextFloat() * 30f + 10f,
                radius = rng.nextFloat() * 3f + 1.5f,
            )
        }
    }

    LaunchedEffect(Unit) {
        // Slash draws in
        slashProgress.animateTo(
            1f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
        // Splatter appears at end
        splatterAlpha.animateTo(
            1f,
            animationSpec = tween(200)
        )
        // Fade splatter out
        kotlinx.coroutines.delay(800)
        splatterAlpha.animateTo(
            0f,
            animationSpec = tween(400)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (slashProgress.value > 0f) {
            drawSlash(slashProgress.value)
        }
        if (splatterAlpha.value > 0f) {
            drawSplatters(splatters, splatterAlpha.value)
        }
    }
}

private data class SplatterDot(
    val offsetRatio: Float,
    val angle: Float,
    val distance: Float,
    val radius: Float,
)

private fun DrawScope.drawSlash(progress: Float) {
    val w = size.width
    val h = size.height

    // Slash goes from upper-left to lower-right across title area
    val startX = w * 0.15f
    val startY = h * 0.35f
    val endX = w * 0.85f
    val endY = h * 0.55f

    val currentEndX = startX + (endX - startX) * progress
    val currentEndY = startY + (endY - startY) * progress

    // Calligraphy brush stroke: thick center, tapered ends
    // Draw as a gradient-colored line with varying width
    val maxStrokeWidth = 6f * density

    // Main stroke
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                AccentRed.copy(alpha = 0.3f),
                AccentRed.copy(alpha = 0.7f),
                AccentRed.copy(alpha = 0.5f),
            ),
            start = Offset(startX, startY),
            end = Offset(currentEndX, currentEndY),
        ),
        start = Offset(startX, startY),
        end = Offset(currentEndX, currentEndY),
        strokeWidth = maxStrokeWidth * bellCurve(progress),
        cap = StrokeCap.Round,
    )

    // Thinner highlight stroke slightly offset
    drawLine(
        color = AccentRed.copy(alpha = 0.15f),
        start = Offset(startX, startY - 2f * density),
        end = Offset(currentEndX, currentEndY - 2f * density),
        strokeWidth = maxStrokeWidth * 0.4f * bellCurve(progress),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSplatters(splatters: List<SplatterDot>, alpha: Float) {
    val w = size.width
    val h = size.height

    // Splash origin near the end of the slash
    val originX = w * 0.8f
    val originY = h * 0.52f

    for (dot in splatters) {
        val rad = Math.toRadians(dot.angle.toDouble()).toFloat()
        val x = originX + cos(rad) * dot.distance * density
        val y = originY + sin(rad) * dot.distance * density

        drawCircle(
            color = AccentRed.copy(alpha = alpha * 0.5f),
            radius = dot.radius * density,
            center = Offset(x, y),
        )
    }
}

/** Bell curve shape: 0→1→0 for brush thickness variation */
private fun bellCurve(t: Float): Float {
    return if (t < 0.5f) t * 2f else (1f - t) * 2f
}
