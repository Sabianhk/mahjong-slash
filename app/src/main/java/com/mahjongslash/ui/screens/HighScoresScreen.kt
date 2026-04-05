package com.mahjongslash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
fun HighScoresScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
) {
    val scores by preferencesManager.highScores.collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            Text(
                text = "記録",
                style = TextStyle(
                    color = WarmWhite,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = "HIGH SCORES",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (scores.isEmpty()) {
                Text(
                    text = "No scores yet",
                    style = TextStyle(
                        color = InkGrey,
                        fontSize = 16.sp,
                    ),
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                for ((index, score) in scores.withIndex()) {
                    val isTop = index == 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = TextStyle(
                                color = if (isTop) AccentGold else InkGrey,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.weight(0.15f),
                        )
                        Text(
                            text = "$score",
                            style = TextStyle(
                                color = if (isTop) AccentGold else WarmWhite,
                                fontSize = if (isTop) 28.sp else 22.sp,
                                fontWeight = FontWeight.Black,
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.85f),
                        )
                    }
                }
            }

            // Back button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 40.dp)
                    .fillMaxWidth(0.5f)
                    .background(BackgroundMid, shape = RoundedCornerShape(8.dp))
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
