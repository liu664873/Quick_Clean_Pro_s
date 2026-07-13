package com.quickcleanpro.phonecleaner.common.permission

import com.quickcleanpro.phonecleaner.common.permission.PermissionType

interface PermissionFeature {
    val key: String
}

data class PermissionSpec<F : PermissionFeature>(
    val feature: F,
    val permissions: List<PermissionType>,
)

data class PermissionStatus(
    val granted: Boolean,
    val missing: List<PermissionType>,
)

sealed interface PermissionRequestPlan {
    data object AlreadyGranted : PermissionRequestPlan

    data class RequestRuntime(
        val permissions: Array<String>,
    ) : PermissionRequestPlan

    data class OpenSettings(
        val permission: PermissionType,
    ) : PermissionRequestPlan

    data object Unavailable : PermissionRequestPlan
}

