package com.mahjongslash.game.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.mahjongslash.game.model.Tile
import com.mahjongslash.game.pool.ObjectPool
import com.mahjongslash.ui.theme.AccentGold
import com.mahjongslash.ui.theme.TileEdge
import com.mahjongslash.ui.theme.TileIvory
import com.mahjongslash.ui.theme.WarmWhite
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mutable shatter fragment — recycled via ObjectPool.
 */
class ShatterFragment {
    var posX = 0f
    var posY = 0f
    var velX = 0f
    var velY = 0f
    var rotation = 0f
    var rotationSpeed = 0f
    var sizeW = 0f
    var sizeH = 0f
    var alpha = 1f
    var colorInt = 0xFFF2E8D5.toInt()
    var active = false

    fun reset() {
        posX = 0f; posY = 0f; velX = 0f; velY = 0f
        rotation = 0f; rotationSpeed = 0f
        sizeW = 0f; sizeH = 0f; alpha = 1f
        active = false
    }
}

/**
 * Shatter effect using pooled fragments. No allocations during gameplay.
 */
class ShatterEffect private constructor() {
    val fragments = Array(FRAGMENTS_PER_EFFECT) { ShatterFragment() }
    var activeCount = 0
    var elapsed = 0f
    var duration = 1.0f
    var alive = false

    val isAlive: Boolean get() = alive && elapsed < duration

    companion object {
        private const val FRAGMENTS_PER_EFFECT = 12
        private const val GRAVITY = 600f
        private const val MAX_POOLED_EFFECTS = 20

        // Pre-allocated color ints to avoid Color.copy() in draw loop
        private val COLORS = intArrayOf(
            0xFFC49B40.toInt(), // AccentGold
            0xFFE8DCC8.toInt(), // WarmWhite
            0xFFF2E8D5.toInt(), // TileIvory
            0xFFC4B49A.toInt(), // TileEdge
        )

        private val pool = ObjectPool<ShatterEffect>(
            capacity = MAX_POOLED_EFFECTS,
            factory = { ShatterEffect() },
            reset = { effect ->
                effect.elapsed = 0f
                effect.alive = false
                effect.activeCount = 0
                for (frag in effect.fragments) frag.reset()
            }
        )

        fun create(tile: Tile, density: Float): ShatterEffect {
            val effect = pool.obtain() ?: ShatterEffect()
            effect.elapsed = 0f
            effect.duration = 1.0f
            effect.alive = true

            val rng = Random(tile.instanceId)
            val w = Tile.WIDTH_DP * density
            val h = Tile.HEIGHT_DP * density
            val cx = tile.position.x
            val cy = tile.position.y

            effect.activeCount = FRAGMENTS_PER_EFFECT
            for (i in 0 until FRAGMENTS_PER_EFFECT) {
                val frag = effect.fragments[i]
                val angle = (i * (360f / FRAGMENTS_PER_EFFECT) + rng.nextFloat() * 20f - 10f) *
                    (Math.PI.toFloat() / 180f)
                val speed = 250f + rng.nextFloat() * 400f

                frag.posX = cx + cos(angle) * w * 0.2f
                frag.posY = cy + sin(angle) * h * 0.2f
                frag.velX = cos(angle) * speed
                frag.velY = sin(angle) * speed - 200f
                frag.rotation = rng.nextFloat() * 360f
                frag.rotationSpeed = (rng.nextFloat() - 0.5f) * 720f
                frag.sizeW = w * (0.2f + rng.nextFloat() * 0.35f)
                frag.sizeH = h * (0.15f + rng.nextFloat() * 0.3f)
                frag.alpha = 1f
                frag.colorInt = COLORS[(rng.nextFloat() * COLORS.size).toInt().coerceIn(0, COLORS.size - 1)]
                frag.active = true
            }

            return effect
        }

        fun recycle(effect: ShatterEffect) {
            pool.recycle(effect)
        }
    }

    fun update(dt: Float) {
        elapsed += dt
        val progress = (elapsed / duration).coerceIn(0f, 1f)

        for (i in 0 until activeCount) {
            val frag = fragments[i]
            if (!frag.active) continue
            frag.velX *= 0.98f
            frag.velY += GRAVITY * dt
            frag.posX += frag.velX * dt
            frag.posY += frag.velY * dt
            frag.rotation += frag.rotationSpeed * dt
            frag.alpha = (1f - progress * progress).coerceAtLeast(0f)
        }
    }
}

// Pre-allocated objects for draw loop — avoids per-frame allocation
private val drawOffset = Offset(0f, 0f)
private val drawSize = Size(0f, 0f)

fun DrawScope.drawShatterEffect(effect: ShatterEffect) {
    for (i in 0 until effect.activeCount) {
        val frag = effect.fragments[i]
        if (!frag.active || frag.alpha <= 0f) continue

        val pivot = Offset(frag.posX, frag.posY)
        rotate(frag.rotation, pivot = pivot) {
            drawRoundRect(
                color = Color(frag.colorInt).copy(alpha = frag.alpha),
                topLeft = Offset(frag.posX - frag.sizeW / 2f, frag.posY - frag.sizeH / 2f),
                size = Size(frag.sizeW, frag.sizeH),
                cornerRadius = CornerRadius(2f * density),
            )
        }
    }
}
