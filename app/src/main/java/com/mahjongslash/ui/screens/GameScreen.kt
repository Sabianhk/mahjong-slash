package com.mahjongslash.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahjongslash.game.engine.FloatingText
import com.mahjongslash.game.engine.GamePhase
import com.mahjongslash.game.engine.GameState
import com.mahjongslash.game.render.TileRenderer.drawTile
import com.mahjongslash.game.render.drawShatterEffect
import com.mahjongslash.game.render.drawSlashTrail
import com.mahjongslash.ui.theme.*
import com.mahjongslash.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()

    // Game loop — runs every frame
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            val frameTime = withFrameNanos { it }
            val dt = ((frameTime - lastFrameTime) / 1_000_000_000.0).toFloat()
                .coerceAtMost(0.05f)
            lastFrameTime = frameTime
            viewModel.update(dt)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Game canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    viewModel.initializeIfNeeded(
                        size.width.toFloat(),
                        size.height.toFloat(),
                        density
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            viewModel.onSlashStart(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            viewModel.onSlashMove(change.position)
                        },
                        onDragEnd = {
                            viewModel.onSlashEndAtLastPosition()
                        },
                        onDragCancel = {
                            viewModel.onSlashEndAtLastPosition()
                        }
                    )
                }
        ) {
            // Draw rice paper grain texture
            drawBackground()

            // Draw slash trails (behind tiles)
            for (trail in state.slashTrails) {
                drawSlashTrail(trail)
            }

            // Draw live tiles
            for (tile in state.tiles) {
                drawTile(tile, textMeasurer)
            }

            // DEBUG: Draw tile hitbox outlines (temporary — remove after validation)
            val dbgDensity = state.debugDensity
            for (tile in state.tiles) {
                val hb = tile.hitBounds(dbgDensity)
                drawRect(
                    color = Color.Cyan.copy(alpha = 0.4f),
                    topLeft = Offset(hb.left, hb.top),
                    size = androidx.compose.ui.geometry.Size(hb.width, hb.height),
                    style = Stroke(width = 2f)
                )
                // Tile center marker
                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.7f),
                    radius = 4f,
                    center = tile.position,
                )
            }

            // DEBUG: Draw last swipe path (temporary — remove after validation)
            if (state.debugLastPath.size >= 2) {
                for (i in 0 until state.debugLastPath.size - 1) {
                    drawLine(
                        color = Color.Yellow.copy(alpha = 0.8f),
                        start = state.debugLastPath[i],
                        end = state.debugLastPath[i + 1],
                        strokeWidth = 3f,
                    )
                }
                // Mark start and end points
                drawCircle(
                    color = Color.Green,
                    radius = 8f,
                    center = state.debugLastPath.first(),
                )
                drawCircle(
                    color = Color.Red,
                    radius = 8f,
                    center = state.debugLastPath.last(),
                )
            }

            // Draw shatter effects (on top of tiles)
            for (effect in state.shatterEffects) {
                drawShatterEffect(effect)
            }

            // Draw floating score/feedback texts
            for (ft in state.floatingTexts) {
                drawFloatingText(ft, textMeasurer)
            }

            // Draw screen flash overlay
            if (state.flashAlpha > 0f) {
                drawRect(
                    color = state.flashColor.copy(alpha = state.flashAlpha),
                )
            }
        }

        // HUD overlay
        GameHud(state = state, modifier = Modifier.statusBarsPadding())

        // Debug overlay (temporary — remove after validation)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            if (state.debugAutoSlash.isNotEmpty()) {
                Text(
                    text = state.debugAutoSlash,
                    style = TextStyle(
                        color = AccentGold.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
            if (state.debugLastSlash.isNotEmpty()) {
                Text(
                    text = state.debugLastSlash,
                    style = TextStyle(
                        color = WarmWhite.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
        }

        // Auto-slash debug button (temporary — remove after validation)
        if (state.phase == GamePhase.PLAYING) {
            Text(
                text = "⚡AUTO",
                style = TextStyle(
                    color = AccentGold.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clickable { viewModel.triggerAutoSlash() }
                    .background(BackgroundDark.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Game over overlay
        if (state.phase == GamePhase.GAME_OVER) {
            GameOverOverlay(
                score = state.score,
                onRestart = {
                    viewModel.restart(state.screenWidth, state.screenHeight, density)
                }
            )
        }
    }
}

private fun DrawScope.drawBackground() {
    drawRect(BackgroundDark)

    val grainColor = TileIvory.copy(alpha = 0.015f)
    val step = 12f * density
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            drawCircle(
                color = grainColor,
                radius = 0.5f * density,
                center = Offset(x, y)
            )
            x += step
        }
        y += step
    }
}

private fun DrawScope.drawFloatingText(
    ft: FloatingText,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    if (ft.alpha <= 0f) return

    val style = TextStyle(
        color = ft.color.copy(alpha = ft.alpha),
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
    )

    val layoutResult = textMeasurer.measure(ft.text, style)
    val x = ft.position.x - layoutResult.size.width / 2f
    val y = ft.position.y + ft.driftY - layoutResult.size.height / 2f

    drawText(
        textLayoutResult = layoutResult,
        topLeft = Offset(x, y),
    )
}

@Composable
private fun GameHud(state: GameState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Top-left: Score + Combo
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = "${state.score}",
                style = TextStyle(
                    color = AccentGold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                ),
            )
            if (state.combo > 1) {
                Text(
                    text = "×${comboMultiplierText(state.combo)} COMBO",
                    style = TextStyle(
                        color = AccentRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // Top-right: Blade health (drawn as small tile shapes)
        Row(modifier = Modifier.align(Alignment.TopEnd)) {
            for (i in 0 until 5) {
                val isAlive = i < state.bladeHealth
                Canvas(
                    modifier = Modifier
                        .width(12.dp)
                        .height(18.dp)
                        .padding(horizontal = 2.dp)
                ) {
                    if (isAlive) {
                        drawRoundRect(
                            color = TileIvory,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                        )
                    } else {
                        drawRoundRect(
                            color = InkGrey.copy(alpha = 0.25f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, onRestart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            onRestart()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "終",
                style = TextStyle(
                    color = AccentRed,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = "GAME OVER",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
            )
            Text(
                text = "$score",
                style = TextStyle(
                    color = AccentGold,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                ),
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "tap to play again",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.3f),
                    fontSize = 14.sp,
                ),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

private fun comboMultiplierText(combo: Int): String = when {
    combo <= 1 -> "1.0"
    combo == 2 -> "1.5"
    combo == 3 -> "2.0"
    combo == 4 -> "2.5"
    else -> "3.0"
}
