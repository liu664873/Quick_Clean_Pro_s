    package com.quickcleanpro.phonecleaner.common.ads

import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction

sealed interface AdScene{
    data object OnboardingScanFinished : AdScene

    data object OnboardingSkipped : AdScene

    data class EnterFeature(
        val feature: FeatureKey,
        val route: String?,
    ) : AdScene

    data class OperationFinished(
        val feature: FeatureKey,
        val action: OperationAction,
        val success: Boolean,
    ) : AdScene

    data class ReturnHome(
        val feature: FeatureKey,
    ) : AdScene

    data class PermissionRejected(
        val feature: FeatureKey,
    ) : AdScene
}

fun FeatureOperationEvent.toAdScene(): AdScene? =
    when (this) {
        is FeatureOperationEvent.OperationFinished -> AdScene.OperationFinished(feature, action, success)
        is FeatureOperationEvent.ScanStarted,
        is FeatureOperationEvent.ScanFinished,
        is FeatureOperationEvent.OperationStarted,
            -> null
    }

fun FeatureOperationEvent.adRequestId(): String =
    when (this) {
        is FeatureOperationEvent.OperationFinished -> "operation_finished_${feature.name}_${action.name}_$success"
        is FeatureOperationEvent.ScanStarted -> "scan_started_${feature.name}"
        is FeatureOperationEvent.ScanFinished -> "scan_finished_${feature.name}_$hasResult"
        is FeatureOperationEvent.OperationStarted -> "operation_started_${feature.name}_${action.name}"
    }
