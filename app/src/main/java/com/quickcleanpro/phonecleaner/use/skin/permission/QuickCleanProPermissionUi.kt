package com.quickcleanpro.phonecleaner.use.skin.permission

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionPromptRequest
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionRequestTarget
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.AppLockOverlayPermissionDialog
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.AppLockUsageAccessPermissionDialog
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.CleanXPermissionCopy
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.CleanXPermissionRequiredDialog
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.InlinePermissionOverlay
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.NotificationEnableGuideDialog

object QuickCleanProPermissionUi {
    @Composable
    fun PermissionPrompt(
        request: PermissionPromptRequest,
        onSubmit: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        if (request.isNotificationCleanerEnableGuideRequest()) {
            NotificationEnableGuideDialog(
                onDismiss = onDismiss,
                onOpenSettings = onSubmit,
            )
            return
        }

        when (request.missingPermission?.key) {
            PermissionType.UsageAccess.key -> {
                AppLockUsageAccessPermissionDialog(
                    onManagePermission = onSubmit,
                    onDismissToHome = onDismiss,
                )
            }
            PermissionType.Overlay.key -> {
                AppLockOverlayPermissionDialog(
                    onAllowNow = onSubmit,
                    onCancel = onDismiss,
                )
            }
            else -> {
                BackHandler { onDismiss() }
                InlinePermissionOverlay(onDismiss = onDismiss) {
                    CleanXPermissionRequiredDialog(
                        copy = request.copyForQuickCleanPRO(),
                        onSubmit = onSubmit,
                        onCancel = onDismiss,
                    )
                }
            }
        }
    }
}

private fun PermissionPromptRequest.isNotificationCleanerEnableGuideRequest(): Boolean {
    val requestTarget = target as? PermissionRequestTarget.Action ?: return false
    return requestTarget.action == CleanXProtectedAction.NotificationCleanerEnable &&
        missingPermission?.key == PermissionType.NotificationListener.key
}

private fun PermissionPromptRequest.copyForQuickCleanPRO(): CleanXPermissionCopy =
    when (val requestTarget = target) {
        is PermissionRequestTarget.Action ->
            copyFor(requestTarget.action, missingPermission?.key)
        is PermissionRequestTarget.Item ->
            copyFor(requestTarget.item, missingPermission?.key)
    }

private fun copyFor(
    item: PermissionType,
    missingPermissionKey: String?,
): CleanXPermissionCopy {
    if (missingPermissionKey == PermissionType.Overlay.key) {
        return overlayCopy()
    }
    if (missingPermissionKey == PermissionType.PostNotifications.key) {
        return postNotificationsCopy()
    }
    val titleRes = R.string.permission_title_required
    val noPersonalRes = R.string.permission_hint_no_personal
    return when (item) {
        PermissionType.StorageFiles,
        PermissionType.MediaImages,
        PermissionType.MediaImagesWithLocation,
        PermissionType.MediaVideo,
        PermissionType.MediaAudio,
        -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_storage_files_desc,
            hint1Res = R.string.permission_hint_files_safe,
            hint2Res = noPersonalRes,
        )
        PermissionType.Location -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_location_desc,
            hint1Res = R.string.permission_hint_network_scan,
            hint2Res = noPersonalRes,
        )
        PermissionType.UsageAccess -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_usage_desc,
            hint1Res = R.string.permission_hint_usage_read,
            hint2Res = noPersonalRes,
        )
        PermissionType.NotificationListener -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_notification_desc,
            hint1Res = R.string.permission_hint_notifications,
            hint2Res = noPersonalRes,
        )
        PermissionType.Overlay -> overlayCopy()
        PermissionType.PostNotifications -> postNotificationsCopy()
    }
}

private fun copyFor(
    action: CleanXProtectedAction,
    missingPermissionKey: String?,
): CleanXPermissionCopy {
    if (missingPermissionKey == PermissionType.Overlay.key) {
        return overlayCopy()
    }
    if (missingPermissionKey == PermissionType.PostNotifications.key) {
        return postNotificationsCopy()
    }
    val titleRes = R.string.permission_title_required
    val noPersonalRes = R.string.permission_hint_no_personal
    return when (action) {
        CleanXProtectedAction.JunkStartScan,
        CleanXProtectedAction.JunkCleanSelected,
        -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_storage_desc,
            hint1Res = R.string.permission_hint_junk_deleted,
            hint2Res = noPersonalRes,
        )
        CleanXProtectedAction.VirusDeepScanStart -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_virus_storage_desc,
            hint1Res = R.string.permission_hint_threat_files,
            hint2Res = noPersonalRes,
        )
        CleanXProtectedAction.WhatsAppStartScan,
        CleanXProtectedAction.WhatsAppCleanSelected,
        -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_whatsapp_storage_desc,
            hint1Res = R.string.permission_hint_whatsapp_files,
            hint2Res = noPersonalRes,
        )
        CleanXProtectedAction.NetworkUsageLoadStats -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_network_usage_desc,
            hint1Res = R.string.permission_hint_usage_read,
            hint2Res = noPersonalRes,
        )
        CleanXProtectedAction.AppLockOpenProtectedArea,
        CleanXProtectedAction.AppLockEnableMonitoring,
        -> CleanXPermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_app_lock_usage_desc,
            hint1Res = R.string.permission_hint_usage_read,
            hint2Res = noPersonalRes,
        )
        else -> copyFor(itemForAction(action), missingPermissionKey)
    }
}

private fun itemForAction(action: CleanXProtectedAction): PermissionType =
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

private fun overlayCopy(): CleanXPermissionCopy =
    CleanXPermissionCopy(
        titleRes = R.string.permission_title_required,
        descriptionRes = R.string.permission_overlay_desc,
        hint1Res = R.string.permission_hint_overlay,
        hint2Res = R.string.permission_hint_no_personal,
    )

private fun postNotificationsCopy(): CleanXPermissionCopy =
    CleanXPermissionCopy(
        titleRes = R.string.permission_title_required,
        descriptionRes = R.string.permission_post_notifications_desc,
        hint1Res = R.string.permission_hint_app_notifications,
        hint2Res = R.string.permission_hint_no_personal,
    )
