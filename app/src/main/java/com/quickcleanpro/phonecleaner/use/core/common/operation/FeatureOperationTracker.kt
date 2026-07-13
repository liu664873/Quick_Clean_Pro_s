package com.quickcleanpro.phonecleaner.use.core.common.operation

import android.util.Log
import com.quickcleanpro.phonecleaner.use.core.ads.InterstitialAdInterceptor
import com.quickcleanpro.phonecleaner.use.core.ads.adRequestId
import com.quickcleanpro.phonecleaner.use.core.ads.toAdScene
import com.quickcleanpro.phonecleaner.use.core.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey

interface FeatureOperationTracker {
    fun track(event: FeatureOperationEvent)

    fun trackWithAd(
        event: FeatureOperationEvent,
        onComplete: () -> Unit,
    ) {
        track(event)
        onComplete()
    }
}

fun FeatureOperationTracker.trackReturnHome(
    feature: FeatureKey,
    onComplete: () -> Unit,
) {
    trackWithAd(FeatureOperationEvent.ReturnHome(feature), onComplete)
}

fun FeatureOperationTracker.trackPermissionRejectedAndLeave(
    feature: FeatureKey,
    onComplete: () -> Unit,
) {
    trackWithAd(FeatureOperationEvent.PermissionRejected(feature), onComplete)
}

class DefaultFeatureOperationTracker(
    private val interstitialAdInterceptor: InterstitialAdInterceptor,
    private val onOperationBusyChanged: (Boolean) -> Unit,
) : FeatureOperationTracker {
    override fun track(event: FeatureOperationEvent) {
        event.operationBusyState()?.let(onOperationBusyChanged)
        AnalyticsTracker.trackFeatureOperation(event)
        Log.d(TAG, "operation event: $event")
    }

    override fun trackWithAd(event: FeatureOperationEvent, onComplete: () -> Unit) {
        track(event)
        interstitialAdInterceptor.interceptFeatureOperation(
            scene = event.toAdScene(),
            requestId = event.adRequestId(),
            onContinue = onComplete,
        )
    }

    private companion object {
        const val TAG = "CleanXOperation"
    }
}

private fun FeatureOperationEvent.operationBusyState(): Boolean? =
    when (this) {
        is FeatureOperationEvent.ScanStarted,
        is FeatureOperationEvent.OperationStarted,
        -> true
        is FeatureOperationEvent.ScanFinished,
        is FeatureOperationEvent.OperationFinished,
        is FeatureOperationEvent.PermissionRejected,
        is FeatureOperationEvent.ResultShown,
        is FeatureOperationEvent.ReturnHome,
        -> false
        is FeatureOperationEvent.ActionRequested -> null
    }
