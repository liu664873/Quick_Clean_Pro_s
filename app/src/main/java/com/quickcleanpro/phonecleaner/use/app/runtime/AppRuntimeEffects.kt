package com.quickcleanpro.phonecleaner.use.app.runtime

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.core.ads.AdvertisePreloader
import com.quickcleanpro.phonecleaner.use.core.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchRequest
import kotlinx.coroutines.delay

private const val EXTERNAL_ACTIVITY_RETURN_AD_COOLDOWN_MS = 1_200L

@Composable
internal fun ExternalActivityLifecycleEffect(lifecycleOwner: LifecycleOwner, coordinator: ExternalActivityAdCoordinator) {
    DisposableEffect(lifecycleOwner, coordinator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) coordinator.markReturn()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            coordinator.dispose()
        }
    }
}

@Composable
internal fun ExternalActivityReturnCooldownEffect(coordinator: ExternalActivityAdCoordinator) {
    LaunchedEffect(coordinator.returnGeneration) {
        if (coordinator.returnGeneration == 0) return@LaunchedEffect
        val generation = coordinator.returnGeneration
        delay(EXTERNAL_ACTIVITY_RETURN_AD_COOLDOWN_MS)
        coordinator.finishReturnCooldown(generation)
    }
}

@Composable
internal fun RouteAnalyticsEffect(route: String?, context: Context) {
    var previousRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(route) {
        val current = route ?: return@LaunchedEffect
        if (current in AppDestination.homeRoutes) {
            AnalyticsTracker.trackHomeEntered(AnalyticsTracker.homepageReferrer(previousRoute))
            AdvertisePreloader.preloadMainPageAds(context.applicationContext)
        }
        AnalyticsTracker.featureForPrimaryRoute(current)?.let(AnalyticsTracker::trackCoreFeatureEntered)
        previousRoute = current
    }
}

@Composable
internal fun NotificationLaunchEffect(
    pendingRequest: AppLaunchRequest?,
    currentRoute: String?,
    navController: NavHostController,
) {
    LaunchedEffect(pendingRequest, currentRoute) {
        if (pendingRequest is AppLaunchRequest.NotificationTarget &&
            currentRoute != null &&
            currentRoute != AppDestination.Splash.route
        ) {
            navController.navigate(AppDestination.Splash.route) { launchSingleTop = true }
        }
    }
}
