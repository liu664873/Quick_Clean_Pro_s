package com.quickcleanpro.phonecleaner.app.runtime

import com.quickcleanpro.phonecleaner.app.runtime.InitializationStatus
import com.quickcleanpro.phonecleaner.app.runtime.SdkInitializationCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SdkInitializationCoordinatorTest {
    @Test
    fun advertiseFailureDoesNotBlockOtherInitializers() = runTest {
        var analyticsInitialized = false
        var notificationDefaultsLoaded = false
        val coordinator =
            SdkInitializationCoordinator(
                scope = this,
                advertiseInitializer = { error("advertise unavailable") },
                analyticsInitializer = { analyticsInitialized = true },
                notificationDefaultsInitializer = { notificationDefaultsLoaded = true },
            )

        coordinator.start()
        advanceUntilIdle()

        assertEquals(InitializationStatus.FAILED, coordinator.state.value.advertise.status)
        assertEquals(InitializationStatus.SUCCEEDED, coordinator.state.value.analytics.status)
        assertEquals(InitializationStatus.SUCCEEDED, coordinator.state.value.notificationDefaults.status)
        assertTrue(analyticsInitialized)
        assertTrue(notificationDefaultsLoaded)
        assertFalse(coordinator.awaitAdvertiseReady())
        assertFalse(coordinator.awaitNotificationDefaultsReady())
    }

    @Test
    fun notificationDefaultsReadyRequiresBothComponents() = runTest {
        val coordinator = readyCoordinator()

        coordinator.start()
        advanceUntilIdle()

        assertTrue(coordinator.awaitNotificationDefaultsReady())
    }

    @Test
    fun notificationDefaultsFailureIsNotReady() = runTest {
        val coordinator =
            SdkInitializationCoordinator(
                scope = this,
                advertiseInitializer = {},
                analyticsInitializer = {},
                notificationDefaultsInitializer = { error("defaults unavailable") },
            )

        coordinator.start()
        advanceUntilIdle()

        assertEquals(InitializationStatus.FAILED, coordinator.state.value.notificationDefaults.status)
        assertFalse(coordinator.awaitNotificationDefaultsReady())
    }

    @Test
    fun notificationDefaultsWaitForAdvertiseCompletion() = runTest {
        val advertiseGate = CompletableDeferred<Unit>()
        var notificationDefaultsLoaded = false
        val coordinator =
            SdkInitializationCoordinator(
                scope = this,
                advertiseInitializer = { advertiseGate.await() },
                analyticsInitializer = {},
                notificationDefaultsInitializer = { notificationDefaultsLoaded = true },
            )

        coordinator.start()
        testScheduler.runCurrent()
        assertFalse(notificationDefaultsLoaded)

        advertiseGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(notificationDefaultsLoaded)
        assertTrue(coordinator.awaitNotificationDefaultsReady())
    }

    @Test
    fun notificationDefaultsAreNotReadyBeforeInitializationStarts() = runTest {
        val coordinator = readyCoordinator()

        assertFalse(coordinator.awaitNotificationDefaultsReady(timeoutMillis = 0L))
    }

    @Test
    fun multipleCallersObserveSameNotificationReadiness() = runTest {
        val coordinator = readyCoordinator()
        val waiters =
            List(3) {
                async { coordinator.awaitNotificationDefaultsReady() }
            }

        coordinator.start()
        advanceUntilIdle()

        assertTrue(waiters.awaitAll().all { it })
    }

    private fun TestScope.readyCoordinator(): SdkInitializationCoordinator =
        SdkInitializationCoordinator(
            scope = this,
            advertiseInitializer = {},
            analyticsInitializer = {},
            notificationDefaultsInitializer = {},
        )
}
