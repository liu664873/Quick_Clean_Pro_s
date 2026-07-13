package com.quickcleanpro.phonecleaner.app.runtime

import com.quickcleanpro.phonecleaner.use.app.runtime.InitializationStatus
import com.quickcleanpro.phonecleaner.use.app.runtime.SdkInitializationCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    }
}
