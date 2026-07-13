package com.quickcleanpro.phonecleaner.use.skin.common.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXBackground
import kotlinx.coroutines.launch

enum class CleanXDeleteCompleteStage {
    Deleting,
    Complete,
}

@Composable
fun CleanXDeleteCompleteAnimation(
    stage: CleanXDeleteCompleteStage,
    modifier: Modifier = Modifier,
    fallbackText: String? = null,
    backgroundColor: Color = CleanXBackground,
    drawBackground: Boolean = true,
) {
    val isComplete = stage == CleanXDeleteCompleteStage.Complete
    val deleteAlpha =
        animateFloatAsState(
            targetValue = if (isComplete) 0f else 1f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "deleteAlpha",
        )
    val backgroundModifier =
        if (drawBackground) {
            Modifier.background(backgroundColor)
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(backgroundModifier),
        contentAlignment = Alignment.TopCenter,
    ) {
        CleanXDeleteAnimation(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = deleteAlpha.value
                    },
                fallbackText = fallbackText,
        )
        Column(
            modifier = Modifier.padding(top = 150.dp)
        ) {
            CleanXDeleteCompleteCheckAnimation(
                isVisible = isComplete,
            )
        }
    }
}

@Composable
private fun CleanXDeleteCompleteCheckAnimation(
    isVisible: Boolean,
) {
    val scale = remember { Animatable(0.35f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            scale.snapTo(0.35f)
            alpha.snapTo(0f)
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                )
            }
        } else {
            scale.snapTo(0.35f)
            alpha.snapTo(0f)
        }
    }

    if (isVisible || alpha.value > 0f) {
        Image(
            painter = painterResource(R.drawable.ic_file_manager_complete),
            contentDescription = null,
            modifier =
                Modifier
                    .size(122.dp)
                    .alpha(alpha.value)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        transformOrigin = TransformOrigin.Center
                    },
        )
    }
}
