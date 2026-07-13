package com.quickcleanpro.phonecleaner.use.core.ads

import com.quickcleanpro.phonecleaner.app.monetization.AdvertiseSdkAdapter
import java.util.concurrent.atomic.AtomicBoolean

object AppOpenAdSuppression {
    private val lock = Any()
    private var activeCount = 0
    private var restoreEnabled: Boolean? = null

    private val activeReasons = mutableMapOf<AppOpenSuppressionReason, Int>()

    fun acquire(reason: AppOpenSuppressionReason): () -> Unit {
        synchronized(lock) {
            if (activeCount == 0) {
                restoreEnabled = runCatching { AdvertiseSdkAdapter.isAppOpenEnabled() }.getOrDefault(true)
                runCatching { AdvertiseSdkAdapter.setAppOpenEnabled(false) }
            }
            activeCount += 1
            activeReasons[reason] = activeReasons.getOrDefault(reason, 0) + 1
        }

        val released = AtomicBoolean(false)
        return {
            if (released.compareAndSet(false, true)) {
                release(reason)
            }
        }
    }

    private fun release(reason: AppOpenSuppressionReason) {
        var shouldRestore = false
        var enabledToRestore = true
        synchronized(lock) {
            if (activeCount <= 0) return
            activeCount -= 1
            val reasonCount = activeReasons.getOrDefault(reason, 0) - 1
            if (reasonCount <= 0) activeReasons.remove(reason) else activeReasons[reason] = reasonCount
            if (activeCount == 0) {
                shouldRestore = true
                enabledToRestore = restoreEnabled ?: true
                restoreEnabled = null
            }
        }

        if (shouldRestore) {
            runCatching { AdvertiseSdkAdapter.setAppOpenEnabled(enabledToRestore) }
        }
    }
}

enum class AppOpenSuppressionReason {
    Startup,
    ExternalActivity,
    Test,
}
