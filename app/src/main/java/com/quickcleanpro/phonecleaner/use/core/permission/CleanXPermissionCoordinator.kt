package com.quickcleanpro.phonecleaner.use.core.permission

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

sealed interface PermissionRequestTarget {
    val key: String

    data class Action(
        val action: CleanXProtectedAction,
    ) : PermissionRequestTarget {
        override val key: String = "action:${action.key}"
    }

    data class Item(
        val item: PermissionType,
    ) : PermissionRequestTarget {
        override val key: String = "item:${item.key}"
    }
}

interface CleanXPermissionCoordinator {
    fun isGranted(action: CleanXProtectedAction): Boolean

    fun isGranted(item: PermissionType): Boolean

    fun guard(
        action: CleanXProtectedAction,
        onGranted: () -> Unit,
    ): PermissionRequestResult {
        return guard(action, onGranted, onRejected = {})
    }

    fun guard(
        action: CleanXProtectedAction,
        onGranted: () -> Unit,
        onRejected: () -> Unit = {},
        onResult: (PermissionRequestResult) -> Unit = {},
    ): PermissionRequestResult

    fun guardDirect(
        action: CleanXProtectedAction,
        onGranted: () -> Unit,
    ): PermissionRequestResult {
        return guardDirect(action, onGranted, onRejected = {})
    }

    fun guardDirect(
        action: CleanXProtectedAction,
        onGranted: () -> Unit,
        onRejected: () -> Unit = {},
        onResult: (PermissionRequestResult) -> Unit = {},
    ): PermissionRequestResult

    fun request(
        item: PermissionType,
        onGranted: () -> Unit = {},
    ): PermissionRequestResult {
        return request(item, onGranted, onRejected = {})
    }

    fun request(
        item: PermissionType,
        onGranted: () -> Unit,
        onRejected: () -> Unit = {},
        onResult: (PermissionRequestResult) -> Unit = {},
    ): PermissionRequestResult

    fun openSettings(
        item: PermissionType,
        onGranted: () -> Unit = {},
        onRejected: () -> Unit = {},
        onResult: (PermissionRequestResult) -> Unit = {},
    ): PermissionRequestResult

    fun request(action: CleanXProtectedAction): PermissionRequestResult {
        return guard(action, onGranted = {})
    }
}
