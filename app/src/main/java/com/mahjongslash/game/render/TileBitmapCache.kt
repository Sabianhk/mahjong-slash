package com.mahjongslash.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.mahjongslash.game.model.Tile
import com.mahjongslash.game.model.TileSet
import com.mahjongslash.game.model.TileSuit
import com.mahjongslash.game.model.TileType

/**
 * Pre-renders all 34 tile types into ImageBitmap at startup.
 * During gameplay, tiles are drawn with a single drawImage() call
 * instead of re-drawing faces/text per frame.
 */
class TileBitmapCache(private val density: Float) {

    private val cache = mutableMapOf<Int, ImageBitmap>()

    // Pre-allocated Paint objects (reused across renders)
    private val shadowPaint = Paint().apply {
        color = 0x803D3228.toInt() // TileShadow at 50% alpha
        isAntiAlias = true
    }
    private val edgePaint = Paint().apply {
        color = 0xFFA89878.toInt() // TileEdgeDark
        isAntiAlias = true
    }
    private val facePaint = Paint().apply {
        color = 0xFFF2E8D5.toInt() // TileIvory
        isAntiAlias = true
    }
    private val innerBorderPaint = Paint().apply {
        color = 0x66C4B49A.toInt() // TileEdge at 40%
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        isAntiAlias = true
    }
    private val outerBorderPaint = Paint().apply {
        color = 0x99A89878.toInt() // TileEdgeDark at 60%
        style = Paint.Style.STROKE
        strokeWidth = 0.8f * density
        isAntiAlias = true
    }
    private val glossPaint = Paint().apply {
        color = 0x14FFFFFF.toInt() // White at 8%
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        color = 0xB36B5D4F.toInt() // InkGrey at 70%
        textSize = 12f * density
    }

    // Pre-allocated RectF for drawing (reused)
    private val rectF = RectF()

    fun initialize() {
        cache.clear()
        for (tileType in TileSet.allTypes) {
            cache[tileType.id] = renderTileBitmap(tileType)
        }
    }

    fun get(tileType: TileType): ImageBitmap? = cache[tileType.id]

    private fun renderTileBitmap(type: TileType): ImageBitmap {
        val w = Tile.WIDTH_DP * density
        val h = Tile.HEIGHT_DP * density
        val depth = Tile.DEPTH_DP * density
        val shadowOff = Tile.SHADOW_DP * density
        val cornerR = 6f * density

        // Bitmap large enough for shadow + depth offset
        val bmpW = (w + shadowOff + depth).toInt() + 2
        val bmpH = (h + shadowOff + depth).toInt() + 2
        val bitmap = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Origin offset so tile face is at (0,0) and shadow/depth extend below-right
        val ox = 0f
        val oy = 0f

        // 1. Shadow
        rectF.set(ox + shadowOff, oy + shadowOff + depth, ox + shadowOff + w, oy + shadowOff + depth + h)
        canvas.drawRoundRect(rectF, cornerR, cornerR, shadowPaint)

        // 2. Side edge (3D depth)
        rectF.set(ox + depth * 0.3f, oy + depth, ox + depth * 0.3f + w, oy + depth + h)
        canvas.drawRoundRect(rectF, cornerR, cornerR, edgePaint)

        // 3. Main face
        rectF.set(ox, oy, ox + w, oy + h)
        canvas.drawRoundRect(rectF, cornerR, cornerR, facePaint)

        // 4. Inner border
        rectF.set(ox + 2f * density, oy + 2f * density, ox + w - 2f * density, oy + h - 2f * density)
        canvas.drawRoundRect(rectF, cornerR - 1f * density, cornerR - 1f * density, innerBorderPaint)

        // 5. Outer border
        rectF.set(ox, oy, ox + w, oy + h)
        canvas.drawRoundRect(rectF, cornerR, cornerR, outerBorderPaint)

        // 6. Main face character
        textPaint.color = faceColor(type)
        textPaint.textSize = if (type.isHonor) 28f * density else 26f * density

        val textX = ox + w / 2f
        val textY = if (type.isHonor) {
            // Center vertically
            val metrics = textPaint.fontMetrics
            oy + h / 2f - (metrics.ascent + metrics.descent) / 2f
        } else {
            // Upper portion
            val metrics = textPaint.fontMetrics
            oy + h * 0.12f - metrics.ascent
        }
        canvas.drawText(type.face, textX, textY, textPaint)

        // 7. Suit label (small, at bottom for suited tiles)
        if (type.isSuited && type.label.isNotEmpty()) {
            val labelX = ox + w / 2f
            val labelMetrics = labelPaint.fontMetrics
            val labelY = oy + h * 0.68f - labelMetrics.ascent
            canvas.drawText(type.label, labelX, labelY, labelPaint)
        }

        // 8. Top-left gloss reflection
        rectF.set(ox + 3f * density, oy + 3f * density, ox + w * 0.4f + 3f * density, oy + h * 0.15f + 3f * density)
        canvas.drawRoundRect(rectF, cornerR, cornerR, glossPaint)

        return bitmap.asImageBitmap()
    }

    private fun faceColor(type: TileType): Int = when (type.suit) {
        TileSuit.CHARACTERS -> 0xFF882222.toInt()
        TileSuit.DOTS -> 0xFF226644.toInt()
        TileSuit.BAMBOO -> 0xFF224466.toInt()
        TileSuit.WIND -> 0xFF2C2419.toInt() // InkBlack
        TileSuit.DRAGON -> when (type.value) {
            1 -> 0xFFB44033.toInt() // AccentRed
            2 -> 0xFF2D7D3A.toInt() // Green
            3 -> 0xFF6B5D4F.toInt() // InkGrey
            else -> 0xFF2C2419.toInt()
        }
    }
}
