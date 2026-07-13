package com.quickcleanpro.phonecleaner.advertise

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRuntimeExternalActivityTest {
    @Test
    fun cancellingExternalLaunchRestoresAppOpenState() {
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver)

        runtime.markExternalActivityLaunch()
        runtime.cancelExternalActivityLaunch()

        assertTrue(driver.appOpenEnabledState)
        assertFalse(driver.suppressNextAppOpen)
        assertFalse(runtime.externalActivityReturning)
    }

    @Test
    fun repeatedLaunchAndDisposeReleaseOnlyTheOwnedSuppression() {
        val driver = FakeAdRuntimeDriver().apply { appOpenEnabledState = false }
        val runtime = fakeAdRuntime(driver)

        runtime.markExternalActivityLaunch()
        runtime.markExternalActivityLaunch()
        runtime.dispose()
        runtime.dispose()

        assertFalse(driver.appOpenEnabledState)
        assertFalse(driver.suppressNextAppOpen)
    }
}
