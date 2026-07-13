package com.quickcleanpro.phonecleaner.use.core.ads

import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction

sealed interface AdScene{
    data object OnboardingScanFinished : AdScene

    data object OnboardingSkipped : AdScene

    data class EnterFeature(
        val feature: FeatureKey,
        val route: String?,
    ) : AdScene

    data class ScanFinished(
        val feature: FeatureKey,
        val hasResult: Boolean,
    ) : AdScene

    data class OperationFinished(
        val feature: FeatureKey,
        val action: OperationAction,
        val success: Boolean,
    ) : AdScene

    data class ReturnHome(
        val feature: FeatureKey,
    ) : AdScene

    data class Reload(
        val feature: FeatureKey,
    ) : AdScene

    data class RecommendClick(
        val feature: FeatureKey,
    ) : AdScene

    data class PermissionRejected(
        val feature: FeatureKey,
    ) : AdScene
}

fun FeatureOperationEvent.toAdScene(): AdScene? =
    when (this) {
        is FeatureOperationEvent.ScanFinished -> AdScene.ScanFinished(feature, hasResult)
        is FeatureOperationEvent.OperationFinished -> AdScene.OperationFinished(feature, action, success)
        is FeatureOperationEvent.ReturnHome -> AdScene.ReturnHome(feature)
        is FeatureOperationEvent.PermissionRejected -> AdScene.PermissionRejected(feature)
        is FeatureOperationEvent.ScanStarted,
        is FeatureOperationEvent.ActionRequested,
        is FeatureOperationEvent.OperationStarted,
        is FeatureOperationEvent.ResultShown,
            -> null
    }

fun FeatureOperationEvent.adRequestId(): String =
    when (this) {
        is FeatureOperationEvent.ScanFinished -> "scan_finished_${feature.name}_$hasResult"
        is FeatureOperationEvent.OperationFinished -> "operation_finished_${feature.name}_${action.name}_$success"
        is FeatureOperationEvent.ReturnHome -> "return_home_${feature.name}"
        is FeatureOperationEvent.PermissionRejected -> "permission_rejected_${feature.name}"
        else -> "event_${feature.name}_${this::class.java.simpleName}"
    }
