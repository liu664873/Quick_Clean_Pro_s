package com.quickcleanpro.phonecleaner.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.navigation.FeatureEntryRouter
import com.quickcleanpro.phonecleaner.common.ads.InterstitialAdRunner
import com.quickcleanpro.phonecleaner.common.ads.AdScene
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureEntryRouterTest {
    @Test
    fun `feature entry waits for ad continuation before mutating stack`() {
        val navigator = RecordingNavigator()
        val interceptor = DeferredInterceptor()
        val router = FeatureEntryRouter(navigator, interceptor)

        router.open(AppDestination.JunkClean)

        assertEquals(emptyList<String>(), navigator.openedRoutes)
        assertEquals(AppDestination.JunkClean.route, interceptor.targetRoute)
        interceptor.continueNavigation?.invoke()
        assertEquals(listOf(AppDestination.JunkClean.route), navigator.openedRoutes)
    }

    @Test
    fun `unknown route bypasses feature entry policy`() {
        val navigator = RecordingNavigator()
        val interceptor = DeferredInterceptor()
        val router = FeatureEntryRouter(navigator, interceptor)

        router.openRoute("unknown")

        assertEquals(listOf("unknown"), navigator.openedRoutes)
        assertEquals(null, interceptor.targetRoute)
    }

    @Test
    fun `notification target waits for ad continuation before resetting stack`() {
        val navigator = RecordingNavigator()
        val interceptor = DeferredInterceptor()
        val router = FeatureEntryRouter(navigator, interceptor)

        router.openNotificationTarget(AppDestination.NotificationCleaner.route)

        assertEquals(emptyList<String>(), navigator.notificationTargets)
        assertEquals(AppDestination.Home.route, interceptor.fromRoute)
        assertEquals(AppDestination.NotificationCleaner.route, interceptor.targetRoute)
        interceptor.continueNavigation?.invoke()
        assertEquals(listOf(AppDestination.NotificationCleaner.route), navigator.notificationTargets)
    }

    @Test
    fun `notification home target bypasses entry ad`() {
        val navigator = RecordingNavigator()
        val interceptor = DeferredInterceptor()
        val router = FeatureEntryRouter(navigator, interceptor)

        router.openNotificationTarget(AppDestination.HomeToolbox.route)

        assertEquals(listOf(AppDestination.HomeToolbox.route), navigator.notificationTargets)
        assertEquals(0, interceptor.runRouteEntryCount)
    }

    @Test
    fun `notification target omits startup routes from ad source`() {
        val splashNavigator = RecordingNavigator(currentRoute = AppDestination.Splash.route)
        val splashInterceptor = DeferredInterceptor()
        FeatureEntryRouter(splashNavigator, splashInterceptor)
            .openNotificationTarget(AppDestination.JunkClean.route)

        assertEquals(null, splashInterceptor.fromRoute)

        val onboardingNavigator = RecordingNavigator(currentRoute = AppDestination.OnboardingScan.route)
        val onboardingInterceptor = DeferredInterceptor()
        FeatureEntryRouter(onboardingNavigator, onboardingInterceptor)
            .openNotificationTarget(AppDestination.JunkClean.route)

        assertEquals(null, onboardingInterceptor.fromRoute)
    }

    private class DeferredInterceptor : InterstitialAdRunner {
        var fromRoute: String? = null
        var targetRoute: String? = null
        var continueNavigation: (() -> Unit)? = null
        var runRouteEntryCount: Int = 0

        override fun runRouteEntry(fromRoute: String?, targetRoute: String?, onContinue: () -> Unit) {
            runRouteEntryCount += 1
            this.fromRoute = fromRoute
            this.targetRoute = targetRoute
            continueNavigation = onContinue
        }

        override fun run(scene: AdScene?, requestId: String, onContinue: () -> Unit) = onContinue()
    }

    private class RecordingNavigator(
        override val currentRoute: String? = AppDestination.Home.route,
    ) : AppNavigator {
        val openedRoutes = mutableListOf<String>()
        val notificationTargets = mutableListOf<String>()

        override fun open(destination: AppDestination, args: Map<String, String>) = openRoute(destination.withArgs(args))
        override fun openRoute(route: String) { openedRoutes += route }
        override fun openNotificationTarget(route: String) { notificationTargets += route }
        override fun replace(destination: AppDestination) = Unit
        override fun resetTo(destination: AppDestination) = Unit
        override fun home() = Unit
        override fun back(): Boolean = true
    }
}
