package com.mahjongslash.game.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mahjongslash.game.model.Tile
import com.mahjongslash.game.model.TileSuit
import com.mahjongslash.ui.theme.*

/**
 * Renders a Mahjong tile with the "Ink & Ivory" art style.
 *
 * The tile is drawn as a thick 3D-looking object with:
 * 1. Warm brown shadow (offset below-right)
 * 2. Side edge (visible bottom and right for depth)
 * 3. Ivory face with rounded corners
 * 4. Engraved symbol (slightly recessed look)
 * 5. Subtle border for definition
 */
object TileRenderer {

    fun DrawScope.drawTile(
        tile: Tile,
        textMeasurer: TextMeasurer,
        alpha: Float = tile.alpha
    ) {
        val d = density
        val w = Tile.WIDTH_DP * d
        val h = Tile.HEIGHT_DP * d
        val depth = Tile.DEPTH_DP * d
        val shadowOff = Tile.SHADOW_DP * d
        val cornerR = 6f * d
        val x = tile.position.x - w / 2f
        val y = tile.position.y - h / 2f

        // 1. Shadow
        drawRoundRect(
            color = TileShadow.copy(alpha = 0.5f * alpha),
            topLeft = Offset(x + shadowOff, y + shadowOff + depth),
            size = Size(w, h),
            cornerRadius = CornerRadius(cornerR),
        )

        // 2. Side edge (bottom face for 3D depth)
        drawRoundRect(
            color = TileEdgeDark.copy(alpha = alpha),
            topLeft = Offset(x + depth * 0.3f, y + depth),
            size = Size(w, h),
            cornerRadius = CornerRadius(cornerR),
        )

        // 3. Main face
        drawRoundRect(
            color = TileIvory.copy(alpha = alpha),
            topLeft = Offset(x, y),
            size = Size(w, h),
            cornerRadius = CornerRadius(cornerR),
        )

        // 4. Subtle inner border (engraved edge)
        drawRoundRect(
            color = TileEdge.copy(alpha = 0.4f * alpha),
            topLeft = Offset(x + 2f * d, y + 2f * d),
            size = Size(w - 4f * d, h - 4f * d),
            cornerRadius = CornerRadius(cornerR - 1f * d),
            style = Stroke(width = 1f * d)
        )

        // 5. Outer border for definition
        drawRoundRect(
            color = TileEdgeDark.copy(alpha = 0.6f * alpha),
            topLeft = Offset(x, y),
            size = Size(w, h),
            cornerRadius = CornerRadius(cornerR),
            style = Stroke(width = 0.8f * d)
        )

        // 6. Main face character
        val faceColor = when (tile.type.suit) {
            TileSuit.CHARACTERS -> Color(0xFF882222).copy(alpha = alpha) // Deep red
            TileSuit.DOTS -> Color(0xFF226644).copy(alpha = alpha)      // Deep green
            TileSuit.BAMBOO -> Color(0xFF224466).copy(alpha = alpha)    // Deep blue
            TileSuit.WIND -> InkBlack.copy(alpha = alpha)
            TileSuit.DRAGON -> when (tile.type.value) {
                1 -> AccentRed.copy(alpha = alpha)     // 中 is red
                2 -> Color(0xFF2D7D3A).copy(alpha = alpha) // 發 is green
                3 -> InkGrey.copy(alpha = alpha)       // 白 is grey/white
                else -> InkBlack.copy(alpha = alpha)
            }
        }

        val faceFontSize = if (tile.type.isHonor) 28.sp else 26.sp
        val faceStyle = TextStyle(
            color = faceColor,
            fontSize = faceFontSize,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )

        val faceResult = textMeasurer.measure(tile.type.face, faceStyle)
        val faceX = x + (w - faceResult.size.width) / 2f
        val faceY = if (tile.type.isHonor) {
            y + (h - faceResult.size.height) / 2f
        } else {
            y + h * 0.12f
        }

        drawText(faceResult, topLeft = Offset(faceX, faceY))

        // 7. Suit label (small, at bottom for suited tiles)
        if (tile.type.isSuited && tile.type.label.isNotEmpty()) {
            val labelStyle = TextStyle(
                color = InkGrey.copy(alpha = 0.7f * alpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            val labelResult = textMeasurer.measure(tile.type.label, labelStyle)
            val labelX = x + (w - labelResult.size.width) / 2f
            val labelY = y + h * 0.68f
            drawText(labelResult, topLeft = Offset(labelX, labelY))
        }

        // 8. Subtle top-left light reflection (gives the tile a slight gloss)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f * alpha),
            topLeft = Offset(x + 3f * d, y + 3f * d),
            size = Size(w * 0.4f, h * 0.15f),
            cornerRadius = CornerRadius(cornerR),
        )
    }
}
