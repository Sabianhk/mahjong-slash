package com.mahjongslash.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mahjongslash.ui.theme.InkBlack
import com.mahjongslash.ui.theme.SlashTrailColor

/**
 * Renders the slash trail as a calligraphy brush stroke.
 * The stroke varies in width based on speed — fast strokes are thin,
 * slow strokes are thick, mimicking a real brush.
 */
data class SlashTrailPoint(
    val position: Offset,
    val timestamp: Long,
    val pressure: Float = 1f // Simulated pressure based on speed
)

data class SlashTrail(
    val points: MutableList<SlashTrailPoint> = mutableListOf(),
    var fadeAlpha: Float = 1f,
    var isFading: Boolean = false,
) {
    companion object {
        const val FADE_DURATION_MS = 400L
        const val MAX_STROKE_WIDTH = 18f
        const val MIN_STROKE_WIDTH = 4f
    }

    val isAlive: Boolean get() = fadeAlpha > 0.01f && points.isNotEmpty()
}

fun DrawScope.drawSlashTrail(trail: SlashTrail) {
    if (trail.points.size < 2) return

    val alpha = trail.fadeAlpha

    // Draw the main stroke path with varying width
    // For simplicity in Phase 1, use a single-width path with brush-like appearance
    val path = Path()
    val firstPoint = trail.points.first()
    path.moveTo(firstPoint.position.x, firstPoint.position.y)

    for (i in 1 until trail.points.size) {
        val prev = trail.points[i - 1]
        val curr = trail.points[i]
        // Smooth the path with quadratic curves
        val midX = (prev.position.x + curr.position.x) / 2f
        val midY = (prev.position.y + curr.position.y) / 2f
        path.quadraticBezierTo(prev.position.x, prev.position.y, midX, midY)
    }
    // Connect to last point
    val lastPoint = trail.points.last()
    path.lineTo(lastPoint.position.x, lastPoint.position.y)

    // Outer glow (wider, semi-transparent)
    drawPath(
        path = path,
        color = SlashTrailColor.copy(alpha = 0.15f * alpha),
        style = Stroke(
            width = SlashTrail.MAX_STROKE_WIDTH * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Main ink stroke
    drawPath(
        path = path,
        color = InkBlack.copy(alpha = 0.8f * alpha),
        style = Stroke(
            width = 6f * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Inner highlight (thin, slightly lighter)
    drawPath(
        path = path,
        color = Color(0xFF4A3D2F).copy(alpha = 0.4f * alpha),
        style = Stroke(
            width = 2f * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
