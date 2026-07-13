package com.quickcleanpro.phonecleaner.common.ui.components.animations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXBackground
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXMutedText
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXTextBody

@Composable
fun CleanXDeleteAnimation(
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackText: String? = null,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("delete_animation/delete_finish.json"),
        imageAssetsFolder = "delete_animation/images/",
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = isPlaying,
        speed = 1f,
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (composition == null) {
            Text(
                text = fallbackText ?: stringResource(R.string.delete_loading_fallback),
                color = CleanXMutedText,
                fontSize = 16.sp,
            )
        } else {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}

@Composable
fun CleanXFullScreenDeleteAnimation(
    modifier: Modifier = Modifier,
    fallbackText: String? = null,
    backgroundColor: Color = CleanXBackground,
    drawBackground: Boolean = true,
) {
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
        contentAlignment = Alignment.Center,
    ) {
        CleanXDeleteAnimation(
            modifier = Modifier.fillMaxSize(),
            fallbackText = fallbackText,
        )
    }
}

@Composable
fun CleanXNotificationGuideAnimation(
    modifier: Modifier = Modifier,
    fallbackText: String? = null,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("notification_animation/notification.json"),
        imageAssetsFolder = "notification_animation/images/",
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
        speed = 1f,
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (composition == null) {
            Text(
                text = fallbackText ?: stringResource(R.string.notification_loading_animation),
                color = CleanXMutedText,
                fontSize = CleanXTextBody,
            )
        } else {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
