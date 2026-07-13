package com.quickcleanpro.phonecleaner.advertise

import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.ads.AdRuntimeState
import com.quickcleanpro.phonecleaner.common.ads.AdScene
import com.quickcleanpro.phonecleaner.common.operation.OperationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRuntimeQueueTest {
    @Test
    fun differentRequestsAreShownSerially() {
        val driver = FakeAdRuntimeDriver()
        val inFlightStates = mutableListOf<Boolean>()
        val runtime = fakeAdRuntime(driver = driver, onInterstitialStateChanged = inFlightStates::add)
        var continueCount = 0

        runtime.run(AdScene.OnboardingSkipped, "first") { continueCount += 1 }
        runtime.run(AdScene.OnboardingScanFinished, "second") { continueCount += 1 }

        assertEquals(1, driver.interstitialAreaKeys.size)
        assertEquals(listOf(true), inFlightStates)
        driver.completeInterstitial(0)
        assertEquals(2, driver.interstitialAreaKeys.size)
        assertEquals(1, continueCount)
        driver.completeInterstitial(1)

        assertEquals(2, continueCount)
        assertEquals(listOf(true, false), inFlightStates)
    }

    @Test
    fun duplicateRequestIsIgnoredUntilOriginalCompletes() {
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver)
        var originalContinueCount = 0
        var duplicateContinueCount = 0

        runtime.run(AdScene.OnboardingSkipped, "same") { originalContinueCount += 1 }
        runtime.run(AdScene.OnboardingSkipped, "same") { duplicateContinueCount += 1 }
        driver.completeInterstitial()

        assertEquals(1, driver.interstitialAreaKeys.size)
        assertEquals(1, originalContinueCount)
        assertEquals(0, duplicateContinueCount)
    }

    @Test
    fun completedRequestIdCanBeUsedAgain() {
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver)
        var continueCount = 0

        runtime.run(AdScene.OnboardingSkipped, "repeat") { continueCount += 1 }
        driver.completeInterstitial()
        runtime.run(AdScene.OnboardingSkipped, "repeat") { continueCount += 1 }
        driver.completeInterstitial()

        assertEquals(2, driver.interstitialAreaKeys.size)
        assertEquals(2, continueCount)
    }

    @Test
    fun repeatedSdkCallbackContinuesExactlyOnce() {
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver)
        var continueCount = 0

        runtime.run(AdScene.OnboardingSkipped, "repeat_callback") { continueCount += 1 }
        driver.completeInterstitial()
        driver.completeInterstitial()

        assertEquals(1, continueCount)
    }

    @Test
    fun blockedOrUnavailableRequestSkipsSdkAndContinues() {
        var state = AdRuntimeState(permissionFlowActive = true)
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver = driver, stateProvider = { state })
        var continueCount = 0

        runtime.run(AdScene.OnboardingSkipped, "permission_blocked") { continueCount += 1 }
        state = AdRuntimeState(featureOperationActive = true)
        runtime.run(AdScene.OnboardingSkipped, "operation_blocked") { continueCount += 1 }
        state = AdRuntimeState()
        driver.activityAvailable = false
        runtime.run(AdScene.OnboardingSkipped, "activity_unavailable") { continueCount += 1 }

        assertTrue(driver.interstitialAreaKeys.isEmpty())
        assertEquals(3, continueCount)
    }

    @Test
    fun permissionRejectedBypassesPermissionAndExternalReturnBlocks() {
        val scheduler = FakeAdRuntimeScheduler()
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(
            driver = driver,
            scheduler = scheduler,
            stateProvider = { AdRuntimeState(permissionFlowActive = true) },
        )

        runtime.markExternalActivityLaunch()
        runtime.onHostResumed()
        runtime.run(AdScene.PermissionRejected(FeatureKey.JUNK_CLEAN), "permission_rejected") {}

        assertEquals(1, driver.interstitialAreaKeys.size)
        driver.completeInterstitial()
    }

    @Test
    fun sdkFailureAndSceneWithoutPlacementContinueImmediately() {
        val driver = FakeAdRuntimeDriver().apply { interstitialStarted = false }
        val runtime = fakeAdRuntime(driver)
        var continueCount = 0

        runtime.run(AdScene.OnboardingSkipped, "sdk_failed") { continueCount += 1 }
        runtime.run(
            AdScene.OperationFinished(FeatureKey.ANTI_VIRUS, OperationAction.CLEAN, success = true),
            "no_placement",
        ) { continueCount += 1 }

        assertTrue(driver.interstitialAreaKeys.isEmpty())
        assertEquals(2, continueCount)
    }

    @Test
    fun sdkExceptionContinuesImmediately() {
        val driver = FakeAdRuntimeDriver().apply {
            interstitialFailure = IllegalStateException("sdk failure")
        }
        val runtime = fakeAdRuntime(driver)
        var continueCount = 0

        runtime.run(AdScene.OnboardingSkipped, "sdk_exception") { continueCount += 1 }

        assertTrue(driver.interstitialAreaKeys.isEmpty())
        assertEquals(1, continueCount)
    }
}
