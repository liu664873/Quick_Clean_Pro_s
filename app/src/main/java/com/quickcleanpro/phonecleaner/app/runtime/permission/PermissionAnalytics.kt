package com.quickcleanpro.phonecleaner.app.runtime.permission

import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.common.permission.PermissionTarget
import com.quickcleanpro.phonecleaner.common.permission.PermissionType

internal object PermissionAnalytics {
    fun trackDialogAccepted(target: PermissionTarget) {
        if (target.requiresStorageFiles()) AnalyticsTracker.trackFileManagerPopup(ifOk = true)
    }

    fun trackDismissed(target: PermissionTarget, dialogVisible: Boolean) {
        if (!target.requiresStorageFiles()) return
        if (dialogVisible) AnalyticsTracker.trackFileManagerPopup(ifOk = false)
        AnalyticsTracker.trackFilePermissionResult(accepted = false)
    }

    fun trackGranted(target: PermissionTarget) {
        if (target.requiresStorageFiles()) AnalyticsTracker.trackFilePermissionResult(accepted = true)
    }
}

private fun PermissionTarget.requiresStorageFiles(): Boolean =
    PermissionType.StorageFiles in requiredPermissions
