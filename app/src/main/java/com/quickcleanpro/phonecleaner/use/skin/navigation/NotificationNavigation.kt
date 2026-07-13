package com.quickcleanpro.phonecleaner.use.skin.navigation

import androidx.navigation.NavHostController
import com.quickcleanpro.phonecleaner.use.core.ads.InterstitialAdInterceptor
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.app.runtime.notification.ToolNotificationIntentFactory

internal fun NavHostController.navigateToNotificationTarget(
    route: String,
    interstitialAdInterceptor: InterstitialAdInterceptor,
) {
    val fromRoute = currentDestination?.route?.takeUnless { it == AppDestination.Splash.route || it == AppDestination.OnboardingScan.route }
    val navigateToTarget = {
        navigateToNotificationTargetNow(route)
    }
    if (route == AppDestination.Home.route || route in ToolNotificationIntentFactory.homeTabRoutes) {
        navigateToTarget()
        return
    }
    interstitialAdInterceptor.interceptRouteEntry(
        fromRoute = fromRoute,
        targetRoute = route,
        onContinue = navigateToTarget,
    )
}

private fun NavHostController.navigateToNotificationTargetNow(route: String) {
    while (popBackStack()) {
        // Clear the existing stack before rebuilding Home -> target.
    }
    val currentRoute = currentDestination?.route
    if (route in ToolNotificationIntentFactory.homeTabRoutes) {
        navigate(route) {
            currentRoute?.let { popUpTo(it) { inclusive = true } }
            launchSingleTop = true
        }
        return
    }
    navigate(AppDestination.Home.route) {
        currentRoute?.let { popUpTo(it) { inclusive = true } }
        launchSingleTop = true
    }
    if (route != AppDestination.Home.route) {
        navigate(route) { launchSingleTop = true }
    }
}
