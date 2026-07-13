package com.quickcleanpro.phonecleaner.advertise

import com.quickcleanpro.phonecleaner.app.monetization.AdResult
import com.quickcleanpro.phonecleaner.app.monetization.InterstitialAdGate
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdCoordinator
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdGateway
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdOpportunity
import com.quickcleanpro.phonecleaner.use.core.ads.AdScene
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InterstitialAdGateTest {
    @Test
    fun `sdk callback resumes suspended request exactly once`() = runTest {
        val gateway = Gateway()
        val gate = InterstitialAdGate(AdCoordinator(gateway))

        val result = async { gate.show(opportunity("request")) }
        runCurrent()
        gateway.close()
        gateway.close()

        assertEquals(AdResult.Completed, result.await())
    }

    @Test
    fun `duplicate request completes as duplicate instead of hanging`() = runTest {
        val gateway = Gateway()
        val gate = InterstitialAdGate(AdCoordinator(gateway))
        val first = async { gate.show(opportunity("same")) }
        runCurrent()

        assertEquals(AdResult.Duplicate, gate.show(opportunity("same")))
        gateway.close()
        assertEquals(AdResult.Completed, first.await())
    }

    private fun opportunity(id: String) = AdOpportunity(AdScene.OnboardingSkipped, id)

    private class Gateway : AdGateway {
        private var callback: (() -> Unit)? = null
        override fun showInterstitial(opportunity: AdOpportunity, onClosed: () -> Unit): Boolean {
            callback = onClosed
            return true
        }

        fun close() = callback?.invoke() ?: Unit
    }
}
