package com.quickcleanpro.phonecleaner.app.runtime

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
import com.quickcleanpro.phonecleaner.common.ads.AdPreloadStage
import com.quickcleanpro.phonecleaner.common.ads.AdRuntime
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchRequest
@Composable
internal fun AdRuntimeLifecycleEffect(lifecycleOwner: LifecycleOwner, adRuntime: AdRuntime) {
    DisposableEffect(lifecycleOwner, adRuntime) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) adRuntime.onHostResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adRuntime.dispose()
        }
    }
}

@Composable
internal fun RouteAnalyticsEffect(route: String?, context: Context, adRuntime: AdRuntime) {
    var previousRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(route) {
        val current = route ?: return@LaunchedEffect
        if (current in AppDestination.homeRoutes) {
            AnalyticsTracker.trackHomeEntered(AnalyticsTracker.homepageReferrer(previousRoute))
            adRuntime.preload(AdPreloadStage.MainPage, context.applicationContext)
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
        if (shouldRouteNotificationRequestToSplash(pendingRequest, currentRoute)) {
            navController.navigate(AppDestination.Splash.route) { launchSingleTop = true }
        }
    }
}

internal fun shouldRouteNotificationRequestToSplash(
    pendingRequest: AppLaunchRequest?,
    currentRoute: String?,
): Boolean =
    pendingRequest is AppLaunchRequest.NotificationTarget &&
        currentRoute != null &&
        currentRoute != AppDestination.Splash.route
