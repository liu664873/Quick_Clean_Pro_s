package com.quickcleanpro.phonecleaner.app.monetization

import com.quickcleanpro.phonecleaner.core.monetization.ads.AdCoordinator
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdOpportunity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

sealed interface AdResult {
    data object Completed : AdResult

    data object Duplicate : AdResult

    data object TimedOut : AdResult

    data object Cancelled : AdResult
}

interface AdGate {
    suspend fun show(opportunity: AdOpportunity): AdResult
}

class InterstitialAdGate(
    private val coordinator: AdCoordinator,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : AdGate {
    override suspend fun show(opportunity: AdOpportunity): AdResult =
        try {
            withTimeout(timeoutMillis) {
                suspendCancellableCoroutine { continuation ->
                    val accepted = coordinator.show(opportunity) {
                        if (continuation.isActive) continuation.resume(AdResult.Completed)
                    }
                    if (!accepted && continuation.isActive) {
                        continuation.resume(AdResult.Duplicate)
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            AdResult.TimedOut
        } catch (_: CancellationException) {
            AdResult.Cancelled
        }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 35_000L
    }
}
