package com.quickcleanpro.phonecleaner.use.app

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.app.runtime.AppRuntimeHost
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchRequest
import com.quickcleanpro.phonecleaner.use.core.startup.NotificationLaunchSource

@Composable
fun AppRoot(
    launchCoordinator: AppLaunchCoordinator,
    onNotificationPermissionGranted: () -> Unit = {},
) {
    AppRuntimeHost(
        launchCoordinator = launchCoordinator,
        onNotificationPermissionGranted = onNotificationPermissionGranted,
    )
}

internal fun shouldLetSplashConsumeNotificationTarget(
    currentRoute: String?,
    request: AppLaunchRequest.NotificationTarget,
): Boolean =
    currentRoute == AppDestination.Splash.route &&
        request.source == NotificationLaunchSource.InitialIntent

internal fun shouldConsumeNotificationTargetImmediately(
    currentRoute: String?,
    request: AppLaunchRequest.NotificationTarget,
): Boolean =
    currentRoute == AppDestination.Splash.route &&
        request.source == NotificationLaunchSource.NewIntent
