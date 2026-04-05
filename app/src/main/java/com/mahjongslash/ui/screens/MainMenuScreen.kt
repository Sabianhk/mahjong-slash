package com.mahjongslash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahjongslash.data.PreferencesManager
import com.mahjongslash.ui.theme.*

@Composable
fun MainMenuScreen(
    preferencesManager: PreferencesManager,
    onPlay: () -> Unit,
    onHighScores: () -> Unit,
    onSettings: () -> Unit,
) {
    val lastScore by preferencesManager.lastScore.collectAsState(initial = 0)
    val highScores by preferencesManager.highScores.collectAsState(initial = emptyList())
    val highScore = highScores.firstOrNull() ?: 0

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
            // Title
            Text(
                text = "切牌",
                style = TextStyle(
                    color = WarmWhite,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                ),
            )

            // High score
            if (highScore > 0) {
                Text(
                    text = "BEST: $highScore",
                    style = TextStyle(
                        color = AccentGold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (lastScore > 0 && lastScore != highScore) {
                Text(
                    text = "LAST: $lastScore",
                    style = TextStyle(
                        color = WarmWhite.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                    ),
                )
            }

            // Tile-shaped buttons
            TileButton(text = "遊 PLAY", onClick = onPlay)
            TileButton(text = "記 SCORES", onClick = onHighScores)
            TileButton(text = "設 SETTINGS", onClick = onSettings)
        }
    }
}

@Composable
private fun TileButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .background(TileIvory, shape = RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = BackgroundDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}
