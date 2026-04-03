package com.mahjongslash.game.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.mahjongslash.game.model.Tile
import com.mahjongslash.ui.theme.AccentGold
import com.mahjongslash.ui.theme.TileEdge
import com.mahjongslash.ui.theme.TileIvory
import com.mahjongslash.ui.theme.WarmWhite
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
    val duration: Float = 1.0f, // seconds — slightly longer for visibility
) {
    val isAlive: Boolean get() = elapsed < duration

    companion object {
        private val GRAVITY = 600f // px/s² — slower fall so fragments stay visible longer

        fun create(tile: Tile, density: Float): ShatterEffect {
            val rng = Random(tile.instanceId)
            val w = Tile.WIDTH_DP * density
            val h = Tile.HEIGHT_DP * density
            val cx = tile.position.x
            val cy = tile.position.y

            val fragmentCount = 12 // More fragments for dramatic burst
            val fragments = (0 until fragmentCount).map { i ->
                val angle = (i * (360f / fragmentCount) + rng.nextFloat() * 20f - 10f) *
                    (Math.PI.toFloat() / 180f)
                val speed = 250f + rng.nextFloat() * 400f
                val fragW = w * (0.2f + rng.nextFloat() * 0.35f)
                val fragH = h * (0.15f + rng.nextFloat() * 0.3f)

                val r = rng.nextFloat()
                val color = when {
                    r < 0.2f -> AccentGold   // Gold sparks for flair
                    r < 0.4f -> WarmWhite    // Bright white pieces
                    r < 0.7f -> TileIvory    // Standard ivory
                    else -> TileEdge         // Edge color
                }

                ShatterFragment(
                    position = Offset(
                        cx + cos(angle) * w * 0.2f,
                        cy + sin(angle) * h * 0.2f
                    ),
                    velocity = Offset(cos(angle) * speed, sin(angle) * speed - 200f),
                    rotation = rng.nextFloat() * 360f,
                    rotationSpeed = (rng.nextFloat() - 0.5f) * 720f,
                    size = Size(fragW, fragH),
                    color = color,
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
