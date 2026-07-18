package com.quickcleanpro.phonecleaner.feature.startup.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.feature.startup.SplashProgressStage

@Composable
internal fun SplashStageProgressBar(
    progress: Float,
    activeStage: SplashProgressStage,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulse =
        if (activeStage == SplashProgressStage.ShowingAd && !paused) {
            val transition = rememberInfiniteTransition(label = "splash-progress")
            transition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 900),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "splash-progress-pulse",
            ).value
        } else {
            0f
        }

    Canvas(
        modifier = modifier.size(width = 320.dp, height = 10.dp),
    ) {
        val radius = size.height / 2
        val cornerRadius = CornerRadius(radius)
        val fillWidth = size.width * progress.coerceIn(0f, 1f)

        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            cornerRadius = cornerRadius,
        )
        if (fillWidth > 0f) {
            drawRoundRect(
                color = Color.White,
                size = Size(fillWidth, size.height),
                cornerRadius = cornerRadius,
            )
        }

        if (activeStage == SplashProgressStage.ShowingAd && !paused && fillWidth > 0f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.12f + pulse * 0.18f),
                radius = radius * (0.75f + pulse * 0.25f),
                center = Offset(fillWidth, radius),
            )
        }
    }
}
