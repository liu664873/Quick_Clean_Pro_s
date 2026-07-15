package com.quickcleanpro.phonecleaner.common.ui.permission

import com.quickcleanpro.phonecleaner.common.permission.PermissionType

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.permission.ProtectedAction
import com.quickcleanpro.phonecleaner.common.permission.PermissionPromptRequest
import com.quickcleanpro.phonecleaner.common.permission.PermissionTarget
import com.quickcleanpro.phonecleaner.common.ui.components.popups.PermissionCopy
import com.quickcleanpro.phonecleaner.common.ui.components.popups.PermissionRequiredDialog
import com.quickcleanpro.phonecleaner.common.ui.components.popups.InlinePermissionOverlay

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
                    PermissionRequiredDialog(
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
    val requestTarget = target as? PermissionTarget.Action ?: return false
    return requestTarget.action == ProtectedAction.NotificationCleanerEnable &&
        missingPermission?.key == PermissionType.NotificationListener.key
}

private fun PermissionPromptRequest.copyForQuickCleanPRO(): PermissionCopy =
    when (val requestTarget = target) {
        is PermissionTarget.Action ->
            copyFor(requestTarget.action, requireNotNull(missingPermission))
        is PermissionTarget.Permission ->
            copyFor(requestTarget.permission)
    }

private fun copyFor(item: PermissionType): PermissionCopy {
    val titleRes = R.string.permission_title_required
    val noPersonalRes = R.string.permission_hint_no_personal
    return when (item) {
        PermissionType.StorageFiles -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_storage_files_desc,
            hint1Res = R.string.permission_hint_files_safe,
            hint2Res = noPersonalRes,
        )
        PermissionType.Location -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_location_desc,
            hint1Res = R.string.permission_hint_network_scan,
            hint2Res = noPersonalRes,
        )
        PermissionType.UsageAccess -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_usage_desc,
            hint1Res = R.string.permission_hint_usage_read,
            hint2Res = noPersonalRes,
        )
        PermissionType.NotificationListener -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_notification_desc,
            hint1Res = R.string.permission_hint_notifications,
            hint2Res = noPersonalRes,
        )
        PermissionType.Overlay -> overlayCopy()
    }
}

private fun copyFor(
    action: ProtectedAction,
    missingPermission: PermissionType,
): PermissionCopy {
    if (missingPermission == PermissionType.Overlay) return overlayCopy()
    val titleRes = R.string.permission_title_required
    val noPersonalRes = R.string.permission_hint_no_personal
    return when (action) {
        ProtectedAction.JunkStartScan,
        ProtectedAction.JunkCleanSelected,
        -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_storage_desc,
            hint1Res = R.string.permission_hint_junk_deleted,
            hint2Res = noPersonalRes,
        )
        ProtectedAction.VirusDeepScanStart -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_virus_storage_desc,
            hint1Res = R.string.permission_hint_threat_files,
            hint2Res = noPersonalRes,
        )
        ProtectedAction.WhatsAppStartScan,
        ProtectedAction.WhatsAppCleanSelected,
        -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_whatsapp_storage_desc,
            hint1Res = R.string.permission_hint_whatsapp_files,
            hint2Res = noPersonalRes,
        )
        ProtectedAction.NetworkUsageLoadStats -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_network_usage_desc,
            hint1Res = R.string.permission_hint_usage_read,
            hint2Res = noPersonalRes,
        )
        ProtectedAction.AppLockOpenProtectedArea,
        ProtectedAction.AppLockEnableMonitoring,
        -> PermissionCopy(
            titleRes = titleRes,
            descriptionRes = R.string.permission_app_lock_usage_desc,
            hint1Res = R.string.permission_hint_usage_read,
            hint2Res = noPersonalRes,
        )
        else -> copyFor(missingPermission)
    }
}

private fun overlayCopy(): PermissionCopy =
    PermissionCopy(
        titleRes = R.string.permission_title_required,
        descriptionRes = R.string.permission_overlay_desc,
        hint1Res = R.string.permission_hint_overlay,
        hint2Res = R.string.permission_hint_no_personal,
    )

