package com.quickcleanpro.phonecleaner.common.permission.runtime

import com.quickcleanpro.phonecleaner.common.permission.PermissionType

import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.common.permission.CleanXPermissionRegistry
import com.quickcleanpro.phonecleaner.common.permission.PermissionRequestTarget

internal object PermissionAnalytics {
    fun trackDialogAccepted(target: PermissionRequestTarget) {
        if (target.isStorageFilesTarget()) {
            AnalyticsTracker.trackFileManagerPopup(ifOk = true)
        }
    }

    fun trackDismissed(target: PermissionRequestTarget, dialogVisible: Boolean) {
        if (target.isStorageFilesTarget()) {
            if (dialogVisible) {
                AnalyticsTracker.trackFileManagerPopup(ifOk = false)
            }
            AnalyticsTracker.trackFilePermissionResult(accepted = false)
        }
    }

    fun trackGranted(target: PermissionRequestTarget) {
        if (target.isStorageFilesTarget()) {
            AnalyticsTracker.trackFilePermissionResult(accepted = true)
        }
    }
}

private fun PermissionRequestTarget.isStorageFilesTarget(): Boolean =
    when (this) {
        is PermissionRequestTarget.Action ->
            CleanXPermissionRegistry.itemForAction(action) == PermissionType.StorageFiles
        is PermissionRequestTarget.Item -> item == PermissionType.StorageFiles
    }
