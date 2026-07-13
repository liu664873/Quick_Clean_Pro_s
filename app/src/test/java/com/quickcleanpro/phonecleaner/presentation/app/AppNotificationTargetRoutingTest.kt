package com.quickcleanpro.phonecleaner.app.runtime

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchRequest
import com.quickcleanpro.phonecleaner.app.runtime.startup.NotificationLaunchSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNotificationTargetRoutingTest {
    @Test
    fun notificationTargetOutsideSplashRoutesThroughSplash() {
        NotificationLaunchSource.entries.forEach { source ->
            assertTrue(
                shouldRouteNotificationRequestToSplash(
                    pendingRequest = notificationRequest(source),
                    currentRoute = AppDestination.Home.route,
                ),
            )
        }
    }

    @Test
    fun notificationTargetAlreadyOnSplashIsConsumedThere() {
        assertFalse(
            shouldRouteNotificationRequestToSplash(
                pendingRequest = notificationRequest(NotificationLaunchSource.InitialIntent),
                currentRoute = AppDestination.Splash.route,
            ),
        )
    }

    @Test
    fun unknownRouteDoesNotConsumeNotificationTarget() {
        assertFalse(
            shouldRouteNotificationRequestToSplash(
                pendingRequest = notificationRequest(NotificationLaunchSource.NewIntent),
                currentRoute = null,
            ),
        )
    }

    @Test
    fun normalOrConsumedRequestDoesNotRouteToSplash() {
        assertFalse(shouldRouteNotificationRequestToSplash(AppLaunchRequest.Normal, AppDestination.Home.route))
        assertFalse(shouldRouteNotificationRequestToSplash(null, AppDestination.Home.route))
    }

    private fun notificationRequest(source: NotificationLaunchSource): AppLaunchRequest.NotificationTarget =
        AppLaunchRequest.NotificationTarget(
            route = AppDestination.NotificationCleaner.route,
            source = source,
        )
}
