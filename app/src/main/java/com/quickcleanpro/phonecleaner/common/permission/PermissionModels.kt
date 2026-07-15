package com.quickcleanpro.phonecleaner.common.permission

import android.content.Intent

enum class ProtectedAction(
    val key: String,
    val requiredPermissions: List<PermissionType>,
) {
    JunkStartScan("junk_start_scan", listOf(PermissionType.StorageFiles)),
    JunkCleanSelected("junk_clean_selected", listOf(PermissionType.StorageFiles)),
    FileManagerLoadFiles("file_manager_load_files", listOf(PermissionType.StorageFiles)),
    FileManagerDeleteFiles("file_manager_delete_files", listOf(PermissionType.StorageFiles)),
    WhatsAppStartScan("whatsapp_start_scan", listOf(PermissionType.StorageFiles)),
    WhatsAppCleanSelected("whatsapp_clean_selected", listOf(PermissionType.StorageFiles)),
    VirusDeepScanStart("virus_deep_scan_start", listOf(PermissionType.StorageFiles)),
    NetworkScanStart("network_scan_start", listOf(PermissionType.Location)),
    AppUsageLoadStats("app_usage_load_stats", listOf(PermissionType.UsageAccess)),
    NetworkUsageLoadStats("network_usage_load_stats", listOf(PermissionType.UsageAccess)),
    NotificationCleanerEnable("notification_cleaner_enable", listOf(PermissionType.NotificationListener)),
    AppLockOpenProtectedArea("app_lock_open_protected_area", listOf(PermissionType.UsageAccess)),
    AppLockEnableMonitoring(
        "app_lock_enable_monitoring",
        listOf(PermissionType.UsageAccess, PermissionType.Overlay),
    ),
    AppLockRequestOverlay("app_lock_request_overlay", listOf(PermissionType.Overlay)),
}

sealed interface PermissionTarget {
    val key: String
    val requiredPermissions: List<PermissionType>

    data class Action(val action: ProtectedAction) : PermissionTarget {
        override val key: String = "action:${action.key}"
        override val requiredPermissions: List<PermissionType> = action.requiredPermissions
    }

    data class Permission(val permission: PermissionType) : PermissionTarget {
        override val key: String = "permission:${permission.key}"
        override val requiredPermissions: List<PermissionType> = listOf(permission)
    }
}

enum class PermissionPromptMode {
    Explained,
    Direct,
}

data class PermissionStatus(
    val granted: Boolean,
    val missing: List<PermissionType>,
)

sealed interface PermissionDecision {
    data object Granted : PermissionDecision

    data class RequestRuntime(
        val permissions: Array<String>,
    ) : PermissionDecision

    data class OpenSettings(
        val intents: List<Intent>,
    ) : PermissionDecision

    data object Unavailable : PermissionDecision
}

