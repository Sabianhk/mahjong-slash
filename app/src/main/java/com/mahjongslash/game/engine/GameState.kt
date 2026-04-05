package com.mahjongslash.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.mahjongslash.game.model.Tile
import com.mahjongslash.game.render.ShatterEffect
import com.mahjongslash.game.render.SlashTrail

/**
 * Immutable snapshot of the game state, consumed by the renderer.
 */
data class GameState(
    val frameCount: Long = 0L,
    val tiles: List<Tile> = emptyList(),
    val shatterEffects: List<ShatterEffect> = emptyList(),
    val slashTrails: List<SlashTrail> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val score: Int = 0,
    val combo: Int = 0,
    val bladeHealth: Int = 5,
    val phase: GamePhase = GamePhase.PLAYING,
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,
    /** Screen flash: color with alpha, decays each frame */
    val flashAlpha: Float = 0f,
    val flashColor: Color = Color.Transparent,
    /** Tile instance IDs that should be highlighted as hints */
    val hintTileIds: Set<Long> = emptySet(),
    /** Screen shake offset in pixels (decays to zero) */
    val shakeOffsetX: Float = 0f,
    val shakeOffsetY: Float = 0f,
    // Stats
    val tilesCleared: Int = 0,
    val maxCombo: Int = 0,
    val totalSlashes: Int = 0,
    val validSlashes: Int = 0,
)

enum class GamePhase {
    PLAYING,
    GAME_OVER_ANIM,  // Tiles freezing and shattering sequentially
    GAME_OVER,
    PAUSED
}

/**
 * Floating score/feedback text that drifts upward and fades.
 */
data class FloatingText(
    var position: Offset,
    val text: String,
    val color: Color,
    var elapsed: Float = 0f,
    val duration: Float = 1.0f,
    val scale: Float = 1.0f,
) {
    val isAlive: Boolean get() = elapsed < duration
    val alpha: Float get() = (1f - (elapsed / duration)).coerceIn(0f, 1f)
    /** Drift upward over lifetime */
    val driftY: Float get() = -elapsed * 120f
    /** Punch-in scale: starts at 1.5x then settles to target over 0.2s */
    val currentScale: Float get() {
        val punch = if (elapsed < 0.2f) 1f + (1f - elapsed / 0.2f) * 0.5f else 1f
        return scale * punch
    }
}
