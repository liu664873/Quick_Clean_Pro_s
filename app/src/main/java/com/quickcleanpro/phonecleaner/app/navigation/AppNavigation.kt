package com.quickcleanpro.phonecleaner.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.quickcleanpro.phonecleaner.app.runtime.AppRuntimeBindings
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchCoordinator

@Composable
internal fun AppNavGraph(
    navController: NavHostController,
    launchCoordinator: AppLaunchCoordinator,
    runtime: AppRuntimeBindings,
    splashPaused: Boolean,
    notificationPermissionUiActive: Boolean,
) {
    val navigator =
        remember(navController, runtime.ads) {
            FeatureEntryRouter(
                navigator = NavHostControllerAppNavigator(navController),
                interstitialAds = runtime.ads,
            )
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            AppRouteTransitions.enterTransition(
                scope = this,
                initialRoute = initialState.destination.route,
                targetRoute = targetState.destination.route,
            )
        },
        exitTransition = {
            AppRouteTransitions.exitTransition(
                scope = this,
                initialRoute = initialState.destination.route,
                targetRoute = targetState.destination.route,
            )
        },
        popEnterTransition = {
            AppRouteTransitions.popEnterTransition(
                scope = this,
                initialRoute = initialState.destination.route,
                targetRoute = targetState.destination.route,
            )
        },
        popExitTransition = {
            AppRouteTransitions.popExitTransition(
                scope = this,
                initialRoute = initialState.destination.route,
                targetRoute = targetState.destination.route,
            )
        },
    ) {
        registerSplashRoute(
            navController = navController,
            navigator = navigator,
            launchCoordinator = launchCoordinator,
            adRuntime = runtime.ads,
            externalActivityLauncher = runtime.externalActivities,
            splashPermissionPaused = splashPaused,
        )
        registerOnboardingRoute(
            navigator = navigator,
            adRuntime = runtime.ads,
        )
        registerHomeRoutes(
            navigator = navigator,
            externalBlockingPromptActive = notificationPermissionUiActive,
        )
        registerCleanRoutes(
            navigator = navigator,
            featureFlow = runtime.featureFlow,
        )
        registerAntiVirusRoutes(
            navController = navController,
            navigator = navigator,
            featureFlow = runtime.featureFlow,
            externalActivities = runtime.externalActivities,
        )
        registerAppLockRoutes(
            navigator = navigator,
            featureFlow = runtime.featureFlow,
        )
        registerToolboxRoutes(
            navigator = navigator,
            featureFlow = runtime.featureFlow,
            externalActivities = runtime.externalActivities,
        )
        registerFileManagerRoutes(
            navController = navController,
            navigator = navigator,
            featureFlow = runtime.featureFlow,
        )
        registerSettingsRoutes(
            navigator = navigator,
            externalActivities = runtime.externalActivities,
        )
    }
}
