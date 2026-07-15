package com.quickcleanpro.phonecleaner.app.navigation

import com.quickcleanpro.phonecleaner.common.ads.InterstitialAdRunner

/** Applies the commercial entry policy before delegating stack mutation. */
class FeatureEntryRouter(
    private val navigator: AppNavigator,
    private val interstitialAds: InterstitialAdRunner,
) : AppNavigator by navigator {
    override fun open(destination: AppDestination, args: Map<String, String>) {
        openAfterEntryAd(destination, destination.withArgs(args))
    }

    override fun openRoute(route: String) {
        val destination = AppDestination.forRoute(route)
        if (destination == null) {
            navigator.openRoute(route)
        } else {
            openAfterEntryAd(destination, route)
        }
    }

    override fun openNotificationTarget(route: String) {
        val fromRoute =
            navigator.currentRoute?.takeUnless {
                it == AppDestination.Splash.route || it == AppDestination.OnboardingScan.route
            }
        val openTarget = { navigator.openNotificationTarget(route) }
        if (route in AppDestination.homeRoutes) {
            openTarget()
            return
        }
        interstitialAds.runRouteEntry(
            fromRoute = fromRoute,
            targetRoute = route,
            onContinue = openTarget,
        )
    }

    private fun openAfterEntryAd(destination: AppDestination, finalRoute: String) {
        interstitialAds.runRouteEntry(
            fromRoute = navigator.currentRoute,
            targetRoute = destination.route,
        ) {
            navigator.openRoute(finalRoute)
        }
    }
}
