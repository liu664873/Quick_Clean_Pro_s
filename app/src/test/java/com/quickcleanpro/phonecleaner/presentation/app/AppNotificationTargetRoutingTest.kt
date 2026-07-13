package com.quickcleanpro.phonecleaner.app

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.common.startup.AppLaunchRequest
import com.quickcleanpro.phonecleaner.common.startup.NotificationLaunchSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNotificationTargetRoutingTest {
    @Test
    fun initialNotificationTargetOnSplashIsConsumedBySplash() {
        val request =
            AppLaunchRequest.NotificationTarget(
                route = AppDestination.NotificationCleaner.route,
                source = NotificationLaunchSource.InitialIntent,
            )

        assertTrue(shouldLetSplashConsumeNotificationTarget(AppDestination.Splash.route, request))
        assertFalse(shouldConsumeNotificationTargetImmediately(AppDestination.Splash.route, request))
    }

    @Test
    fun newIntentNotificationTargetOnSplashIsConsumedImmediately() {
        val request =
            AppLaunchRequest.NotificationTarget(
                route = AppDestination.NotificationCleaner.route,
                source = NotificationLaunchSource.NewIntent,
            )

        assertFalse(shouldLetSplashConsumeNotificationTarget(AppDestination.Splash.route, request))
        assertTrue(shouldConsumeNotificationTargetImmediately(AppDestination.Splash.route, request))
    }

    @Test
    fun initialNotificationTargetOutsideSplashIsDeferredToSplash() {
        val request =
            AppLaunchRequest.NotificationTarget(
                route = AppDestination.NotificationCleaner.route,
                source = NotificationLaunchSource.InitialIntent,
            )

        assertFalse(shouldLetSplashConsumeNotificationTarget(AppDestination.Home.route, request))
        assertFalse(shouldConsumeNotificationTargetImmediately(AppDestination.Home.route, request))
    }

    @Test
    fun newIntentNotificationTargetOutsideSplashIsDeferredToSplash() {
        val request =
            AppLaunchRequest.NotificationTarget(
                route = AppDestination.NotificationCleaner.route,
                source = NotificationLaunchSource.NewIntent,
            )

        assertFalse(shouldLetSplashConsumeNotificationTarget(AppDestination.Home.route, request))
        assertFalse(shouldConsumeNotificationTargetImmediately(AppDestination.Home.route, request))
    }

    @Test
    fun notificationTargetIsNotConsumedWhenCurrentRouteIsUnknown() {
        val request =
            AppLaunchRequest.NotificationTarget(
                route = AppDestination.NotificationCleaner.route,
                source = NotificationLaunchSource.NewIntent,
            )

        assertFalse(shouldLetSplashConsumeNotificationTarget(null, request))
        assertFalse(shouldConsumeNotificationTargetImmediately(null, request))
    }
}
