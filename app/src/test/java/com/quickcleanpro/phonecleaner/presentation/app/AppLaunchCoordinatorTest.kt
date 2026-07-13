package com.quickcleanpro.phonecleaner.common.startup

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLaunchCoordinatorTest {
    @Test
    fun initialIntentWithoutNotificationTargetProducesNormalRequest() {
        val coordinator = coordinator()

        coordinator.onCreate(null)

        assertEquals(AppLaunchRequest.Normal, coordinator.pendingRequest.value)
    }

    @Test
    fun notificationTargetUsesNewIntentSource() {
        val coordinator =
            coordinator(
                targetRouteResolver = { NOTIFICATION_ROUTE },
            )

        coordinator.onNewIntent(Intent())

        assertEquals(
            AppLaunchRequest.NotificationTarget(
                route = NOTIFICATION_ROUTE,
                source = NotificationLaunchSource.NewIntent,
            ),
            coordinator.pendingRequest.value,
        )
    }

    @Test
    fun newIntentReplacesPendingInitialNotificationTarget() {
        var resolveTarget = INITIAL_NOTIFICATION_ROUTE
        val coordinator =
            coordinator(
                targetRouteResolver = { resolveTarget },
            )

        coordinator.onCreate(Intent())
        resolveTarget = NEW_NOTIFICATION_ROUTE
        coordinator.onNewIntent(Intent())

        assertEquals(
            AppLaunchRequest.NotificationTarget(
                route = NEW_NOTIFICATION_ROUTE,
                source = NotificationLaunchSource.NewIntent,
            ),
            coordinator.pendingRequest.value,
        )
    }

    @Test
    fun newIntentWithoutNotificationTargetKeepsPendingInitialTarget() {
        var resolveTarget: String? = INITIAL_NOTIFICATION_ROUTE
        val coordinator =
            coordinator(
                targetRouteResolver = { resolveTarget },
            )

        coordinator.onCreate(Intent())
        resolveTarget = null
        coordinator.onNewIntent(Intent())

        assertEquals(
            AppLaunchRequest.NotificationTarget(
                route = INITIAL_NOTIFICATION_ROUTE,
                source = NotificationLaunchSource.InitialIntent,
            ),
            coordinator.pendingRequest.value,
        )
    }

    @Test
    fun consumeRequestReturnsPendingRequestAndResetsIt() {
        val coordinator =
            coordinator(
                targetRouteResolver = { NOTIFICATION_ROUTE },
            )
        coordinator.onCreate(Intent())

        val consumed = coordinator.consumeRequest()

        assertEquals(
            AppLaunchRequest.NotificationTarget(
                route = NOTIFICATION_ROUTE,
                source = NotificationLaunchSource.InitialIntent,
            ),
            consumed,
        )
        assertEquals(AppLaunchRequest.Normal, coordinator.pendingRequest.value)
    }

    @Test
    fun repeatedConsumeRequestDoesNotReplayNotificationTarget() {
        val coordinator =
            coordinator(
                targetRouteResolver = { NOTIFICATION_ROUTE },
            )
        coordinator.onNewIntent(Intent())

        assertTrue(coordinator.consumeRequest() is AppLaunchRequest.NotificationTarget)

        assertEquals(AppLaunchRequest.Normal, coordinator.consumeRequest())
        assertEquals(AppLaunchRequest.Normal, coordinator.pendingRequest.value)
    }

    @Test
    fun consumeRequestIfCurrentConsumesMatchingRequestOnlyOnce() {
        val coordinator =
            coordinator(
                targetRouteResolver = { NOTIFICATION_ROUTE },
            )
        coordinator.onNewIntent(Intent())
        val currentRequest = coordinator.pendingRequest.value

        assertTrue(coordinator.consumeRequestIfCurrent(currentRequest))
        assertFalse(coordinator.consumeRequestIfCurrent(currentRequest))
        assertEquals(AppLaunchRequest.Normal, coordinator.pendingRequest.value)
    }

    @Test
    fun consumeRequestIfCurrentDoesNotConsumeNewerRequest() {
        val coordinator =
            coordinator(
                targetRouteResolver = { NOTIFICATION_ROUTE },
            )

        val staleRequest = AppLaunchRequest.Normal
        coordinator.onNewIntent(Intent())

        val consumed = coordinator.consumeRequestIfCurrent(staleRequest)

        assertTrue(!consumed)
        assertEquals(
            AppLaunchRequest.NotificationTarget(
                route = NOTIFICATION_ROUTE,
                source = NotificationLaunchSource.NewIntent,
            ),
            coordinator.pendingRequest.value,
        )
    }

    @Test
    fun initialNotificationTargetUsesInitialIntentSource() {
        val coordinator =
            coordinator(
                targetRouteResolver = { NOTIFICATION_ROUTE },
            )

        coordinator.onCreate(Intent())

        assertEquals(
            AppLaunchRequest.NotificationTarget(
                route = NOTIFICATION_ROUTE,
                source = NotificationLaunchSource.InitialIntent,
            ),
            coordinator.pendingRequest.value,
        )
    }

    private fun coordinator(targetRouteResolver: (Intent?) -> String? = { null }): AppLaunchCoordinator =
        AppLaunchCoordinator(
            targetRouteResolver = targetRouteResolver,
        )

    private companion object {
        const val NOTIFICATION_ROUTE = "notification_cleaner"
        const val INITIAL_NOTIFICATION_ROUTE = "initial_notification"
        const val NEW_NOTIFICATION_ROUTE = "new_notification"
    }
}
