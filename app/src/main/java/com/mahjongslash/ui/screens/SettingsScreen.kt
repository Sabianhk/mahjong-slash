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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahjongslash.data.PreferencesManager
import com.mahjongslash.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
) {
    val soundEnabled by preferencesManager.soundEnabled.collectAsState(initial = true)
    val hapticsEnabled by preferencesManager.hapticsEnabled.collectAsState(initial = true)
    val leftHanded by preferencesManager.leftHanded.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

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
                text = "設定",
                style = TextStyle(
                    color = WarmWhite,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = "SETTINGS",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Settings toggles styled as tile objects
            SettingToggle(
                label = "音 SOUND",
                enabled = soundEnabled,
                onToggle = { scope.launch { preferencesManager.setSoundEnabled(!soundEnabled) } },
            )
            SettingToggle(
                label = "振 HAPTICS",
                enabled = hapticsEnabled,
                onToggle = { scope.launch { preferencesManager.setHapticsEnabled(!hapticsEnabled) } },
            )
            SettingToggle(
                label = "左 LEFT HAND",
                enabled = leftHanded,
                onToggle = { scope.launch { preferencesManager.setLeftHanded(!leftHanded) } },
            )

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

@Composable
private fun SettingToggle(label: String, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .padding(vertical = 8.dp)
            .background(
                if (enabled) TileIvory else BackgroundMid,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = if (enabled) BackgroundDark else InkGrey,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = if (enabled) "ON" else "OFF",
            style = TextStyle(
                color = if (enabled) AccentGold else InkGrey.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            ),
        )
    }
}
