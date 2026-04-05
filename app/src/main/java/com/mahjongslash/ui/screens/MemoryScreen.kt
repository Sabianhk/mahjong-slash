package com.mahjongslash.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahjongslash.game.engine.MemoryCard
import com.mahjongslash.game.engine.MemoryDifficulty
import com.mahjongslash.game.engine.MemoryEngine
import com.mahjongslash.game.engine.MemoryPhase
import com.mahjongslash.game.engine.MemoryState
import com.mahjongslash.ui.theme.*

@Composable
fun MemoryDifficultyScreen(
    onSelectDifficulty: (MemoryDifficulty) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "憶牌",
                style = TextStyle(
                    color = WarmWhite,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                ),
            )
            Text(
                text = "TILE MEMORY",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            for (diff in MemoryDifficulty.entries) {
                val gridInfo = "${diff.cols}×${diff.rows} — ${(diff.cols * diff.rows) / 2} pairs"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .background(TileIvory, shape = RoundedCornerShape(8.dp))
                        .clickable { onSelectDifficulty(diff) }
                        .padding(vertical = 14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${diff.kanji} ${diff.label}",
                            style = TextStyle(
                                color = BackgroundDark,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            ),
                        )
                        Text(
                            text = gridInfo,
                            style = TextStyle(
                                color = InkBrown.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .background(InkBrown.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "戻 BACK",
                    style = TextStyle(
                        color = WarmWhite.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
fun MemoryGameScreen(
    difficulty: MemoryDifficulty,
    onWin: (score: Int, moves: Int, timeMs: Long, difficulty: MemoryDifficulty) -> Unit,
    onBack: () -> Unit,
) {
    val engine = remember { MemoryEngine().apply { initialize(difficulty) } }
    var state by remember { mutableStateOf(MemoryState()) }

    // Game loop
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            val frameTime = withFrameNanos { it }
            val dt = ((frameTime - lastFrameTime) / 1_000_000_000.0).toFloat()
                .coerceAtMost(0.1f)
            lastFrameTime = frameTime
            state = engine.update(dt)
        }
    }

    // Navigate on win
    LaunchedEffect(state.phase) {
        if (state.phase == MemoryPhase.WON) {
            kotlinx.coroutines.delay(800) // brief pause to see final match
            onWin(engine.calculateScore(), state.moves, state.elapsedMs, state.difficulty)
        }
    }

    val timeStr = formatTime(state.elapsedMs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${state.matches}/${state.totalPairs}",
                        style = TextStyle(
                            color = AccentGold,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                    Text(
                        text = "PAIRS",
                        style = TextStyle(
                            color = WarmWhite.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                        ),
                    )
                }

                Text(
                    text = timeStr,
                    style = TextStyle(
                        color = WarmWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${state.moves}",
                        style = TextStyle(
                            color = WarmWhite,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                    Text(
                        text = "MOVES",
                        style = TextStyle(
                            color = WarmWhite.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (row in 0 until state.rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (col in 0 until state.cols) {
                                val idx = row * state.cols + col
                                if (idx < state.cards.size) {
                                    MemoryCardView(
                                        card = state.cards[idx],
                                        onClick = { engine.onCardTap(idx) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(InkBrown.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "戻 QUIT",
                    style = TextStyle(
                        color = WarmWhite.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Flip animation
    val flipProgress = remember { Animatable(0f) }
    LaunchedEffect(card.faceUp, card.matched) {
        val target = if (card.faceUp || card.matched) 1f else 0f
        flipProgress.animateTo(target, animationSpec = tween(250))
    }

    val isShowingFace = flipProgress.value > 0.5f
    // ScaleX simulates card flip: 1 -> 0 -> 1
    val scaleX = if (flipProgress.value <= 0.5f) {
        1f - flipProgress.value * 2f
    } else {
        (flipProgress.value - 0.5f) * 2f
    }

    val bgColor = when {
        card.matched -> AccentGold.copy(alpha = 0.15f)
        isShowingFace -> TileIvory
        else -> InkBrown
    }

    val matchedAlpha = if (card.matched) 0.6f else 1f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(0.75f) // mahjong tile proportions
            .alpha(matchedAlpha)
            .graphicsLayer {
                this.scaleX = scaleX.coerceAtLeast(0.01f)
            }
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(enabled = !card.faceUp && !card.matched) { onClick() }
    ) {
        if (isShowingFace) {
            // Show tile face
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = card.tileType.face,
                    style = TextStyle(
                        color = if (card.tileType.suit == com.mahjongslash.game.model.TileSuit.DRAGON)
                            AccentRed else BackgroundDark,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                    ),
                    textAlign = TextAlign.Center,
                )
                if (card.tileType.label.isNotEmpty()) {
                    Text(
                        text = card.tileType.label,
                        style = TextStyle(
                            color = InkBrown.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        } else {
            // Face-down — show pattern
            Text(
                text = "牌",
                style = TextStyle(
                    color = TileEdge.copy(alpha = 0.4f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
        }
    }
}

@Composable
fun MemoryResultScreen(
    score: Int,
    moves: Int,
    timeMs: Long,
    difficulty: MemoryDifficulty,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
) {
    val timeStr = formatTime(timeMs)
    val totalPairs = (difficulty.cols * difficulty.rows) / 2
    val perfectMoves = totalPairs
    val efficiency = if (moves > 0) (perfectMoves * 100 / moves) else 0

    val rank = when {
        efficiency >= 90 -> "達" to "MASTER"
        efficiency >= 70 -> "上" to "SKILLED"
        efficiency >= 50 -> "良" to "GOOD"
        else -> "初" to "NOVICE"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Rank
            Text(
                text = rank.first,
                style = TextStyle(
                    color = AccentRed,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = rank.second,
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Score
            Text(
                text = "$score",
                style = TextStyle(
                    color = AccentGold,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                StatColumn(value = "$moves", label = "MOVES")
                StatColumn(value = timeStr, label = "TIME")
                StatColumn(value = "$efficiency%", label = "EFF")
            }

            Text(
                text = "${difficulty.kanji} ${difficulty.label}",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Play Again
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .background(TileIvory, shape = RoundedCornerShape(8.dp))
                    .clickable { onPlayAgain() }
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    text = "再 PLAY AGAIN",
                    style = TextStyle(
                        color = BackgroundDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // Menu
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .background(InkBrown.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    .clickable { onMenu() }
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    text = "選 MENU",
                    style = TextStyle(
                        color = WarmWhite.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TextStyle(
                color = WarmWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            ),
        )
        Text(
            text = label,
            style = TextStyle(
                color = WarmWhite.copy(alpha = 0.4f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            ),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return if (mins > 0) "${mins}:${secs.toString().padStart(2, '0')}"
    else "${secs}s"
}
