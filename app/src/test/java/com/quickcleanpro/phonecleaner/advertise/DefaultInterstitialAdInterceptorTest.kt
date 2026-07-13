package com.quickcleanpro.phonecleaner.advertise

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import com.quickcleanpro.phonecleaner.use.core.ads.AdScene
import com.quickcleanpro.phonecleaner.use.core.ads.DefaultInterstitialAdInterceptor
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultInterstitialAdInterceptorTest {
    @Test
    fun routeEntryShowsInterstitialBeforeContinue() {
        val recorder = RecordingInterstitials()
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var continued = false

        interceptor.interceptRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.JunkClean.route,
        ) {
            continued = true
        }

        assertEquals(listOf(AdScene.EnterFeature(FeatureKey.JUNK_CLEAN, AppDestination.JunkClean.route)), recorder.scenes)
        assertEquals(listOf("route_enter_JUNK_CLEAN_scan"), recorder.requestIds)
        assertTrue(continued)
    }

    @Test
    fun nonEntryRouteContinuesWithoutAd() {
        val recorder = RecordingInterstitials()
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var continued = false

        interceptor.interceptRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.Settings.route,
        ) {
            continued = true
        }

        assertTrue(recorder.scenes.isEmpty())
        assertTrue(continued)
    }

    @Test
    fun featureOperationUsesInterstitialWhenSceneExists() {
        val recorder = RecordingInterstitials()
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var continued = false
        val scene = AdScene.OperationFinished(FeatureKey.JUNK_CLEAN, OperationAction.CLEAN, success = true)

        interceptor.interceptFeatureOperation(
            scene = scene,
            requestId = "operation_finished_JUNK_CLEAN_CLEAN_true",
        ) {
            continued = true
        }

        assertEquals(listOf(scene), recorder.scenes)
        assertTrue(continued)
    }

    @Test
    fun returnHomeFeatureOperationUsesInterstitial() {
        val recorder = RecordingInterstitials()
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var continued = false
        val scene = AdScene.ReturnHome(FeatureKey.NETWORK_SPEED)

        interceptor.interceptFeatureOperation(
            scene = scene,
            requestId = "return_home_NETWORK_SPEED",
        ) {
            continued = true
        }

        assertEquals(listOf(scene), recorder.scenes)
        assertTrue(continued)
    }

    @Test
    fun routeAdInFlightDropsSecondEntryNavigation() {
        val recorder = RecordingInterstitials(autoContinue = false)
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var firstContinued = false
        var secondContinued = false

        interceptor.interceptRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.JunkClean.route,
        ) {
            firstContinued = true
        }
        interceptor.interceptRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.DeviceInfo.route,
        ) {
            secondContinued = true
        }

        assertEquals(1, recorder.scenes.size)
        assertFalse(firstContinued)
        assertFalse(secondContinued)
    }

    @Test
    fun duplicateRouteEntryIsDroppedWhileFirstRequestIsInFlight() {
        val recorder = RecordingInterstitials(autoContinue = false)
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var continueCount = 0

        repeat(2) {
            interceptor.interceptRouteEntry(
                fromRoute = AppDestination.Home.route,
                targetRoute = AppDestination.JunkClean.route,
            ) {
                continueCount += 1
            }
        }

        assertEquals(1, recorder.scenes.size)
        assertEquals(0, continueCount)
    }

    @Test
    fun routeEntryAcceptsNextRequestAfterInFlightRequestCompletes() {
        val recorder = RecordingInterstitials(autoContinue = false)
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var continueCount = 0

        interceptor.interceptRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.JunkClean.route,
        ) {
            continueCount += 1
        }
        recorder.completeLast()
        interceptor.interceptRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.DeviceInfo.route,
        ) {
            continueCount += 1
        }

        assertEquals(2, recorder.scenes.size)
        assertEquals(1, continueCount)
    }

    @Test
    fun routeEntryContinuesExactlyOnceWhenSdkCallbackRepeats() {
        val recorder = RecordingInterstitials(autoContinue = false)
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var continueCount = 0

        interceptor.interceptRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.JunkClean.route,
        ) {
            continueCount += 1
        }

        recorder.completeLast()
        recorder.completeLast()

        assertEquals(1, continueCount)
    }

    @Test
    fun featureOperationContinuesExactlyOnceWhenSdkCallbackRepeats() {
        val recorder = RecordingInterstitials(autoContinue = false)
        val interceptor = DefaultInterstitialAdInterceptor(recorder::show)
        var continueCount = 0

        interceptor.interceptFeatureOperation(
            scene = AdScene.ReturnHome(FeatureKey.NETWORK_SPEED),
            requestId = "return_home_NETWORK_SPEED",
        ) {
            continueCount += 1
        }

        recorder.completeLast()
        recorder.completeLast()

        assertEquals(1, continueCount)
    }

    private class RecordingInterstitials(
        private val autoContinue: Boolean = true,
    ) {
        val scenes = mutableListOf<AdScene>()
        val requestIds = mutableListOf<String>()
        private val callbacks = mutableListOf<() -> Unit>()

        fun show(
            scene: AdScene,
            requestId: String,
            onContinue: () -> Unit,
        ) {
            scenes += scene
            requestIds += requestId
            callbacks += onContinue
            if (autoContinue) onContinue()
        }

        fun completeLast() {
            callbacks.last().invoke()
        }
    }
}
