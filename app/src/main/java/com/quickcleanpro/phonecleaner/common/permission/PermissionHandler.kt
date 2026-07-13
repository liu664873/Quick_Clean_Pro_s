package com.quickcleanpro.phonecleaner.common.permission

import com.quickcleanpro.phonecleaner.common.permission.PermissionType

import android.content.Context
import android.content.Intent

interface PermissionHandler {
    val permission: PermissionType

    fun isGranted(context: Context): Boolean

    fun runtimePermissions(context: Context): List<String>

    fun settingsIntents(context: Context): List<Intent>
}

interface RuntimePermissionDenialStore {
    fun hasDenied(permission: PermissionType): Boolean

    fun markDenied(permission: PermissionType)

    fun hasRequestedBefore(permission: PermissionType): Boolean = hasDenied(permission)

    fun markRequested(permission: PermissionType) = Unit

    fun shouldRequestRuntimePermission(
        context: Context,
        permission: PermissionType,
        runtimePermissions: Array<String>,
    ): Boolean = !hasDenied(permission)
}
