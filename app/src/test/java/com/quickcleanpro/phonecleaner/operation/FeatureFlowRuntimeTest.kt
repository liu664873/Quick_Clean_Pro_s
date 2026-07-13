package com.quickcleanpro.phonecleaner.operation

import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.ads.AdScene
import com.quickcleanpro.phonecleaner.common.ads.InterstitialAdRunner
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.DefaultFeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureFlowRuntimeTest {
    @Test
    fun `scan lifecycle updates busy state and continues without ad`() {
        val ads = RecordingAdRunner()
        val busyStates = mutableListOf<Boolean>()
        val events = mutableListOf<FeatureOperationEvent>()
        val runtime = runtime(ads, busyStates, events)
        var continuationCount = 0

        runtime.handleOperation(FeatureOperationEvent.ScanStarted(FeatureKey.PHOTOS)) {
            continuationCount += 1
        }
        runtime.handleOperation(FeatureOperationEvent.ScanFinished(FeatureKey.PHOTOS, hasResult = true)) {
            continuationCount += 1
        }

        assertEquals(listOf(true, false), busyStates)
        assertEquals(2, events.size)
        assertEquals(2, continuationCount)
        assertEquals(emptyList<AdRequest>(), ads.requests)
    }

    @Test
    fun `operation completion keeps original scene and request id`() {
        val ads = RecordingAdRunner()
        val busyStates = mutableListOf<Boolean>()
        val runtime = runtime(ads, busyStates)
        var continuationCount = 0
        val event =
            FeatureOperationEvent.OperationFinished(
                feature = FeatureKey.JUNK_CLEAN,
                action = OperationAction.CLEAN,
                success = true,
            )

        runtime.handleOperation(event) { continuationCount += 1 }

        assertEquals(listOf(false), busyStates)
        assertEquals(
            AdRequest(
                scene = AdScene.OperationFinished(FeatureKey.JUNK_CLEAN, OperationAction.CLEAN, success = true),
                requestId = "operation_finished_JUNK_CLEAN_CLEAN_true",
            ),
            ads.requests.single(),
        )
        assertEquals(0, continuationCount)

        ads.continueRequest()
        ads.continueRequest()
        assertEquals(1, continuationCount)
    }

    @Test
    fun `return exit clears busy and keeps return ad mapping`() {
        val ads = RecordingAdRunner()
        val busyStates = mutableListOf<Boolean>()
        val runtime = runtime(ads, busyStates)

        runtime.exit(FeatureKey.NETWORK_SPEED, FeatureExitReason.Return) {}

        assertEquals(listOf(false), busyStates)
        assertEquals(
            AdRequest(
                scene = AdScene.ReturnHome(FeatureKey.NETWORK_SPEED),
                requestId = "return_home_NETWORK_SPEED",
            ),
            ads.requests.single(),
        )
    }

    @Test
    fun `permission rejection keeps exception scene and continues once`() {
        val ads = RecordingAdRunner()
        val runtime = runtime(ads)
        var continuationCount = 0

        runtime.exit(FeatureKey.APP_USAGE, FeatureExitReason.PermissionRejected) {
            continuationCount += 1
        }

        assertEquals(
            AdRequest(
                scene = AdScene.PermissionRejected(FeatureKey.APP_USAGE),
                requestId = "permission_rejected_APP_USAGE",
            ),
            ads.requests.single(),
        )
        ads.continueRequest()
        ads.continueRequest()
        assertEquals(1, continuationCount)
    }

    private fun runtime(
        ads: RecordingAdRunner,
        busyStates: MutableList<Boolean> = mutableListOf(),
        events: MutableList<FeatureOperationEvent> = mutableListOf(),
    ): DefaultFeatureFlowRuntime =
        DefaultFeatureFlowRuntime(
            interstitialAds = ads,
            onOperationBusyChanged = busyStates::add,
            trackOperation = events::add,
        )

    private data class AdRequest(
        val scene: AdScene?,
        val requestId: String,
    )

    private class RecordingAdRunner : InterstitialAdRunner {
        val requests = mutableListOf<AdRequest>()
        private var continuation: (() -> Unit)? = null

        override fun run(
            scene: AdScene?,
            requestId: String,
            onContinue: () -> Unit,
        ) {
            requests += AdRequest(scene, requestId)
            continuation = onContinue
        }

        override fun runRouteEntry(
            fromRoute: String?,
            targetRoute: String?,
            onContinue: () -> Unit,
        ) = onContinue()

        fun continueRequest() {
            continuation?.invoke()
        }
    }
}
