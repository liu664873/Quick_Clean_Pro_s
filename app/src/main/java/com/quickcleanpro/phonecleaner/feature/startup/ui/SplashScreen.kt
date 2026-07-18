package com.quickcleanpro.phonecleaner.feature.startup.ui

import com.quickcleanpro.phonecleaner.feature.startup.SplashAction
import com.quickcleanpro.phonecleaner.feature.startup.SplashProgressStage
import com.quickcleanpro.phonecleaner.feature.startup.SplashUiState
import com.quickcleanpro.phonecleaner.feature.startup.targetProgress

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CircularAppLogo
import com.quickcleanpro.phonecleaner.common.ui.components.stableNavigationBarsPadding
import com.quickcleanpro.phonecleaner.common.ui.theme.LocalAppThemeTokens
import com.quickcleanpro.phonecleaner.common.ui.theme.QuickCleanProAppTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SplashScreen(
    state: SplashUiState,
    onAction: (SplashAction) -> Unit,
    onOpenLegalDocument: (SplashLegalDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {}

    val scaleAnim = remember { Animatable(0.3f) }
    val alphaAnim = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }
    var visualReadyDispatched by remember { mutableStateOf(false) }
    var visualFinished by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val latestOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(state.paused) {
        if (state.paused) return@LaunchedEffect
        if (scaleAnim.value < 1f) {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        }
        if (alphaAnim.value < 1f) {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600),
            )
        }
    }

    LaunchedEffect(state.paused, visualReadyDispatched, visualFinished) {
        if (state.paused || visualFinished) return@LaunchedEffect

        if (!visualReadyDispatched) {
            visualReadyDispatched = true
            latestOnAction(SplashAction.VisualReady)
        }
    }

    LaunchedEffect(state.showOpenAdOverlay, visualFinished) {
        if (!state.showOpenAdOverlay || visualFinished) return@LaunchedEffect
        withFrameNanos { }
        latestOnAction(SplashAction.OpenAdOverlayReady)
    }

    LaunchedEffect(state.paused, state.finishRequested, state.progressStage, visualFinished) {
        if (visualFinished) return@LaunchedEffect
        val targetProgress = state.progressStage.targetProgress
        if (state.paused) {
            if (state.progressStage == SplashProgressStage.ShowingAd &&
                progressAnim.value < targetProgress
            ) {
                progressAnim.animateTo(
                    targetValue = targetProgress,
                    animationSpec =
                        tween(
                            durationMillis =
                                remainingDurationMillis(
                                    current = progressAnim.value,
                                    start = state.progressStage.startProgress,
                                    target = targetProgress,
                                    durationMillis = state.progressStage.durationMillis,
                                ),
                            easing = FastOutSlowInEasing,
                        ),
                )
            }
            return@LaunchedEffect
        }
        if (progressAnim.value < targetProgress) {
            progressAnim.animateTo(
                targetValue = targetProgress,
                animationSpec =
                    tween(
                        durationMillis =
                            remainingDurationMillis(
                                current = progressAnim.value,
                                start = state.progressStage.startProgress,
                                target = targetProgress,
                                durationMillis = state.progressStage.durationMillis,
                            ),
                        easing =
                            if (state.progressStage == SplashProgressStage.RequestingAd) {
                                LinearEasing
                            } else {
                                FastOutSlowInEasing
                            },
                    ),
            )
        }
        if (state.progressStage != SplashProgressStage.Finishing || !state.finishRequested) {
            return@LaunchedEffect
        }
        delay(STARTUP_FINISH_HOLD_MS)
        visualFinished = true
        latestOnAction(SplashAction.VisualFinished)
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.35f),
                                ),
                        ),
                    ),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 160.dp)
                    .alpha(alphaAnim.value)
                    .scale(scaleAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
//            CircularAppLogo(
//                contentDescription = null,
//                modifier =
//                    Modifier
//                        .size(100.dp)
//                        .border(1.35.dp, Color.White, CircleShape),
//            )
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(121.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp,
            )
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            SplashStageProgressBar(
                progress = progressAnim.value,
                activeStage = state.progressStage,
                paused = state.paused,
            )

            FlowRow(
                modifier =
                    Modifier
                        .width(330.dp)
                        .wrapContentHeight()
                        .stableNavigationBarsPadding(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.splash_accept_prefix),
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = LocalAppThemeTokens.current.colors.splashTextMuted,
                )
                Text(
                    modifier =
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            onOpenLegalDocument(SplashLegalDocument.Terms)
                        },
                    text = stringResource(R.string.settings_terms_of_service),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White,
                )
                Text(
                    text = " | ",
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = LocalAppThemeTokens.current.colors.splashTextMuted,
                )
                Text(
                    modifier =
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            onOpenLegalDocument(SplashLegalDocument.Privacy)
                        },
                    text = stringResource(R.string.settings_privacy_policy),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White,
                )
            }
        }

        if (state.showOpenAdOverlay) {
            OpenAdWaitingOverlay()
        }
    }
}

@Composable
private fun OpenAdWaitingOverlay() {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(min = 220.dp, max = 320.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.White,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFF267A5E),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.open_ad_loading),
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF202124),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewSplashScreen() {
    QuickCleanProAppTheme {
        SplashScreen(
            state = SplashUiState(),
            onAction = {},
            onOpenLegalDocument = {},
        )
    }
}

enum class SplashLegalDocument {
    Terms,
    Privacy,
}

private const val STARTUP_FINISH_HOLD_MS = 300L

private val SplashProgressStage.startProgress: Float
    get() =
        when (this) {
            SplashProgressStage.Preparing -> 0f
            SplashProgressStage.RequestingAd -> 0.25f
            SplashProgressStage.ShowingAd -> 0.50f
            SplashProgressStage.Finishing -> 0.75f
            SplashProgressStage.Completed -> 1f
        }

private val SplashProgressStage.durationMillis: Int
    get() =
        when (this) {
            SplashProgressStage.Preparing -> 1_200
            SplashProgressStage.RequestingAd -> 4_800
            SplashProgressStage.ShowingAd -> 900
            SplashProgressStage.Finishing -> 800
            SplashProgressStage.Completed -> 1
        }

private fun remainingDurationMillis(
    current: Float,
    start: Float,
    target: Float,
    durationMillis: Int,
): Int {
    val totalDistance = target - start
    if (totalDistance <= 0f) return durationMillis
    val remainingRatio = ((target - current) / totalDistance).coerceIn(0f, 1f)
    return (durationMillis * remainingRatio).roundToInt().coerceAtLeast(1)
}
