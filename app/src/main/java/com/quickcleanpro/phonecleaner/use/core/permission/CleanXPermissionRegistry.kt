package com.quickcleanpro.phonecleaner.use.core.permission

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionFeature
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionManager
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionSpec
import com.quickcleanpro.phonecleaner.use.core.permission.RuntimePermissionDenialStore
import com.quickcleanpro.phonecleaner.use.core.permission.commonPermissionHandlers
import com.quickcleanpro.phonecleaner.use.core.source.local.hasDeniedLocationRuntimePermission
import com.quickcleanpro.phonecleaner.use.core.source.local.hasDeniedNotificationRuntimePermission
import com.quickcleanpro.phonecleaner.use.core.source.local.hasRequestedLocationRuntimePermissionBefore
import com.quickcleanpro.phonecleaner.use.core.source.local.saveLocationRuntimePermissionDenied
import com.quickcleanpro.phonecleaner.use.core.source.local.saveLocationRuntimePermissionRequestedBefore
import com.quickcleanpro.phonecleaner.use.core.source.local.saveNotificationRuntimePermissionDenied

enum class CleanXProtectedAction(
    override val key: String,
) : PermissionFeature {
    JunkStartScan("junk_start_scan"),
    JunkCleanSelected("junk_clean_selected"),
    FileManagerLoadFiles("file_manager_load_files"),
    FileManagerDeleteFiles("file_manager_delete_files"),
    WhatsAppStartScan("whatsapp_start_scan"),
    WhatsAppCleanSelected("whatsapp_clean_selected"),
    VirusDeepScanStart("virus_deep_scan_start"),
    NetworkScanStart("network_scan_start"),
    AppUsageLoadStats("app_usage_load_stats"),
    NetworkUsageLoadStats("network_usage_load_stats"),
    NotificationCleanerEnable("notification_cleaner_enable"),
    AppLockOpenProtectedArea("app_lock_open_protected_area"),
    AppLockEnableMonitoring("app_lock_enable_monitoring"),
    AppLockRequestOverlay("app_lock_request_overlay"),
    PostNotificationsEnable("post_notifications_enable"),
}

object CleanXPermissionRegistry {
    val actionSpecs: List<PermissionSpec<CleanXProtectedAction>> =
        listOf(
            PermissionSpec(CleanXProtectedAction.JunkStartScan, listOf(PermissionType.StorageFiles)),
            PermissionSpec(CleanXProtectedAction.JunkCleanSelected, listOf(PermissionType.StorageFiles)),
            PermissionSpec(CleanXProtectedAction.FileManagerLoadFiles, listOf(PermissionType.StorageFiles)),
            PermissionSpec(CleanXProtectedAction.FileManagerDeleteFiles, listOf(PermissionType.StorageFiles)),
            PermissionSpec(CleanXProtectedAction.WhatsAppStartScan, listOf(PermissionType.StorageFiles)),
            PermissionSpec(CleanXProtectedAction.WhatsAppCleanSelected, listOf(PermissionType.StorageFiles)),
            PermissionSpec(CleanXProtectedAction.VirusDeepScanStart, listOf(PermissionType.StorageFiles)),
            PermissionSpec(CleanXProtectedAction.NetworkScanStart, listOf(PermissionType.Location)),
            PermissionSpec(CleanXProtectedAction.AppUsageLoadStats, listOf(PermissionType.UsageAccess)),
            PermissionSpec(CleanXProtectedAction.NetworkUsageLoadStats, listOf(PermissionType.UsageAccess)),
            PermissionSpec(CleanXProtectedAction.NotificationCleanerEnable, listOf(PermissionType.NotificationListener)),
            PermissionSpec(CleanXProtectedAction.AppLockOpenProtectedArea, listOf(PermissionType.UsageAccess)),
            PermissionSpec(
                CleanXProtectedAction.AppLockEnableMonitoring,
                listOf(PermissionType.UsageAccess, PermissionType.Overlay),
            ),
            PermissionSpec(CleanXProtectedAction.AppLockRequestOverlay, listOf(PermissionType.Overlay)),
            PermissionSpec(CleanXProtectedAction.PostNotificationsEnable, listOf(PermissionType.PostNotifications)),
        )

    val permissionItemSpecs: List<PermissionSpec<PermissionType>> =
        listOf(
            PermissionSpec(PermissionType.StorageFiles, listOf(PermissionType.StorageFiles)),
            PermissionSpec(PermissionType.Location, listOf(PermissionType.Location)),
            PermissionSpec(PermissionType.UsageAccess, listOf(PermissionType.UsageAccess)),
            PermissionSpec(
                PermissionType.NotificationListener,
                listOf(PermissionType.NotificationListener),
            ),
            PermissionSpec(PermissionType.Overlay, listOf(PermissionType.Overlay)),
            PermissionSpec(
                PermissionType.PostNotifications,
                listOf(PermissionType.PostNotifications),
            ),
        )

    fun protectedActionPermissionManager(context: Context): PermissionManager<CleanXProtectedAction> =
        PermissionManager(
            specs = actionSpecs,
            handlers = commonPermissionHandlers(),
            denialStore = CleanXRuntimePermissionDenialStore(context.applicationContext),
        )

    fun permissionItemManager(context: Context): PermissionManager<PermissionType> =
        PermissionManager(
            specs = permissionItemSpecs,
            handlers = commonPermissionHandlers(),
            denialStore = CleanXRuntimePermissionDenialStore(context.applicationContext),
        )

    fun itemForAction(action: CleanXProtectedAction): PermissionType =
        when (action) {
            CleanXProtectedAction.JunkStartScan,
            CleanXProtectedAction.JunkCleanSelected,
            CleanXProtectedAction.FileManagerLoadFiles,
            CleanXProtectedAction.FileManagerDeleteFiles,
            CleanXProtectedAction.WhatsAppStartScan,
            CleanXProtectedAction.WhatsAppCleanSelected,
            CleanXProtectedAction.VirusDeepScanStart,
            -> PermissionType.StorageFiles
            CleanXProtectedAction.NetworkScanStart -> PermissionType.Location
            CleanXProtectedAction.AppUsageLoadStats,
            CleanXProtectedAction.NetworkUsageLoadStats,
            CleanXProtectedAction.AppLockOpenProtectedArea,
            CleanXProtectedAction.AppLockEnableMonitoring,
            -> PermissionType.UsageAccess
            CleanXProtectedAction.NotificationCleanerEnable -> PermissionType.NotificationListener
            CleanXProtectedAction.AppLockRequestOverlay -> PermissionType.Overlay
            CleanXProtectedAction.PostNotificationsEnable -> PermissionType.PostNotifications
        }
}

class CleanXRuntimePermissionDenialStore(
    private val context: Context,
) : RuntimePermissionDenialStore {
    override fun hasDenied(permission: PermissionType): Boolean =
        when (permission.key) {
            PermissionType.Location.key -> hasDeniedLocationRuntimePermission(context)
            PermissionType.PostNotifications.key -> hasDeniedNotificationRuntimePermission(context)
            else -> false
        }

    override fun hasRequestedBefore(permission: PermissionType): Boolean =
        when (permission.key) {
            PermissionType.Location.key -> hasRequestedLocationRuntimePermissionBefore(context)
            else -> hasDenied(permission)
        }

    override fun markRequested(permission: PermissionType) {
        when (permission.key) {
            PermissionType.Location.key -> saveLocationRuntimePermissionRequestedBefore(context)
        }
    }

    override fun shouldRequestRuntimePermission(
        context: Context,
        permission: PermissionType,
        runtimePermissions: Array<String>,
    ): Boolean =
        when (permission.key) {
            PermissionType.Location.key -> {
                if (!hasRequestedBefore(permission)) {
                    true
                } else {
                    runtimePermissions.any { runtimePermission ->
                        context.findActivity()?.shouldShowRequestPermissionRationale(runtimePermission) == true
                    }
                }
            }
            else -> super<RuntimePermissionDenialStore>.shouldRequestRuntimePermission(
                context,
                permission,
                runtimePermissions,
            )
        }

    override fun markDenied(permission: PermissionType) {
        when (permission.key) {
            PermissionType.Location.key -> saveLocationRuntimePermissionDenied(context)
            PermissionType.PostNotifications.key -> saveNotificationRuntimePermissionDenied(context)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
