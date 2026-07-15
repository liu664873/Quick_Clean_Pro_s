package com.quickcleanpro.phonecleaner.app.navigation.register

import android.os.SystemClock
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.AppConfig
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.common.ads.AdRuntime
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.common.intent.openUrl
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.feature.startup.SplashAction
import com.quickcleanpro.phonecleaner.feature.startup.SplashEffect
import com.quickcleanpro.phonecleaner.feature.startup.SplashViewModel
import com.quickcleanpro.phonecleaner.feature.startup.ui.SplashLegalDocument
import com.quickcleanpro.phonecleaner.feature.startup.ui.SplashScreen
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerSplashRoute(
    navController: NavHostController,
    navigator: AppNavigator,
    launchCoordinator: AppLaunchCoordinator,
    adRuntime: AdRuntime,
    externalActivityLauncher: ExternalActivityLauncher,
    splashPermissionPaused: Boolean,
) {
    composable(AppDestination.Splash.route) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val viewModel: SplashViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val pendingRequest by launchCoordinator.pendingRequest.collectAsStateWithLifecycle()
        val splashStartedAt = remember { SystemClock.elapsedRealtime() }
        var externalLinkActive by remember { mutableStateOf(false) }
        var displayTracked by remember { mutableStateOf(false) }

        LaunchedEffect(pendingRequest) {
            val request = pendingRequest ?: return@LaunchedEffect
            if (launchCoordinator.consumeRequestIfCurrent(request)) {
                viewModel.onAction(SplashAction.LaunchRequestChanged(request))
            }
        }
        LaunchedEffect(splashPermissionPaused) {
            viewModel.onAction(SplashAction.PermissionPauseChanged(splashPermissionPaused))
        }
        LaunchedEffect(viewModel, adRuntime, navigator, navController) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    SplashEffect.RunColdStartAd ->
                        adRuntime.runColdStart(
                            context = context.applicationContext,
                            onOpenAdStateChanged = { active ->
                                viewModel.onAction(SplashAction.OpenAdStateChanged(active))
                            },
                            onFinished = { viewModel.onAction(SplashAction.OpenAdFinished) },
                        )
                    is SplashEffect.OpenNotificationTarget ->
                        navigator.openNotificationTarget(effect.route)
                    is SplashEffect.Navigate -> navigator.replace(effect.destination)
                }
            }
        }
        DisposableEffect(lifecycleOwner, viewModel) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && externalLinkActive) {
                    externalLinkActive = false
                    viewModel.onAction(SplashAction.ExternalLinkStateChanged(active = false))
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        SplashScreen(
            state = state,
            onAction = { action ->
                if (action == SplashAction.VisualFinished && !displayTracked) {
                    displayTracked = true
                    AnalyticsTracker.trackSplashDisplay(SystemClock.elapsedRealtime() - splashStartedAt)
                }
                viewModel.onAction(action)
            },
            onOpenLegalDocument = { document ->
                externalLinkActive = true
                viewModel.onAction(SplashAction.ExternalLinkStateChanged(active = true))
                externalActivityLauncher.markLaunch()
                val opened =
                    when (document) {
                        SplashLegalDocument.Terms -> {
                            AnalyticsTracker.trackTerms(AnalyticsTracker.Referrer.LAUNCHPAGE)
                            context.openUrl(AppConfig.TERMS_OF_SERVICE_URL)
                        }
                        SplashLegalDocument.Privacy -> {
                            AnalyticsTracker.trackPrivacy(AnalyticsTracker.Referrer.LAUNCHPAGE)
                            context.openUrl(AppConfig.PRIVACY_POLICY_URL)
                        }
                    }
                if (!opened) {
                    externalActivityLauncher.cancelLaunch()
                    externalLinkActive = false
                    viewModel.onAction(SplashAction.ExternalLinkStateChanged(active = false))
                }
            },
        )
    }
}
