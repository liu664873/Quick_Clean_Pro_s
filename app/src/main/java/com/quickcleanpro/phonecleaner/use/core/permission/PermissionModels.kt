package com.quickcleanpro.phonecleaner.use.core.permission

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

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

enum class PermissionRequestResult {
    Started,
    Granted,
    Denied,
    Dismissed,
    SettingsUnavailable,
    Busy,
}

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

