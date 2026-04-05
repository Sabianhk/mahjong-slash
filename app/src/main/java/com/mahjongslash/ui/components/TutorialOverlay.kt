package com.mahjongslash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahjongslash.ui.theme.*
import kotlinx.coroutines.delay

private data class TutorialStep(
    val kanji: String,
    val title: String,
    val description: String,
    val icon: String,
)

private val steps = listOf(
    TutorialStep(
        kanji = "切",
        title = "SLASH PAIRS",
        description = "Swipe through two matching\ntiles to slash them!",
        icon = "⚔",
    ),
    TutorialStep(
        kanji = "連",
        title = "BUILD COMBOS",
        description = "Slash fast to chain combos\nfor bonus points!",
        icon = "🔥",
    ),
    TutorialStep(
        kanji = "刃",
        title = "GUARD YOUR BLADE",
        description = "Miss 3 times and it's game over.\nSlash carefully!",
        icon = "⚠",
    ),
)

@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }

    // Animated slash line for visual flair
    val slashProgress = remember { Animatable(0f) }
    LaunchedEffect(currentStep) {
        slashProgress.snapTo(0f)
        delay(300)
        slashProgress.animateTo(1f, animationSpec = tween(400, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (currentStep < steps.lastIndex) {
                    currentStep++
                } else {
                    onDismiss()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            val step = steps[currentStep]

            // Kanji
            Text(
                text = step.kanji,
                style = TextStyle(
                    color = AccentRed,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated slash accent line
            Canvas(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
            ) {
                val w = size.width * slashProgress.value
                drawLine(
                    color = AccentGold,
                    start = Offset(size.width / 2f - w / 2f, size.height / 2f),
                    end = Offset(size.width / 2f + w / 2f, size.height / 2f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "${step.icon}  ${step.title}",
                style = TextStyle(
                    color = AccentGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = step.description,
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Step indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (i in steps.indices) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (i == currentStep) AccentGold else WarmWhite.copy(alpha = 0.25f),
                                shape = CircleShape,
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tap hint
            Text(
                text = if (currentStep < steps.lastIndex) "TAP TO CONTINUE" else "TAP TO PLAY",
                style = TextStyle(
                    color = WarmWhite.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                ),
            )
        }
    }
}
