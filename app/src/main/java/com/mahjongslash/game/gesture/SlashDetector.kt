package com.mahjongslash.game.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.mahjongslash.game.model.Tile
import kotlin.math.sqrt

/**
 * Detects slash gestures and determines which tiles are intersected.
 *
 * A valid slash requires:
 * - Minimum path length of 100dp (prevents accidental taps)
 * - Touch down → drag → touch up
 *
 * Hit testing uses expanded tile bounds (+ 8dp padding for forgiveness).
 */
object SlashDetector {

    private const val MIN_SLASH_LENGTH_DP = 100f

    /**
     * Given a list of swipe path points and the active tiles,
     * returns the set of tiles whose hit bounds were crossed by the swipe path.
     */
    fun detectSlashedTiles(
        pathPoints: List<Offset>,
        tiles: List<Tile>,
        density: Float
    ): List<Tile> {
        if (pathPoints.size < 2) return emptyList()

        // Check minimum path length
        val totalLength = pathLength(pathPoints) / density
        if (totalLength < MIN_SLASH_LENGTH_DP) return emptyList()

        val slashed = mutableSetOf<Long>()
        val result = mutableListOf<Tile>()

        // For each segment of the path, check intersection with tile hit bounds
        for (i in 0 until pathPoints.size - 1) {
            val p1 = pathPoints[i]
            val p2 = pathPoints[i + 1]

            for (tile in tiles) {
                if (tile.instanceId in slashed) continue
                val bounds = tile.hitBounds(density)
                if (lineIntersectsRect(p1, p2, bounds)) {
                    slashed.add(tile.instanceId)
                    result.add(tile)
                }
            }
        }

        return result
    }

    private fun pathLength(points: List<Offset>): Float {
        var length = 0f
        for (i in 0 until points.size - 1) {
            length += distance(points[i], points[i + 1])
        }
        return length
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Checks if a line segment from p1 to p2 intersects a rectangle.
     * Uses the Liang-Barsky line clipping algorithm.
     */
    private fun lineIntersectsRect(p1: Offset, p2: Offset, rect: Rect): Boolean {
        // Quick check: if either point is inside the rect
        if (rect.contains(p1) || rect.contains(p2)) return true

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y

        val p = floatArrayOf(-dx, dx, -dy, dy)
        val q = floatArrayOf(
            p1.x - rect.left,
            rect.right - p1.x,
            p1.y - rect.top,
            rect.bottom - p1.y
        )

        var tMin = 0f
        var tMax = 1f

        for (i in 0 until 4) {
            if (p[i] == 0f) {
                if (q[i] < 0f) return false
            } else {
                val t = q[i] / p[i]
                if (p[i] < 0f) {
                    tMin = maxOf(tMin, t)
                } else {
                    tMax = minOf(tMax, t)
                }
                if (tMin > tMax) return false
            }
        }

        return true
    }
}
