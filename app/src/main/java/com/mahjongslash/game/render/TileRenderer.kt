package com.mahjongslash.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.mahjongslash.game.model.Tile

/**
 * Renders tiles using pre-cached ImageBitmaps from TileBitmapCache.
 * During gameplay, each tile is a single drawImage() call.
 */
object TileRenderer {

    var bitmapCache: TileBitmapCache? = null

    private const val SPAWN_ANIM_DURATION_MS = 300L
    private const val SPAWN_SCALE_START = 0.5f

    fun DrawScope.drawTile(
        tile: Tile,
        @Suppress("UNUSED_PARAMETER") textMeasurer: Any? = null,
        alpha: Float = tile.alpha
    ) {
        val cached = bitmapCache?.get(tile.type)
        if (cached != null) {
            val w = Tile.WIDTH_DP * density
            val h = Tile.HEIGHT_DP * density
            val x = tile.position.x - w / 2f
            val y = tile.position.y - h / 2f

            // Spawn animation: scale up from 0.5 to 1.0 over 300ms with overshoot
            val age = System.currentTimeMillis() - tile.spawnTime
            val spawnScale = if (age < SPAWN_ANIM_DURATION_MS) {
                val t = age.toFloat() / SPAWN_ANIM_DURATION_MS
                // Overshoot ease-out: goes to ~1.05 then settles to 1.0
                val eased = 1f - (1f - t) * (1f - t)
                val overshoot = if (t < 0.7f) 0f else kotlin.math.sin((t - 0.7f) / 0.3f * Math.PI.toFloat()) * 0.06f
                SPAWN_SCALE_START + (1f - SPAWN_SCALE_START) * eased + overshoot
            } else 1f

            if (spawnScale < 0.99f) {
                scale(spawnScale, pivot = Offset(tile.position.x, tile.position.y)) {
                    drawImage(
                        image = cached,
                        dstOffset = IntOffset(x.toInt(), y.toInt()),
                        dstSize = IntSize(cached.width, cached.height),
                        alpha = alpha,
                    )
                }
            } else {
                drawImage(
                    image = cached,
                    dstOffset = IntOffset(x.toInt(), y.toInt()),
                    dstSize = IntSize(cached.width, cached.height),
                    alpha = alpha,
                )
            }
        }
    }
}
