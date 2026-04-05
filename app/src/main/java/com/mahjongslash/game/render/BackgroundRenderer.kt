package com.mahjongslash.game.render

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Pre-renders the rice paper grain background texture into an ImageBitmap.
 * During gameplay, a single drawImage() call blits the cached texture.
 */
class BackgroundRenderer(private val density: Float) {

    var cachedBackground: ImageBitmap? = null
        private set

    // Pre-allocated paints
    private val bgPaint = Paint().apply {
        color = 0xFF1A1714.toInt() // BackgroundDark
        style = Paint.Style.FILL
    }
    private val grainPaint = Paint().apply {
        color = 0x04F2E8D5.toInt() // TileIvory at ~1.5% alpha
        isAntiAlias = true
    }

    fun initialize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw rice paper grain dots
        val step = 12f * density
        val radius = 0.5f * density
        var y = 0f
        while (y < height) {
            var x = 0f
            while (x < width) {
                canvas.drawCircle(x, y, radius, grainPaint)
                x += step
            }
            y += step
        }

        cachedBackground = bitmap.asImageBitmap()
    }
}
