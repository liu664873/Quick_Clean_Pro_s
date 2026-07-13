package com.quickcleanpro.phonecleaner.app.navigation

import com.quickcleanpro.phonecleaner.use.core.ads.InterstitialAdInterceptor

/** Applies the commercial entry policy before delegating stack mutation. */
class FeatureEntryRouter(
    private val navigator: AppNavigator,
    private val adInterceptor: InterstitialAdInterceptor,
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

    private fun openAfterEntryAd(destination: AppDestination, finalRoute: String) {
        adInterceptor.interceptRouteEntry(
            fromRoute = navigator.currentRoute,
            targetRoute = destination.route,
        ) {
            navigator.openRoute(finalRoute)
        }
    }
}
