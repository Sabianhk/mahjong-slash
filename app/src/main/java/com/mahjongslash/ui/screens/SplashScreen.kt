package com.mahjongslash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahjongslash.ui.components.AppBackground
import com.mahjongslash.ui.components.TitleSlashEffect
import com.mahjongslash.ui.theme.*

@Composable
fun SplashScreen(onTap: () -> Unit) {
    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onTap() },
            contentAlignment = Alignment.Center
        ) {
            // Slash animation across title area
            TitleSlashEffect()

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Main title — brush calligraphy style
            Text(
                text = "切牌",
                style = TextStyle(
                    color = WarmWhite,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp,
                ),
            )

            // Subtitle
            Text(
                text = "MAHJONG SLASH",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                ),
                modifier = Modifier.padding(top = 8.dp)
            )

            // Red seal stamp
            Text(
                text = "印",
                style = TextStyle(
                    color = AccentRed,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                ),
                modifier = Modifier.padding(top = 40.dp)
            )

            // Tap prompt
            Text(
                text = "tap to begin",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.25f),
                    fontSize = 14.sp,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 60.dp)
            )
            }
        }
    }
}
