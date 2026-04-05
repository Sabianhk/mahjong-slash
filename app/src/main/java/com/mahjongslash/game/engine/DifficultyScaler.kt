package com.mahjongslash.game.engine

/**
 * Ramps difficulty over time. Provides current spawn interval,
 * max tiles, and base speed based on elapsed game time.
 */
class DifficultyScaler {

    private var gameStartTime = 0L

    fun reset() {
        gameStartTime = System.currentTimeMillis()
    }

    /** Seconds elapsed since game start */
    val elapsedSeconds: Float
        get() = if (gameStartTime == 0L) 0f
                else (System.currentTimeMillis() - gameStartTime) / 1000f

    /** Current difficulty tier (0-4) based on 30s intervals */
    val tier: Int
        get() = (elapsedSeconds / 30f).toInt().coerceIn(0, 4)

    /** Seconds between spawn groups */
    val spawnInterval: Float
        get() = when (tier) {
            0 -> 2.0f
            1 -> 1.5f
            2 -> 1.2f
            3 -> 0.9f
            else -> 0.6f
        }

    /** Maximum alive tiles on screen */
    val maxTiles: Int
        get() = when (tier) {
            0 -> 6
            1 -> 8
            2 -> 10
            3 -> 12
            else -> 14
        }

    /** Base speed in dp/s for tile movement */
    val baseSpeedDpPerSec: Float
        get() = when (tier) {
            0 -> 30f
            1 -> 40f
            2 -> 55f
            3 -> 70f
            else -> 85f
        }

    /** Speed variance range in dp/s */
    val speedVarianceDpPerSec: Float
        get() = when (tier) {
            0 -> 10f
            1 -> 15f
            2 -> 20f
            3 -> 25f
            else -> 30f
        }
}
