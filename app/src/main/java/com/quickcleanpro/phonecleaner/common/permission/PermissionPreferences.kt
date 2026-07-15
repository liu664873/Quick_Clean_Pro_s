package com.quickcleanpro.phonecleaner.common.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.quickcleanpro.phonecleaner.common.storage.preferences.AppPreferences

class PermissionPreferences(
    appPreferences: AppPreferences,
) {
    private val store = appPreferences.store

    fun hasDeniedLocationRuntimePermission(): Boolean =
        store.getBoolean(KEY_LOCATION_RUNTIME_PERMISSION_DENIED, false)

    fun saveLocationRuntimePermissionDenied() {
        store.edit().putBoolean(KEY_LOCATION_RUNTIME_PERMISSION_DENIED, true).apply()
    }

    fun hasRequestedLocationRuntimePermissionBefore(): Boolean =
        store.getBoolean(
            KEY_LOCATION_RUNTIME_PERMISSION_REQUESTED_BEFORE,
            hasDeniedLocationRuntimePermission(),
        )

    fun saveLocationRuntimePermissionRequestedBefore() {
        store.edit().putBoolean(KEY_LOCATION_RUNTIME_PERMISSION_REQUESTED_BEFORE, true).apply()
    }

    fun hasRequestedNotificationRuntimePermissionBefore(): Boolean =
        store.getBoolean(
            KEY_NOTIFICATION_RUNTIME_PERMISSION_REQUESTED_BEFORE,
            store.getInt(KEY_NOTIFICATION_RUNTIME_PERMISSION_REQUEST_COUNT, 0) > 0,
        )

    fun saveNotificationRuntimePermissionRequestedBefore() {
        store.edit().putBoolean(KEY_NOTIFICATION_RUNTIME_PERMISSION_REQUESTED_BEFORE, true).apply()
    }

    fun readLastNotificationPermissionCustomPromptAt(): Long =
        store.getLong(KEY_LAST_NOTIFICATION_PERMISSION_CUSTOM_PROMPT_AT, 0L)

    fun saveLastNotificationPermissionCustomPromptAt(timestampMillis: Long) {
        store.edit().putLong(KEY_LAST_NOTIFICATION_PERMISSION_CUSTOM_PROMPT_AT, timestampMillis).apply()
    }
}

class AppRuntimePermissionDenialStore(
    private val preferences: PermissionPreferences,
) : RuntimePermissionDenialStore {
    override fun hasDenied(permission: PermissionType): Boolean =
        permission == PermissionType.Location && preferences.hasDeniedLocationRuntimePermission()

    override fun hasRequestedBefore(permission: PermissionType): Boolean =
        permission == PermissionType.Location && preferences.hasRequestedLocationRuntimePermissionBefore()

    override fun markRequested(permission: PermissionType) {
        if (permission == PermissionType.Location) {
            preferences.saveLocationRuntimePermissionRequestedBefore()
        }
    }

    override fun shouldRequestRuntimePermission(
        context: Context,
        permission: PermissionType,
        runtimePermissions: Array<String>,
    ): Boolean {
        if (permission != PermissionType.Location) return super.shouldRequestRuntimePermission(
            context,
            permission,
            runtimePermissions,
        )
        if (!hasRequestedBefore(permission)) return true
        return runtimePermissions.any { context.findActivity()?.shouldShowRequestPermissionRationale(it) == true }
    }

    override fun markDenied(permission: PermissionType) {
        if (permission == PermissionType.Location) {
            preferences.saveLocationRuntimePermissionDenied()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private const val KEY_LOCATION_RUNTIME_PERMISSION_DENIED = "location_runtime_permission_denied"
private const val KEY_LOCATION_RUNTIME_PERMISSION_REQUESTED_BEFORE = "location_runtime_permission_requested_before"
private const val KEY_NOTIFICATION_RUNTIME_PERMISSION_REQUESTED_BEFORE = "notification_runtime_permission_requested_before"
private const val KEY_NOTIFICATION_RUNTIME_PERMISSION_REQUEST_COUNT = "notification_runtime_permission_request_count"
private const val KEY_LAST_NOTIFICATION_PERMISSION_CUSTOM_PROMPT_AT = "last_notification_permission_custom_prompt_at"
