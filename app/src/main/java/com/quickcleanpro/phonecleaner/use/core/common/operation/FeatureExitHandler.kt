package com.quickcleanpro.phonecleaner.use.core.common.operation

import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey

class FeatureExitHandler(
    private val tracker: FeatureOperationTracker,
) {
    fun exitBack(
        feature: FeatureKey,
        navigateBack: () -> Unit,
    ) {
        tracker.trackReturnHome(feature, navigateBack)
    }

    fun exitHome(
        feature: FeatureKey,
        navigateHome: () -> Unit,
    ) {
        tracker.trackReturnHome(feature, navigateHome)
    }

    fun exitAfterPermissionRejected(
        feature: FeatureKey,
        navigate: () -> Unit,
    ) {
        tracker.trackPermissionRejectedAndLeave(feature, navigate)
    }

    fun exitAfterComplete(
        feature: FeatureKey,
        navigate: () -> Unit,
    ) {
        tracker.trackReturnHome(feature, navigate)
    }
}

fun FeatureOperationTracker.exitHandler(): FeatureExitHandler = FeatureExitHandler(this)
