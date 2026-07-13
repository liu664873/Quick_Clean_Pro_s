package com.quickcleanpro.phonecleaner.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.common.ads.AdRuntime
import com.quickcleanpro.phonecleaner.common.ads.AdScene
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.feature.onboarding.ui.OnboardingScanScreen

@Composable
fun OnboardingRoute(
    navigator: AppNavigator,
    adRuntime: AdRuntime,
    viewModel: OnboardingScanViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel) {
        AnalyticsTracker.trackGuideScanningEntered()
        viewModel.onAction(OnboardingAction.Entered)
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.ScanFinished -> {
                    AnalyticsTracker.trackGuideScanResultEntered()
                    adRuntime.run(
                        scene = AdScene.OnboardingScanFinished,
                        requestId = "onboarding_scan_finish",
                    ) {}
                }
                is OnboardingEffect.Complete -> {
                    if (effect.skipped) {
                        AnalyticsTracker.trackGuideSkipClicked()
                    } else {
                        AnalyticsTracker.trackGuideContinueClicked()
                    }
                    adRuntime.run(
                        scene = AdScene.OnboardingSkipped,
                        requestId = if (effect.skipped) "onboarding_skip" else "onboarding_get_started",
                    ) {
                        navigator.replace(AppDestination.Home)
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(OnboardingAction.Refresh)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OnboardingScanScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigate = navigator::replace,
    )
}
