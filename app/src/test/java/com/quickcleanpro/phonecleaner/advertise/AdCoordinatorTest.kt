package com.quickcleanpro.phonecleaner.advertise

import com.quickcleanpro.phonecleaner.core.monetization.ads.AdCoordinator
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdGateway
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdOpportunity
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdPolicy
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdRuntimeState
import com.quickcleanpro.phonecleaner.use.core.ads.AdScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdCoordinatorTest {
    @Test
    fun differentRequestsAreShownSerially() {
        val gateway = RecordingGateway()
        val inFlightStates = mutableListOf<Boolean>()
        val coordinator = AdCoordinator(gateway, onInFlightChanged = inFlightStates::add)
        var continueCount = 0

        coordinator.show(opportunity("first")) { continueCount += 1 }
        coordinator.show(opportunity("second")) { continueCount += 1 }

        assertEquals(listOf("first"), gateway.requestIds)
        assertEquals(listOf(true), inFlightStates)
        gateway.close(0)
        assertEquals(listOf("first", "second"), gateway.requestIds)
        assertEquals(1, continueCount)
        gateway.close(1)

        assertEquals(2, continueCount)
        assertEquals(listOf(true, false), inFlightStates)
        assertFalse(coordinator.isInFlight)
    }

    @Test
    fun duplicateRequestIsIgnoredUntilOriginalCompletes() {
        val gateway = RecordingGateway()
        val coordinator = AdCoordinator(gateway)
        var originalContinueCount = 0
        var duplicateContinueCount = 0

        coordinator.show(opportunity("same")) { originalContinueCount += 1 }
        coordinator.show(opportunity("same")) { duplicateContinueCount += 1 }
        gateway.close(0)

        assertEquals(listOf("same"), gateway.requestIds)
        assertEquals(1, originalContinueCount)
        assertEquals(0, duplicateContinueCount)
    }

    @Test
    fun repeatedSdkCallbackContinuesExactlyOnce() {
        val gateway = RecordingGateway()
        val coordinator = AdCoordinator(gateway)
        var continueCount = 0

        coordinator.show(opportunity("repeat")) { continueCount += 1 }
        gateway.close(0)
        gateway.close(0)

        assertEquals(1, continueCount)
        assertFalse(coordinator.isInFlight)
    }

    @Test
    fun blockedOpportunitySkipsGatewayAndContinues() {
        val gateway = RecordingGateway()
        val coordinator =
            AdCoordinator(
                gateway = gateway,
                policy = AdPolicy { AdRuntimeState(forceDisableAds = true) },
            )
        var continueCount = 0

        coordinator.show(opportunity("blocked")) { continueCount += 1 }

        assertTrue(gateway.requestIds.isEmpty())
        assertEquals(1, continueCount)
        assertFalse(coordinator.isInFlight)
    }

    @Test
    fun gatewayFailureContinuesAndReleasesState() {
        val gateway = RecordingGateway(started = false)
        val coordinator = AdCoordinator(gateway)
        var continueCount = 0

        coordinator.show(opportunity("failed")) { continueCount += 1 }

        assertEquals(1, continueCount)
        assertFalse(coordinator.isInFlight)
    }

    private fun opportunity(requestId: String): AdOpportunity =
        AdOpportunity(
            scene = AdScene.OnboardingSkipped,
            requestId = requestId,
        )

    private class RecordingGateway(
        private val started: Boolean = true,
    ) : AdGateway {
        val requestIds = mutableListOf<String>()
        private val callbacks = mutableListOf<() -> Unit>()

        override fun showInterstitial(
            opportunity: AdOpportunity,
            onClosed: () -> Unit,
        ): Boolean {
            requestIds += opportunity.requestId
            callbacks += onClosed
            return started
        }

        fun close(index: Int) {
            callbacks[index].invoke()
        }
    }
}
