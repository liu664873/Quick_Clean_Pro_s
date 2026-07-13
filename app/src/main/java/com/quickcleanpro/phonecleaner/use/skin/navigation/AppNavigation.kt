package com.quickcleanpro.phonecleaner.use.skin.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.FeatureEntryRouter
import com.quickcleanpro.phonecleaner.app.navigation.NavHostControllerAppNavigator
import com.quickcleanpro.phonecleaner.use.core.ads.InterstitialAdInterceptor
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchCoordinator

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    launchCoordinator: AppLaunchCoordinator?,
    splashPaused: Boolean,
    externalBlockingPromptActive: Boolean,
    splashNotificationPermissionPrompt: @Composable () -> Unit,
    homeNotificationPermissionPrompt: @Composable () -> Unit,
    interstitialAdInterceptor: InterstitialAdInterceptor,
    routeDependencies: AppRouteDependencies,
) {
    val navigator =
        remember(navController, interstitialAdInterceptor) {
            FeatureEntryRouter(
                navigator = NavHostControllerAppNavigator(navController),
                adInterceptor = interstitialAdInterceptor,
            )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
        registerStartupRoutes(
            navController = navController,
            navigator = navigator,
            routeDependencies = routeDependencies,
            interstitialAdInterceptor = interstitialAdInterceptor,
            splashPaused = splashPaused,
            launchCoordinator = launchCoordinator,
            splashNotificationPermissionPrompt = splashNotificationPermissionPrompt,
        )
        registerHomeRoutes(
            navigator = navigator,
            externalBlockingPromptActive = externalBlockingPromptActive,
            homeNotificationPermissionPrompt = homeNotificationPermissionPrompt,
        )
        registerCleanRoutes(navigator, routeDependencies)
        registerAntiVirusRoutes(navController, navigator, routeDependencies)
        registerAppLockRoutes(navigator, routeDependencies)
        registerToolboxRoutes(navigator, routeDependencies)
        registerFileManagerRoutes(navController, navigator, routeDependencies)
        registerSettingsRoutes(navigator, routeDependencies)
    }
}
