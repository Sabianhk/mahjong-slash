package com.mahjongslash.game.engine

import androidx.compose.ui.geometry.Offset
import com.mahjongslash.game.model.Tile
import com.mahjongslash.game.model.TileState
import com.mahjongslash.game.render.ShatterEffect
import com.mahjongslash.game.render.SlashTrail

/**
 * Immutable snapshot of the game state, consumed by the renderer.
 */
data class GameState(
    val tiles: List<Tile> = emptyList(),
    val shatterEffects: List<ShatterEffect> = emptyList(),
    val slashTrails: List<SlashTrail> = emptyList(),
    val score: Int = 0,
    val combo: Int = 0,
    val bladeHealth: Int = 3,
    val phase: GamePhase = GamePhase.PLAYING,
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,
)

enum class GamePhase {
    PLAYING,
    GAME_OVER,
    PAUSED
}
