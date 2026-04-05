package com.mahjongslash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahjongslash.ui.components.AppBackground
import com.mahjongslash.ui.theme.*

@Composable
fun GameOverScreen(
    score: Int,
    tilesCleared: Int,
    maxCombo: Int,
    accuracy: Int,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
) {
    val grade = gradeForScore(score)

    AppBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Grade stamp
            Text(
                text = grade.kanji,
                style = TextStyle(
                    color = AccentRed,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = grade.english,
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            // Final score
            Text(
                text = "$score",
                style = TextStyle(
                    color = AccentGold,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                ),
                modifier = Modifier.padding(top = 24.dp)
            )

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("TILES", "$tilesCleared")
                StatItem("COMBO", "×$maxCombo")
                StatItem("ACC", "$accuracy%")
            }

            // Buttons
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 40.dp)
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
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(0.6f)
                    .background(BackgroundMid, shape = RoundedCornerShape(8.dp))
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
                    textAlign = TextAlign.Center,
                )
            }
        }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
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
                color = InkGrey,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
        )
    }
}

private data class Grade(val kanji: String, val english: String)

private fun gradeForScore(score: Int): Grade = when {
    score >= 5000 -> Grade("神", "DIVINE")
    score >= 3000 -> Grade("極", "EXTREME")
    score >= 1500 -> Grade("優", "EXCELLENT")
    score >= 500 -> Grade("良", "GOOD")
    else -> Grade("初", "NOVICE")
}
