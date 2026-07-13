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

    private class DeferredInterceptor : InterstitialAdRunner {
        var targetRoute: String? = null
        var continueNavigation: (() -> Unit)? = null

        override fun runRouteEntry(fromRoute: String?, targetRoute: String?, onContinue: () -> Unit) {
            this.targetRoute = targetRoute
            continueNavigation = onContinue
        }

        override fun run(scene: AdScene?, requestId: String, onContinue: () -> Unit) = onContinue()
    }

    private class RecordingNavigator : AppNavigator {
        override val currentRoute: String? = AppDestination.Home.route
        val openedRoutes = mutableListOf<String>()

        override fun open(destination: AppDestination, args: Map<String, String>) = openRoute(destination.withArgs(args))
        override fun openRoute(route: String) { openedRoutes += route }
        override fun replace(destination: AppDestination) = Unit
        override fun resetTo(destination: AppDestination) = Unit
        override fun home() = Unit
        override fun back(): Boolean = true
    }
}
