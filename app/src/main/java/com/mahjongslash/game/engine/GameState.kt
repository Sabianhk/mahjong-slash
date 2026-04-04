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
    /** Debug: last slash diagnostics (temporary — remove after validation) */
    val debugLastSlash: String = "",
    /** Debug: last swipe path for visual overlay (temporary) */
    val debugLastPath: List<Offset> = emptyList(),
    /** Debug: density used by engine (temporary) */
    val debugDensity: Float = 1f,
    /** Debug: auto-slash result (temporary) */
    val debugAutoSlash: String = "",
)

enum class GamePhase {
    PLAYING,
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
) {
    val isAlive: Boolean get() = elapsed < duration
    val alpha: Float get() = (1f - (elapsed / duration)).coerceIn(0f, 1f)
    /** Drift upward over lifetime */
    val driftY: Float get() = -elapsed * 120f
}
