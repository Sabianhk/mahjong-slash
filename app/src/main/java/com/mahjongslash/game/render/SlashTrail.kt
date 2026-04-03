package com.mahjongslash.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mahjongslash.ui.theme.AccentGold
import com.mahjongslash.ui.theme.AccentRed
import com.mahjongslash.ui.theme.SlashGlowColor
import com.mahjongslash.ui.theme.SlashTrailColor
import com.mahjongslash.ui.theme.WarmWhite

/**
 * Renders the slash trail as a bright calligraphy brush stroke
 * visible against the dark background.
 *
 * On match result, the trail briefly flashes gold (success) or red (failure).
 */
data class SlashTrailPoint(
    val position: Offset,
    val timestamp: Long,
    val pressure: Float = 1f
)

data class SlashTrail(
    val points: MutableList<SlashTrailPoint> = mutableListOf(),
    var fadeAlpha: Float = 1f,
    var isFading: Boolean = false,
    /** Set on match result: gold for success, red for penalty, null while drawing */
    var resultColor: Color? = null,
) {
    companion object {
        const val FADE_DURATION_MS = 500L
        const val MAX_STROKE_WIDTH = 18f
        const val MIN_STROKE_WIDTH = 4f
    }

    val isAlive: Boolean get() = fadeAlpha > 0.01f && points.isNotEmpty()
}

fun DrawScope.drawSlashTrail(trail: SlashTrail) {
    if (trail.points.size < 2) return

    val alpha = trail.fadeAlpha
    val resultColor = trail.resultColor

    val path = Path()
    val firstPoint = trail.points.first()
    path.moveTo(firstPoint.position.x, firstPoint.position.y)

    for (i in 1 until trail.points.size) {
        val prev = trail.points[i - 1]
        val curr = trail.points[i]
        val midX = (prev.position.x + curr.position.x) / 2f
        val midY = (prev.position.y + curr.position.y) / 2f
        path.quadraticBezierTo(prev.position.x, prev.position.y, midX, midY)
    }
    val lastPoint = trail.points.last()
    path.lineTo(lastPoint.position.x, lastPoint.position.y)

    // Choose colors based on match result
    val glowColor = resultColor ?: SlashGlowColor
    val mainColor = resultColor ?: SlashTrailColor
    val coreColor = resultColor?.copy(alpha = 0.9f) ?: WarmWhite

    // Outer glow — wide, warm, clearly visible
    drawPath(
        path = path,
        color = glowColor.copy(alpha = 0.35f * alpha),
        style = Stroke(
            width = SlashTrail.MAX_STROKE_WIDTH * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Main stroke — bright ivory/result color
    drawPath(
        path = path,
        color = mainColor.copy(alpha = 0.85f * alpha),
        style = Stroke(
            width = 7f * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Inner bright core
    drawPath(
        path = path,
        color = coreColor.copy(alpha = 0.6f * alpha),
        style = Stroke(
            width = 2.5f * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
