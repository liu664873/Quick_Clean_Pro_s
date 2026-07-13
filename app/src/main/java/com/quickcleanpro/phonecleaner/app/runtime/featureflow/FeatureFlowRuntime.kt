package com.quickcleanpro.phonecleaner.app.runtime.featureflow

import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.ads.AdScene
import com.quickcleanpro.phonecleaner.common.ads.InterstitialAdRunner
import com.quickcleanpro.phonecleaner.common.ads.adRequestId
import com.quickcleanpro.phonecleaner.common.ads.once
import com.quickcleanpro.phonecleaner.common.ads.toAdScene
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker

interface FeatureFlowRuntime {
    fun handleOperation(
        event: FeatureOperationEvent,
        onContinue: () -> Unit = {},
    )

    fun exit(
        feature: FeatureKey,
        reason: FeatureExitReason,
        onContinue: () -> Unit,
    )
}

class DefaultFeatureFlowRuntime(
    private val interstitialAds: InterstitialAdRunner,
    private val onOperationBusyChanged: (Boolean) -> Unit,
    private val trackOperation: (FeatureOperationEvent) -> Unit = AnalyticsTracker::trackFeatureOperation,
) : FeatureFlowRuntime {
    override fun handleOperation(
        event: FeatureOperationEvent,
        onContinue: () -> Unit,
    ) {
        val continueOnce = once(onContinue)
        onOperationBusyChanged(event.operationBusyState())
        trackOperation(event)
        val scene = event.toAdScene()
        if (scene == null) {
            continueOnce()
            return
        }
        interstitialAds.run(
            scene = scene,
            requestId = event.adRequestId(),
            onContinue = continueOnce,
        )
    }

    override fun exit(
        feature: FeatureKey,
        reason: FeatureExitReason,
        onContinue: () -> Unit,
    ) {
        val continueOnce = once(onContinue)
        onOperationBusyChanged(false)
        interstitialAds.run(
            scene = reason.toAdScene(feature),
            requestId = reason.adRequestId(feature),
            onContinue = continueOnce,
        )
    }
}

private fun FeatureOperationEvent.operationBusyState(): Boolean =
    when (this) {
        is FeatureOperationEvent.ScanStarted,
        is FeatureOperationEvent.OperationStarted,
        -> true
        is FeatureOperationEvent.ScanFinished,
        is FeatureOperationEvent.OperationFinished,
        -> false
    }

private fun FeatureExitReason.toAdScene(feature: FeatureKey): AdScene =
    when (this) {
        FeatureExitReason.Return -> AdScene.ReturnHome(feature)
        FeatureExitReason.PermissionRejected -> AdScene.PermissionRejected(feature)
    }

private fun FeatureExitReason.adRequestId(feature: FeatureKey): String =
    when (this) {
        FeatureExitReason.Return -> "return_home_${feature.name}"
        FeatureExitReason.PermissionRejected -> "permission_rejected_${feature.name}"
    }
