package com.mahjongslash.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mahjongslash.R
import com.mahjongslash.ui.theme.BackgroundDark

/**
 * Shared background for all UI screens.
 * Draws the dark base color with an ink-wash texture overlay.
 * @param bgAlpha opacity of the background image (1.0 = full, 0.5 = half)
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    bgAlpha: Float = 1f,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_menu_texture),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha),
        )
        content()
    }
}
