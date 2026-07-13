package com.quickcleanpro.phonecleaner.common.ui.components.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CleanXScanProgress(
    captionText: String,
    modifier: Modifier = Modifier,
    captionColor: Color = Color(0xFF2D3748),
    ringSize: Dp = 260.dp,
    ringWidth: Dp = 20.dp,
    ringColor: Color = Color(0xFF22A9E8),
    ringBackgroundColor: Color = Color(0xFF22A9E8).copy(alpha = 0.12f),
    animationDurationMillis: Int = 900,
    centerContent: @Composable BoxScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            CleanXScanRingAnimation(
                modifier = Modifier.size(ringSize),
                ringWidth = ringWidth,
                ringColor = ringColor,
                backgroundColor = ringBackgroundColor,
                animationDurationMillis = animationDurationMillis,
                content = centerContent,
            )
            Text(
                text = captionText,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                color = captionColor,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(top = 316.dp)
                        .size(width = 260.dp, height = 60.dp),
            )
        }
    }
}
