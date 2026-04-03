package com.mahjongslash.game.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * A live tile instance on the game canvas.
 * Each tile has a type, position, velocity, and lifecycle state.
 */
data class Tile(
    val instanceId: Long,
    val type: TileType,
    var position: Offset,
    var velocity: Offset,
    val spawnTime: Long,
    var state: TileState = TileState.ALIVE,
    var alpha: Float = 1f,
    var shatterElapsed: Float = 0f,
) {
    companion object {
        /** Tile dimensions in dp — will be converted to px at render time */
        const val WIDTH_DP = 56f
        const val HEIGHT_DP = 72f
        const val DEPTH_DP = 6f   // Side edge thickness for 3D effect
        const val SHADOW_DP = 4f  // Shadow offset
        const val HIT_PADDING_DP = 8f // Extra hit area for forgiveness
    }

    /** Bounding rect in pixel coordinates (set by renderer based on density) */
    fun bounds(density: Float): Rect {
        val w = WIDTH_DP * density
        val h = HEIGHT_DP * density
        return Rect(
            left = position.x - w / 2f,
            top = position.y - h / 2f,
            right = position.x + w / 2f,
            bottom = position.y + h / 2f
        )
    }

    /** Expanded bounds for hit detection */
    fun hitBounds(density: Float): Rect {
        val padding = HIT_PADDING_DP * density
        return bounds(density).inflate(padding)
    }
}

enum class TileState {
    ALIVE,
    SHATTERING,
    DEAD
}

/** Inflates a Rect by the given amount on all sides */
private fun Rect.inflate(amount: Float): Rect = Rect(
    left = left - amount,
    top = top - amount,
    right = right + amount,
    bottom = bottom + amount
)
