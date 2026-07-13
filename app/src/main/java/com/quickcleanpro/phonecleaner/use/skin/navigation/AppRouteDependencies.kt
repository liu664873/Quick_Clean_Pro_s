package com.quickcleanpro.phonecleaner.use.skin.navigation

import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationTracker
import com.quickcleanpro.phonecleaner.use.core.common.platform.ExternalActivityLaunchHandler
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXPermissionCoordinator

data class AppRouteDependencies(
    val permissions: CleanXPermissionCoordinator,
    val operations: FeatureOperationTracker,
    val externalActivities: ExternalActivityLaunchHandler,
)
