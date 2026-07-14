package com.quickcleanpro.phonecleaner.app.navigation

import androidx.navigation.NavHostController
import com.quickcleanpro.phonecleaner.common.ads.InterstitialAdRunner
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

internal fun NavHostController.navigateToNotificationTarget(
    route: String,
    interstitialAds: InterstitialAdRunner,
) {
    val fromRoute = currentDestination?.route?.takeUnless { it == AppDestination.Splash.route || it == AppDestination.OnboardingScan.route }
    val navigateToTarget = {
        navigateToNotificationTargetNow(route)
    }
    if (route in AppDestination.homeRoutes) {
        navigateToTarget()
        return
    }
    interstitialAds.runRouteEntry(
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
    if (route in AppDestination.homeRoutes && route != AppDestination.Home.route) {
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
