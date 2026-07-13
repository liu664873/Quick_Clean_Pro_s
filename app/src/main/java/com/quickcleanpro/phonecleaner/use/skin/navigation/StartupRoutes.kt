package com.quickcleanpro.phonecleaner.use.skin.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.use.core.ads.AdScene
import com.quickcleanpro.phonecleaner.use.core.ads.InterstitialAdInterceptor
import com.quickcleanpro.phonecleaner.use.core.ads.StartupAdCoordinator
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchRequest
import com.quickcleanpro.phonecleaner.use.core.startup.NotificationLaunchSource
import com.quickcleanpro.phonecleaner.use.app.MyApp
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.onboarding.OnboardingScanScreen
import com.quickcleanpro.phonecleaner.use.skin.startup.SplashScreen
import com.quickcleanpro.phonecleaner.use.feature.startup.presentation.SplashViewModel
import com.quickcleanpro.phonecleaner.use.feature.onboarding.presentation.OnboardingScanViewModel
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch

internal fun NavGraphBuilder.registerStartupRoutes(
    navController: NavHostController,
    navigator: AppNavigator,
    routeDependencies: AppRouteDependencies,
    interstitialAdInterceptor: InterstitialAdInterceptor,
    splashPaused: Boolean,
    launchCoordinator: AppLaunchCoordinator?,
    splashNotificationPermissionPrompt: @Composable () -> Unit = {},
) {
    composable(AppDestination.Splash.route) {
        val context = LocalContext.current
        val viewModel: SplashViewModel = koinViewModel()
        val externalActivityLaunchHandler = routeDependencies.externalActivities
        val activity = context.findActivity()
        val startupScope = rememberCoroutineScope()
        val pendingRequestState = launchCoordinator?.pendingRequest?.collectAsStateWithLifecycle()
        val pendingRequest = pendingRequestState?.value

        LaunchedEffect(pendingRequest) {
            val request = pendingRequest
            if (request is AppLaunchRequest.NotificationTarget &&
                request.source == NotificationLaunchSource.NewIntent &&
                launchCoordinator.consumeRequestIfCurrent(request)
            ) {
                navController.navigateToNotificationTarget(request.route, interstitialAdInterceptor)
            }
        }
        var finishStartupNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }
        var openAdShowing by remember { mutableStateOf(false) }
        SplashScreen(
            paused = splashPaused || openAdShowing,
            externalActivityLaunchHandler = externalActivityLaunchHandler,
            onStartupReady = { onStartupComplete ->
                when (val request = launchCoordinator?.consumeRequest() ?: AppLaunchRequest.Normal) {
                    is AppLaunchRequest.NotificationTarget -> {
                        finishStartupNavigation = {
                            navController.navigateToNotificationTarget(request.route, interstitialAdInterceptor)
                        }
                        onStartupComplete()
                    }
                    AppLaunchRequest.Normal -> {
                        val targetScreen = startupTargetScreen(viewModel)
                        finishStartupNavigation = {
                            navigator.replace(targetScreen)
                        }
                        startupScope.launch {
                            val sdkInitialization =
                                (context.applicationContext as? MyApp)?.sdkInitialization
                            sdkInitialization?.awaitAdvertiseReady()
                            StartupAdCoordinator.runColdStart(
                                activity = activity,
                                context = context.applicationContext,
                                onOpenAdShowing = { openAdShowing = true },
                                onOpenAdFinished = { openAdShowing = false },
                                onFinished = onStartupComplete,
                            )
                        }
                    }
                }
            },
            onSplashFinished = {
                val navigation = finishStartupNavigation
                finishStartupNavigation = null
                navigation?.invoke() ?: navigator.replace(startupTargetScreen(viewModel))
            },
        )
        splashNotificationPermissionPrompt()
    }

    composable(AppDestination.OnboardingScan.route) {
        val viewModel: OnboardingScanViewModel = koinViewModel()
        OnboardingScanScreen(
            viewModel = viewModel,
            onSkipToHome = {
                interstitialAdInterceptor.interceptFeatureOperation(
                    scene = AdScene.OnboardingSkipped,
                    requestId = "onboarding_skip",
                ) {
                    navigator.replace(AppDestination.Home)
                }
            },
            onScanFinishedAd = {
                interstitialAdInterceptor.interceptFeatureOperation(
                    scene = AdScene.OnboardingScanFinished,
                    requestId = "onboarding_scan_finish",
                ) {}
            },
            onGetStartedToHome = {
                interstitialAdInterceptor.interceptFeatureOperation(
                    scene = AdScene.OnboardingSkipped,
                    requestId = "onboarding_get_started",
                ) {
                    navigator.replace(AppDestination.Home)
                }
            },
        )
    }
}

private fun startupTargetScreen(viewModel: SplashViewModel): AppDestination =
    if (viewModel.shouldShowOnboardingScan()) {
        AppDestination.OnboardingScan
    } else {
        AppDestination.Home
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
