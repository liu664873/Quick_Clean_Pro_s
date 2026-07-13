package com.quickcleanpro.phonecleaner.app.permission

sealed interface PermissionState {
    val isGranted: Boolean
        get() = this is Granted

    val granted: Boolean
        get() = isGranted

    data object Granted : PermissionState

    data class Denied(
        val permanently: Boolean = false,
    ) : PermissionState {
        val isPermanentlyDenied: Boolean
            get() = permanently
    }

    data object Unavailable : PermissionState
}

sealed interface PermissionOutcome {
    data object Granted : PermissionOutcome

    data class Denied(val permanently: Boolean) : PermissionOutcome

    data object Dismissed : PermissionOutcome

    data object Unavailable : PermissionOutcome

    data object Busy : PermissionOutcome
}

sealed interface PermissionStartResult {
    data object Started : PermissionStartResult

    data class Completed(val outcome: PermissionOutcome) : PermissionStartResult

    data object Busy : PermissionStartResult
}
