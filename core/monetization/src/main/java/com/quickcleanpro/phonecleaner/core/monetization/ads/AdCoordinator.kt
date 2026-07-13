package com.quickcleanpro.phonecleaner.core.monetization.ads

import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

class AdCoordinator(
    private val gateway: AdGateway,
    private val policy: AdPolicy = AdPolicy(),
    private val onInFlightChanged: (Boolean) -> Unit = {},
) {
    private val lock = Any()
    private val queuedRequests = ArrayDeque<PendingRequest>()
    private val knownRequestIds = mutableSetOf<String>()
    private var activeRequest: PendingRequest? = null

    val isInFlight: Boolean
        get() = synchronized(lock) { activeRequest != null }

    fun show(opportunity: AdOpportunity, onContinue: () -> Unit): Boolean {
        val request = PendingRequest(opportunity, once(onContinue))
        var startNow = false
        val accepted =
            synchronized(lock) {
                if (!knownRequestIds.add(opportunity.requestId)) {
                    false
                } else {
                    if (activeRequest == null) {
                        activeRequest = request
                        startNow = true
                    } else {
                        queuedRequests.addLast(request)
                    }
                    true
                }
            }

        if (!accepted) return false
        if (startNow) {
            onInFlightChanged(true)
            execute(request)
        }
        return true
    }

    private fun execute(request: PendingRequest) {
        val finish = once { complete(request) }
        if (!policy.canShow(request.opportunity)) {
            finish()
            return
        }

        val started =
            try {
                gateway.showInterstitial(request.opportunity, finish)
            } catch (_: Throwable) {
                false
            }
        if (!started) finish()
    }

    private fun complete(request: PendingRequest) {
        var nextRequest: PendingRequest? = null
        var becameIdle = false
        val completed =
            synchronized(lock) {
                if (activeRequest !== request) {
                    false
                } else {
                    knownRequestIds.remove(request.opportunity.requestId)
                    nextRequest = queuedRequests.pollFirst()
                    activeRequest = nextRequest
                    becameIdle = nextRequest == null
                    true
                }
            }
        if (!completed) return

        if (becameIdle) onInFlightChanged(false)
        try {
            request.onContinue()
        } finally {
            nextRequest?.let(::execute)
        }
    }

    private data class PendingRequest(
        val opportunity: AdOpportunity,
        val onContinue: () -> Unit,
    )
}

private fun once(block: () -> Unit): () -> Unit {
    val called = AtomicBoolean(false)
    return {
        if (called.compareAndSet(false, true)) block()
    }
}
