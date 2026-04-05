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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    viewModel: GameViewModel = viewModel(),
    onGameOver: (score: Int, tilesCleared: Int, maxCombo: Int, totalSlashes: Int, validSlashes: Int) -> Unit = { _, _, _, _, _ -> },
) {
    val state by viewModel.state.collectAsState()
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current
    viewModel.hapticView = view

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
            // Draw background outside shake (stays stable)
            val bgBitmap = viewModel.bgRenderer?.cachedBackground
            if (bgBitmap != null) {
                drawImage(bgBitmap)
            } else {
                drawRect(BackgroundDark)
            }

            // Apply screen shake offset to all game elements
            translate(left = state.shakeOffsetX, top = state.shakeOffsetY) {
                // Draw slash trails (behind tiles)
                for (trail in state.slashTrails) {
                    drawSlashTrail(trail)
                }

                // Draw hint glow behind hinted tiles
                if (state.hintTileIds.isNotEmpty()) {
                    for (tile in state.tiles) {
                        if (tile.instanceId in state.hintTileIds) {
                            val w = com.mahjongslash.game.model.Tile.WIDTH_DP * density
                            val h = com.mahjongslash.game.model.Tile.HEIGHT_DP * density
                            val glowPad = 8f * density
                            drawRoundRect(
                                color = AccentGold.copy(alpha = 0.4f),
                                topLeft = Offset(
                                    tile.position.x - w / 2f - glowPad,
                                    tile.position.y - h / 2f - glowPad
                                ),
                                size = androidx.compose.ui.geometry.Size(w + glowPad * 2, h + glowPad * 2),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * density),
                            )
                        }
                    }
                }

                // Draw live tiles
                for (tile in state.tiles) {
                    drawTile(tile)
                }

                // Draw shatter effects (on top of tiles)
                for (effect in state.shatterEffects) {
                    drawShatterEffect(effect)
                }

                // Draw floating score/feedback texts
                for (ft in state.floatingTexts) {
                    drawFloatingText(ft, textMeasurer)
                }
            }

            // Danger vignette — intensifies as blade health drops
            val maxHealth = 3
            if (state.bladeHealth < maxHealth) {
                val intensity = 1f - (state.bladeHealth.toFloat() / maxHealth)
                val vignetteAlpha = intensity * 0.6f // max 60% opacity at 0 health
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = maxOf(size.width, size.height) * 0.7f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            AccentRed.copy(alpha = vignetteAlpha),
                        ),
                        center = center,
                        radius = radius,
                    ),
                )
            }

            // Draw screen flash overlay (outside shake — full screen)
            if (state.flashAlpha > 0f) {
                drawRect(
                    color = state.flashColor.copy(alpha = state.flashAlpha),
                )
            }
        }

        // HUD overlay
        GameHud(
            state = state,
            onPause = { viewModel.pause() },
            modifier = Modifier.statusBarsPadding()
        )

        // Pause overlay
        if (state.phase == GamePhase.PAUSED) {
            PauseOverlay(
                onResume = { viewModel.resume() },
                onRestart = { viewModel.restart(state.screenWidth, state.screenHeight, density) }
            )
        }

        // Navigate to game over screen
        if (state.phase == GamePhase.GAME_OVER) {
            LaunchedEffect(Unit) {
                onGameOver(
                    state.score,
                    state.tilesCleared,
                    state.maxCombo,
                    state.totalSlashes,
                    state.validSlashes,
                )
            }
        }
    }
}

private fun DrawScope.drawFloatingText(
    ft: FloatingText,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    if (ft.alpha <= 0f) return

    val scaledSize = 28.sp * ft.currentScale
    val style = TextStyle(
        color = ft.color.copy(alpha = ft.alpha),
        fontSize = scaledSize,
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
private fun GameHud(state: GameState, onPause: () -> Unit, modifier: Modifier = Modifier) {
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

        // Top-right: Blade health + Pause button
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until 3) {
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

            // Pause button — red seal stamp 止
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(36.dp)
                    .background(AccentRed.copy(alpha = 0.85f), shape = androidx.compose.foundation.shape.CircleShape)
                    .clickable { onPause() }
            ) {
                Text(
                    text = "止",
                    style = TextStyle(
                        color = WarmWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onMenu: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "暫停",
                style = TextStyle(
                    color = WarmWhite,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = "PAUSED",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            // Resume button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 40.dp)
                    .background(TileIvory, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable { onResume() }
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "續 RESUME",
                    style = TextStyle(
                        color = BackgroundDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // Restart button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .background(AccentRed.copy(alpha = 0.8f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable { onRestart() }
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "再 RESTART",
                    style = TextStyle(
                        color = WarmWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
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
