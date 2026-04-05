package com.mahjongslash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahjongslash.data.PreferencesManager
import com.mahjongslash.ui.components.AppBackground
import com.mahjongslash.ui.theme.*

@Composable
fun MainMenuScreen(
    preferencesManager: PreferencesManager,
    onPlay: () -> Unit,
    onMemory: () -> Unit = {},
    onHighScores: () -> Unit,
    onSettings: () -> Unit,
) {
    val lastScore by preferencesManager.lastScore.collectAsState(initial = 0)
    val highScores by preferencesManager.highScores.collectAsState(initial = emptyList())
    val highScore = highScores.firstOrNull() ?: 0

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar — score badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (highScore > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                AccentGold.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚡ $highScore",
                            style = TextStyle(
                                color = BackgroundDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Title area
            Text(
                text = "切牌",
                style = TextStyle(
                    color = WarmWhite,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                ),
            )

            if (highScore > 0) {
                Text(
                    text = "BEST: $highScore",
                    style = TextStyle(
                        color = AccentGold.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (lastScore > 0 && lastScore != highScore) {
                Text(
                    text = "LAST: $lastScore",
                    style = TextStyle(
                        color = WarmWhite.copy(alpha = 0.35f),
                        fontSize = 12.sp,
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.12f))

            // Primary game mode buttons — prominent cards
            GameModeButton(
                kanji = "遊",
                label = "SLASH",
                icon = "⚔",
                isPrimary = true,
                onClick = onPlay,
            )

            Spacer(modifier = Modifier.height(12.dp))

            GameModeButton(
                kanji = "憶",
                label = "MEMORY",
                icon = "🀄",
                isPrimary = false,
                onClick = onMemory,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Secondary buttons — subtler, glass-morphism style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton(
                    kanji = "記",
                    label = "SCORES",
                    onClick = onHighScores,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    kanji = "設",
                    label = "SETTINGS",
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

@Composable
private fun GameModeButton(
    kanji: String,
    label: String,
    icon: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isPrimary) TileIvory else Color.White
    val bgAlpha = if (isPrimary) 0.95f else 0.88f
    val borderColor = if (isPrimary) AccentGold.copy(alpha = 0.4f) else TileEdge.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor.copy(alpha = bgAlpha))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Kanji + label
            Text(
                text = kanji,
                style = TextStyle(
                    color = BackgroundDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = "  $label",
                style = TextStyle(
                    color = BackgroundDark.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
            )

            Spacer(modifier = Modifier.weight(1f))

            // Icon badge
            Text(
                text = icon,
                style = TextStyle(
                    fontSize = 22.sp,
                ),
            )
        }
    }
}

@Composable
private fun SecondaryButton(
    kanji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark.copy(alpha = 0.7f))
            .border(1.dp, AccentGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = kanji,
                style = TextStyle(
                    color = AccentGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = label,
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
            )
        }
    }
}
