package com.quickcleanpro.phonecleaner.advertise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRuntimeStartupTest {
    @Test
    fun cachedConsentShowsOpenAdAndSettlesBeforeFinishing() {
        val driver = FakeAdRuntimeDriver()
        val scheduler = FakeAdRuntimeScheduler()
        val runtime = fakeAdRuntime(driver = driver, scheduler = scheduler)
        val openStates = mutableListOf<Boolean>()
        var preloadCount = 0
        var finishCount = 0

        runtime.runColdStart(
            preloadStartup = { preloadCount += 1 },
            onOpenAdStateChanged = openStates::add,
            onFinished = { finishCount += 1 },
        )
        driver.completeConsent()
        driver.loadOpenAd()
        driver.closeOpenAd()
        scheduler.advanceBy(799L)

        assertEquals(0, finishCount)
        assertFalse(driver.appOpenEnabledState)

        scheduler.advanceBy(1L)

        assertEquals(listOf(true, false), openStates)
        assertEquals(1, preloadCount)
        assertEquals(1, finishCount)
        assertTrue(driver.appOpenEnabledState)
    }

    @Test
    fun consentInitializationTimeoutStillAdvancesToConsentAndOpenAd() {
        val driver = FakeAdRuntimeDriver().apply { consentCached = false }
        val scheduler = FakeAdRuntimeScheduler()
        val runtime = fakeAdRuntime(driver = driver, scheduler = scheduler)
        var finishCount = 0

        runtime.runColdStart(preloadStartup = {}, onFinished = { finishCount += 1 })
        scheduler.advanceBy(6_500L)
        driver.completeConsent()
        driver.loadOpenAd()
        driver.closeOpenAd()
        scheduler.advanceBy(800L)

        assertEquals(1, finishCount)
    }

    @Test
    fun openAdStartTimeoutReleasesSuppressionAndFinishes() {
        val driver = FakeAdRuntimeDriver()
        val scheduler = FakeAdRuntimeScheduler()
        val runtime = fakeAdRuntime(driver = driver, scheduler = scheduler)
        var finishCount = 0

        runtime.runColdStart(preloadStartup = {}, onFinished = { finishCount += 1 })
        driver.completeConsent()
        scheduler.advanceBy(6_500L)

        assertEquals(1, finishCount)
        assertTrue(driver.appOpenEnabledState)
    }

    @Test
    fun loadedOpenAdTotalTimeoutReleasesSuppressionAndFinishes() {
        val driver = FakeAdRuntimeDriver()
        val scheduler = FakeAdRuntimeScheduler()
        val runtime = fakeAdRuntime(driver = driver, scheduler = scheduler)
        val openStates = mutableListOf<Boolean>()
        var finishCount = 0

        runtime.runColdStart(
            preloadStartup = {},
            onOpenAdStateChanged = openStates::add,
            onFinished = { finishCount += 1 },
        )
        driver.completeConsent()
        driver.loadOpenAd()
        scheduler.advanceBy(30_000L)

        assertEquals(listOf(true, false), openStates)
        assertEquals(1, finishCount)
        assertTrue(driver.appOpenEnabledState)
    }

    @Test
    fun unavailableActivitySkipsConsentAndOpenAd() {
        val driver = FakeAdRuntimeDriver().apply { activityAvailable = false }
        val runtime = fakeAdRuntime(driver)
        val openStates = mutableListOf<Boolean>()
        var finishCount = 0

        runtime.runColdStart(
            preloadStartup = {},
            onOpenAdStateChanged = openStates::add,
            onFinished = { finishCount += 1 },
        )

        assertEquals(listOf(false), openStates)
        assertEquals(1, finishCount)
        assertTrue(driver.appOpenEnabledState)
    }

    @Test
    fun externalActivityReturnKeepsSuppressionForCooldown() {
        val driver = FakeAdRuntimeDriver()
        val scheduler = FakeAdRuntimeScheduler()
        val runtime = fakeAdRuntime(driver = driver, scheduler = scheduler)

        runtime.markExternalActivityLaunch()
        assertFalse(driver.appOpenEnabledState)
        assertTrue(driver.suppressNextAppOpen)

        runtime.onHostResumed()
        scheduler.advanceBy(1_199L)
        assertTrue(runtime.externalActivityReturning)
        assertFalse(driver.appOpenEnabledState)

        scheduler.advanceBy(1L)
        assertFalse(runtime.externalActivityReturning)
        assertTrue(driver.appOpenEnabledState)
        assertFalse(driver.suppressNextAppOpen)
    }

    @Test
    fun startupAndExternalActivitySuppressionUseReferenceCounting() {
        val driver = FakeAdRuntimeDriver()
        val scheduler = FakeAdRuntimeScheduler()
        val runtime = fakeAdRuntime(driver = driver, scheduler = scheduler)

        runtime.runColdStart(preloadStartup = {}, onFinished = {})
        runtime.markExternalActivityLaunch()
        driver.completeConsent()
        driver.loadOpenAd()
        driver.closeOpenAd()
        scheduler.advanceBy(800L)

        assertFalse(driver.appOpenEnabledState)

        runtime.onHostResumed()
        scheduler.advanceBy(1_200L)

        assertTrue(driver.appOpenEnabledState)
    }
}
