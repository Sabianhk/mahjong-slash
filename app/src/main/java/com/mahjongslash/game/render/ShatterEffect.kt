package com.mahjongslash.game.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.mahjongslash.game.model.Tile
import com.mahjongslash.ui.theme.TileEdge
import com.mahjongslash.ui.theme.TileIvory
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A shatter effect that breaks a tile into fragments that fly outward.
 * Each fragment is an irregularly-sized piece of ivory with rotation and fade.
 */
data class ShatterFragment(
    var position: Offset,
    var velocity: Offset,
    var rotation: Float,
    var rotationSpeed: Float,
    var size: Size,
    var alpha: Float = 1f,
    val color: Color,
)

data class ShatterEffect(
    val fragments: List<ShatterFragment>,
    var elapsed: Float = 0f,
    val duration: Float = 0.8f, // seconds
) {
    val isAlive: Boolean get() = elapsed < duration

    companion object {
        private val GRAVITY = 800f // px/s²

        fun create(tile: Tile, density: Float): ShatterEffect {
            val rng = Random(tile.instanceId)
            val w = Tile.WIDTH_DP * density
            val h = Tile.HEIGHT_DP * density
            val cx = tile.position.x
            val cy = tile.position.y

            val fragments = (0 until 8).map { i ->
                // Distribute fragments around the tile center
                val angle = (i * 45f + rng.nextFloat() * 30f - 15f) * (Math.PI.toFloat() / 180f)
                val speed = 200f + rng.nextFloat() * 300f
                val fragW = w * (0.2f + rng.nextFloat() * 0.3f)
                val fragH = h * (0.15f + rng.nextFloat() * 0.25f)

                ShatterFragment(
                    position = Offset(
                        cx + cos(angle) * w * 0.2f,
                        cy + sin(angle) * h * 0.2f
                    ),
                    velocity = Offset(cos(angle) * speed, sin(angle) * speed - 150f),
                    rotation = rng.nextFloat() * 360f,
                    rotationSpeed = (rng.nextFloat() - 0.5f) * 720f,
                    size = Size(fragW, fragH),
                    color = if (rng.nextFloat() > 0.3f) TileIvory else TileEdge,
                )
            }
            return ShatterEffect(fragments)
        }
    }

    fun update(dt: Float) {
        elapsed += dt
        val progress = (elapsed / duration).coerceIn(0f, 1f)

        for (frag in fragments) {
            frag.velocity = Offset(
                frag.velocity.x * 0.98f,
                frag.velocity.y + GRAVITY * dt
            )
            frag.position = Offset(
                frag.position.x + frag.velocity.x * dt,
                frag.position.y + frag.velocity.y * dt
            )
            frag.rotation += frag.rotationSpeed * dt
            frag.alpha = (1f - progress * progress).coerceAtLeast(0f)
        }
    }
}

fun DrawScope.drawShatterEffect(effect: ShatterEffect) {
    for (frag in effect.fragments) {
        if (frag.alpha <= 0f) continue
        rotate(frag.rotation, pivot = frag.position) {
            drawRoundRect(
                color = frag.color.copy(alpha = frag.alpha),
                topLeft = Offset(
                    frag.position.x - frag.size.width / 2f,
                    frag.position.y - frag.size.height / 2f
                ),
                size = frag.size,
                cornerRadius = CornerRadius(2f * density),
            )
        }
    }
}
