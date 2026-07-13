package com.quickcleanpro.phonecleaner.use.app.runtime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.core.ads.InterstitialAdInterceptor
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchRequest
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppNavGraph
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies

@Composable
internal fun AppShell(
    navController: NavHostController,
    launchCoordinator: AppLaunchCoordinator,
    pendingRequest: AppLaunchRequest?,
    currentRoute: String?,
    splashPaused: Boolean,
    externalBlockingPromptActive: Boolean,
    splashNotificationPermissionPrompt: @Composable () -> Unit,
    homeNotificationPermissionPrompt: @Composable () -> Unit,
    interstitialAdInterceptor: InterstitialAdInterceptor,
    interstitialAdInFlight: Boolean,
    routeDependencies: AppRouteDependencies,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AppNavGraph(
            navController = navController,
            launchCoordinator = launchCoordinator,
            startDestination = AppDestination.Splash.route,
            splashPaused = splashPaused,
            externalBlockingPromptActive = externalBlockingPromptActive,
            splashNotificationPermissionPrompt = splashNotificationPermissionPrompt,
            homeNotificationPermissionPrompt = homeNotificationPermissionPrompt,
            interstitialAdInterceptor = interstitialAdInterceptor,
            routeDependencies = routeDependencies,
        )
        InterstitialInteractionBlocker(interstitialAdInFlight)
    }
    NotificationLaunchEffect(pendingRequest, currentRoute, navController)
}

@Composable
private fun InterstitialInteractionBlocker(enabled: Boolean) {
    if (!enabled) return
    BackHandler {}
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {},
    )
}
